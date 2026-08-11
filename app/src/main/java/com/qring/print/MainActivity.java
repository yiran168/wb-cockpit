package com.qring.print;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity implements PrinterManager.SnapshotListener {
    private TextView status,detail,statePill;
    private PrinterManager printer;
    private boolean autoReconnectTried;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);printer=PrinterManager.get(this);setContentView(build());
        if(!Compat.hasBluetoothPermissions(this))Compat.requestBluetoothPermissions(this);handleIncoming(getIntent());
    }
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleIncoming(i);}
    @Override protected void onResume(){super.onResume();printer.addListener(this);onSnapshot(printer.snapshot());if(!autoReconnectTried&&Compat.hasBluetoothPermissions(this)&&printer.enabled()&&!printer.connected()){autoReconnectTried=true;printer.reconnectLast(this,(ok,msg)->{});}}
    @Override protected void onPause(){printer.removeListener(this);super.onPause();}

    private View build(){
        Ui.resolvePalette(this);
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setBackgroundColor(Ui.BG);
        ScrollView sc=new ScrollView(this);sc.setClipToPadding(false);sc.setFillViewport(true);
        LinearLayout root=Ui.page(this,"错题打印");sc.addView(root);shell.addView(sc,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout hero=Ui.card(this);int heroStart=Ui.isDark()?0xff27294c:0xfff0f0ff,heroEnd=Ui.CARD;GradientDrawable heroBg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{heroStart,heroEnd});heroBg.setCornerRadius(Ui.dp(this,24));heroBg.setStroke(Ui.dp(this,1),Ui.BORDER);hero.setBackground(heroBg);hero.setPadding(Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,16));
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);status=new TextView(this);status.setText("准备连接打印机");status.setTextSize(20);status.setTextColor(Ui.TEXT);status.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);copy.addView(status);detail=Ui.hint(this,"连接后会显示电量、状态、型号与固件信息");copy.addView(detail);top.addView(copy,new LinearLayout.LayoutParams(0,-2,1));statePill=Ui.pill(this,"未连接",Ui.WARNING);top.addView(statePill);hero.addView(top);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button connect=Ui.primaryButton(this,"扫描并连接"),refresh=Ui.button(this,"刷新状态");connect.setOnClickListener(v->startActivity(new Intent(this,DevicePickerActivity.class)));refresh.setOnClickListener(v->{if(!ensureBt())return;Ui.pulse(statePill);printer.refresh(this,(ok,msg)->Ui.toast(this,msg));});LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,Ui.dp(this,52),1);ap.setMargins(0,Ui.dp(this,8),Ui.dp(this,6),0);actions.addView(connect,ap);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,Ui.dp(this,52),1);bp.setMargins(Ui.dp(this,6),Ui.dp(this,8),0,0);actions.addView(refresh,bp);hero.addView(actions);root.addView(hero);

        root.addView(Ui.section(this,"快捷创作","所有打印入口都带最终 384 点黑白预览，确认后才会发送。"));
        LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);root.addView(grid);
        addAdaptiveCards(grid,
                Ui.featureCard(this,"Aa","文本","排版 · 字体 · 竖排",v->go(TextPrintActivity.class)),
                Ui.featureCard(this,"▧","图片","照片 · 抖动 · 旋转",v->go(ImagePrintActivity.class)),
                Ui.featureCard(this,"PDF","PDF 文档","裁白边 · 分页 · 批打",v->go(PdfPrintActivity.class)),
                Ui.featureCard(this,"DOC","Office","Word · PPT · Excel",v->go(OfficePrintActivity.class)),
                Ui.featureCard(this,"QR","二维码","网址 · 文本 · 容错级别",v->go(QrCodeActivity.class)),
                Ui.featureCard(this,"|||","条形码","一维码 · 二维条码 · GS1",v->go(BarcodePrintActivity.class)),
                Ui.featureCard(this,"✦","自定义打印","自由拖拽 · 缩放 · 精确位置 · 图层",v->go(CanvasEditorActivity.class)),
                Ui.featureCard(this,"表","数据批打","Excel / CSV · 变量",v->go(BatchPrintActivity.class)),
                Ui.featureCard(this,"#","序列批打","2 / 10 / 16 / 26 / 36",v->go(SerialPrintActivity.class)));

        root.addView(Ui.section(this,"智能工具","把常用任务缩短到两三步完成。"));
        LinearLayout smart=Ui.card(this);smart.addView(navRow("⌗","扫码打印","识别条码后直接生成标签",ScanCodeActivity.class));smart.addView(divider());smart.addView(navRow("DB","商品信息库","扫码查商品 · 自定义字段",ProductDatabaseActivity.class));smart.addView(divider());smart.addView(navRow("WWW","网页打印","网页预览 · 整页热敏输出",WebPrintActivity.class));root.addView(smart);

        root.addView(Ui.section(this,"资料与设备","模板、记录与设备信息都保存在本地。"));
        LinearLayout local=Ui.card(this);local.addView(navRow("P","我的打印机","保存 · 重命名 · 快速重连",MyDevicesActivity.class));local.addView(divider());local.addView(navRow("T","模板库","内置模板 + 我的模板",BuiltInTemplateActivity.class));local.addView(divider());local.addView(navRow("⌁","扫码取模","Qrint 模板码 · 本地行业模板",TemplateCodeActivity.class));local.addView(divider());local.addView(navRow("↻","打印历史","缩略图 · 重打 · 复制模板",HistoryActivity.class));local.addView(divider());local.addView(navRow("⇅","备份迁移","设置 · 模板 · 历史 · 商品库",BackupActivity.class));local.addView(divider());local.addView(navRow("✓","兼容诊断","状态 · 协议 · 384点测试页",DiagnosticsActivity.class));root.addView(local);

        TextView footer=Ui.hint(this,"Android 5.0+ · API 36 · 203DPI / 384-dot 打印头 · 10–57mm 用户校准纸宽 · 纯 Java SPP");footer.setGravity(Gravity.CENTER);footer.setPadding(0,Ui.dp(this,16),0,Ui.dp(this,10));root.addView(footer);

        shell.addView(bottomBar());return shell;
    }


    @Override public boolean dispatchTouchEvent(MotionEvent e){if(Ui.handleTabSwipe(this,MainActivity.class,e))return true;return super.dispatchTouchEvent(e);}
    private void addAdaptiveCards(LinearLayout grid,View... cards){int cols=Ui.isWide(this)?3:2;for(int i=0;i<cards.length;i+=cols){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.TOP);for(int c=0;c<cols;c++){int index=i+c;LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,Ui.isWide(this)?176:184),1);lp.setMargins(c==0?0:Ui.dp(this,6),0,c==cols-1?0:Ui.dp(this,6),0);if(index<cards.length)row.addView(cards[index],lp);else{Space spacer=new Space(this);row.addView(spacer,lp);}}grid.addView(row);}}
    private View divider(){View v=new View(this);v.setBackgroundColor(Ui.BORDER);v.setLayoutParams(new LinearLayout.LayoutParams(-1,Ui.dp(this,1)));return v;}
    private View navRow(String mark,String title,String sub,Class<?> c){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,Ui.dp(this,7),0,Ui.dp(this,7));TextView icon=Ui.pill(this,mark,Ui.PRIMARY);row.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,52),Ui.dp(this,34)));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);TextView t=new TextView(this);t.setText(title);t.setTextSize(15);t.setTextColor(Ui.TEXT);t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);TextView s=Ui.hint(this,sub);s.setPadding(0,0,0,0);tx.addView(t);tx.addView(s);row.addView(tx,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=new TextView(this);arrow.setText("›");arrow.setTextSize(28);arrow.setTextColor(Ui.MUTED);row.addView(arrow);row.setOnClickListener(v->go(c));Ui.pressMotion(row);return row;}
    private View bottomBar(){return Ui.bottomNav(this,MainActivity.class);}
    private void go(Class<?> c){startActivity(new Intent(this,c));}
    private boolean ensureBt(){if(!printer.available()){Ui.toast(this,"此设备没有可用蓝牙硬件");return false;}if(!Compat.hasBluetoothPermissions(this)){Compat.requestBluetoothPermissions(this);return false;}return true;}

    @Override public void onSnapshot(PrinterSnapshot s){runOnUiThread(()->{if(isFinishing()||isDestroyed()||status==null)return;boolean ok=s.connected;status.setText(ok?(s.deviceName==null||s.deviceName.isEmpty()?"打印机已连接":s.deviceName):"准备连接打印机");statePill.setText(ok?(s.status==null?"已连接":s.status.faultMessage()==null?"状态正常":"需检查"):"未连接");statePill.setTextColor(ok?Ui.SUCCESS:Ui.WARNING);StringBuilder b=new StringBuilder();if(ok){if(s.battery!=null)b.append("电量 ").append(s.battery).append("% · ");b.append(s.summary());if(!s.model.isEmpty())b.append("\n").append(s.model);if(!s.firmware.isEmpty())b.append(" · 固件 ").append(s.firmware);}else b.append("连接后自动读取电量、纸张状态、型号与固件信息");detail.setText(b.toString());});}

    private void handleIncoming(Intent i){if(i==null)return;String action=i.getAction(),type=i.getType();if(type==null)return;if(Intent.ACTION_SEND.equals(action)&&type.equals("text/plain")){CharSequence shared=i.getCharSequenceExtra(Intent.EXTRA_TEXT);if(shared!=null){Intent n=new Intent(this,TextPrintActivity.class);n.putExtra("incoming_text",shared.toString());startActivity(n);i.setAction(null);return;}}Uri u=null;if(Intent.ACTION_SEND.equals(action)){u=Compat.parcelableExtra(i,Intent.EXTRA_STREAM,Uri.class);}else if(Intent.ACTION_VIEW.equals(action))u=i.getData();if(u==null)return;Class<?> c=null;if(type.equals("application/pdf"))c=PdfPrintActivity.class;else if(type.startsWith("image/"))c=ImagePrintActivity.class;else if(type.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")||type.equals("text/csv")||type.equals("text/comma-separated-values"))c=BatchPrintActivity.class;else if(type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")||type.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")||type.equals("text/plain"))c=OfficePrintActivity.class;if(c!=null){Intent n=new Intent(this,c);n.setData(u);n.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);n.putExtra("incoming_uri",u.toString());startActivity(n);i.setAction(null);}}
}
