package com.qring.print;

import android.content.Context;
import android.net.Uri;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/**
 * 轻量 OOXML 文本读取器：只提取 DOCX/PPTX 中的可见文字，不尝试伪装成 Office 高保真渲染器。
 * 全程 Java/Android framework，无 native ABI 依赖。
 */
final class OfficeTextReader {
    private static final int MAX_CHARS=300_000;
    private OfficeTextReader(){}

    static String read(Context c, Uri uri, String displayName) throws Exception {
        String n=displayName==null?"":displayName.toLowerCase(Locale.ROOT);
        if(n.endsWith(".docx"))return readDocx(c,uri);
        if(n.endsWith(".pptx"))return readPptx(c,uri);
        if(n.endsWith(".txt")||n.endsWith(".md")||n.endsWith(".log"))return readPlain(c,uri);
        // Some providers hide or alter extensions. Detect OOXML structure as a fallback.
        String byZip=probeOoxml(c,uri);
        if(byZip!=null)return byZip;
        return readPlain(c,uri);
    }

    private static String readPlain(Context c,Uri uri)throws Exception{
        try(InputStream in=c.getContentResolver().openInputStream(uri)){
            if(in==null)throw new IOException("无法打开文档");
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n,total=0;
            while((n=in.read(b))>0){total+=n;if(total>2_000_000)throw new IOException("文本文件过大（最多 2MB）");out.write(b,0,n);}
            return trim(new String(out.toByteArray(),StandardCharsets.UTF_8));
        }
    }

    private static String readDocx(Context c,Uri uri)throws Exception{
        try(InputStream raw=c.getContentResolver().openInputStream(uri);ZipInputStream z=new ZipInputStream(new BufferedInputStream(raw))){
            ZipEntry e;while((e=z.getNextEntry())!=null){
                if("word/document.xml".equals(e.getName()))return trim(parseXmlText(z,true));
                z.closeEntry();
            }
        }
        throw new IOException("DOCX 缺少 word/document.xml");
    }

    private static String readPptx(Context c,Uri uri)throws Exception{
        TreeMap<Integer,String> slides=new TreeMap<>();
        try(InputStream raw=c.getContentResolver().openInputStream(uri);ZipInputStream z=new ZipInputStream(new BufferedInputStream(raw))){
            ZipEntry e;while((e=z.getNextEntry())!=null){
                String name=e.getName();int no=slideNumber(name);
                if(no>0)slides.put(no,trim(parseXmlText(z,true)));
                z.closeEntry();
                if(slides.size()>500)throw new IOException("PPTX 页数过多（最多 500 页）");
            }
        }
        if(slides.isEmpty())throw new IOException("PPTX 中没有找到幻灯片文字");
        StringBuilder out=new StringBuilder();
        for(Map.Entry<Integer,String> s:slides.entrySet()){
            if(out.length()>0)out.append("\n\n");
            out.append("【第 ").append(s.getKey()).append(" 页】\n").append(s.getValue());
            if(out.length()>MAX_CHARS){out.setLength(MAX_CHARS);out.append("\n…文档过长，已截断");break;}
        }
        return trim(out.toString());
    }

    /** Returns extracted text for OOXML, or null if the ZIP is not a supported Office Open XML file. */
    private static String probeOoxml(Context c,Uri uri)throws Exception{
        boolean doc=false,ppt=false;
        try(InputStream raw=c.getContentResolver().openInputStream(uri);ZipInputStream z=new ZipInputStream(new BufferedInputStream(raw))){
            ZipEntry e;int seen=0;while((e=z.getNextEntry())!=null&&seen++<200){String n=e.getName();if("word/document.xml".equals(n)){doc=true;break;}if(slideNumber(n)>0){ppt=true;break;}z.closeEntry();}
        }catch(ZipException e){return null;}
        if(doc)return readDocx(c,uri);if(ppt)return readPptx(c,uri);return null;
    }

    private static String parseXmlText(InputStream in,boolean paragraphBreaks)throws Exception{
        XmlPullParser x=Xml.newPullParser();x.setInput(in,"UTF-8");StringBuilder out=new StringBuilder();int event=x.getEventType();boolean captureText=false;
        while(event!=XmlPullParser.END_DOCUMENT){
            if(event==XmlPullParser.START_TAG){String n=local(x.getName());captureText="t".equals(n);if("tab".equals(n))appendLimited(out,"\t");else if("br".equals(n))newline(out);}
            else if(event==XmlPullParser.TEXT&&captureText){String t=x.getText();if(t!=null&&!t.isEmpty())appendLimited(out,t);}
            else if(event==XmlPullParser.END_TAG){String n=local(x.getName());if("t".equals(n))captureText=false;else if(paragraphBreaks&&"p".equals(n))newline(out);else if(paragraphBreaks&&"tc".equals(n))appendLimited(out,"\t");}
            if(out.length()>=MAX_CHARS){out.append("\n…文档过长，已截断");break;}
            event=x.next();
        }
        return out.toString();
    }

    private static String local(String n){if(n==null)return "";int i=n.indexOf(':');return i>=0?n.substring(i+1):n;}
    private static void newline(StringBuilder b){int n=b.length();if(n>0&&b.charAt(n-1)!='\n')appendLimited(b,"\n");}
    private static void appendLimited(StringBuilder b,String s){if(s==null||s.isEmpty()||b.length()>=MAX_CHARS)return;int remain=MAX_CHARS-b.length();b.append(s,0,Math.min(remain,s.length()));}
    private static String trim(String s){return s==null?"":s.replace("\u0000","").replaceAll("[ \\t]+\\n","\n").replaceAll("\\n{4,}","\n\n\n").trim();}
    private static int slideNumber(String name){
        if(name==null||!name.startsWith("ppt/slides/slide")||!name.endsWith(".xml"))return -1;
        String x=name.substring("ppt/slides/slide".length(),name.length()-4);try{return Integer.parseInt(x);}catch(Exception e){return -1;}
    }
}
