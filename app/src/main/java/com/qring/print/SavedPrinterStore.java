package com.qring.print;

import android.content.Context;
import org.json.*;
import java.util.*;

final class SavedPrinterStore {
    static final class Item {String address,name,alias;long time;Item(String a,String n,String al,long t){address=a;name=n;alias=al;time=t;}}
    private SavedPrinterStore(){}
    static synchronized void touch(Context c,String address,String name){
        if(address==null||address.isEmpty())return;
        JSONArray a=raw(c),out=new JSONArray();JSONObject first=new JSONObject();
        try{first.put("address",address);first.put("name",name==null?"":name);first.put("alias",aliasFor(a,address));first.put("time",System.currentTimeMillis());out.put(first);
            for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&!address.equalsIgnoreCase(o.optString("address")))out.put(o);}
            c.getSharedPreferences("saved_printers",0).edit().putString("items",out.toString()).apply();
        }catch(Throwable ignored){}
    }
    static synchronized List<Item> list(Context c){ArrayList<Item> out=new ArrayList<>();JSONArray a=raw(c);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)out.add(new Item(o.optString("address"),o.optString("name"),o.optString("alias"),o.optLong("time")));}return out;}
    static synchronized void rename(Context c,String address,String alias){JSONArray a=raw(c);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&address.equalsIgnoreCase(o.optString("address")))try{o.put("alias",alias==null?"":alias.trim());}catch(Throwable ignored){}}c.getSharedPreferences("saved_printers",0).edit().putString("items",a.toString()).apply();}
    static synchronized void remove(Context c,String address){JSONArray a=raw(c),out=new JSONArray();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&!address.equalsIgnoreCase(o.optString("address")))out.put(o);}c.getSharedPreferences("saved_printers",0).edit().putString("items",out.toString()).apply();}
    static String display(Item i){return i.alias!=null&&!i.alias.trim().isEmpty()?i.alias:(i.name==null||i.name.isEmpty()?"打印机":i.name);}
    private static String aliasFor(JSONArray a,String address){for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&address.equalsIgnoreCase(o.optString("address")))return o.optString("alias");}return "";}
    private static JSONArray raw(Context c){try{return new JSONArray(c.getSharedPreferences("saved_printers",0).getString("items","[]"));}catch(Throwable e){return new JSONArray();}}
}
