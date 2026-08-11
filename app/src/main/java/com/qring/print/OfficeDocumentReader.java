package com.qring.print;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Lightweight, offline Office reader for thermal-print-friendly previews. */
final class OfficeDocumentReader {
    static final class Document {
        final String name;
        final ArrayList<String> sections=new ArrayList<>();
        Document(String n){name=n;}
    }
    private OfficeDocumentReader(){}

    static Document read(Context c,Uri uri)throws Exception{
        String name=displayName(c,uri);String lower=name.toLowerCase(Locale.ROOT);
        if(lower.endsWith(".docx"))return readDocx(c,uri,name);
        if(lower.endsWith(".pptx"))return readPptx(c,uri,name);
        if(lower.endsWith(".xlsx")||lower.endsWith(".csv"))return readTable(c,uri,name);
        if(lower.endsWith(".txt")||lower.endsWith(".md")||lower.endsWith(".log"))return readText(c,uri,name);
        String type=c.getContentResolver().getType(uri);
        if(type!=null&&type.contains("wordprocessingml"))return readDocx(c,uri,name);
        if(type!=null&&type.contains("presentationml"))return readPptx(c,uri,name);
        if(type!=null&&(type.contains("spreadsheetml")||type.contains("csv")))return readTable(c,uri,name);
        return readText(c,uri,name);
    }

    private static Document readDocx(Context c,Uri u,String name)throws Exception{
        byte[] xml=zipPart(c,u,"word/document.xml",16_000_000);if(xml==null)throw new IOException("DOCX 中找不到正文");
        StringBuilder all=new StringBuilder();XmlPullParser p=Xml.newPullParser();p.setInput(new ByteArrayInputStream(xml),"UTF-8");int ev=p.getEventType();
        while(ev!=XmlPullParser.END_DOCUMENT){String n=p.getName();
            if(ev==XmlPullParser.START_TAG&&"t".equals(n)){String t=p.nextText();if(t!=null)all.append(t);}
            else if(ev==XmlPullParser.START_TAG&&("tab".equals(n)||"br".equals(n)))all.append(' ');
            else if(ev==XmlPullParser.END_TAG&&"p".equals(n))all.append('\n');
            ev=p.next();if(all.length()>2_000_000)throw new IOException("Word 文档文字过多");}
        Document d=new Document(name);splitText(all.toString(),d.sections,2600);return ensure(d);
    }

    private static Document readPptx(Context c,Uri u,String name)throws Exception{
        TreeMap<Integer,byte[]> slides=new TreeMap<>();
        try(InputStream raw=c.getContentResolver().openInputStream(u);ZipInputStream z=new ZipInputStream(new BufferedInputStream(raw))){ZipEntry e;byte[] buf=new byte[8192];
            while((e=z.getNextEntry())!=null){String n=e.getName();if(n.matches("ppt/slides/slide\\d+\\.xml")){int idx=slideIndex(n);ByteArrayOutputStream b=new ByteArrayOutputStream();int x;while((x=z.read(buf))>0){b.write(buf,0,x);if(b.size()>4_000_000)throw new IOException("单页 PPT 内容过大");}slides.put(idx,b.toByteArray());}z.closeEntry();if(slides.size()>500)throw new IOException("PPT 页数过多");}}
        if(slides.isEmpty())throw new IOException("PPTX 中找不到幻灯片");Document d=new Document(name);
        for(Map.Entry<Integer,byte[]> en:slides.entrySet()){StringBuilder s=new StringBuilder("第 "+en.getKey()+" 页\n\n");XmlPullParser p=Xml.newPullParser();p.setInput(new ByteArrayInputStream(en.getValue()),"UTF-8");int ev=p.getEventType();while(ev!=XmlPullParser.END_DOCUMENT){if(ev==XmlPullParser.START_TAG&&"t".equals(p.getName())){String t=p.nextText();if(t!=null&&!t.trim().isEmpty())s.append(t).append('\n');}ev=p.next();}d.sections.add(s.toString());}
        return ensure(d);
    }

    private static Document readTable(Context c,Uri u,String name)throws Exception{
        DataTableReader.Table t=DataTableReader.read(c,u);Document d=new Document(name);StringBuilder s=new StringBuilder();int rowsPer=18;int row=0;
        if(!t.headers.isEmpty())s.append(join(t.headers)).append('\n').append(repeat('─',Math.min(36,Math.max(8,s.length()/2)))).append('\n');
        for(ArrayList<String> r:t.rows){s.append(join(r)).append('\n');row++;if(row%rowsPer==0){d.sections.add(s.toString());s=new StringBuilder();if(!t.headers.isEmpty())s.append(join(t.headers)).append('\n');}}
        if(s.length()>0)d.sections.add(s.toString());return ensure(d);
    }

    private static Document readText(Context c,Uri u,String name)throws Exception{
        StringBuilder s=new StringBuilder();try(Reader r=new InputStreamReader(c.getContentResolver().openInputStream(u),StandardCharsets.UTF_8)){char[] b=new char[8192];int n;while((n=r.read(b))>0){s.append(b,0,n);if(s.length()>2_000_000)throw new IOException("文档文字过多");}}
        Document d=new Document(name);splitText(s.toString(),d.sections,2600);return ensure(d);
    }

    private static void splitText(String text,List<String> out,int max){String src=text.replace("\r","").trim();if(src.isEmpty())return;int at=0;while(at<src.length()){int end=Math.min(src.length(),at+max);if(end<src.length()){int nl=src.lastIndexOf('\n',end);if(nl>at+max/2)end=nl+1;}out.add(src.substring(at,end).trim());at=end;}}
    private static String join(List<String> r){StringBuilder b=new StringBuilder();for(int i=0;i<r.size();i++){if(i>0)b.append("  |  ");String v=r.get(i)==null?"":r.get(i).trim();b.append(v);}return b.toString();}
    private static String repeat(char c,int n){char[] a=new char[Math.max(0,n)];Arrays.fill(a,c);return new String(a);}
    private static Document ensure(Document d)throws IOException{if(d.sections.isEmpty())throw new IOException("没有可打印内容");return d;}
    private static int slideIndex(String n){try{String x=n.substring(n.lastIndexOf("slide")+5,n.lastIndexOf('.'));return Integer.parseInt(x);}catch(Exception e){return 999999;}}
    private static byte[] zipPart(Context c,Uri u,String target,int max)throws Exception{try(InputStream raw=c.getContentResolver().openInputStream(u);ZipInputStream z=new ZipInputStream(new BufferedInputStream(raw))){ZipEntry e;byte[] buf=new byte[8192];while((e=z.getNextEntry())!=null){if(target.equals(e.getName())){ByteArrayOutputStream b=new ByteArrayOutputStream();int n;while((n=z.read(buf))>0){b.write(buf,0,n);if(b.size()>max)throw new IOException("文档内容过大");}return b.toByteArray();}z.closeEntry();}}return null;}
    private static String displayName(Context c,Uri u){String n=null;try(Cursor cur=c.getContentResolver().query(u,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cur!=null&&cur.moveToFirst())n=cur.getString(0);}catch(Throwable ignored){}return n==null?"Office 文档":n;}
}
