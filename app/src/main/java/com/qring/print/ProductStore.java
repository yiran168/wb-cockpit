package com.qring.print;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class ProductStore {
    private static final String FILE="product_database.json";
    private ProductStore(){}
    static void save(Context c,DataTableReader.Table t) throws Exception{
        JSONObject root=new JSONObject();JSONArray hs=new JSONArray();for(String h:t.headers)hs.put(h);root.put("headers",hs);
        JSONArray rs=new JSONArray();int limit=Math.min(5000,t.rows.size());for(int i=0;i<limit;i++){JSONArray r=new JSONArray();for(String v:t.rows.get(i))r.put(v);rs.put(r);}root.put("rows",rs);
        try(OutputStream o=c.openFileOutput(FILE,Context.MODE_PRIVATE)){o.write(root.toString().getBytes(StandardCharsets.UTF_8));}
    }
    static DataTableReader.Table load(Context c) throws Exception{
        StringBuilder b=new StringBuilder();try(Reader r=new InputStreamReader(c.openFileInput(FILE),StandardCharsets.UTF_8)){char[] x=new char[8192];int n;while((n=r.read(x))>0)b.append(x,0,n);}
        JSONObject root=new JSONObject(b.toString());DataTableReader.Table t=new DataTableReader.Table();JSONArray hs=root.getJSONArray("headers");for(int i=0;i<hs.length();i++)t.headers.add(hs.optString(i,"字段"+(i+1)));JSONArray rs=root.getJSONArray("rows");for(int i=0;i<rs.length();i++){JSONArray a=rs.getJSONArray(i);ArrayList<String> row=new ArrayList<>();for(int j=0;j<a.length();j++)row.add(a.optString(j,""));while(row.size()<t.headers.size())row.add("");t.rows.add(row);}return t;
    }
    static boolean exists(Context c){return new File(c.getFilesDir(),FILE).isFile();}
}
