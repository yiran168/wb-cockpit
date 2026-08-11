package com.qring.print;

import android.app.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class HistoryActivity extends Activity {
    private LinearLayout list;
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());}
    @Override protected void onResume(){super.onResume();render();}
    private View build(){Ui.resolvePalette(this);LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setBackgroundColor(Ui.BG);ScrollView sc=new ScrollView(this);sc.setClipToPadding(false);sc.setFillViewport(true);LinearLayout root=Ui.page(this,"打印历史");sc.addView(root);shell.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout intro=Ui.card(this);intro.addView(Ui.hint(this,"每次成功打印都会保存本地记录与可用的点阵/缩略图。重打仍会进入最终 384 点预览确认。"));Button clear=Ui.button(this,"清空全部历史");clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("清空历史").setMessage("清空全部打印历史和缩略图？").setPositiveButton("清空",(d,w)->{HistoryStore.clear(this);render();}).setNegativeButton("取消",null).show());intro.addView(clear);root.addView(intro);
        root.addView(Ui.section(this,"最近打印","可预览、重打、重命名或复制为模板。"));list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);shell.addView(Ui.bottomNav(this,HistoryActivity.class));return shell;}
    private void render(){
        list.removeAllViews();List<HistoryStore.Item> a=HistoryStore.list(this);if(a.isEmpty()){list.addView(Ui.emptyState(this,"↻","还没有打印记录","成功打印一次后，这里会自动保留最终点阵缩略图、参数与重打入口。","去创建标签",v->startActivity(new android.content.Intent(this,CanvasEditorActivity.class))));return;}
        for(HistoryStore.Item item:a){LinearLayout card=Ui.card(this);LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);head.setGravity(Gravity.CENTER_VERTICAL);head.addView(Ui.pill(this,item.type,Ui.PRIMARY));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(Ui.dp(this,10),0,0,0);TextView title=new TextView(this);title.setText(item.summary);title.setTextSize(16);title.setTextColor(Ui.TEXT);title.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);title.setMaxLines(2);tx.addView(title);TextView time=Ui.hint(this,HistoryStore.formatTime(item.time));time.setPadding(0,0,0,0);tx.addView(time);head.addView(tx,new LinearLayout.LayoutParams(0,-2,1));card.addView(head);
            Bitmap b=HistoryStore.loadPreview(item);if(b!=null){ImageView iv=new ZoomablePreviewView(this);iv.setAdjustViewBounds(true);iv.setScaleType(ImageView.ScaleType.FIT_CENTER);iv.setBackgroundColor(0xffffffff);iv.setImageBitmap(b);card.addView(Ui.previewFrame(this,iv,"历史预览"),new LinearLayout.LayoutParams(-1,Ui.dp(this,190)));}
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button rp=Ui.primaryButton(this,"重打"),cp=Ui.button(this,"复制模板"),rn=Ui.button(this,"重命名");rp.setOnClickListener(x->reprint(item));cp.setOnClickListener(x->copyAsTemplate(item));rn.setOnClickListener(x->rename(item));row.addView(rp,new LinearLayout.LayoutParams(0,Ui.dp(this,50),1));row.addView(cp,new LinearLayout.LayoutParams(0,Ui.dp(this,50),1));row.addView(rn,new LinearLayout.LayoutParams(0,Ui.dp(this,50),1));card.addView(row);list.addView(card);}
    }
    private void copyAsTemplate(HistoryStore.Item item){try{Bitmap b=HistoryStore.load(item);if(b==null){Ui.toast(this,"该历史记录没有可复制图像");return;}try{TemplateStore.save(this,item.summary+" 副本",b);}finally{if(!b.isRecycled())b.recycle();}Ui.toast(this,"已复制到本地模板");}catch(Throwable e){Ui.toast(this,"复制失败："+e.getMessage());}}
    private void rename(HistoryStore.Item item){EditText e=new EditText(this);e.setText(item.summary);new AlertDialog.Builder(this).setTitle("重命名打印记录").setView(e).setPositiveButton("保存",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty()){HistoryStore.rename(this,item.id,n);render();}}).setNegativeButton("取消",null).show();}
    private void reprint(HistoryStore.Item item){try{RasterEncoder.Raster direct=HistoryStore.loadRaster(item);if(direct!=null){PrinterManager.get(this).print(this,new PrinterManager.BitmapHolder(direct),(ok,msg)->{if(!"已取消打印".equals(msg))Ui.toast(this,msg);});return;}Bitmap b=HistoryStore.load(item);if(b==null){Ui.toast(this,"该历史记录没有可重打图像");return;}RasterEncoder.Raster r;try{r=RasterEncoder.encode(b,180,RasterEncoder.Dither.THRESHOLD);}finally{if(!b.isRecycled())b.recycle();}PrinterManager.get(this).print(this,new PrinterManager.BitmapHolder(r),(ok,msg)->{if(!"已取消打印".equals(msg))Ui.toast(this,msg);});}catch(OutOfMemoryError e){Ui.toast(this,"历史图像过大，当前设备内存不足");}catch(Throwable e){Ui.toast(this,"历史记录读取失败："+e.getMessage());}}
    @Override public boolean dispatchTouchEvent(MotionEvent e){if(Ui.handleTabSwipe(this,HistoryActivity.class,e))return true;return super.dispatchTouchEvent(e);}

}
