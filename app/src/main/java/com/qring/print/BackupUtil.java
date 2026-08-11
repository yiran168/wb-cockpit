package com.qring.print;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

final class BackupUtil {
    private static final int VERSION=3;
    private BackupUtil(){}

    static void exportBackup(Context c, Uri uri) throws Exception {
        try(OutputStream raw=c.getContentResolver().openOutputStream(uri);
            ZipOutputStream z=new ZipOutputStream(new BufferedOutputStream(raw))){
            JSONObject meta=new JSONObject();
            meta.put("format","QrintPrint-Android-backup");
            meta.put("version",VERSION);
            meta.put("createdAt",System.currentTimeMillis());
            putText(z,"meta.json",meta.toString());

            SharedPreferences sp=c.getSharedPreferences("settings",0);
            JSONObject settings=new JSONObject();
            settings.put("density",sp.getInt("density",1));
            settings.put("threshold",sp.getInt("threshold",190));
            settings.put("copies",sp.getInt("copies",1));
            settings.put("paper_unit",sp.getString("paper_unit","mm"));
            settings.put("paper_height_mm",sp.getInt("paper_height_mm",0));
            settings.put("media_mode",sp.getString("media_mode","continuous"));
            settings.put("paper_width_tenths_mm",sp.getInt("paper_width_tenths_mm",570));
            settings.put("label_length_tenths_mm",sp.getInt("label_length_tenths_mm",300));
            settings.put("content_width_mode",sp.getInt("content_width_mode",0));
            settings.put("content_width_tenths_mm",sp.getInt("content_width_tenths_mm",480));
            settings.put("paper_alignment",sp.getInt("paper_alignment",0));
            settings.put("content_alignment",sp.getInt("content_alignment",0));
            settings.put("content_vertical_alignment",sp.getInt("content_vertical_alignment",0));
            settings.put("trim_side_blank",sp.getBoolean("trim_side_blank",true));
            settings.put("auto_trim_length",sp.getBoolean("auto_trim_length",true));
            settings.put("content_scale_percent",sp.getInt("content_scale_percent",100));
            settings.put("feed_before",sp.getInt("feed_before",10));
            settings.put("feed_after",sp.getInt("feed_after",100));
            settings.put("shutdown_seconds",sp.getInt("shutdown_seconds",0));
            settings.put("x_offset",sp.getInt("x_offset",0));
            settings.put("y_offset",sp.getInt("y_offset",0));
            settings.put("print_direction",sp.getInt("print_direction",0));
            settings.put("appearance",sp.getInt("appearance",0));
            settings.put("reduce_motion",sp.getBoolean("reduce_motion",false));
            settings.put("haptic",sp.getBoolean("haptic",true));
            settings.put(PrintSound.KEY_ENABLED,sp.getBoolean(PrintSound.KEY_ENABLED,true));
            settings.put(PrintSound.KEY_MODE,sp.getInt(PrintSound.KEY_MODE,0));
            settings.put(PrintSound.KEY_VOLUME,sp.getInt(PrintSound.KEY_VOLUME,62));
            putText(z,"settings.json",settings.toString());

            JSONArray templates=new JSONArray();
            for(TemplateStore.Item item:TemplateStore.list(c)){
                JSONObject o=new JSONObject();
                o.put("id",item.id);o.put("name",item.name);o.put("time",item.time);
                String entry="templates/"+safe(item.id)+".png";o.put("entry",entry);templates.put(o);
                File f=new File(item.path);if(f.isFile())putFile(z,entry,f);
            }
            putText(z,"templates.json",templates.toString());

            JSONArray history=new JSONArray();
            for(HistoryStore.Item item:HistoryStore.list(c)){
                JSONObject o=new JSONObject();
                o.put("id",item.id);o.put("type",item.type);o.put("summary",item.summary);o.put("time",item.time);
                if(item.path!=null&&!item.path.isEmpty()){
                    File f=new File(item.path);
                    if(f.isFile()){
                        String ext=item.path.toLowerCase(Locale.ROOT).endsWith(".qrp")?".qrp":".png";
                        String entry="history/"+safe(item.id)+ext;
                        o.put("entry",entry);o.put("format",ext.substring(1));putFile(z,entry,f);
                    }
                }
                history.put(o);
            }
            putText(z,"history.json",history.toString());

            File product=new File(c.getFilesDir(),"product_database.json");
            if(product.isFile())putFile(z,"product_database.json",product);
        }
    }

    static String importBackup(Context c, Uri uri) throws Exception {
        HashMap<String,byte[]> entries=new HashMap<>();
        try(InputStream raw=c.getContentResolver().openInputStream(uri);
            ZipInputStream z=new ZipInputStream(new BufferedInputStream(raw))){
            ZipEntry e; byte[] buf=new byte[8192]; long total=0;
            while((e=z.getNextEntry())!=null){
                String name=e.getName();
                if(e.isDirectory()||!safeEntry(name)){z.closeEntry();continue;}
                ByteArrayOutputStream b=new ByteArrayOutputStream();int n;
                while((n=z.read(buf))>0){b.write(buf,0,n);total+=n;if(total>80_000_000L)throw new IOException("备份包过大");}
                entries.put(name,b.toByteArray());z.closeEntry();
            }
        }
        JSONObject meta=new JSONObject(text(entries.get("meta.json")));
        if(!"QrintPrint-Android-backup".equals(meta.optString("format")))throw new IOException("不是 QrintPrint 备份文件");
        if(meta.optInt("version",0)>VERSION)throw new IOException("备份版本过新，请升级应用");

        byte[] setBytes=entries.get("settings.json");
        if(setBytes!=null){
            JSONObject s=new JSONObject(text(setBytes));
            c.getSharedPreferences("settings",0).edit()
                    .putInt("density",clamp(s.optInt("density",1),0,5))
                    .putInt("threshold",clamp(s.optInt("threshold",190),0,255))
                    .putInt("copies",clamp(s.optInt("copies",1),1,20))
                    .putString("paper_unit","inch".equals(s.optString("paper_unit"))?"inch":"mm")
                    .putInt("paper_height_mm",clamp(s.optInt("paper_height_mm",0),0,1000))
                    .putString("media_mode","label".equals(s.optString("media_mode"))?"label":"continuous")
                    .putInt("paper_width_tenths_mm",clamp(s.optInt("paper_width_tenths_mm",570),100,570))
                    .putInt("label_length_tenths_mm",clamp(s.optInt("label_length_tenths_mm",300),10,10000))
                    .putInt("content_width_mode",clamp(s.optInt("content_width_mode",0),0,2))
                    .putInt("content_width_tenths_mm",clamp(s.optInt("content_width_tenths_mm",480),10,570))
                    .putInt("paper_alignment",clamp(s.optInt("paper_alignment",0),0,2))
                    .putInt("content_alignment",clamp(s.optInt("content_alignment",0),0,2))
                    .putInt("content_vertical_alignment",clamp(s.optInt("content_vertical_alignment",0),0,2))
                    .putBoolean("trim_side_blank",s.optBoolean("trim_side_blank",true))
                    .putBoolean("auto_trim_length",s.optBoolean("auto_trim_length",true))
                    .putInt("content_scale_percent",clamp(s.optInt("content_scale_percent",100),25,200))
                    .putInt("feed_before",clamp(s.optInt("feed_before",10),0,2000))
                    .putInt("feed_after",clamp(s.optInt("feed_after",100),0,4000))
                    .putInt("shutdown_seconds",clamp(s.optInt("shutdown_seconds",0),0,65535))
                    .putInt("x_offset",clamp(s.optInt("x_offset",0),-96,96))
                    .putInt("y_offset",clamp(s.optInt("y_offset",0),-1000,1000))
                    .putInt("print_direction",s.optInt("print_direction",0)==180?180:0)
                    .putInt("appearance",clamp(s.optInt("appearance",0),0,2))
                    .putBoolean("reduce_motion",s.optBoolean("reduce_motion",false))
                    .putBoolean("haptic",s.optBoolean("haptic",true))
                    .putBoolean(PrintSound.KEY_ENABLED,s.optBoolean(PrintSound.KEY_ENABLED,true))
                    .putInt(PrintSound.KEY_MODE,clamp(s.optInt(PrintSound.KEY_MODE,0),0,PrintSound.RANDOM_GENERATED))
                    .putInt(PrintSound.KEY_VOLUME,clamp(s.optInt(PrintSound.KEY_VOLUME,62),0,100)).apply();
        }

        File tdir=new File(c.getFilesDir(),"templates");if(!tdir.exists()&&!tdir.mkdirs())throw new IOException("无法创建模板目录");
        JSONArray tj=entries.containsKey("templates.json")?new JSONArray(text(entries.get("templates.json"))):new JSONArray();
        JSONArray outT=new JSONArray();
        for(int i=0;i<tj.length();i++){
            JSONObject o=tj.getJSONObject(i);String id=safe(o.optString("id",Long.toString(System.currentTimeMillis()+i)));
            String entry=o.optString("entry");byte[] data=entries.get(entry);if(data==null)continue;
            File f=new File(tdir,id+".png");writeFile(f,data);
            JSONObject n=new JSONObject();n.put("id",id);n.put("name",o.optString("name","模板"));n.put("path",f.getAbsolutePath());n.put("time",o.optLong("time",System.currentTimeMillis()));outT.put(n);
        }
        c.getSharedPreferences("templates2",0).edit().putString("items",outT.toString()).apply();

        File hdir=new File(c.getFilesDir(),"history");if(!hdir.exists())hdir.mkdirs();
        JSONArray hj=entries.containsKey("history.json")?new JSONArray(text(entries.get("history.json"))):new JSONArray();
        JSONArray outH=new JSONArray();
        for(int i=0;i<hj.length()&&i<100;i++){
            JSONObject o=hj.getJSONObject(i);String id=safe(o.optString("id",Long.toString(System.currentTimeMillis()+i)));
            String path="";String entry=o.optString("entry");byte[] data=entries.get(entry);
            if(data!=null){
                // v2 keeps QRP raster extension. v1 backups may have stored QRP bytes under a .png name,
                // so inspect the magic header as a backward-compatible repair path.
                boolean qrp="qrp".equalsIgnoreCase(o.optString("format")) || looksLikeQrp(data);
                File f=new File(hdir,id+(qrp?".qrp":".png"));writeFile(f,data);path=f.getAbsolutePath();
            }
            JSONObject n=new JSONObject();n.put("id",id);n.put("type",o.optString("type"));n.put("summary",o.optString("summary"));n.put("path",path);n.put("time",o.optLong("time",System.currentTimeMillis()));outH.put(n);
        }
        c.getSharedPreferences("history",0).edit().putString("items",outH.toString()).apply();

        byte[] product=entries.get("product_database.json");
        if(product!=null)writeFile(new File(c.getFilesDir(),"product_database.json"),product);
        return "已恢复设置、"+outT.length()+" 个模板、"+outH.length()+" 条打印记录"+(product!=null?"和商品库":"");
    }

    private static void putText(ZipOutputStream z,String name,String text)throws Exception{
        z.putNextEntry(new ZipEntry(name));z.write(text.getBytes(StandardCharsets.UTF_8));z.closeEntry();
    }
    private static void putFile(ZipOutputStream z,String name,File f)throws Exception{
        z.putNextEntry(new ZipEntry(name));try(InputStream in=new FileInputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)z.write(b,0,n);}z.closeEntry();
    }
    private static void writeFile(File f,byte[] data)throws Exception{try(OutputStream o=new FileOutputStream(f)){o.write(data);}}
    private static String text(byte[] b)throws IOException{if(b==null)throw new IOException("备份文件不完整");return new String(b,StandardCharsets.UTF_8);}
    private static boolean safeEntry(String n){return n!=null&&!n.contains("..")&&!n.startsWith("/")&&!n.startsWith("\\");}
    private static boolean looksLikeQrp(byte[] data){return data!=null&&data.length>=8&&data[0]=='Q'&&data[1]=='R'&&data[2]=='P'&&(data[3]=='1'||data[3]=='2');}
    private static String safe(String s){String x=s==null?"":s.replaceAll("[^A-Za-z0-9_-]","");return x.isEmpty()?Long.toString(System.currentTimeMillis()):x;}
    private static int clamp(int v,int a,int b){return Math.max(a,Math.min(b,v));}
}
