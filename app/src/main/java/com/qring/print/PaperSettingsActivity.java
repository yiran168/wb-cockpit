package com.qring.print;

import android.app.*;
import android.graphics.*;
import android.os.Bundle;
import android.text.*;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import java.util.Locale;

/** User-calibrated media geometry. The printer only detects paper-present/paper-out. */
public class PaperSettingsActivity extends Activity {
    private EditText paperWidth,labelLength,customContentWidth,feedBefore,feedAfter,shutdown,xOffset,yOffset;
    private Spinner unit,direction,mediaMode,widthMode,paperAlign,contentAlign,verticalAlign;
    private SeekBar contentScale;
    private CheckBox trimSides,autoLength;
    private ImageView preview;
    private TextView previewMeta,algorithmHint;
    private Bitmap previewBitmap;

    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());updateFieldStates();updatePreview();}

    private View build(){
        ScrollView sc=new ScrollView(this);LinearLayout root=Ui.page(this,"纸张 / 标签尺寸");sc.addView(root);

        LinearLayout sensor=Ui.card(this);
        sensor.addView(Ui.pill(this,"红外仅检测有纸 / 缺纸",Ui.WARNING));
        sensor.addView(Ui.hint(this,"这类机器没有自动测量纸宽、标签宽度或标签长度的传感器。APP 改用“用户尺寸 + 203DPI 点数换算 + 内容边界分析”排版：所有来源先生成黑白点阵，再按你声明的纸宽/标签宽度和内容宽度缩放，最终预览与真正发送共用同一份 384-dot 结果。"));
        root.addView(sensor);

        preview=new ZoomablePreviewView(this);preview.setBackgroundColor(Color.WHITE);
        root.addView(Ui.previewFrame(this,preview,"物理纸张 / 可用打印区 / 内容宽度示意"),new LinearLayout.LayoutParams(-1,Ui.dp(this,300)));
        previewMeta=Ui.hint(this,"");root.addView(previewMeta);

        LinearLayout card=Ui.card(this);
        mediaMode=new Spinner(this);mediaMode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"连续纸 / 小票（长度由内容决定）","标签纸（宽度和长度都手动设置）"}));mediaMode.setSelection("label".equals(sp().getString("media_mode","continuous"))?1:0);card.addView(mediaMode);
        unit=new Spinner(this);unit.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"毫米 mm","英寸 inch"}));String saved=sp().getString("paper_unit","mm");unit.setSelection("inch".equals(saved)?1:0);card.addView(unit);

        float widthMm=PrinterProfile.clampPaperWidthMm(sp().getInt("paper_width_tenths_mm",570)/10f);
        paperWidth=field(card,"实际纸张 / 标签宽度（10–57mm）",formatUnit(widthMm),true);
        int oldH=Math.max(0,sp().getInt("paper_height_mm",0));float lenMm=sp().getInt("label_length_tenths_mm",oldH*10)/10f;
        labelLength=field(card,"标签长度（例如 30mm；连续纸无需设置）",lenMm<=0?formatUnit(30):formatUnit(lenMm),true);

        widthMode=new Spinner(this);widthMode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"内容宽度：自动（按文字 / 图片有效内容）","内容宽度：铺满当前纸张可打印区","内容宽度：自定义毫米"}));widthMode.setSelection(clamp(sp().getInt("content_width_mode",0),0,2));card.addView(widthMode);
        float defaultContent=Math.min(widthMm,PrinterProfile.printableWidthMm());float customMm=sp().getInt("content_width_tenths_mm",Math.round(defaultContent*10))/10f;
        customContentWidth=field(card,"自定义内容宽度",formatUnit(customMm),true);

        paperAlign=new Spinner(this);paperAlign.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"装纸位置：居中","装纸位置：靠左","装纸位置：靠右"}));paperAlign.setSelection(clamp(sp().getInt("paper_alignment",0),0,2));card.addView(paperAlign);
        contentAlign=new Spinner(this);contentAlign.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"内容对齐：居中","内容对齐：靠左","内容对齐：靠右"}));contentAlign.setSelection(clamp(sp().getInt("content_alignment",0),0,2));card.addView(contentAlign);
        verticalAlign=new Spinner(this);verticalAlign.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"标签内垂直：顶部","标签内垂直：居中","标签内垂直：底部"}));verticalAlign.setSelection(clamp(sp().getInt("content_vertical_alignment",0),0,2));card.addView(verticalAlign);

        trimSides=new CheckBox(this);trimSides.setText("自动识别并裁掉内容左右空白");trimSides.setChecked(sp().getBoolean("trim_side_blank",true));card.addView(trimSides);
        autoLength=new CheckBox(this);autoLength.setText("连续纸自动去掉末尾空白，让长度由实际内容决定");autoLength.setChecked(sp().getBoolean("auto_trim_length",true));card.addView(autoLength);

        TextView scaleLabel=Ui.hint(this,"内容二次缩放："+sp().getInt("content_scale_percent",100)+"%（宽高同比例）");card.addView(scaleLabel);contentScale=new SeekBar(this);contentScale.setMax(175);contentScale.setProgress(Math.max(0,Math.min(175,sp().getInt("content_scale_percent",100)-25)));contentScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){scaleLabel.setText("内容二次缩放："+(25+p)+"%（宽高同比例）");updatePreview();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});card.addView(contentScale);

        direction=new Spinner(this);direction.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"打印方向 0°","打印方向 180°"}));direction.setSelection(get("print_direction",0)==180?1:0);card.addView(direction);
        feedBefore=field(card,"打印前走纸点数",Integer.toString(get("feed_before",10)),false);
        feedAfter=field(card,"打印后走纸点数（标签纸建议从 0 开始校准）",Integer.toString(get("feed_after",100)),false);
        xOffset=fieldSigned(card,"水平校准 X（-96..96 dots）",Integer.toString(get("x_offset",0)));
        yOffset=fieldSigned(card,"垂直校准 Y（-1000..1000 dots）",Integer.toString(get("y_offset",0)));
        shutdown=field(card,"自动关机秒数（0=不修改）",Integer.toString(get("shutdown_seconds",0)),false);
        root.addView(card);

        algorithmHint=Ui.hint(this,"");algorithmHint.setPadding(0,Ui.dp(this,6),0,Ui.dp(this,8));root.addView(algorithmHint);

        android.widget.AdapterView.OnItemSelectedListener listener=new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){updateFieldStates();updatePreview();}};
        mediaMode.setOnItemSelectedListener(listener);widthMode.setOnItemSelectedListener(listener);paperAlign.setOnItemSelectedListener(listener);contentAlign.setOnItemSelectedListener(listener);verticalAlign.setOnItemSelectedListener(listener);direction.setOnItemSelectedListener(listener);
        unit.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){boolean first=true;int last=unit.getSelectedItemPosition();public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){if(first){first=false;last=pos;updatePreview();return;}convertField(paperWidth,last,pos);convertField(labelLength,last,pos);convertField(customContentWidth,last,pos);last=pos;updatePreview();}});
        TextWatcher tw=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){updatePreview();}public void afterTextChanged(Editable e){}};
        for(EditText e:new EditText[]{paperWidth,labelLength,customContentWidth,feedBefore,feedAfter,xOffset,yOffset})e.addTextChangedListener(tw);
        trimSides.setOnCheckedChangeListener((b,c)->updatePreview());autoLength.setOnCheckedChangeListener((b,c)->updatePreview());

        Button reset=Ui.button(this,"恢复 57mm 连续纸推荐参数");reset.setOnClickListener(v->{mediaMode.setSelection(0);unit.setSelection(0);paperWidth.setText("57");widthMode.setSelection(0);customContentWidth.setText(String.format(Locale.US,"%.1f",PrinterProfile.printableWidthMm()));paperAlign.setSelection(0);contentAlign.setSelection(0);verticalAlign.setSelection(0);trimSides.setChecked(true);autoLength.setChecked(true);contentScale.setProgress(75);feedBefore.setText("10");feedAfter.setText("100");xOffset.setText("0");yOffset.setText("0");direction.setSelection(0);Ui.haptic(v);});root.addView(reset);
        Button save=Ui.primaryButton(this,"保存尺寸与排版规则");save.setOnClickListener(v->save());root.addView(save);
        return sc;
    }

    private android.content.SharedPreferences sp(){return getSharedPreferences("settings",0);}
    private int get(String k,int d){return sp().getInt(k,d);}
    private int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
    private EditText field(LinearLayout root,String hint,String value,boolean decimal){EditText e=new EditText(this);e.setHint(hint);e.setInputType(decimal?(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL):InputType.TYPE_CLASS_NUMBER);e.setText(value);e.setTextDirection(View.TEXT_DIRECTION_LTR);root.addView(e,new LinearLayout.LayoutParams(-1,Ui.dp(this,52)));return e;}
    private EditText fieldSigned(LinearLayout root,String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);e.setText(value);e.setTextDirection(View.TEXT_DIRECTION_LTR);root.addView(e,new LinearLayout.LayoutParams(-1,Ui.dp(this,52)));return e;}
    private int intVal(EditText e,int d){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception x){return d;}}
    private double doubleVal(EditText e,double d){try{return Double.parseDouble(e.getText().toString().trim());}catch(Exception x){return d;}}
    private float toMm(EditText e,float d){double v=Math.max(0.01,doubleVal(e,d));return (float)(unit!=null&&unit.getSelectedItemPosition()==1?v*25.4:v);}
    private float currentPaperWidthMm(){return PrinterProfile.clampPaperWidthMm(toMm(paperWidth,57));}
    private float currentLabelLengthMm(){return Math.max(1f,Math.min(1000f,toMm(labelLength,30)));}
    private float currentContentWidthMm(){float max=Math.min(currentPaperWidthMm(),PrinterProfile.printableWidthMm());return Math.max(1f,Math.min(max,toMm(customContentWidth,max)));}
    private String formatUnit(float mm){return unit!=null&&unit.getSelectedItemPosition()==1?String.format(Locale.US,"%.2f",mm/25.4f):String.format(Locale.US,Math.abs(mm-Math.round(mm))<.01?"%.0f":"%.1f",mm);}
    private void convertField(EditText e,int from,int to){double v=doubleVal(e,0);if(v<=0||from==to)return;double next=from==0&&to==1?v/25.4:from==1&&to==0?v*25.4:v;e.setText(to==1?String.format(Locale.US,"%.2f",next):String.format(Locale.US,"%.1f",next));}

    private void updateFieldStates(){
        if(labelLength!=null){boolean label=mediaMode!=null&&mediaMode.getSelectedItemPosition()==1;labelLength.setEnabled(label);labelLength.setAlpha(label?1f:.42f);verticalAlign.setEnabled(label);verticalAlign.setAlpha(label?1f:.42f);autoLength.setEnabled(!label);autoLength.setAlpha(label?.42f:1f);}
        if(customContentWidth!=null){boolean custom=widthMode!=null&&widthMode.getSelectedItemPosition()==2;customContentWidth.setEnabled(custom);customContentWidth.setAlpha(custom?1f:.42f);}
    }

    private void updatePreview(){
        if(preview==null||paperWidth==null)return;
        try{
            float widthMm=currentPaperWidthMm(),labelMm=currentLabelLengthMm(),customMm=currentContentWidthMm();int paperDots=PrinterProfile.usableDotsForPaper(widthMm);int mode=widthMode==null?0:widthMode.getSelectedItemPosition();int scale=contentScale==null?100:25+contentScale.getProgress();boolean label=mediaMode!=null&&mediaMode.getSelectedItemPosition()==1;
            int contentDots=mode==1?paperDots:mode==2?Math.min(paperDots,PrinterProfile.mmToDots(customMm)):Math.min(paperDots,Math.max(80,(int)(paperDots*.62f)));contentDots=Math.max(1,Math.min(paperDots,Math.round(contentDots*scale/100f)));
            int paperStart=alignStart(384,paperDots,paperAlign==null?0:paperAlign.getSelectedItemPosition());int contentStart=paperStart+alignStart(paperDots,contentDots,contentAlign==null?0:contentAlign.getSelectedItemPosition());
            Bitmap b=Bitmap.createBitmap(384,280,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(0xfff2f3f7);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);c.drawRoundRect(new RectF(10,18,374,258),16,16,p);
            float x0=10+354*(paperStart/384f),x1=10+354*((paperStart+paperDots)/384f);p.setColor(0xffeef0ff);c.drawRect(x0,34,x1,242,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xff5b5ce2);c.drawRect(x0,34,x1,242,p);p.setStyle(Paint.Style.FILL);
            float cx0=10+354*(contentStart/384f),cx1=10+354*((contentStart+contentDots)/384f);p.setColor(0xff181b25);c.drawRect(cx0,82,cx1,92,p);c.drawRect(cx0,108,Math.min(cx1,cx0+(cx1-cx0)*.78f),116,p);c.drawRect(cx0,134,Math.min(cx1,cx0+(cx1-cx0)*.9f),142,p);
            p.setTextSize(15);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(paperDots+" dots 可用",Math.max(14,x0+6),64,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(14);c.drawText("内容 "+contentDots+" dots",Math.max(14,cx0+4),174,p);c.drawText(label?("标签长度 "+String.format(Locale.US,"%.1f",labelMm)+"mm"):"连续纸：长度由内容点阵决定",20,222,p);
            Bitmap old=previewBitmap;previewBitmap=b;preview.setImageBitmap(b);if(old!=null&&old!=b&&!old.isRecycled())old.recycle();
            String modeText=mode==0?"按内容自动":mode==1?"铺满纸张":"自定义 "+String.format(Locale.US,"%.1fmm",customMm);previewMeta.setText(String.format(Locale.US,"用户纸宽 %.1fmm → 可用 %d/%d dots · 内容宽度 %s · %d%% · %s",widthMm,paperDots,384,modeText,scale,label?String.format(Locale.US,"标签 %.1fmm 长",labelMm):"自动长度"));
            algorithmHint.setText(label?"标签算法：按你输入的宽度确定可用点数，按标签长度生成固定高度点阵；支持顶部/居中/底部垂直对齐。若内容过高，会等比例缩小以完整放入，不会悄悄裁掉。由于没有间隙/黑标尺寸传感器，首次使用某种标签需用 X/Y 与走纸量做一次校准。":"连续纸算法：默认检测黑色内容的上下左右有效边界；宽度可按内容自然大小、铺满纸张或指定毫米，长度从第一行有效内容到最后一行有效内容自动决定。预览使用的就是之后发送给打印机的最终 384-dot 点阵。" );
        }catch(Throwable ignored){}
    }

    private int alignStart(int outer,int inner,int align){inner=Math.max(1,Math.min(outer,inner));return align==1?0:align==2?outer-inner:(outer-inner)/2;}

    private void save(){
        float widthMm=currentPaperWidthMm(),labelMm=currentLabelLengthMm(),customMm=currentContentWidthMm();int pre=clamp(intVal(feedBefore,10),0,2000),post=clamp(intVal(feedAfter,100),0,4000),xo=clamp(intVal(xOffset,0),-96,96),yo=clamp(intVal(yOffset,0),-1000,1000),sd=clamp(intVal(shutdown,0),0,65535),dir=direction.getSelectedItemPosition()==1?180:0,scale=25+contentScale.getProgress();boolean inch=unit.getSelectedItemPosition()==1,label=mediaMode.getSelectedItemPosition()==1;
        sp().edit().putString("paper_unit",inch?"inch":"mm").putString("media_mode",label?"label":"continuous")
                .putInt("paper_width_tenths_mm",Math.round(widthMm*10f)).putInt("label_length_tenths_mm",Math.round(labelMm*10f)).putInt("paper_height_mm",label?Math.round(labelMm):0)
                .putInt("content_width_mode",widthMode.getSelectedItemPosition()).putInt("content_width_tenths_mm",Math.round(customMm*10f))
                .putInt("paper_alignment",paperAlign.getSelectedItemPosition()).putInt("content_alignment",contentAlign.getSelectedItemPosition()).putInt("content_vertical_alignment",verticalAlign.getSelectedItemPosition())
                .putBoolean("trim_side_blank",trimSides.isChecked()).putBoolean("auto_trim_length",autoLength.isChecked())
                .putInt("content_scale_percent",scale).putInt("feed_before",pre).putInt("feed_after",post).putInt("x_offset",xo).putInt("y_offset",yo).putInt("print_direction",dir).putInt("shutdown_seconds",sd).apply();
        Ui.haptic(preview);if(sd>0&&PrinterManager.get(this).connected())PrinterManager.get(this).setShutdownTime(this,sd,(ok,msg)->Ui.toast(this,msg));else Ui.toast(this,"尺寸已保存："+String.format(Locale.US,"%.1fmm",widthMm)+(label?" × "+String.format(Locale.US,"%.1fmm",labelMm):" · 自动长度"));
    }

    @Override protected void onDestroy(){if(previewBitmap!=null&&!previewBitmap.isRecycled())previewBitmap.recycle();previewBitmap=null;super.onDestroy();}
}
