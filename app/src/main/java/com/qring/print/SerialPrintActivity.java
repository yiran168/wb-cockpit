package com.qring.print;

import android.app.*;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Bundle;
import android.widget.*;
import com.google.zxing.BarcodeFormat;

public class SerialPrintActivity extends Activity {
    private static final String PREF="serial_resume";
    private EditText start,step,count,prefix,suffix,padding;
    private Spinner base,kind;
    private TextView sample,progress;private ImageView preview;private Bitmap previewBitmap;

    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());}

    private android.view.View build(){
        ScrollView sc=new ScrollView(this);LinearLayout root=Ui.page(this,"序列号批量打印");sc.addView(root);
        root.addView(Ui.hint(this,"使用说明：序列批打支持 2/10/16/26/36 进制、前后缀、步长、固定宽度，并支持暂停后从下一张继续。26 进制可选 0=A 或常用的 1=A。"));
        start=input(root,"起始值，例如 1");step=input(root,"步长，例如 1");count=input(root,"数量，最多 200");prefix=input(root,"前缀，可空");suffix=input(root,"后缀，可空");padding=input(root,"固定宽度/补零位数，0=不补");
        start.setText("1");step.setText("1");count.setText("10");padding.setText("0");
        base=new Spinner(this);base.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"二进制","十进制","十六进制","26进制 0=A","26进制 1=A","36进制"}));base.setSelection(1);root.addView(base);
        kind=new Spinner(this);kind.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"纯文字","二维码 + 文字","Code128 + 文字"}));root.addView(kind);
        Button p=Ui.button(this,"更新示例");p.setOnClickListener(v->updateSample());root.addView(p);sample=Ui.hint(this,"");root.addView(sample);
        preview=new ZoomablePreviewView(this);preview.setAdjustViewBounds(true);preview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);preview.setBackgroundColor(Color.WHITE);root.addView(Ui.previewFrame(this,preview,"首条序列号实时预览"),new LinearLayout.LayoutParams(-1,Ui.dp(this,280)));
        Button print=Ui.primaryButton(this,"批量打印序列号");print.setOnClickListener(v->printFrom(0));root.addView(print);
        Button pause=Ui.button(this,"暂停当前批打");pause.setOnClickListener(v->{PrinterManager.get(this).requestCancelBatch();progress.setText("正在请求暂停，将在当前标签完成后停止…");});root.addView(pause);
        Button resume=Ui.button(this,"继续上次序列号批打");resume.setOnClickListener(v->resume());root.addView(resume);
        progress=Ui.hint(this,hasResume()?"检测到上次未完成的序列号批打任务":"");root.addView(progress);
        android.text.TextWatcher tw=new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int d){}public void onTextChanged(CharSequence s,int a,int b,int c){updateSample();}public void afterTextChanged(android.text.Editable e){}};start.addTextChangedListener(tw);step.addTextChangedListener(tw);count.addTextChangedListener(tw);prefix.addTextChangedListener(tw);suffix.addTextChangedListener(tw);padding.addTextChangedListener(tw);android.widget.AdapterView.OnItemSelectedListener sl=new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){updateSample();}public void onNothingSelected(android.widget.AdapterView<?> p){}};base.setOnItemSelectedListener(sl);kind.setOnItemSelectedListener(sl);updateSample();return sc;
    }

    private EditText input(LinearLayout root,String hint){EditText e=new EditText(this);e.setHint(hint);root.addView(e,new LinearLayout.LayoutParams(-1,Ui.dp(this,54)));return e;}
    private long number(EditText e,long d){try{return Long.parseLong(e.getText().toString().trim());}catch(Exception x){return d;}}
    private int width(){return (int)Math.max(0,Math.min(64,number(padding,0)));}
    private String serial(long v){return serial(v,base.getSelectedItemPosition(),width(),prefix.getText().toString(),suffix.getText().toString());}
    private String serial(long v,int mode,int width,String pre,String suf){return pre+SerialUtil.format(v,mode,width)+suf;}

    private void updateSample(){
        long s=number(start,1),d=number(step,1);int n=(int)Math.max(1,Math.min(200,number(count,10)));
        StringBuilder b=new StringBuilder("示例：");for(int i=0;i<Math.min(5,n);i++){if(i>0)b.append("  ·  ");b.append(serial(s+d*i));}sample.setText(b.toString());updatePreview();
    }

    private void updatePreview(){if(preview==null)return;try{String text=serial(number(start,1));Bitmap b=render(text,kind.getSelectedItemPosition());Bitmap old=previewBitmap;previewBitmap=b;preview.setImageBitmap(b);if(old!=null&&old!=b&&!old.isRecycled())old.recycle();Ui.pulse(preview);}catch(Throwable e){preview.setImageDrawable(null);}}

    private Bitmap render(String text,int kindPos)throws Exception{
        if(kindPos==0)return RasterEncoder.textBitmap(text,32*getResources().getDisplayMetrics().scaledDensity,true,1,8);
        BarcodeFormat f=kindPos==1?BarcodeFormat.QR_CODE:BarcodeFormat.CODE_128;
        Bitmap code=BarcodeUtil.make(text,f,kindPos==1?260:360,kindPos==1?260:120);
        Bitmap caption=RasterEncoder.textBitmap(text,24*getResources().getDisplayMetrics().scaledDensity,false,1,4);
        int h=code.getHeight()+caption.getHeight()+12;Bitmap out=Bitmap.createBitmap(384,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(out);c.drawColor(Color.WHITE);float x=(384-code.getWidth())/2f;c.drawBitmap(code,x,0,null);c.drawBitmap(caption,0,code.getHeight()+12,null);code.recycle();caption.recycle();return out;
    }

    private void printFrom(int offset){
        final long s=number(start,1),d=number(step,1);final int n=(int)Math.max(1,Math.min(200,number(count,10)));
        if(offset<0||offset>=n){clearResume();Ui.toast(this,"序列号任务已经完成");return;}
        final int mode=base.getSelectedItemPosition(),kindPos=kind.getSelectedItemPosition(),w=width();
        final String pre=prefix.getText().toString(),suf=suffix.getText().toString();final int begin=offset,remain=n-offset;
        saveConfig(begin,s,d,n,mode,kindPos,w,pre,suf);progress.setText("准备批量打印，剩余 "+remain+" 个序列号…");
        PrinterManager.get(this).printGenerated(this,remain,index->{
            int original=begin+index;String text=serial(s+d*original,mode,w,pre,suf);Bitmap b=render(text,kindPos);
            try{return kindPos==0?RasterEncoder.encode(b,128,RasterEncoder.Dither.THRESHOLD):RasterEncoder.encodePreserveMargins(b,128,RasterEncoder.Dither.THRESHOLD);}finally{if(!b.isRecycled())b.recycle();}
        },(done,total,msg)->{int next=begin+done;saveNext(next);progress.setText("本次已完成 "+done+" / "+total+" · 总进度 "+next+" / "+n+" · "+msg);},(ok,msg)->{
            Ui.toast(this,msg);progress.setText(msg);if(ok){clearResume();HistoryStore.add(this,"序列号",serial(s,mode,w,pre,suf)+" … 共 "+n+" 个");}
        });
    }

    private void resume(){
        SharedPreferences p=getSharedPreferences(PREF,0);if(!p.getBoolean("active",false)){Ui.toast(this,"没有可继续的序列号任务");return;}
        start.setText(Long.toString(p.getLong("start",1)));step.setText(Long.toString(p.getLong("step",1)));count.setText(Integer.toString(p.getInt("count",10)));padding.setText(Integer.toString(p.getInt("width",0)));prefix.setText(p.getString("prefix",""));suffix.setText(p.getString("suffix",""));base.setSelection(p.getInt("base",1));kind.setSelection(p.getInt("kind",0));updateSample();printFrom(p.getInt("next",0));
    }
    private boolean hasResume(){return getSharedPreferences(PREF,0).getBoolean("active",false);}
    private void saveConfig(int next,long s,long d,int n,int mode,int kindPos,int w,String pre,String suf){getSharedPreferences(PREF,0).edit().putBoolean("active",true).putInt("next",next).putLong("start",s).putLong("step",d).putInt("count",n).putInt("base",mode).putInt("kind",kindPos).putInt("width",w).putString("prefix",pre).putString("suffix",suf).apply();}
    private void saveNext(int next){SharedPreferences p=getSharedPreferences(PREF,0);int n=p.getInt("count",0);p.edit().putBoolean("active",next<n).putInt("next",next).apply();}
    private void clearResume(){getSharedPreferences(PREF,0).edit().clear().apply();}
}
