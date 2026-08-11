package com.qring.print;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;
import java.util.*;

public class BatchPrintActivity extends Activity {
    private static final int PICK=801;private static final String PREF="batch_resume";
    private DataTableReader.Table table;private EditText template,from,to;private TextView info,progress;private ImageView livePreview;private Bitmap liveBitmap;private Uri currentUri;private volatile int completed;
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());Uri incoming=getIntent().getData();if(incoming==null){String x=getIntent().getStringExtra("incoming_uri");if(x!=null)incoming=Uri.parse(x);}if(incoming!=null){try{getContentResolver().takePersistableUriPermission(incoming,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Throwable ignored){}load(incoming,false);}}
    private View build(){
        ScrollView sc=new ScrollView(this);LinearLayout root=Ui.page(this,"Excel / CSV 批打");sc.addView(root);
        LinearLayout source=Ui.card(this);source.addView(Ui.hint(this,"首行作为字段名 · {字段名} 变量替换 · {ROW}/{SEQ10}/{SEQ2}/{SEQ16}/{SEQ26}/{SEQ26_0}/{SEQ36} · 支持断点续打"));Button pick=Ui.primaryButton(this,"选择 Excel / CSV");pick.setOnClickListener(v->pick());source.addView(pick);info=Ui.hint(this,"尚未导入数据");source.addView(info);root.addView(source);
        LinearLayout tpl=Ui.card(this);tpl.addView(Ui.hint(this,"标签模板"));template=new EditText(this);template.setHint("示例：\n姓名：{姓名}\n编号：{编号}");template.setMinLines(5);tpl.addView(template,new LinearLayout.LayoutParams(-1,Ui.dp(this,150)));LinearLayout range=new LinearLayout(this);range.setOrientation(LinearLayout.HORIZONTAL);from=new EditText(this);from.setHint("起始行");from.setInputType(2);to=new EditText(this);to.setHint("结束行");to.setInputType(2);range.addView(from,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));range.addView(to,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));tpl.addView(range);root.addView(tpl);
        livePreview=new ZoomablePreviewView(this);livePreview.setAdjustViewBounds(true);livePreview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);livePreview.setBackgroundColor(android.graphics.Color.WHITE);root.addView(Ui.previewFrame(this,livePreview,"起始行实时预览"),new LinearLayout.LayoutParams(-1,Ui.dp(this,280)));
        Button print=Ui.primaryButton(this,"预览首条并开始批打");print.setOnClickListener(v->printBatch(false));root.addView(print);LinearLayout control=new LinearLayout(this);control.setOrientation(LinearLayout.HORIZONTAL);Button pause=Ui.button(this,"暂停"),resume=Ui.button(this,"继续上次");pause.setOnClickListener(v->{PrinterManager.get(this).requestCancelBatch();progress.setText("正在请求暂停，将在当前标签完成后停止…");});resume.setOnClickListener(v->resume());control.addView(pause,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));control.addView(resume,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));root.addView(control);progress=Ui.hint(this,hasResume()?"检测到可继续的批打任务":"");root.addView(progress);
        TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){updateLivePreview();}public void afterTextChanged(Editable e){}};template.addTextChangedListener(watcher);from.addTextChangedListener(watcher);return sc;
    }
    private void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/csv","text/comma-separated-values","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/octet-stream"});try{startActivityForResult(i,PICK);}catch(Throwable e){Ui.toast(this,"无法打开文件选择器");}}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==PICK&&res==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Throwable ignored){}load(u,false);}}
    private void load(Uri u,boolean keepRange){try{DataTableReader.Table t=DataTableReader.read(this,u);if(t.headers.isEmpty()){Ui.toast(this,"没有读取到表格数据");return;}table=t;currentUri=u;StringBuilder b=new StringBuilder("字段：");for(String h:table.headers)b.append(" {").append(h).append("}");b.append("\n数据行：").append(table.rows.size());info.setText(b.toString());if(!keepRange){from.setText(table.rows.isEmpty()?"":"1");to.setText(table.rows.isEmpty()?"":Integer.toString(table.rows.size()));}if(template.getText().length()==0){StringBuilder x=new StringBuilder();for(String h:table.headers){if(x.length()>0)x.append('\n');x.append(h).append("：{").append(h).append("}");}template.setText(x.toString());}updateLivePreview();}catch(Throwable e){Ui.toast(this,"导入失败："+e.getMessage());}}
    private String renderText(int row){return renderText(table,row,template.getText().toString());}
    private String renderText(int row,String tpl){return renderText(table,row,tpl);}
    private String renderText(DataTableReader.Table data,int row,String tpl){if(data==null||row<0||row>=data.rows.size())return "";long seq=row+1L;String out=tpl.replace("{ROW}",Long.toString(seq)).replace("{SEQ10}",Long.toString(seq)).replace("{SEQ2}",SerialUtil.format(seq,SerialUtil.BIN,0)).replace("{SEQ16}",SerialUtil.format(seq,SerialUtil.HEX,0)).replace("{SEQ26}",SerialUtil.base26One(seq)).replace("{SEQ26_0}",SerialUtil.base26Zero(seq)).replace("{SEQ36}",SerialUtil.format(seq,SerialUtil.BASE36,0));for(int i=0;i<data.headers.size();i++)out=out.replace("{"+data.headers.get(i)+"}",data.value(row,i));return out;}
    private Bitmap renderBitmap(int row){return renderBitmap(table,row,template.getText().toString());}
    private Bitmap renderBitmap(int row,String tpl){return renderBitmap(table,row,tpl);}
    private Bitmap renderBitmap(DataTableReader.Table data,int row,String tpl){float px=24*getResources().getDisplayMetrics().scaledDensity;return RasterEncoder.textBitmap(renderText(data,row,tpl),px,false,0,5);}
    private void updateLivePreview(){if(livePreview==null||table==null||table.rows.isEmpty())return;try{Bitmap next=renderBitmap(Math.max(0,Math.min(table.rows.size()-1,parse(from,1)-1)));Bitmap old=liveBitmap;liveBitmap=next;livePreview.setImageBitmap(next);if(old!=null&&old!=next&&!old.isRecycled())old.recycle();Ui.pulse(livePreview);}catch(Throwable ignored){}}
    private void printBatch(boolean resumed){
        if(table==null||table.rows.isEmpty()){Ui.toast(this,"请先导入数据");return;}
        int a=parse(from,1)-1,b=parse(to,table.rows.size())-1;
        a=Math.max(0,Math.min(a,table.rows.size()-1));b=Math.max(a,Math.min(b,table.rows.size()-1));
        if(b-a+1>200){Ui.toast(this,"单次最多 200 条，请分批打印");return;}
        if(currentUri==null){Ui.toast(this,"缺少数据源文件");return;}
        final int start=a,end=b,total=end-start+1;final String tpl=template.getText().toString();final DataTableReader.Table data=table;
        completed=0;saveResume(start,end);progress.setText("准备批量打印 "+total+" 条标签…");
        PrinterManager.get(this).printGenerated(this,total,index->{
            Bitmap bmp=renderBitmap(data,start+index,tpl);
            try{return RasterEncoder.encode(bmp,212,RasterEncoder.Dither.THRESHOLD);}
            finally{if(bmp!=null&&!bmp.isRecycled())bmp.recycle();}
        },(done,count,msg)->{
            completed=done;saveNext(start+done,end);progress.setText("已完成 "+done+" / "+count+" · "+msg);
        },(ok,msg)->{
            Ui.toast(this,msg);progress.setText(msg);
            if(ok){clearResume();HistoryStore.add(this,"批量数据","第 "+(start+1)+"-"+(end+1)+" 行，共 "+total+" 条");}
            else saveNext(start+completed,end);
        });
    }
    private void resume(){android.content.SharedPreferences p=getSharedPreferences(PREF,0);if(!p.getBoolean("active",false)){Ui.toast(this,"没有可继续的批打任务");return;}String us=p.getString("uri","");if(us.isEmpty()){Ui.toast(this,"上次数据源已失效");return;}template.setText(p.getString("template",""));int next=p.getInt("next",0),end=p.getInt("end",next);from.setText(Integer.toString(next+1));to.setText(Integer.toString(end+1));load(Uri.parse(us),true);if(table!=null)printBatch(true);}
    private boolean hasResume(){return getSharedPreferences(PREF,0).getBoolean("active",false);}
    private void saveResume(int start,int end){getSharedPreferences(PREF,0).edit().putBoolean("active",true).putString("uri",currentUri.toString()).putString("template",template.getText().toString()).putInt("next",start).putInt("end",end).apply();}
    private void saveNext(int next,int end){getSharedPreferences(PREF,0).edit().putBoolean("active",next<=end).putInt("next",next).putInt("end",end).apply();}
    private void clearResume(){getSharedPreferences(PREF,0).edit().clear().apply();}
    private int parse(EditText e,int def){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception x){return def;}}
    @Override protected void onDestroy(){if(liveBitmap!=null&&!liveBitmap.isRecycled())liveBitmap.recycle();liveBitmap=null;super.onDestroy();}
}
