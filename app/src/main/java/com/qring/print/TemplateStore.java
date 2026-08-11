package com.qring.print;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.json.*;
import java.io.*;
import java.util.*;

final class TemplateStore {
    static final class Item {
        String id,name,path; long time;
        Item(String i,String n,String p,long t){id=i;name=n;path=p;time=t;}
    }
    private TemplateStore(){}

    static synchronized Item save(Context c,String name,Bitmap bmp) throws Exception{
        String id=Long.toString(System.currentTimeMillis());
        File dir=new File(c.getFilesDir(),"templates"); if(!dir.exists()&&!dir.mkdirs())throw new IOException("无法创建模板目录");
        File f=new File(dir,id+".png");
        try(FileOutputStream out=new FileOutputStream(f)){if(!bmp.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("PNG 保存失败");}
        JSONArray old=readJson(c), next=new JSONArray();
        JSONObject o=new JSONObject();o.put("id",id);o.put("name",name);o.put("path",f.getAbsolutePath());o.put("time",System.currentTimeMillis());next.put(o);
        for(int i=0;i<old.length();i++)next.put(old.get(i));
        c.getSharedPreferences("templates2",0).edit().putString("items",next.toString()).apply();
        return new Item(id,name,f.getAbsolutePath(),System.currentTimeMillis());
    }
    static synchronized List<Item> list(Context c){
        ArrayList<Item> r=new ArrayList<>();JSONArray a=readJson(c);
        for(int i=0;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);String p=o.optString("path");if(new File(p).exists())r.add(new Item(o.optString("id"),o.optString("name","模板"),p,o.optLong("time")));}catch(Throwable ignored){}
        return r;
    }
    static Bitmap loadPreview(Item i){
        if(i==null||i.path==null||i.path.isEmpty())return null;
        try{BitmapFactory.Options probe=new BitmapFactory.Options();probe.inJustDecodeBounds=true;BitmapFactory.decodeFile(i.path,probe);int sample=1;while(Math.max(probe.outWidth/sample,probe.outHeight/sample)>1200)sample*=2;BitmapFactory.Options opt=new BitmapFactory.Options();opt.inSampleSize=sample;opt.inPreferredConfig=Bitmap.Config.RGB_565;return BitmapFactory.decodeFile(i.path,opt);}catch(Throwable e){return null;}
    }
    static Bitmap load(Item i){return BitmapFactory.decodeFile(i.path);}
    static synchronized Item duplicate(Context c,Item source,String newName) throws Exception{
        if(source==null)throw new IOException("模板不存在");
        Bitmap b=load(source);if(b==null)throw new IOException("模板图片损坏");
        try{return save(c,(newName==null||newName.trim().isEmpty())?source.name+" 副本":newName.trim(),b);}
        finally{if(!b.isRecycled())b.recycle();}
    }
    static synchronized void rename(Context c,String id,String name){
        JSONArray a=readJson(c),n=new JSONArray();
        for(int i=0;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);if(id.equals(o.optString("id")))o.put("name",name);n.put(o);}catch(Throwable ignored){}
        c.getSharedPreferences("templates2",0).edit().putString("items",n.toString()).apply();
    }
    static synchronized void delete(Context c,String id){
        JSONArray a=readJson(c),n=new JSONArray();
        for(int i=0;i<a.length();i++)try{JSONObject o=a.getJSONObject(i);if(id.equals(o.optString("id"))){try{new File(o.optString("path")).delete();}catch(Throwable ignored){}}else n.put(o);}catch(Throwable ignored){}
        c.getSharedPreferences("templates2",0).edit().putString("items",n.toString()).apply();
    }
    private static JSONArray readJson(Context c){try{return new JSONArray(c.getSharedPreferences("templates2",0).getString("items","[]"));}catch(Throwable e){return new JSONArray();}}
}
