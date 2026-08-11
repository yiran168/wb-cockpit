package com.qring.print;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.*;
import android.view.View;
import android.widget.*;
import com.google.zxing.*;
import java.util.*;

/** Local scan-to-template equivalent. It never pretends to understand proprietary HPRT consumable codes. */
public class TemplateCodeActivity extends Activity {
    private static final int PICK=1801,CAMERA=1802;
    private Spinner templates;private ImageView codePreview,sourcePreview,templatePreview;private TextView state;private Bitmap qr,source,template;
    private int selected=-1;

    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());generateCode();}
    private View build(){
        ScrollView sc=new ScrollView(this);sc.setClipToPadding(false);LinearLayout root=Ui.page(this,"扫码取模");sc.addView(root);
        LinearLayout explain=Ui.card(this);explain.addView(Ui.hint(this,"使用说明：Qrint 模板码是本应用自有的离线模板索引，只识别本应用生成的模板码，不冒充任何厂商官方耗材码。另一台安装相同版本的设备扫描后，可直接调出对应行业模板。"));root.addView(explain);

        root.addView(Ui.section(this,"生成模板码","选择本地行业模板，实时生成可分享/可打印的二维码。"));
        LinearLayout make=Ui.card(this);String[] names=new String[BuiltInTemplateActivity.templateCount()];for(int i=0;i<names.length;i++)names[i]=BuiltInTemplateActivity.templateName(i);templates=new Spinner(this);templates.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));make.addView(templates);codePreview=new ZoomablePreviewView(this);codePreview.setAdjustViewBounds(true);codePreview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);codePreview.setBackgroundColor(Color.WHITE);make.addView(Ui.previewFrame(this,codePreview,"Qrint 模板码预览"),new LinearLayout.LayoutParams(-1,Ui.dp(this,250)));Button printCode=Ui.button(this,"打印这个模板码");printCode.setOnClickListener(v->printCode());make.addView(printCode);root.addView(make);
        templates.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){generateCode();}public void onNothingSelected(android.widget.AdapterView<?> p){}});

        root.addView(Ui.section(this,"扫描取模","支持相机拍照或从图片中识别，识别后立即显示模板预览。"));
        LinearLayout scan=Ui.card(this);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button cam=Ui.primaryButton(this,"相机拍照"),pick=Ui.button(this,"从图片识别");cam.setOnClickListener(v->camera());pick.setOnClickListener(v->pick());row.addView(cam,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));row.addView(pick,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));scan.addView(row);state=Ui.hint(this,"等待扫描 Qrint 模板码");scan.addView(state);root.addView(scan);
        sourcePreview=new ZoomablePreviewView(this);sourcePreview.setAdjustViewBounds(true);sourcePreview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);sourcePreview.setBackgroundColor(Color.WHITE);root.addView(Ui.previewFrame(this,sourcePreview,"识别图片"),new LinearLayout.LayoutParams(-1,Ui.dp(this,180)));
        templatePreview=new ZoomablePreviewView(this);templatePreview.setAdjustViewBounds(true);templatePreview.setScaleType(ImageView.ScaleType.FIT_CENTER);templatePreview.setBackgroundColor(Color.WHITE);root.addView(Ui.previewFrame(this,templatePreview,"识别到的模板预览"),new LinearLayout.LayoutParams(-1,Ui.dp(this,340)));
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button save=Ui.button(this,"保存到我的模板"),print=Ui.primaryButton(this,"预览确认并打印");save.setOnClickListener(v->saveTemplate());print.setOnClickListener(v->printTemplate());actions.addView(save,new LinearLayout.LayoutParams(0,Ui.dp(this,54),1));actions.addView(print,new LinearLayout.LayoutParams(0,Ui.dp(this,54),1));root.addView(actions);return sc;
    }

    private String codeFor(int index){return "qrint://template/builtin/"+index;}
    private void generateCode(){if(templates==null||codePreview==null)return;try{int i=Math.max(0,templates.getSelectedItemPosition());Bitmap next=BarcodeUtil.make(codeFor(i),BarcodeFormat.QR_CODE,320,320);Bitmap old=qr;qr=next;codePreview.setImageBitmap(next);if(old!=null&&old!=next&&!old.isRecycled())old.recycle();Ui.pulse(codePreview);}catch(Throwable e){Ui.toast(this,"模板码生成失败："+e.getMessage());}}
    private void printCode(){if(qr==null)generateCode();if(qr==null)return;try{RasterEncoder.Raster r=RasterEncoder.encodePreserveMargins(qr,128,RasterEncoder.Dither.THRESHOLD);PrinterManager.get(this).print(this,new PrinterManager.BitmapHolder(r),(ok,msg)->{if(!"已取消打印".equals(msg))Ui.toast(this,msg);if(ok)HistoryStore.addRaster(this,"模板码",BuiltInTemplateActivity.templateName(templates.getSelectedItemPosition()),r);});}catch(Throwable e){Ui.toast(this,"模板码打印失败："+e.getMessage());}}
    private void camera(){Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);try{startActivityForResult(i,CAMERA);}catch(Throwable e){Ui.toast(this,"没有可用相机应用");}}
    private void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");try{startActivityForResult(i,PICK);}catch(Throwable e){Ui.toast(this,"无法打开图片选择器");}}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null)return;try{Bitmap b=null;if(req==PICK&&data.getData()!=null)b=ImageLoader.load(this,data.getData());else if(req==CAMERA&&data.getExtras()!=null){Object o=data.getExtras().get("data");if(o instanceof Bitmap)b=(Bitmap)o;}if(b!=null)decode(b);}catch(Throwable e){Ui.toast(this,"识别失败："+e.getMessage());}}
    private void decode(Bitmap b)throws Exception{Result r=BarcodeDecodeUtil.decode(b);Bitmap old=source;source=b;sourcePreview.setImageBitmap(b);if(old!=null&&old!=b&&!old.isRecycled())old.recycle();String text=r.getText()==null?"":r.getText().trim();String prefix="qrint://template/builtin/";if(!text.startsWith(prefix))throw new Exception("这不是 Qrint 模板码");int i=Integer.parseInt(text.substring(prefix.length()));if(i<0||i>=BuiltInTemplateActivity.templateCount())throw new Exception("模板编号超出当前版本范围");selected=i;Bitmap next=BuiltInTemplateActivity.makeTemplate(i);Bitmap prev=template;template=next;templatePreview.setImageBitmap(next);if(prev!=null&&prev!=next&&!prev.isRecycled())prev.recycle();state.setText("已识别："+BuiltInTemplateActivity.templateName(i)+" · 可保存或打印");Ui.pulse(templatePreview);}
    private void saveTemplate(){if(template==null||selected<0){Ui.toast(this,"请先扫描模板码");return;}try{TemplateStore.save(this,BuiltInTemplateActivity.templateName(selected),template);Ui.toast(this,"已保存到我的模板");}catch(Throwable e){Ui.toast(this,"保存失败："+e.getMessage());}}
    private void printTemplate(){if(template==null||selected<0){Ui.toast(this,"请先扫描模板码");return;}try{RasterEncoder.Raster r=RasterEncoder.encode(template,180,RasterEncoder.Dither.THRESHOLD);PrinterManager.get(this).print(this,new PrinterManager.BitmapHolder(r),(ok,msg)->{if(!"已取消打印".equals(msg))Ui.toast(this,msg);if(ok)HistoryStore.addRaster(this,"扫码取模",BuiltInTemplateActivity.templateName(selected),r);});}catch(Throwable e){Ui.toast(this,"模板打印失败："+e.getMessage());}}
    @Override protected void onDestroy(){for(Bitmap b:new Bitmap[]{qr,source,template})if(b!=null&&!b.isRecycled())b.recycle();qr=source=template=null;super.onDestroy();}
}
