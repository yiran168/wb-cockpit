package com.qring.print;

import android.app.*;
import android.graphics.*;
import android.os.*;
import android.text.*;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import com.google.zxing.BarcodeFormat;

/** Barcodes other than QR, explicitly split into 1D and 2D categories. */
public class BarcodePrintActivity extends Activity {
    private static final String[] ONE_LABELS={"Code 128","Code 39","Code 93","EAN-13","EAN-8","UPC-A","UPC-E","ITF","Codabar","GS1-128（FNC1）"};
    private static final BarcodeFormat[] ONE_FORMATS={BarcodeFormat.CODE_128,BarcodeFormat.CODE_39,BarcodeFormat.CODE_93,BarcodeFormat.EAN_13,BarcodeFormat.EAN_8,BarcodeFormat.UPC_A,BarcodeFormat.UPC_E,BarcodeFormat.ITF,BarcodeFormat.CODABAR,BarcodeFormat.CODE_128};
    private static final String[] TWO_LABELS={"Data Matrix","PDF417","Aztec"};
    private static final BarcodeFormat[] TWO_FORMATS={BarcodeFormat.DATA_MATRIX,BarcodeFormat.PDF_417,BarcodeFormat.AZTEC};
    private EditText content;private Spinner category,kind;private CheckBox showContent;private ZoomablePreviewView preview;private TextView rule,meta;private Bitmap bitmap,previewBitmap;

    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());updateKinds();generate();}
    private View build(){
        ScrollView sc=new ScrollView(this);LinearLayout root=Ui.page(this,"条形码");sc.addView(root);
        LinearLayout input=Ui.card(this);
        category=new Spinner(this);category.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"一维条码 · 商品 / 物流 / 编号","二维条码 · Data Matrix / PDF417 / Aztec"}));input.addView(category);
        kind=new Spinner(this);input.addView(kind);
        content=new EditText(this);content.setHint("输入编码内容");content.setSingleLine(false);content.setMinLines(2);content.setMaxLines(6);input.addView(content);
        rule=Ui.hint(this,"");input.addView(rule);
        showContent=new CheckBox(this);showContent.setText("一维条码下方显示原始内容");showContent.setChecked(true);input.addView(showContent);root.addView(input);
        preview=new ZoomablePreviewView(this);preview.setBackgroundColor(Color.WHITE);root.addView(Ui.previewFrame(this,preview,"条码实时预览"),new LinearLayout.LayoutParams(-1,Ui.dp(this,350)));meta=Ui.hint(this,"请输入内容");root.addView(meta);
        Button fill=Ui.button(this,"填入符合当前规则的示例");fill.setOnClickListener(v->{content.setText(BarcodeUtil.sample(currentFormat()));content.setSelection(content.length());Ui.haptic(v);});root.addView(fill);
        Button print=Ui.primaryButton(this,"预览确认并打印");print.setOnClickListener(v->doPrint());root.addView(print);
        category.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){updateKinds();generate();}});
        kind.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){updateRuleAndKeyboard();generate();}});
        content.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){generate();}public void afterTextChanged(Editable e){}});
        showContent.setOnCheckedChangeListener((b,c)->generate());return sc;
    }

    private boolean twoD(){return category!=null&&category.getSelectedItemPosition()==1;}
    private BarcodeFormat currentFormat(){int i=kind==null?0:Math.max(0,kind.getSelectedItemPosition());return twoD()?TWO_FORMATS[Math.min(i,TWO_FORMATS.length-1)]:ONE_FORMATS[Math.min(i,ONE_FORMATS.length-1)];}
    private String currentLabel(){int i=kind==null?0:Math.max(0,kind.getSelectedItemPosition());return twoD()?TWO_LABELS[Math.min(i,TWO_LABELS.length-1)]:ONE_LABELS[Math.min(i,ONE_LABELS.length-1)];}
    private boolean gs1(){return !twoD()&&kind!=null&&kind.getSelectedItemPosition()==ONE_LABELS.length-1;}
    private void updateKinds(){if(kind==null)return;kind.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,twoD()?TWO_LABELS:ONE_LABELS));kind.setSelection(0);showContent.setVisibility(twoD()?View.GONE:View.VISIBLE);updateRuleAndKeyboard();}
    private void updateRuleAndKeyboard(){if(rule==null||content==null)return;BarcodeFormat f=currentFormat();String r=gs1()?"GS1-128：使用 Code 128 + FNC1。可用 | 作为字段分隔符；例如 (01)6901234567892|(10)LOT01":BarcodeUtil.rule(f);rule.setText(r+"\n示例："+BarcodeUtil.sample(f));boolean digits=f==BarcodeFormat.EAN_13||f==BarcodeFormat.EAN_8||f==BarcodeFormat.UPC_A||f==BarcodeFormat.UPC_E||f==BarcodeFormat.ITF;content.setInputType(digits?InputType.TYPE_CLASS_NUMBER:(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE));}

    private void generate(){
        if(content==null)return;String raw=content.getText().toString();if(raw.trim().isEmpty()){clearPreview();if(meta!=null)meta.setText("请输入内容。切换制式时不会再自动拿错误长度的默认字符串去生成，因此不会出现截图中的 ITF / EAN / UPC 位数报错。" );return;}
        try{BarcodeFormat f=currentFormat();String prepared;if(gs1()){String plain=BarcodeUtil.prepare(raw,BarcodeFormat.CODE_128);prepared="\u00f1"+plain.replace("|","\u00f1");}else prepared=BarcodeUtil.prepare(raw,f);boolean matrix=f==BarcodeFormat.DATA_MATRIX||f==BarcodeFormat.AZTEC;int h=matrix?320:(f==BarcodeFormat.PDF_417?180:150);Bitmap code=BarcodeUtil.make(prepared,f,360,h);Bitmap next=(!twoD()&&showContent.isChecked())?BarcodeUtil.withCaption(code,raw,20*getResources().getDisplayMetrics().scaledDensity):code;if(next!=code&&!code.isRecycled())code.recycle();Bitmap old=bitmap;bitmap=next;if(old!=null&&old!=bitmap&&!old.isRecycled())old.recycle();showFinalRaster();meta.setText(currentLabel()+" · 规则校验通过 · "+raw.length()+" 字符 · 当前预览已经套用纸张/标签宽度规则");}
        catch(Throwable e){clearPreview();if(meta!=null)meta.setText("当前内容不能生成 "+currentLabel()+"："+BarcodeUtil.friendly(e));}
    }
    private void showFinalRaster(){try{RasterEncoder.Raster raw=RasterEncoder.encodePreserveMargins(bitmap,128,RasterEncoder.Dither.THRESHOLD);RasterEncoder.Raster shown=PrinterManager.get(this).previewRaster(raw);Bitmap next=RasterEncoder.toBitmap(shown,1400);Bitmap old=previewBitmap;previewBitmap=next;if(preview!=null){preview.setImageBitmap(next);preview.reset();Ui.pulse(preview);}if(old!=null&&old!=next&&!old.isRecycled())old.recycle();}catch(Throwable e){if(meta!=null)meta.setText("预览生成失败："+BarcodeUtil.friendly(e));}}
    private void clearPreview(){Bitmap old=bitmap;bitmap=null;Bitmap op=previewBitmap;previewBitmap=null;if(preview!=null)preview.setImageDrawable(null);if(old!=null&&!old.isRecycled())old.recycle();if(op!=null&&!op.isRecycled())op.recycle();}
    private void doPrint(){if(bitmap==null)generate();if(bitmap==null){Ui.toast(this,"当前内容还不符合条码规则");return;}RasterEncoder.Raster r=RasterEncoder.encodePreserveMargins(bitmap,128,RasterEncoder.Dither.THRESHOLD);String label=currentLabel(),summary=content.getText().toString();PrinterManager.get(this).print(this,new PrinterManager.BitmapHolder(r),(ok,msg)->{if(!"已取消打印".equals(msg))Ui.toast(this,msg);if(ok)HistoryStore.addRaster(this,"条形码",label+" · "+summary,r);});}
    @Override protected void onDestroy(){clearPreview();super.onDestroy();}
}
