package com.qring.print;

import android.app.*;
import android.graphics.*;
import android.os.*;
import android.text.*;
import android.view.View;
import android.widget.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/** Dedicated QR-code screen. Kept separate from one-dimensional/product barcodes. */
public class QrCodeActivity extends Activity {
    private EditText content;private Spinner level;private ZoomablePreviewView preview;private TextView meta;private Bitmap bitmap,previewBitmap;
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());generate();}

    private View build(){
        ScrollView sc=new ScrollView(this);LinearLayout root=Ui.page(this,"二维码");sc.addView(root);
        LinearLayout input=Ui.card(this);content=new EditText(this);content.setHint("输入文字、网址、Wi-Fi 信息、编号等");content.setMinLines(3);content.setMaxLines(8);input.addView(content);
        level=new Spinner(this);level.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"容错 L · 容量最大","容错 M · 推荐","容错 Q · 较高","容错 H · 最高"}));level.setSelection(1);input.addView(level);
        input.addView(Ui.hint(this,"二维码支持中文和任意 UTF-8 文本。实际打印宽度不在这里写死，由“纸张 / 标签尺寸”中的内容宽度规则控制；最终打印前会再次显示同一份 384-dot 点阵预览。"));root.addView(input);
        preview=new ZoomablePreviewView(this);preview.setBackgroundColor(Color.WHITE);root.addView(Ui.previewFrame(this,preview,"二维码实时预览"),new LinearLayout.LayoutParams(-1,Ui.dp(this,360)));meta=Ui.hint(this,"请输入内容");root.addView(meta);
        Button print=Ui.primaryButton(this,"预览确认并打印");print.setOnClickListener(v->doPrint());root.addView(print);
        content.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){generate();}public void afterTextChanged(Editable e){}});
        level.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){generate();}});
        return sc;
    }

    private ErrorCorrectionLevel ecc(){int p=level==null?1:level.getSelectedItemPosition();return p==0?ErrorCorrectionLevel.L:p==2?ErrorCorrectionLevel.Q:p==3?ErrorCorrectionLevel.H:ErrorCorrectionLevel.M;}
    private void generate(){
        String s=content==null?"":content.getText().toString();
        if(s.trim().isEmpty()){clearPreview();if(meta!=null)meta.setText("请输入二维码内容；空内容不会再拿默认英文去硬生成其它制式。" );return;}
        try{Bitmap next=BarcodeUtil.make(s,BarcodeFormat.QR_CODE,340,340,ecc());Bitmap old=bitmap;bitmap=next;if(old!=null&&old!=bitmap&&!old.isRecycled())old.recycle();showFinalRaster();if(meta!=null)meta.setText("QR · "+s.length()+" 字符 · 容错 "+ecc()+" · 当前预览已经套用纸张/标签宽度规则");}
        catch(Throwable e){clearPreview();if(meta!=null)meta.setText("二维码生成失败："+BarcodeUtil.friendly(e));}
    }
    private void showFinalRaster(){try{RasterEncoder.Raster raw=RasterEncoder.encodePreserveMargins(bitmap,128,RasterEncoder.Dither.THRESHOLD);RasterEncoder.Raster shown=PrinterManager.get(this).previewRaster(raw);Bitmap next=RasterEncoder.toBitmap(shown,1400);Bitmap old=previewBitmap;previewBitmap=next;if(preview!=null){preview.setImageBitmap(next);preview.reset();Ui.pulse(preview);}if(old!=null&&old!=next&&!old.isRecycled())old.recycle();}catch(Throwable e){if(meta!=null)meta.setText("预览生成失败："+BarcodeUtil.friendly(e));}}
    private void clearPreview(){Bitmap old=bitmap;bitmap=null;Bitmap op=previewBitmap;previewBitmap=null;if(preview!=null)preview.setImageDrawable(null);if(old!=null&&!old.isRecycled())old.recycle();if(op!=null&&!op.isRecycled())op.recycle();}
    private void doPrint(){if(bitmap==null)generate();if(bitmap==null){Ui.toast(this,"请先输入可生成的二维码内容");return;}RasterEncoder.Raster r=RasterEncoder.encodePreserveMargins(bitmap,128,RasterEncoder.Dither.THRESHOLD);PrinterManager.get(this).print(this,new PrinterManager.BitmapHolder(r),(ok,msg)->{if(!"已取消打印".equals(msg))Ui.toast(this,msg);if(ok)HistoryStore.addRaster(this,"二维码",content.getText().toString(),r);});}
    @Override protected void onDestroy(){clearPreview();super.onDestroy();}
}
