package com.qring.print;

import android.app.*;
import android.graphics.*;
import android.os.*;
import android.view.View;
import android.widget.*;

public class DiagnosticsActivity extends Activity implements PrinterManager.SnapshotListener {
    private TextView report;private ImageView preview;private Bitmap previewBitmap;
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());}
    @Override protected void onResume(){super.onResume();PrinterManager.get(this).addListener(this);refreshLocal();}
    @Override protected void onPause(){PrinterManager.get(this).removeListener(this);super.onPause();}
    private View build(){
        ScrollView sc=new ScrollView(this);LinearLayout root=Ui.page(this,"兼容性 / 打印机诊断");sc.addView(root);
        report=Ui.hint(this,"");report.setTextSize(15);root.addView(report);
        preview=new ZoomablePreviewView(this);preview.setAdjustViewBounds(true);preview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);preview.setBackgroundColor(Color.WHITE);previewBitmap=makeTestBitmap();preview.setImageBitmap(previewBitmap);root.addView(Ui.previewFrame(this,preview,"384 点诊断页预览"),new LinearLayout.LayoutParams(-1,Ui.dp(this,280)));
        Button status=Ui.button(this,"读取打印机状态 / 电量");status.setOnClickListener(v->PrinterManager.get(this).refresh(this,(ok,msg)->Ui.toast(this,msg)));root.addView(status);
        Button test=Ui.button(this,"打印 384 点诊断测试页");test.setOnClickListener(v->printTest());root.addView(test);
        root.addView(Ui.hint(this,"测试页包含边界框、灰阶抖动、细线和文字，可用于检查偏移、浓度、缺线与蓝牙数据稳定性。"));
        return sc;
    }
    private void refreshLocal(){PrinterManager p=PrinterManager.get(this);PrinterSnapshot s=p.snapshot();StringBuilder b=new StringBuilder();
        b.append("Android API：").append(Build.VERSION.SDK_INT).append("\n");
        b.append("设备：").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        if(Build.VERSION.SDK_INT>=21)b.append("ABI：").append(java.util.Arrays.toString(Build.SUPPORTED_ABIS)).append("\n");
        b.append("应用架构：纯 Java/Kotlin 兼容链路（无 native .so）\n");
        b.append("蓝牙硬件：").append(p.available()?"可用":"不可用").append(" · 蓝牙：").append(p.enabled()?"已开启":"未开启").append("\n");
        b.append("蓝牙权限：").append(Compat.hasBluetoothPermissions(this)?"已授予":"未授予").append("\n");
        b.append("打印机：").append(s.summary()).append("\n");
        b.append("协议参数：384 dots / 48 bytes per row / 1024-byte chunks / SPP");
        report.setText(b.toString());
    }
    @Override public void onSnapshot(PrinterSnapshot s){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())refreshLocal();});}
    private Bitmap makeTestBitmap(){
        Bitmap b=Bitmap.createBitmap(384,520,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRect(1,1,382,518,p);
        for(int x=16;x<368;x+=16)c.drawLine(x,55,x,130,p);
        p.setStyle(Paint.Style.FILL);p.setTextSize(24);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText("QrintPrint 384点诊断页",18,38,p);
        p.setTextSize(18);p.setTypeface(Typeface.DEFAULT);c.drawText("SPP / Qring 私有协议",18,165,p);
        for(int i=0;i<8;i++){p.setColor(Color.rgb(i*32,i*32,i*32));c.drawRect(18+i*43,195,18+i*43+38,255,p);}
        p.setColor(Color.BLACK);for(int y=290;y<=410;y+=20){p.setStrokeWidth(Math.max(1,(y-270)/20f));c.drawLine(20,y,364,y,p);}
        p.setTextSize(18);c.drawText("四边 / 灰阶 / 细线 / 文字",18,470,p);return b;
    }
    private void printTest(){
        try{Bitmap b=makeTestBitmap();RasterEncoder.Raster r;try{r=RasterEncoder.encode(b,190,RasterEncoder.Dither.FLOYD);}finally{if(b!=null&&!b.isRecycled())b.recycle();}PrinterManager.get(this).print(this,new PrinterManager.BitmapHolder(r),(ok,msg)->{if(!"已取消打印".equals(msg))Ui.toast(this,msg);if(ok)HistoryStore.addRaster(this,"诊断","384点诊断测试页",r);});}
        catch(Throwable e){Ui.toast(this,"诊断页生成失败："+e.getMessage());}
    }
    @Override protected void onDestroy(){if(previewBitmap!=null&&!previewBitmap.isRecycled())previewBitmap.recycle();previewBitmap=null;super.onDestroy();}
}
