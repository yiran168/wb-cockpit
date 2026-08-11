package com.qring.print;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import java.util.WeakHashMap;

public class App extends Application implements Application.ActivityLifecycleCallbacks {
    private int started;
    private final WeakHashMap<Activity,Integer> appearanceApplied=new WeakHashMap<>();
    @Override public void onCreate(){ super.onCreate(); registerActivityLifecycleCallbacks(this); }
    private int appearance(){return getSharedPreferences("settings",0).getInt("appearance",0);}
    @Override public void onActivityStarted(Activity a){ if(started++==0) PrinterManager.get(this).setForeground(true); }
    @Override public void onActivityStopped(Activity a){ if(started>0 && --started==0) PrinterManager.get(this).setForeground(false); }
    @Override public void onActivityCreated(Activity a, Bundle b){appearanceApplied.put(a,appearance());}
    @Override public void onActivityResumed(Activity a){Integer applied=appearanceApplied.get(a);int now=appearance();if(applied!=null&&applied!=now&&!a.isFinishing()){appearanceApplied.put(a,now);a.recreate();}}
    @Override public void onActivityPaused(Activity a){}
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b){}
    @Override public void onActivityDestroyed(Activity a){appearanceApplied.remove(a);}
}
