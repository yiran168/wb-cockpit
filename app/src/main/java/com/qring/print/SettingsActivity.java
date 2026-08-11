package com.qring.print;

import android.app.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class SettingsActivity extends Activity {
    private ImageView preview;private Bitmap previewBitmap;private Spinner appearance,printSoundMode;private CheckBox reduceMotion,haptic,printSoundEnabled;private SeekBar printSoundVolume;private boolean appearanceReady;
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());}

    private View build(){
        Ui.resolvePalette(this);
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setBackgroundColor(Ui.BG);ScrollView sc=new ScrollView(this);sc.setFillViewport(true);LinearLayout root=Ui.page(this,"设置");sc.addView(root);shell.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        int density=getSharedPreferences("settings",0).getInt("density",1),threshold=getSharedPreferences("settings",0).getInt("threshold",190),copies=getSharedPreferences("settings",0).getInt("copies",1);int originalAppearance=getSharedPreferences("settings",0).getInt("appearance",0);

        root.addView(Ui.section(this,"打印效果","所有任务最终都会进入同一套 384 点真实点阵确认层。"));
        preview=new ZoomablePreviewView(this);preview.setBackgroundColor(Color.WHITE);root.addView(Ui.previewFrame(this,preview,"阈值实时预览 · 双指缩放 / 拖动"),new LinearLayout.LayoutParams(-1,Ui.dp(this,240)));
        LinearLayout printCard=Ui.card(this);
        TextView dLabel=Ui.hint(this,"打印浓度："+density+" · 由打印机硬件执行");printCard.addView(dLabel);SeekBar d=new SeekBar(this);d.setMax(5);d.setProgress(density);printCard.addView(d);
        TextView tLabel=Ui.hint(this,"图片二值化阈值："+threshold);printCard.addView(tLabel);SeekBar t=new SeekBar(this);t.setMax(255);t.setProgress(threshold);printCard.addView(t);
        TextView cLabel=Ui.hint(this,"默认打印份数："+copies);printCard.addView(cLabel);SeekBar c=new SeekBar(this);c.setMax(19);c.setProgress(Math.max(0,copies-1));printCard.addView(c);root.addView(printCard);
        d.setOnSeekBarChangeListener(new SimpleSeek(v->dLabel.setText("打印浓度："+v+" · 由打印机硬件执行")));t.setOnSeekBarChangeListener(new SimpleSeek(v->{tLabel.setText("图片二值化阈值："+v);updatePreview(v);}));c.setOnSeekBarChangeListener(new SimpleSeek(v->cLabel.setText("默认打印份数："+(v+1))));

        root.addView(Ui.section(this,"外观与交互","外观不仅换色：柔光纸张会改变背景层次、圆角与卡片悬浮感；墨夜模式会降低高亮和阴影。切换后当前页立即刷新，其它已打开页面在返回时也会自动刷新。"));
        LinearLayout ux=Ui.card(this);ux.addView(Ui.hint(this,"外观模式"));appearance=new Spinner(this);appearance.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"跟随系统","柔光纸张","墨夜深色"}));appearance.setSelection(Math.max(0,Math.min(2,originalAppearance)));ux.addView(appearance);
        reduceMotion=new CheckBox(this);reduceMotion.setText("减少动效（旧设备 / 低性能设备推荐）");reduceMotion.setChecked(getSharedPreferences("settings",0).getBoolean("reduce_motion",false));ux.addView(reduceMotion);
        haptic=new CheckBox(this);haptic.setText("关键操作触感反馈");haptic.setChecked(getSharedPreferences("settings",0).getBoolean("haptic",true));ux.addView(haptic);
        Button testHaptic=Ui.button(this,"测试触感反馈");testHaptic.setOnClickListener(v->{getSharedPreferences("settings",0).edit().putBoolean("haptic",haptic.isChecked()).apply();Ui.haptic(v);Ui.toast(this,haptic.isChecked()?"已触发一次短振动；若仍无感觉，请检查系统触感/振动设置":"触感反馈当前已关闭");});ux.addView(testHaptic);
        TextView responsive=Ui.hint(this,"手机使用紧凑双列；平板 / 折叠屏自动增加边距和网格列数。低内存设备会自动关闭非必要动画，但不会关闭触感反馈。 ");responsive.setPadding(0,Ui.dp(this,8),0,0);ux.addView(responsive);root.addView(ux);
        appearance.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){if(!appearanceReady){appearanceReady=true;return;}int old=getSharedPreferences("settings",0).getInt("appearance",0);if(old!=pos){getSharedPreferences("settings",0).edit().putInt("appearance",pos).apply();getWindow().getDecorView().postDelayed(()->{if(!isFinishing())recreate();},80);}}});

        root.addView(Ui.section(this,"打印点击音效","点击最终“确认打印”时播放。内置 10 种离线合成音，可固定选择、随机从 10 种中挑选，或每次程序随机生成一个短音效；不会影响蓝牙打印数据。"));
        LinearLayout soundCard=Ui.card(this);
        printSoundEnabled=new CheckBox(this);printSoundEnabled.setText("启用打印点击音效");printSoundEnabled.setChecked(getSharedPreferences("settings",0).getBoolean(PrintSound.KEY_ENABLED,true));soundCard.addView(printSoundEnabled);
        soundCard.addView(Ui.hint(this,"音效样式"));printSoundMode=new Spinner(this);printSoundMode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,PrintSound.MODE_NAMES));printSoundMode.setSelection(Math.max(0,Math.min(PrintSound.RANDOM_GENERATED,getSharedPreferences("settings",0).getInt(PrintSound.KEY_MODE,0))));soundCard.addView(printSoundMode);
        int soundVol=getSharedPreferences("settings",0).getInt(PrintSound.KEY_VOLUME,62);TextView soundVolLabel=Ui.hint(this,"音效音量："+soundVol+"%");soundCard.addView(soundVolLabel);printSoundVolume=new SeekBar(this);printSoundVolume.setMax(100);printSoundVolume.setProgress(soundVol);soundCard.addView(printSoundVolume);printSoundVolume.setOnSeekBarChangeListener(new SimpleSeek(v->soundVolLabel.setText("音效音量："+v+"%")));
        Button testSound=Ui.button(this,"试听当前打印音效");testSound.setOnClickListener(v->{int mode=printSoundMode.getSelectedItemPosition();int vol=printSoundVolume.getProgress();PrintSound.play(this,mode,vol);Ui.haptic(v);});soundCard.addView(testSound);
        root.addView(soundCard);

        root.addView(Ui.section(this,"版本说明与开源致谢","突出展示开源参考来源、使用说明和当前版本修复内容。"));
        Button release=Ui.button(this,"查看 v1.5.1 说明 / 感谢 Thisko 开源");release.setOnClickListener(v->Ui.openFromCard(this,v,()->startActivity(new android.content.Intent(this,ReleaseInfoActivity.class))));root.addView(release);

        root.addView(Ui.section(this,"纸张 / 标签尺寸与设备","机器的红外传感器只负责有纸/缺纸，不能识别纸宽、标签宽或标签长度。APP 由用户声明 10–57mm 纸宽；连续纸长度按内容自动结束，标签纸宽/长可手动设置。所有值换算为 203DPI 点阵，并统一进入最终 384-dot 预览。"));
        Button paper=Ui.button(this,"纸张 / 标签宽高 / 内容宽度 / 203DPI 校准");paper.setOnClickListener(v->startActivity(new android.content.Intent(this,PaperSettingsActivity.class)));root.addView(paper);
        Button custom=Ui.button(this,"打开自定义打印画布");custom.setOnClickListener(v->startActivity(new android.content.Intent(this,CanvasEditorActivity.class)));root.addView(custom);
        Button disconnect=Ui.button(this,"断开当前打印机");disconnect.setOnClickListener(v->{PrinterManager.get(this).disconnect();Ui.toast(this,"已请求断开");});root.addView(disconnect);

        Button save=Ui.primaryButton(this,"保存全部设置");save.setOnClickListener(v->{int nextAppearance=appearance.getSelectedItemPosition();getSharedPreferences("settings",0).edit().putInt("density",d.getProgress()).putInt("threshold",t.getProgress()).putInt("copies",c.getProgress()+1).putInt("appearance",nextAppearance).putBoolean("reduce_motion",reduceMotion.isChecked()).putBoolean("haptic",haptic.isChecked()).putBoolean(PrintSound.KEY_ENABLED,printSoundEnabled.isChecked()).putInt(PrintSound.KEY_MODE,printSoundMode.getSelectedItemPosition()).putInt(PrintSound.KEY_VOLUME,printSoundVolume.getProgress()).apply();Ui.haptic(v);Ui.toast(this,"设置已保存并应用");});root.addView(save);
        root.addView(Ui.hint(this,"APK 使用独立应用 ID com.yiran168.cuotiprint，避免与其它使用 com.qring.print 的测试包/同名软件被系统误认为“升级”。"));
        updatePreview(threshold);shell.addView(Ui.bottomNav(this,SettingsActivity.class));return shell;
    }

    private void updatePreview(int threshold){if(preview==null)return;try{Bitmap b=Bitmap.createBitmap(384,180,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);p.setTextSize(28);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText("错题打印 · 57mm / 203DPI",18,38,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(18);c.drawText("384 dots · 字体 / 线条 / 图片阈值",18,68,p);for(int x=0;x<320;x++){int g=(int)(255*x/319f);p.setColor(Color.rgb(g,g,g));c.drawLine(32+x,90,32+x,142,p);}RasterEncoder.Raster r=RasterEncoder.encode(b,threshold,RasterEncoder.Dither.THRESHOLD);r=PrinterManager.get(this).previewRaster(r);Bitmap next=RasterEncoder.toBitmap(r,500);Bitmap old=previewBitmap;previewBitmap=next;preview.setImageBitmap(next);Ui.pulse(preview);if(old!=null&&old!=next&&!old.isRecycled())old.recycle();}catch(Throwable ignored){}}
    interface IntConsumer{void accept(int v);}static class SimpleSeek implements SeekBar.OnSeekBarChangeListener{final IntConsumer c;SimpleSeek(IntConsumer c){this.c=c;}public void onProgressChanged(SeekBar s,int p,boolean f){c.accept(p);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}}
    @Override protected void onDestroy(){if(previewBitmap!=null&&!previewBitmap.isRecycled())previewBitmap.recycle();previewBitmap=null;super.onDestroy();}
    @Override public boolean dispatchTouchEvent(MotionEvent e){if(Ui.handleTabSwipe(this,SettingsActivity.class,e))return true;return super.dispatchTouchEvent(e);}

}
