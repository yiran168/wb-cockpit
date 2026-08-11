package com.qring.print;

import android.content.Context;
import android.net.Uri;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

final class DataTableReader {
    static final class Table {
        final ArrayList<String> headers=new ArrayList<>();
        final ArrayList<ArrayList<String>> rows=new ArrayList<>();
        String value(int row,int col){return row>=0&&row<rows.size()&&col>=0&&col<rows.get(row).size()?rows.get(row).get(col):"";}
    }
    private DataTableReader(){}

    static Table read(Context c, Uri uri) throws Exception{
        String name=uri.toString().toLowerCase(Locale.ROOT);
        String type=c.getContentResolver().getType(uri);
        if(name.endsWith(".xlsx") || (type!=null&&type.contains("spreadsheetml"))) return readXlsx(c,uri);
        return readCsv(c,uri);
    }

    private static Table readCsv(Context c,Uri uri)throws Exception{
        StringBuilder all=new StringBuilder();
        try(Reader r=new InputStreamReader(c.getContentResolver().openInputStream(uri),StandardCharsets.UTF_8)){
            char[] buf=new char[8192];int n;while((n=r.read(buf))>0){all.append(buf,0,n);if(all.length()>8_000_000)throw new IOException("CSV 文件过大");}
        }
        ArrayList<ArrayList<String>> parsed=parseCsv(all.toString());
        Table t=new Table();if(parsed.isEmpty())return t;
        normalizeHeaders(t.headers,parsed.remove(0));t.rows.addAll(parsed);normalizeRows(t);return t;
    }

    private static ArrayList<ArrayList<String>> parseCsv(String s){
        ArrayList<ArrayList<String>> rows=new ArrayList<>();ArrayList<String> row=new ArrayList<>();StringBuilder cell=new StringBuilder();boolean quote=false;
        for(int i=0;i<s.length();i++){
            char ch=s.charAt(i);
            if(quote){if(ch=='"'){if(i+1<s.length()&&s.charAt(i+1)=='"'){cell.append('"');i++;}else quote=false;}else cell.append(ch);}
            else if(ch=='"')quote=true;
            else if(ch==','){row.add(cell.toString());cell.setLength(0);}
            else if(ch=='\n'){row.add(cell.toString());cell.setLength(0);if(!allBlank(row))rows.add(row);row=new ArrayList<>();}
            else if(ch!='\r')cell.append(ch);
        }
        row.add(cell.toString());if(!allBlank(row))rows.add(row);return rows;
    }

    private static boolean allBlank(List<String> r){for(String s:r)if(s!=null&&!s.trim().isEmpty())return false;return true;}

    private static Table readXlsx(Context c,Uri uri)throws Exception{
        HashMap<String,byte[]> parts=new HashMap<>();
        try(InputStream raw=c.getContentResolver().openInputStream(uri);ZipInputStream z=new ZipInputStream(new BufferedInputStream(raw))){
            ZipEntry e;byte[] buf=new byte[8192];
            while((e=z.getNextEntry())!=null){String n=e.getName();if(n.equals("xl/sharedStrings.xml")||n.equals("xl/worksheets/sheet1.xml")){ByteArrayOutputStream b=new ByteArrayOutputStream();int x;while((x=z.read(buf))>0){b.write(buf,0,x);if(b.size()>16_000_000)throw new IOException("XLSX 工作表过大");}parts.put(n,b.toByteArray());}z.closeEntry();}
        }
        byte[] sheet=parts.get("xl/worksheets/sheet1.xml");if(sheet==null)throw new IOException("找不到第一个工作表");
        ArrayList<String> shared=parseShared(parts.get("xl/sharedStrings.xml"));
        ArrayList<ArrayList<String>> rows=parseSheet(sheet,shared);Table t=new Table();if(rows.isEmpty())return t;
        normalizeHeaders(t.headers,rows.remove(0));t.rows.addAll(rows);normalizeRows(t);return t;
    }

    private static ArrayList<String> parseShared(byte[] xml)throws Exception{
        ArrayList<String> out=new ArrayList<>();if(xml==null)return out;XmlPullParser p=Xml.newPullParser();p.setInput(new ByteArrayInputStream(xml),"UTF-8");
        StringBuilder current=null;int event=p.getEventType();while(event!=XmlPullParser.END_DOCUMENT){
            if(event==XmlPullParser.START_TAG&&"si".equals(p.getName()))current=new StringBuilder();
            else if(event==XmlPullParser.START_TAG&&"t".equals(p.getName())&&current!=null){String txt=p.nextText();if(txt!=null)current.append(txt);}
            else if(event==XmlPullParser.END_TAG&&"si".equals(p.getName())&&current!=null){out.add(current.toString());current=null;}
            event=p.next();}
        return out;
    }

    private static ArrayList<ArrayList<String>> parseSheet(byte[] xml,ArrayList<String> shared)throws Exception{
        ArrayList<ArrayList<String>> out=new ArrayList<>();XmlPullParser p=Xml.newPullParser();p.setInput(new ByteArrayInputStream(xml),"UTF-8");
        ArrayList<String> row=null;int col=-1;String type=null,value=null;int event=p.getEventType();
        while(event!=XmlPullParser.END_DOCUMENT){String n=p.getName();
            if(event==XmlPullParser.START_TAG&&"row".equals(n))row=new ArrayList<>();
            else if(event==XmlPullParser.START_TAG&&"c".equals(n)){String ref=p.getAttributeValue(null,"r");col=ref==null?-1:columnIndex(ref);type=p.getAttributeValue(null,"t");value="";}
            else if(event==XmlPullParser.START_TAG&&("v".equals(n)||("t".equals(n)&&"inlineStr".equals(type))))value=p.nextText();
            else if(event==XmlPullParser.END_TAG&&"c".equals(n)&&row!=null){while(row.size()<=Math.max(0,col))row.add("");String v=value==null?"":value;if("s".equals(type)){try{int i=Integer.parseInt(v);v=(i>=0&&i<shared.size())?shared.get(i):v;}catch(Exception ignored){}}row.set(Math.max(0,col),v);}
            else if(event==XmlPullParser.END_TAG&&"row".equals(n)&&row!=null){if(!allBlank(row))out.add(row);row=null;}
            event=p.next();}
        return out;
    }

    private static int columnIndex(String ref){int v=0,i=0;while(i<ref.length()&&Character.isLetter(ref.charAt(i))){char c=Character.toUpperCase(ref.charAt(i));v=v*26+(c-'A'+1);i++;}return Math.max(0,v-1);}
    private static void normalizeHeaders(ArrayList<String> dst,List<String> src){HashSet<String> used=new HashSet<>();for(int i=0;i<src.size();i++){String h=src.get(i)==null?"":src.get(i).trim();if(h.isEmpty())h="字段"+(i+1);String base=h;int n=2;while(used.contains(h))h=base+"_"+(n++);used.add(h);dst.add(h);}}
    private static void normalizeRows(Table t){for(ArrayList<String> r:t.rows)while(r.size()<t.headers.size())r.add("");}
}
