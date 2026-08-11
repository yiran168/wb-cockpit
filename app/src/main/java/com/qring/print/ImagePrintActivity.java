package com.qring.print;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.View;
import android.widget.*;

public class ImagePrintActivity extends Activity {
    private static final int PICK=201;
    private ImageView original,preview;private Bitmap bitmap,thermalPreview;private Spinner mode;private TextView info,thresholdLabel,cropLabel,modeHelp;private SeekBar threshold,crop;
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());String incoming=getIntent().getStringExtra("incoming_uri");if(incoming!=null)load(Uri.parse(incoming));}
    private View build(){
        ScrollView sc=new ScrollView(this);LinearLayout root=Ui.page(this,"图片打印");sc.addView(root);
        LinearLayout source=Ui.card(this);Button pick=Ui.primaryButton(this,"选择照片 / 图片");pick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");try{startActivityForResult(i,PICK);}catch(Throwable e){Ui.toast(this,"无法打开文件选择器");}});source.addView(pick);info=Ui.hint(this,"支持相册、文件管理器和系统分享；大图自动按内存安全尺寸解码。");source.addView(info);root.addView(source);
        original=new ZoomablePreviewView(this);original.setAdjustViewBounds(true);original.setScaleType(ImageView.ScaleType.CENTER_INSIDE);original.setBackgroundColor(Color.WHITE);root.addView(Ui.previewFrame(this,original,"原图"),new LinearLayout.LayoutParams(-1,Ui.dp(this,180)));
        preview=new ZoomablePreviewView(this);preview.setAdjustViewBounds(true);preview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);preview.setBackgroundColor(Color.WHITE);root.addView(Ui.previewFrame(this,preview,"实时热敏打印效果"),new LinearLayout.LayoutParams(-1,Ui.dp(this,330)));
        LinearLayout controls=Ui.card(this);controls.addView(Ui.hint(this,"图像算法"));mode=new Spinner(this);mode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{
                "Floyd-Steinberg · 通用照片",
                "Atkinson · 人像 / 柔和灰阶",
                "Jarvis-Judice-Ninke · 渐变细腻",
                "Sierra Lite · 清晰省算力",
                "Stucki · 高细节照片",
                "Ordered 8×8 · 插图 / 屏幕图",
                "Bayer 4×4 · 图形 / 纹理",
                "阈值 · 文字截图 / 条码"
        }));controls.addView(mode);modeHelp=Ui.hint(this,"Floyd：均衡；Atkinson：亮部更自然；Jarvis/Stucki：细节丰富；Sierra Lite：速度快；Ordered/Bayer：规则网点；阈值：边缘最硬。");controls.addView(modeHelp);thresholdLabel=Ui.hint(this,"黑白阈值：180");controls.addView(thresholdLabel);threshold=new SeekBar(this);threshold.setMax(155);threshold.setProgress(80);controls.addView(threshold);cropLabel=Ui.hint(this,"中心裁切：0%（每边）");controls.addView(cropLabel);crop=new SeekBar(this);crop.setMax(35);crop.setProgress(0);controls.addView(crop);LinearLayout ops=new LinearLayout(this);ops.setOrientation(LinearLayout.HORIZONTAL);Button rot=Ui.button(this,"旋转 90°"),mirror=Ui.button(this,"左右镜像");ops.addView(rot,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));ops.addView(mirror,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));controls.addView(ops);Button applyCrop=Ui.button(this,"应用中心裁切");controls.addView(applyCrop);root.addView(controls);
        LinearLayout saveRow=new LinearLayout(this);saveRow.setOrientation(LinearLayout.HORIZONTAL);Button save=Ui.button(this,"保存模板"),print=Ui.primaryButton(this,"预览确认并打印");saveRow.addView(save,new LinearLayout.LayoutParams(0,Ui.dp(this,54),1));saveRow.addView(print,new LinearLayout.LayoutParams(0,Ui.dp(this,54),1));root.addView(saveRow);
        rot.setOnClickListener(v->rotate());mirror.setOnClickListener(v->mirror());applyCrop.setOnClickListener(v->applyCrop());crop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){cropLabel.setText("中心裁切："+p+"%（每边）");}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});save.setOnClickListener(v->{if(bitmap==null){Ui.toast(this,"请先选择图片");return;}Bitmap fitted=null;try{fitted=RasterEncoder.fitToWidth(bitmap);TemplateStore.save(this,"图片模板 "+HistoryStore.formatTime(System.currentTimeMillis()),fitted);Ui.toast(this,"模板已保存");}catch(Throwable e){Ui.toast(this,"保存失败："+e.getMessage());}finally{if(fitted!=null&&fitted!=bitmap&&!fitted.isRecycled())fitted.recycle();}});print.setOnClickListener(v->doPrint());
        mode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){updatePreview();}public void onNothingSelected(android.widget.AdapterView<?> p){}});threshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){thresholdLabel.setText("黑白阈值："+(100+p));if(f)updatePreview();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){updatePreview();}});return sc;
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==PICK&&result==RESULT_OK&&data!=null&&data.getData()!=null)load(data.getData());}
    private void load(Uri u){try{Bitmap next=ImageLoader.load(this,u);if(next==null)throw new Exception("图片解码为空");Bitmap old=bitmap;bitmap=next;original.setImageBitmap(bitmap);if(old!=null&&old!=next&&!old.isRecycled())old.recycle();info.setText("已载入 "+bitmap.getWidth()+" × "+bitmap.getHeight()+" · 已启用内存保护");updatePreview();}catch(OutOfMemoryError e){Ui.toast(this,"图片过大，当前设备内存不足");}catch(Throwable e){Ui.toast(this,"读取图片失败："+e.getMessage());}}
    private RasterEncoder.Dither dither(){
        int p=mode.getSelectedItemPosition();
        switch(p){
            case 1:return RasterEncoder.Dither.ATKINSON;
            case 2:return RasterEncoder.Dither.JARVIS;
            case 3:return RasterEncoder.Dither.SIERRA_LITE;
            case 4:return RasterEncoder.Dither.STUCKI;
            case 5:return RasterEncoder.Dither.ORDERED;
            case 6:return RasterEncoder.Dither.BAYER;
            case 7:return RasterEncoder.Dither.THRESHOLD;
            default:return RasterEncoder.Dither.FLOYD;
        }
    }
    private int th(){return 100+threshold.getProgress();}
    private void updatePreview(){if(bitmap==null)return;try{RasterEncoder.Raster r=RasterEncoder.encode(bitmap,th(),dither());r=PrinterManager.get(this).previewRaster(r);Bitmap next=RasterEncoder.toBitmap(r,1800);Bitmap old=thermalPreview;thermalPreview=next;preview.setImageBitmap(next);if(old!=null&&old!=next&&!old.isRecycled())old.recycle();Ui.pulse(preview);}catch(OutOfMemoryError e){Ui.toast(this,"生成预览时内存不足");}catch(Throwable e){Ui.toast(this,"预览失败："+e.getMessage());}}
    private void rotate(){if(bitmap==null){Ui.toast(this,"请先选择图片");return;}try{Matrix m=new Matrix();m.postRotate(90);Bitmap old=bitmap;Bitmap out=Bitmap.createBitmap(old,0,0,old.getWidth(),old.getHeight(),m,true);bitmap=out;original.setImageBitmap(bitmap);if(old!=out&&!old.isRecycled())old.recycle();updatePreview();}catch(Throwable e){Ui.toast(this,"旋转失败："+e.getMessage());}}
    private void mirror(){if(bitmap==null){Ui.toast(this,"请先选择图片");return;}try{Matrix m=new Matrix();m.setScale(-1,1);m.postTranslate(bitmap.getWidth(),0);Bitmap old=bitmap;Bitmap out=Bitmap.createBitmap(old.getWidth(),old.getHeight(),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(out);c.drawBitmap(old,m,null);bitmap=out;original.setImageBitmap(bitmap);if(old!=out&&!old.isRecycled())old.recycle();updatePreview();}catch(Throwable e){Ui.toast(this,"镜像失败："+e.getMessage());}}
    private void applyCrop(){if(bitmap==null){Ui.toast(this,"请先选择图片");return;}int pct=crop==null?0:crop.getProgress();if(pct<=0){Ui.toast(this,"裁切比例为 0%" );return;}try{Bitmap old=bitmap;int dx=Math.round(old.getWidth()*pct/100f),dy=Math.round(old.getHeight()*pct/100f);int w=old.getWidth()-dx*2,h=old.getHeight()-dy*2;if(w<32||h<32){Ui.toast(this,"裁切后图片过小");return;}Bitmap out=Bitmap.createBitmap(old,dx,dy,w,h);bitmap=out;original.setImageBitmap(bitmap);if(old!=out&&!old.isRecycled())old.recycle();crop.setProgress(0);info.setText("已裁切为 "+w+" × "+h+" · 可继续旋转/镜像/打印");updatePreview();}catch(OutOfMemoryError e){Ui.toast(this,"裁切时内存不足");}catch(Throwable e){Ui.toast(this,"裁切失败："+e.getMessage());}}
    private void doPrint(){if(bitmap==null){Ui.toast(this,"请先选择图片");return;}try{RasterEncoder.Raster r=RasterEncoder.encode(bitmap,th(),dither());PrinterManager.get(this).print(this,new PrinterManager.BitmapHolder(r),(ok,msg)->{if(!"已取消打印".equals(msg))Ui.toast(this,msg);if(ok)HistoryStore.addRaster(this,"图片","图片打印",r);});}catch(OutOfMemoryError e){Ui.toast(this,"图片过大，当前设备内存不足，请裁小后重试");}catch(Throwable e){Ui.toast(this,"图片处理失败："+e.getMessage());}}
    @Override protected void onDestroy(){if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();if(thermalPreview!=null&&!thermalPreview.isRecycled())thermalPreview.recycle();bitmap=null;thermalPreview=null;super.onDestroy();}
}
