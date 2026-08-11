package com.qring.print;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.*;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.*;
import java.util.*;

public class PdfPrintActivity extends Activity {
    private static final int PICK=301; private static final String PREF="pdf_resume";
    private Uri uri; private int pageIndex=0,pageCount=0;
    private ImageView preview,printPreview; private TextView pageInfo,progress,cropLabel,clarityLabel; private Bitmap current,thermalPreview; private EditText from,to; private int rotationQuarter=0; private SeekBar crop,clarity; private CheckBox autoCrop; private Spinner effect,segments;
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());String incoming=getIntent().getStringExtra("incoming_uri");if(incoming!=null){uri=Uri.parse(incoming);pageIndex=0;render();}}

    private View build(){
        ScrollView sc=new ScrollView(this);LinearLayout root=Ui.page(this,"PDF / 多页打印");sc.addView(root);
        Button pick=Ui.button(this,"选择 PDF");pick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);try{startActivityForResult(i,PICK);}catch(Throwable e){Ui.toast(this,"无法打开文件选择器");}});root.addView(pick);
        pageInfo=Ui.hint(this,"未选择 PDF");root.addView(pageInfo);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button prev=Ui.button(this,"上一页"),next=Ui.button(this,"下一页");prev.setOnClickListener(v->{if(pageIndex>0){pageIndex--;render();}});next.setOnClickListener(v->{if(pageIndex+1<pageCount){pageIndex++;render();}});row.addView(prev,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));row.addView(next,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));root.addView(row);
        LinearLayout ops=new LinearLayout(this);ops.setOrientation(LinearLayout.HORIZONTAL);Button rot=Ui.button(this,"旋转 90°");Button reset=Ui.button(this,"恢复方向");ops.addView(rot,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));ops.addView(reset,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));root.addView(ops);rot.setOnClickListener(v->{rotationQuarter=(rotationQuarter+1)%4;render();});reset.setOnClickListener(v->{rotationQuarter=0;render();});
        cropLabel=Ui.hint(this,"裁剪四周：0%");root.addView(cropLabel);crop=new SeekBar(this);crop.setMax(20);crop.setProgress(0);crop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){cropLabel.setText("裁剪四周："+p+"%");}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){render();}});root.addView(crop);
        autoCrop=new CheckBox(this);autoCrop.setText("自动裁掉 PDF 四周空白");autoCrop.setChecked(true);autoCrop.setOnCheckedChangeListener((v,checked)->render());root.addView(autoCrop);
        effect=new Spinner(this);effect.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Floyd 照片/文档","阈值 文字/线稿","Ordered 有序"}));root.addView(effect);effect.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){updatePrintPreview();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        root.addView(Ui.hint(this,"智能分割：把一页纵向切成多个连续标签（每段之间按走纸设置留间距）"));segments=new Spinner(this);segments.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"不分割（1 段）","每页 2 段","每页 3 段","每页 4 段","每页 5 段","每页 6 段"}));root.addView(segments);
        clarityLabel=Ui.hint(this,"清晰度阈值：190");root.addView(clarityLabel);clarity=new SeekBar(this);clarity.setMax(115);clarity.setProgress(50);clarity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar v,int p,boolean f){clarityLabel.setText("清晰度阈值："+(140+p));}public void onStartTrackingTouch(SeekBar v){}public void onStopTrackingTouch(SeekBar v){updatePrintPreview();}});root.addView(clarity);
        preview=new ZoomablePreviewView(this);preview.setAdjustViewBounds(true);preview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);preview.setBackgroundColor(Color.WHITE);root.addView(Ui.previewFrame(this,preview,"PDF 页面预览"),new LinearLayout.LayoutParams(-1,Ui.dp(this,260)));
        printPreview=new ZoomablePreviewView(this);printPreview.setAdjustViewBounds(true);printPreview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);printPreview.setBackgroundColor(Color.WHITE);root.addView(Ui.previewFrame(this,printPreview,"实时热敏打印效果"),new LinearLayout.LayoutParams(-1,Ui.dp(this,280)));
        Button print=Ui.primaryButton(this,"预览确认并打印当前页");print.setOnClickListener(v->printCurrent());root.addView(print);
        root.addView(Ui.hint(this,"批量页码范围（从 1 开始；为控制内存，单次最多 50 页）"));LinearLayout range=new LinearLayout(this);range.setOrientation(LinearLayout.HORIZONTAL);from=new EditText(this);from.setHint("起始页");from.setInputType(2);to=new EditText(this);to.setHint("结束页");to.setInputType(2);range.addView(from,new LinearLayout.LayoutParams(0,Ui.dp(this,56),1));range.addView(to,new LinearLayout.LayoutParams(0,Ui.dp(this,56),1));root.addView(range);
        Button batch=Ui.primaryButton(this,"预览首条并批量打印");batch.setOnClickListener(v->printRange());root.addView(batch);Button pause=Ui.button(this,"暂停 PDF 批量打印");pause.setOnClickListener(v->{PrinterManager.get(this).requestCancelBatch();progress.setText("正在请求暂停，将在当前标签完成后停止…");});root.addView(pause);Button resume=Ui.button(this,"继续上次 PDF 批量打印");resume.setOnClickListener(v->resumeRange());root.addView(resume);progress=Ui.hint(this,hasResume()?"检测到上次未完成的 PDF 批打任务":"");root.addView(progress);return sc;
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==PICK&&result==RESULT_OK&&data!=null&&data.getData()!=null){uri=data.getData();try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Throwable ignored){}pageIndex=0;render();}}
    private void render(){
        if(uri==null)return;try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(uri,"r");PdfRenderer renderer=new PdfRenderer(fd)){pageCount=renderer.getPageCount();if(pageCount<=0){Ui.toast(this,"PDF 没有页面");return;}pageIndex=Math.max(0,Math.min(pageIndex,pageCount-1));Bitmap old=current;current=renderPage(renderer,pageIndex);preview.setImageBitmap(current);if(old!=null&&old!=current&&!old.isRecycled())old.recycle();updatePrintPreview();pageInfo.setText("第 "+(pageIndex+1)+" / "+pageCount+" 页");if(from.getText().length()==0)from.setText("1");if(to.getText().length()==0)to.setText(Integer.toString(pageCount));}catch(Throwable e){Ui.toast(this,"PDF 读取失败："+e.getMessage());}
    }

    private void updatePrintPreview(){if(printPreview==null||current==null)return;try{RasterEncoder.Raster r=RasterEncoder.encode(current,threshold(),dither());r=PrinterManager.get(this).previewRaster(r);Bitmap next=RasterEncoder.toBitmap(r,1800);Bitmap old=thermalPreview;thermalPreview=next;printPreview.setImageBitmap(next);if(old!=null&&old!=next&&!old.isRecycled())old.recycle();Ui.pulse(printPreview);}catch(OutOfMemoryError e){Ui.toast(this,"PDF 预览内存不足");}catch(Throwable ignored){}}
    private static final class RenderConfig{
        final int rotation,cropPct,threshold,segments,effect; final boolean autoCrop;
        RenderConfig(int r,int c,boolean a,int t,int s,int e){rotation=r;cropPct=c;autoCrop=a;threshold=t;segments=s;effect=e;}
        RasterEncoder.Dither dither(){return effect==1?RasterEncoder.Dither.THRESHOLD:(effect==2?RasterEncoder.Dither.ORDERED:RasterEncoder.Dither.FLOYD);}
    }
    private RenderConfig config(){return new RenderConfig(rotationQuarter,crop==null?0:crop.getProgress(),autoCrop!=null&&autoCrop.isChecked(),threshold(),segmentCount(),effect==null?0:effect.getSelectedItemPosition());}
    private Bitmap renderPage(PdfRenderer renderer,int idx){return renderPage(renderer,idx,config());}
    private Bitmap renderPage(PdfRenderer renderer,int idx,RenderConfig cfg){
        try(PdfRenderer.Page p=renderer.openPage(idx)){
            int w=QringProtocol.WIDTH_DOTS;int h=Math.max(1,Math.min(8000,Math.round(w*p.getHeight()/(float)Math.max(1,p.getWidth()))));
            Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);p.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_PRINT);return transform(b,cfg);
        }
    }
    private Bitmap transform(Bitmap src,RenderConfig cfg){
        Bitmap b=src,next;
        if(cfg.autoCrop){next=autoCropWhitespace(b);if(next!=b){b.recycle();b=next;}}
        int cp=cfg.cropPct;if(cp>0){int dx=Math.max(0,Math.round(b.getWidth()*cp/100f)),dy=Math.max(0,Math.round(b.getHeight()*cp/100f));int cw=b.getWidth()-dx*2,ch=b.getHeight()-dy*2;if(cw>20&&ch>20){next=Bitmap.createBitmap(b,dx,dy,cw,ch);if(next!=b){b.recycle();b=next;}}}
        if(cfg.rotation!=0){Matrix m=new Matrix();m.postRotate(cfg.rotation*90f);next=Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),m,true);if(next!=b){b.recycle();b=next;}}
        next=RasterEncoder.fitToWidth(b);if(next!=b){b.recycle();b=next;}return b;
    }
    private Bitmap autoCropWhitespace(Bitmap src){
        int w=src.getWidth(),h=src.getHeight();if(w<8||h<8)return src;
        int[] row=new int[w];int left=w,top=h,right=-1,bottom=-1;
        for(int y=0;y<h;y++){
            src.getPixels(row,0,w,0,y,w,1);
            for(int x=0;x<w;x++){
                int c=row[x],a=Color.alpha(c),r=Color.red(c),g=Color.green(c),bl=Color.blue(c);
                int gray=(r*30+g*59+bl*11)/100;
                if(a>24&&gray<245){if(x<left)left=x;if(x>right)right=x;if(y<top)top=y;if(y>bottom)bottom=y;}
            }
        }
        if(right<left||bottom<top)return src;
        int m=6;left=Math.max(0,left-m);top=Math.max(0,top-m);right=Math.min(w-1,right+m);bottom=Math.min(h-1,bottom+m);
        if(left==0&&top==0&&right==w-1&&bottom==h-1)return src;
        return Bitmap.createBitmap(src,left,top,right-left+1,bottom-top+1);
    }
    private int threshold(){return Math.max(0,Math.min(255,140+(clarity==null?50:clarity.getProgress())));}
    private RasterEncoder.Dither dither(){int p=effect==null?0:effect.getSelectedItemPosition();return p==1?RasterEncoder.Dither.THRESHOLD:(p==2?RasterEncoder.Dither.ORDERED:RasterEncoder.Dither.FLOYD);}

    private int segmentCount(){return segments==null?1:Math.max(1,Math.min(6,segments.getSelectedItemPosition()+1));}
    private Bitmap segment(Bitmap page,int index,int count){
        if(page==null||count<=1)return page;index=Math.max(0,Math.min(count-1,index));int h=page.getHeight();int y0=(int)((long)h*index/count),y1=(int)((long)h*(index+1)/count);int sh=Math.max(1,y1-y0);return Bitmap.createBitmap(page,0,y0,page.getWidth(),Math.min(sh,h-y0));
    }

    private void printCurrent(){
        if(current==null){Ui.toast(this,"请先选择 PDF");return;}
        final int segCount=segmentCount(),th=threshold();final RasterEncoder.Dither dm=dither();
        try{
            if(segCount==1){RasterEncoder.Raster r=RasterEncoder.encode(current,th,dm);PrinterManager.get(this).print(this,new PrinterManager.BitmapHolder(r),(ok,msg)->{Ui.toast(this,msg);if(ok)HistoryStore.addRaster(this,"PDF","第 "+(pageIndex+1)+" 页",r);});return;}
            final Uri source=uri;final int stablePageIndex=pageIndex;final RenderConfig cfg=config();progress.setText("当前页将分割为 "+segCount+" 段打印…");
            PrinterManager.get(this).printGenerated(this,segCount,index->{Bitmap page=null,part=null;try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(source,"r");PdfRenderer renderer=new PdfRenderer(fd)){page=renderPage(renderer,stablePageIndex,cfg);part=segment(page,index,segCount);return RasterEncoder.encode(part,th,dm);}finally{if(part!=null&&part!=page&&!part.isRecycled())part.recycle();if(page!=null&&!page.isRecycled())page.recycle();}},(done,total,msg)->progress.setText("当前页分割打印 "+done+" / "+total),(ok,msg)->{Ui.toast(this,msg);progress.setText(msg);if(ok)HistoryStore.add(this,"PDF 分割","第 "+(stablePageIndex+1)+" 页 · "+segCount+" 段");});
        }catch(OutOfMemoryError e){Ui.toast(this,"PDF 页面过大，当前设备内存不足");}catch(Throwable e){Ui.toast(this,"PDF 打印准备失败："+e.getMessage());}
    }
    private void printRange(){
        if(uri==null){Ui.toast(this,"请先选择 PDF");return;}
        int a,b;try{a=Integer.parseInt(from.getText().toString());b=Integer.parseInt(to.getText().toString());}
        catch(Throwable e){Ui.toast(this,"页码格式不正确");return;}
        if(a<1||b<a||b>pageCount){Ui.toast(this,"页码范围应为 1 到 "+pageCount);return;}
        if(b-a+1>50){Ui.toast(this,"单次最多打印 50 页");return;}
        startRange(a,b,0,config());
    }

    private void startRange(int start,int end,int offset,RenderConfig cfg){
        final int pages=end-start+1,segCount=cfg.segments,total=pages*segCount;
        if(offset<0||offset>=total){clearResume();Ui.toast(this,"PDF 批打任务已经完成");return;}
        final int begin=offset,remain=total-offset;final Uri source=uri;
        saveResume(source,start,end,begin,cfg);
        progress.setText("准备批量打印 "+pages+" 页"+(segCount>1?"，每页 "+segCount+" 段":"")+" · 剩余 "+remain+" 个任务…");
        PrinterManager.get(this).printGenerated(this,remain,index->{
            int task=begin+index,pageOffset=task/segCount,segIndex=task%segCount;Bitmap bmp=null,part=null;
            try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(source,"r");PdfRenderer renderer=new PdfRenderer(fd)){
                bmp=renderPage(renderer,start-1+pageOffset,cfg);part=segment(bmp,segIndex,segCount);return RasterEncoder.encode(part,cfg.threshold,cfg.dither());
            }finally{if(part!=null&&part!=bmp&&!part.isRecycled())part.recycle();if(bmp!=null&&!bmp.isRecycled())bmp.recycle();}
        },(done,count,msg)->{int next=begin+done;saveNext(next);progress.setText("本次 "+done+" / "+count+" · 总进度 "+next+" / "+total+" · "+msg);},(ok,msg)->{
            Ui.toast(this,msg);progress.setText(msg);if(ok){clearResume();HistoryStore.add(this,"PDF 批量",start+"-"+end+" 页"+(segCount>1?" · 每页 "+segCount+" 段":""));}
        });
    }

    private void resumeRange(){
        android.content.SharedPreferences p=getSharedPreferences(PREF,0);if(!p.getBoolean("active",false)){Ui.toast(this,"没有可继续的 PDF 任务");return;}
        String us=p.getString("uri","");if(us.isEmpty()){Ui.toast(this,"上次 PDF 数据源已失效");clearResume();return;}
        uri=Uri.parse(us);rotationQuarter=p.getInt("rotation",0);crop.setProgress(p.getInt("crop",0));autoCrop.setChecked(p.getBoolean("auto_crop",true));clarity.setProgress(Math.max(0,Math.min(115,p.getInt("threshold",190)-140)));segments.setSelection(Math.max(0,Math.min(5,p.getInt("segments",1)-1)));effect.setSelection(Math.max(0,Math.min(2,p.getInt("effect",0))));
        int start=p.getInt("start",1),end=p.getInt("end",start),next=p.getInt("next",0);from.setText(Integer.toString(start));to.setText(Integer.toString(end));
        try{render();RenderConfig cfg=config();startRange(start,end,next,cfg);}catch(Throwable e){Ui.toast(this,"无法继续上次 PDF："+e.getMessage());}
    }
    private boolean hasResume(){return getSharedPreferences(PREF,0).getBoolean("active",false);}
    private void saveResume(Uri source,int start,int end,int next,RenderConfig cfg){getSharedPreferences(PREF,0).edit().putBoolean("active",true).putString("uri",source.toString()).putInt("start",start).putInt("end",end).putInt("next",next).putInt("rotation",cfg.rotation).putInt("crop",cfg.cropPct).putBoolean("auto_crop",cfg.autoCrop).putInt("threshold",cfg.threshold).putInt("segments",cfg.segments).putInt("effect",cfg.effect).apply();}
    private void saveNext(int next){android.content.SharedPreferences p=getSharedPreferences(PREF,0);int total=Math.max(1,(p.getInt("end",1)-p.getInt("start",1)+1)*p.getInt("segments",1));p.edit().putBoolean("active",next<total).putInt("next",next).apply();}
    private void clearResume(){getSharedPreferences(PREF,0).edit().clear().apply();}
    @Override protected void onDestroy(){super.onDestroy();if(current!=null&&!current.isRecycled())current.recycle();if(thermalPreview!=null&&!thermalPreview.isRecycled())thermalPreview.recycle();current=null;thermalPreview=null;}

}
