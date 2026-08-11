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

public class ScanCodeActivity extends Activity {
    private static final int PICK=901,CAMERA=902;
    private EditText result;private ImageView sourcePreview,outputPreview;private Spinner outputKind;private Bitmap sourceBitmap,output,outputThermalPreview;
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());}
    private View build(){
        ScrollView sc=new ScrollView(this);LinearLayout root=Ui.page(this,"扫码识别");sc.addView(root);
        LinearLayout source=Ui.card(this);source.addView(Ui.hint(this,"支持 QR、Code128、Code39、EAN、UPC 等；识别完成后可继续编辑内容，再生成文字标签或二维码。"));LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button photo=Ui.button(this,"从图片识别"),cam=Ui.button(this,"相机拍照识别");photo.setOnClickListener(v->pick());cam.setOnClickListener(v->camera());row.addView(photo,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));row.addView(cam,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));source.addView(row);root.addView(source);
        sourcePreview=new ZoomablePreviewView(this);sourcePreview.setAdjustViewBounds(true);sourcePreview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);sourcePreview.setBackgroundColor(Color.WHITE);root.addView(Ui.previewFrame(this,sourcePreview,"识别图片"),new LinearLayout.LayoutParams(-1,Ui.dp(this,220)));
        LinearLayout edit=Ui.card(this);result=new EditText(this);result.setHint("识别结果，也可以手动修改");result.setMinLines(3);edit.addView(result);outputKind=new Spinner(this);outputKind.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"输出为文字标签","输出为 QR 二维码"}));edit.addView(outputKind);root.addView(edit);
        outputPreview=new ZoomablePreviewView(this);outputPreview.setAdjustViewBounds(true);outputPreview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);outputPreview.setBackgroundColor(Color.WHITE);root.addView(Ui.previewFrame(this,outputPreview,"最终标签预览"),new LinearLayout.LayoutParams(-1,Ui.dp(this,300)));
        Button print=Ui.primaryButton(this,"预览确认并打印");print.setOnClickListener(v->printOutput());root.addView(print);
        result.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){updateOutput();}public void afterTextChanged(Editable e){}});outputKind.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){updateOutput();}public void onNothingSelected(android.widget.AdapterView<?> p){}});return sc;
    }
    private void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");try{startActivityForResult(i,PICK);}catch(Throwable e){Ui.toast(this,"无法打开图片选择器");}}
    private void camera(){Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);try{startActivityForResult(i,CAMERA);}catch(Throwable e){Ui.toast(this,"没有可用相机应用");}}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null)return;try{Bitmap b=null;if(req==PICK&&data.getData()!=null)b=ImageLoader.load(this,data.getData());else if(req==CAMERA&&data.getExtras()!=null){Object o=data.getExtras().get("data");if(o instanceof Bitmap)b=(Bitmap)o;}if(b!=null)decode(b);}catch(Throwable e){Ui.toast(this,"识别失败："+e.getMessage());}}
    private void decode(Bitmap b)throws Exception{Result r=BarcodeDecodeUtil.decode(b);Bitmap old=sourceBitmap;sourceBitmap=b;result.setText(r.getText());sourcePreview.setImageBitmap(b);if(old!=null&&old!=b&&!old.isRecycled())old.recycle();Ui.pulse(sourcePreview);Ui.toast(this,"已识别 "+r.getBarcodeFormat());}
    private String value(){return result.getText().toString().trim();}
    private void updateOutput(){if(outputPreview==null)return;String v=value();if(v.isEmpty()){Bitmap old=output;output=null;Bitmap op=outputThermalPreview;outputThermalPreview=null;outputPreview.setImageDrawable(null);if(old!=null&&!old.isRecycled())old.recycle();if(op!=null&&!op.isRecycled())op.recycle();return;}try{Bitmap next;if(outputKind.getSelectedItemPosition()==0)next=RasterEncoder.textBitmap(v,28*getResources().getDisplayMetrics().scaledDensity,false,1,6);else{Bitmap code=BarcodeUtil.make(v,BarcodeFormat.QR_CODE,320,320);next=Bitmap.createBitmap(384,344,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(next);c.drawColor(Color.WHITE);c.drawBitmap(code,32,12,null);code.recycle();}Bitmap old=output;output=next;if(old!=null&&old!=next&&!old.isRecycled())old.recycle();RasterEncoder.Raster rr=outputKind.getSelectedItemPosition()==0?RasterEncoder.encode(next,128,RasterEncoder.Dither.THRESHOLD):RasterEncoder.encodePreserveMargins(next,128,RasterEncoder.Dither.THRESHOLD);rr=PrinterManager.get(this).previewRaster(rr);Bitmap shown=RasterEncoder.toBitmap(rr,1200);Bitmap op=outputThermalPreview;outputThermalPreview=shown;outputPreview.setImageBitmap(shown);if(op!=null&&op!=shown&&!op.isRecycled())op.recycle();Ui.pulse(outputPreview);}catch(Throwable e){Bitmap old=output;output=null;Bitmap op=outputThermalPreview;outputThermalPreview=null;if(old!=null&&!old.isRecycled())old.recycle();if(op!=null&&!op.isRecycled())op.recycle();outputPreview.setImageDrawable(null);Ui.toast(this,"预览生成失败："+e.getMessage());}}
    private void printOutput(){if(output==null)updateOutput();if(output==null){Ui.toast(this,"没有可打印内容");return;}String type=outputKind.getSelectedItemPosition()==0?"扫码文字":"扫码二维码",summary=value();RasterEncoder.Raster r=outputKind.getSelectedItemPosition()==0?RasterEncoder.encode(output,128,RasterEncoder.Dither.THRESHOLD):RasterEncoder.encodePreserveMargins(output,128,RasterEncoder.Dither.THRESHOLD);PrinterManager.get(this).print(this,new PrinterManager.BitmapHolder(r),(ok,msg)->{if(!"已取消打印".equals(msg))Ui.toast(this,msg);if(ok)HistoryStore.addRaster(this,type,summary,r);});}
    @Override protected void onDestroy(){if(sourceBitmap!=null&&!sourceBitmap.isRecycled())sourceBitmap.recycle();if(output!=null&&!output.isRecycled())output.recycle();if(outputThermalPreview!=null&&!outputThermalPreview.isRecycled())outputThermalPreview.recycle();sourceBitmap=null;output=null;outputThermalPreview=null;super.onDestroy();}
}
