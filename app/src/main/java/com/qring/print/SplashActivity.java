package com.qring.print;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Branded cover screen so cold start never presents a blank white window. */
public class SplashActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable openMain=this::launch;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        Ui.resolvePalette(this);
        setContentView(buildCover());
        handler.postDelayed(openMain,520);
    }

    private View buildCover(){
        FrameLayout root=new FrameLayout(this);
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Ui.isDark()?0xff101219:0xfff8f7ff, Ui.isDark()?0xff202344:0xffececff});
        root.setBackground(bg);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setGravity(Gravity.CENTER_HORIZONTAL);content.setPadding(Ui.dp(this,28),Ui.dp(this,48),Ui.dp(this,28),Ui.dp(this,36));
        FrameLayout mark=new FrameLayout(this);mark.setBackground(Ui.rounded(Ui.PRIMARY,Ui.dp(this,28)));mark.setElevation(Ui.dp(this,8));
        TextView paper=new TextView(this);paper.setText("错\n题");paper.setTextColor(Color.WHITE);paper.setTextSize(30);paper.setGravity(Gravity.CENTER);paper.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));mark.addView(paper,new FrameLayout.LayoutParams(-1,-1));
        content.addView(mark,new LinearLayout.LayoutParams(Ui.dp(this,104),Ui.dp(this,104)));
        TextView title=new TextView(this);title.setText("错题打印");title.setTextColor(Ui.TEXT);title.setTextSize(31);title.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));title.setGravity(Gravity.CENTER);title.setPadding(0,Ui.dp(this,24),0,Ui.dp(this,6));content.addView(title);
        TextView sub=Ui.hint(this,"Qring / BeePrt · 57 mm · 203 DPI · 384 dots");sub.setTextSize(14);sub.setGravity(Gravity.CENTER);content.addView(sub);
        LinearLayout ribbon=Ui.card(this);ribbon.setGravity(Gravity.CENTER_HORIZONTAL);ribbon.setPadding(Ui.dp(this,16),Ui.dp(this,12),Ui.dp(this,16),Ui.dp(this,12));
        TextView r1=Ui.hint(this,"本地排版 · 实时点阵预览 · SPP 直连打印");r1.setGravity(Gravity.CENTER);ribbon.addView(r1);
        TextView r2=Ui.hint(this,"不依赖厂商云服务");r2.setGravity(Gravity.CENTER);r2.setTextColor(Ui.SUCCESS);ribbon.addView(r2);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.setMargins(0,Ui.dp(this,28),0,0);content.addView(ribbon,rp);
        root.addView(content,new FrameLayout.LayoutParams(-1,-1));
        return root;
    }

    private void launch(){
        if(isFinishing())return;
        Intent incoming=getIntent();
        Intent next=new Intent(incoming);next.setClass(this,MainActivity.class);next.removeCategory(Intent.CATEGORY_LAUNCHER);        try{startActivity(next);}catch(Throwable ignored){startActivity(new Intent(this,MainActivity.class));}
        finish();overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }

    @Override public void onBackPressed(){handler.removeCallbacks(openMain);super.onBackPressed();}
    @Override protected void onDestroy(){handler.removeCallbacks(openMain);super.onDestroy();}
}
