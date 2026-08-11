package com.qring.print;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.json.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

final class HistoryStore {
    static final class Item{
        String id,type,summary,path;long time;
        Item(String i,String t,String s,String p,long tm){id=i;type=t;summary=s;path=p;time=tm;}
    }
    private static final byte[] RASTER_MAGIC_V1=new byte[]{'Q','R','P','1'};
    private static final byte[] RASTER_MAGIC_V2=new byte[]{'Q','R','P','2'};
    private HistoryStore(){}

    static void add(Context c,String type,String summary){addInternal(c,type,summary,null,null);}
    static void add(Context c,String type,String summary,Bitmap bitmap){addInternal(c,type,summary,bitmap,null);}
    /** 保存最终 1-bit 光栅，重打时不再进行二次抖动，文件也远小于整张 ARGB PNG。 */
    static void addRaster(Context c,String type,String summary,RasterEncoder.Raster raster){addInternal(c,type,summary,null,raster);}

    private static synchronized void addInternal(Context c,String type,String summary,Bitmap bitmap,RasterEncoder.Raster raster){
        try{
            String id=Long.toString(System.currentTimeMillis());String path="";
            if(raster!=null){
                File dir=new File(c.getFilesDir(),"history");if(!dir.exists())dir.mkdirs();File f=new File(dir,id+".qrp");
                try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)))){
                    out.write(RASTER_MAGIC_V2);out.writeInt(raster.height);out.writeBoolean(raster.preserveMargins);out.write(raster.data);
                }
                path=f.getAbsolutePath();
            }else if(bitmap!=null){
                File dir=new File(c.getFilesDir(),"history");if(!dir.exists())dir.mkdirs();File f=new File(dir,id+".png");
                try(FileOutputStream out=new FileOutputStream(f)){bitmap.compress(Bitmap.CompressFormat.PNG,90,out);}path=f.getAbsolutePath();
            }
            JSONArray a=readJson(c);JSONObject o=new JSONObject();o.put("id",id);o.put("time",System.currentTimeMillis());o.put("type",type);o.put("summary",summary);o.put("path",path);
            JSONArray n=new JSONArray();n.put(o);for(int i=0;i<Math.min(a.length(),99);i++)n.put(a.get(i));
            c.getSharedPreferences("history",0).edit().putString("items",n.toString()).apply();cleanup(c,n);
        }catch(Throwable ignored){}
    }

    static synchronized List<Item> list(Context c){ArrayList<Item> r=new ArrayList<>();JSONArray a=readJson(c);for(int i=0;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);r.add(new Item(o.optString("id"),o.optString("type"),o.optString("summary"),o.optString("path"),o.optLong("time")));}catch(Throwable ignored){}return r;}

    static RasterEncoder.Raster loadRaster(Item i){
        if(i==null||i.path==null||!i.path.endsWith(".qrp"))return null;
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(i.path)))){
            byte[] magic=new byte[4];in.readFully(magic);boolean v2=java.util.Arrays.equals(magic,RASTER_MAGIC_V2);if(!v2&&!java.util.Arrays.equals(magic,RASTER_MAGIC_V1))return null;
            int h=in.readInt();if(h<=0||h>8000)return null;boolean keep=v2&&in.readBoolean();int len=h*QringProtocol.WIDTH_BYTES;byte[] data=new byte[len];in.readFully(data);return new RasterEncoder.Raster(h,data,keep);
        }catch(Throwable e){return null;}
    }

    static Bitmap loadPreview(Item i){
        if(i==null||i.path==null||i.path.isEmpty())return null;
        RasterEncoder.Raster r=loadRaster(i);if(r!=null)return RasterEncoder.toBitmap(r,1200);
        try{BitmapFactory.Options probe=new BitmapFactory.Options();probe.inJustDecodeBounds=true;BitmapFactory.decodeFile(i.path,probe);int sample=1;while(Math.max(probe.outWidth/sample,probe.outHeight/sample)>1200)sample*=2;BitmapFactory.Options opt=new BitmapFactory.Options();opt.inSampleSize=sample;opt.inPreferredConfig=Bitmap.Config.RGB_565;return BitmapFactory.decodeFile(i.path,opt);}catch(Throwable e){return null;}
    }
    static Bitmap load(Item i){RasterEncoder.Raster r=loadRaster(i);if(r!=null)return RasterEncoder.toBitmap(r,r.height);return i==null||i.path==null||i.path.isEmpty()?null:BitmapFactory.decodeFile(i.path);}

    static synchronized void rename(Context c,String id,String summary){JSONArray a=readJson(c);for(int i=0;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);if(id.equals(o.optString("id"))){o.put("summary",summary);break;}}catch(Throwable ignored){}c.getSharedPreferences("history",0).edit().putString("items",a.toString()).apply();}
    static synchronized void clear(Context c){for(Item i:list(c))if(i.path!=null&&!i.path.isEmpty())try{new File(i.path).delete();}catch(Throwable ignored){}c.getSharedPreferences("history",0).edit().remove("items").apply();}
    static String formatTime(long t){return new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date(t));}
    private static JSONArray readJson(Context c){try{return new JSONArray(c.getSharedPreferences("history",0).getString("items","[]"));}catch(Throwable e){return new JSONArray();}}
    private static void cleanup(Context c,JSONArray keep){HashSet<String> paths=new HashSet<>();for(int i=0;i<keep.length();i++)try{String p=keep.getJSONObject(i).optString("path");if(!p.isEmpty())paths.add(p);}catch(Throwable ignored){}File dir=new File(c.getFilesDir(),"history");File[] fs=dir.listFiles();if(fs!=null)for(File f:fs)if(!paths.contains(f.getAbsolutePath()))try{f.delete();}catch(Throwable ignored){}}
}
