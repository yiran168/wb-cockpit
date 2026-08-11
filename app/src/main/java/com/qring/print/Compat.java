package com.qring.print;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;

final class Compat {
    static final int REQ_BT = 7001;
    static final int REQ_LOCAL_NETWORK = 7002;
    private static final String PERM_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK";
    private Compat() {}

    static boolean hasBluetoothPermissions(Activity a) {
        if (Build.VERSION.SDK_INT >= 31) {
            return Api31.hasBluetoothPermissions(a);
        }
        if (Build.VERSION.SDK_INT >= 23) {
            return a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    static void requestBluetoothPermissions(Activity a) {
        if (Build.VERSION.SDK_INT >= 31) {
            Api31.requestBluetoothPermissions(a);
        } else if (Build.VERSION.SDK_INT >= 23) {
            a.requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, REQ_BT);
        }
    }

    /** Local-network permission is enforced only when both the OS and this app target API 37+. */
    static boolean hasLocalNetworkPermission(Activity a){
        if(Build.VERSION.SDK_INT<37 || a.getApplicationInfo().targetSdkVersion<37)return true;
        return Api37.hasLocalNetworkPermission(a);
    }

    static void requestLocalNetworkPermission(Activity a){
        if(Build.VERSION.SDK_INT>=37 && a.getApplicationInfo().targetSdkVersion>=37)Api37.requestLocalNetworkPermission(a);
    }

    @SuppressWarnings("deprecation")
    static <T> T parcelableExtra(Intent i,String key,Class<T> type){
        if(i==null)return null;
        if(Build.VERSION.SDK_INT>=33)return Api33.parcelableExtra(i,key,type);
        Object value=i.getParcelableExtra(key);
        return type.isInstance(value)?type.cast(value):null;
    }

    /** Bluetooth discovery/bond broadcasts come from outside this app, so API 33+ uses EXPORTED. */
    @SuppressWarnings("deprecation")
    static void registerBluetoothReceiver(Context c, BroadcastReceiver r, IntentFilter f){
        if(Build.VERSION.SDK_INT>=33)Api33.registerExported(c,r,f);
        else c.registerReceiver(r,f);
    }

    @android.annotation.TargetApi(31)
    private static final class Api31{
        static boolean hasBluetoothPermissions(Activity a){
            return a.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED
                    && a.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED;
        }
        static void requestBluetoothPermissions(Activity a){
            a.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN},REQ_BT);
        }
    }

    @android.annotation.TargetApi(33)
    private static final class Api33{
        static <T> T parcelableExtra(Intent i,String key,Class<T> type){return i.getParcelableExtra(key,type);}
        static void registerExported(Context c,BroadcastReceiver r,IntentFilter f){c.registerReceiver(r,f,Context.RECEIVER_EXPORTED);}
    }

    @android.annotation.TargetApi(37)
    private static final class Api37{
        static boolean hasLocalNetworkPermission(Activity a){
            return a.checkSelfPermission(PERM_LOCAL_NETWORK)==PackageManager.PERMISSION_GRANTED;
        }
        static void requestLocalNetworkPermission(Activity a){
            a.requestPermissions(new String[]{PERM_LOCAL_NETWORK},REQ_LOCAL_NETWORK);
        }
    }
}
