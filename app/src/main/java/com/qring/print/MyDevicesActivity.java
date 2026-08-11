package com.qring.print;

import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MyDevicesActivity extends Activity {
    private LinearLayout list;
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());}
    @Override protected void onResume(){super.onResume();render();}

    private View build(){
        ScrollView sc=new ScrollView(this);sc.setClipToPadding(false);LinearLayout root=Ui.page(this,"我的打印机");sc.addView(root);
        LinearLayout intro=Ui.card(this);intro.addView(Ui.hint(this,"连接成功的打印机会自动保存到本机。移除只删除 App 记录，不会取消 Android 系统蓝牙配对。"));
        Button discover=Ui.primaryButton(this,"扫描 / 添加打印机");discover.setOnClickListener(v->startActivity(new Intent(this,DevicePickerActivity.class)));intro.addView(discover);root.addView(intro);
        root.addView(Ui.section(this,"已保存设备","点击连接即可快速恢复打印；支持本地备注名称。"));
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);return sc;
    }

    private void render(){
        list.removeAllViews();List<SavedPrinterStore.Item> items=SavedPrinterStore.list(this);PrinterSnapshot snap=PrinterManager.get(this).snapshot();
        if(items.isEmpty()){
            list.addView(Ui.emptyState(this,"BT","还没有保存的打印机","先扫描并连接一次，设备会自动出现在这里；以后可以一键重连。","扫描打印机",v->startActivity(new Intent(this,DevicePickerActivity.class))));return;
        }
        for(SavedPrinterStore.Item i:items){
            boolean current=snap.connected && i.address!=null && i.address.equalsIgnoreCase(snap.address);
            LinearLayout card=Ui.card(this);LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);head.setGravity(Gravity.CENTER_VERTICAL);
            TextView icon=Ui.pill(this,current?"✓":"P",current?Ui.SUCCESS:Ui.PRIMARY);head.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,54),Ui.dp(this,36)));
            LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);copy.setPadding(Ui.dp(this,10),0,0,0);
            TextView title=new TextView(this);title.setText(SavedPrinterStore.display(i));title.setTextSize(17);title.setTextColor(Ui.TEXT);title.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);copy.addView(title);
            TextView meta=Ui.hint(this,(i.address==null?"":i.address)+(i.name==null||i.name.isEmpty()?"":" · "+i.name));meta.setPadding(0,0,0,0);copy.addView(meta);head.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
            head.addView(Ui.pill(this,current?"已连接":"已保存",current?Ui.SUCCESS:Ui.MUTED));card.addView(head);
            if(current){TextView live=Ui.hint(this,"实时状态 · "+snap.summary()+(snap.battery==null?"":" · 电量 "+snap.battery+"%"));live.setTextColor(snap.status!=null&&snap.status.faultMessage()!=null?Ui.WARNING:Ui.SUCCESS);card.addView(live);}
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button connect=Ui.primaryButton(this,current?"重新连接":"连接"),rename=Ui.button(this,"重命名"),remove=Ui.button(this,"移除");
            connect.setOnClickListener(v->connect(i));rename.setOnClickListener(v->rename(i));remove.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("从应用中移除“"+SavedPrinterStore.display(i)+"”？").setPositiveButton("移除",(d,w)->{SavedPrinterStore.remove(this,i.address);render();}).setNegativeButton("取消",null).show());
            row.addView(connect,new LinearLayout.LayoutParams(0,Ui.dp(this,50),1));row.addView(rename,new LinearLayout.LayoutParams(0,Ui.dp(this,50),1));row.addView(remove,new LinearLayout.LayoutParams(0,Ui.dp(this,50),1));card.addView(row);list.addView(card);
        }
    }

    private void connect(SavedPrinterStore.Item i){
        if(!Compat.hasBluetoothPermissions(this)){Compat.requestBluetoothPermissions(this);return;}
        try{BluetoothManager bm=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);BluetoothAdapter a=bm!=null?bm.getAdapter():BluetoothAdapter.getDefaultAdapter();if(a==null){Ui.toast(this,"设备没有蓝牙硬件");return;}BluetoothDevice d=a.getRemoteDevice(i.address);PrinterManager.get(this).connect(this,d,(ok,msg)->{Ui.toast(this,msg);render();});}
        catch(Throwable e){Ui.toast(this,"连接失败："+e.getMessage());}
    }
    private void rename(SavedPrinterStore.Item i){EditText e=new EditText(this);e.setText(SavedPrinterStore.display(i));new AlertDialog.Builder(this).setTitle("打印机名称").setView(e).setPositiveButton("保存",(d,w)->{SavedPrinterStore.rename(this,i.address,e.getText().toString());render();}).setNegativeButton("取消",null).show();}
}
