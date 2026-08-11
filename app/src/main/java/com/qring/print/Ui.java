package com.qring.print;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import java.util.WeakHashMap;

/**
 * Dependency-free design system shared by every screen.
 *
 * The project intentionally avoids Compose/Material Components so the exact same APK can
 * keep the API 21 floor while targeting the stable API 36 toolchain.  All high-level screens therefore
 * obtain colors, spacing, cards, motion and preview chrome from this class.
 */
final class Ui {
    interface PrintConfirm { void confirm(int copies); }

    // Mutable palette: page() resolves Auto / Light / Dark before each screen is built.
    static int BG=Color.rgb(247,248,252);
    static int CARD=Color.WHITE;
    static int TEXT=Color.rgb(24,27,37);
    static int MUTED=Color.rgb(105,112,130);
    static int PRIMARY=Color.rgb(91,92,226);
    static int PRIMARY_DARK=Color.rgb(67,68,196);
    static int SOFT=Color.rgb(237,238,255);
    static int BORDER=Color.rgb(229,232,240);
    static int SUCCESS=Color.rgb(31,166,117);
    static int WARNING=Color.rgb(222,139,35);
    static int DANGER=Color.rgb(211,74,92);
    static int SURFACE_ALT=Color.rgb(242,243,247);
    private static boolean dark;
    private static int appearanceMode;
    private static int CARD_RADIUS=20, BUTTON_RADIUS=16, CARD_ELEVATION=1;

    private Ui() {}

    static int dp(Activity a,int v){return (int)(v*a.getResources().getDisplayMetrics().density+0.5f);}
    static boolean isWide(Activity a){return a.getResources().getConfiguration().smallestScreenWidthDp>=600;}
    static boolean isDark(){return dark;}

    private static boolean motionEnabled(Context c){
        try{
            if(c.getSharedPreferences("settings",0).getBoolean("reduce_motion",false))return false;
            ActivityManager am=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
            return am==null || !am.isLowRamDevice();
        }catch(Throwable ignored){return true;}
    }

    static void resolvePalette(Activity a){
        int mode=0;
        try{mode=a.getSharedPreferences("settings",0).getInt("appearance",0);}catch(Throwable ignored){}
        boolean systemDark=(a.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        appearanceMode=mode;
        dark=mode==2 || (mode==0 && systemDark);
        if(dark){
            BG=Color.rgb(16,18,25); CARD=Color.rgb(25,28,38); TEXT=Color.rgb(244,246,251);
            MUTED=Color.rgb(167,174,194); PRIMARY=Color.rgb(139,140,246); PRIMARY_DARK=Color.rgb(112,113,226);
            SOFT=Color.rgb(38,40,76); BORDER=Color.rgb(43,47,61); SUCCESS=Color.rgb(77,211,160);
            WARNING=Color.rgb(239,177,86); DANGER=Color.rgb(242,112,128); SURFACE_ALT=Color.rgb(22,25,34);
            CARD_RADIUS=18; BUTTON_RADIUS=14; CARD_ELEVATION=0;
        }else if(mode==1){
            // “柔光纸张”不是单纯换色：更暖的纸张背景、更大的卡片圆角和层次更明显的悬浮感。
            BG=Color.rgb(249,247,243); CARD=Color.rgb(255,254,251); TEXT=Color.rgb(31,29,27);
            MUTED=Color.rgb(111,105,98); PRIMARY=Color.rgb(84,82,218); PRIMARY_DARK=Color.rgb(63,61,188);
            SOFT=Color.rgb(239,237,255); BORDER=Color.rgb(229,224,216); SUCCESS=Color.rgb(29,156,109);
            WARNING=Color.rgb(213,132,32); DANGER=Color.rgb(199,70,86); SURFACE_ALT=Color.rgb(244,241,235);
            CARD_RADIUS=24; BUTTON_RADIUS=18; CARD_ELEVATION=2;
        }else{
            BG=Color.rgb(247,248,252); CARD=Color.WHITE; TEXT=Color.rgb(24,27,37);
            MUTED=Color.rgb(105,112,130); PRIMARY=Color.rgb(91,92,226); PRIMARY_DARK=Color.rgb(67,68,196);
            SOFT=Color.rgb(237,238,255); BORDER=Color.rgb(229,232,240); SUCCESS=Color.rgb(31,166,117);
            WARNING=Color.rgb(222,139,35); DANGER=Color.rgb(211,74,92); SURFACE_ALT=Color.rgb(242,243,247);
            CARD_RADIUS=20; BUTTON_RADIUS=16; CARD_ELEVATION=1;
        }
    }

    static GradientDrawable rounded(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);return g;}
    static GradientDrawable roundedStroke(int color,int strokeColor,float radius,int stroke){GradientDrawable g=rounded(color,radius);g.setStroke(stroke,strokeColor);return g;}
    static Drawable ripple(Activity a,int color,Drawable content){return new RippleDrawable(ColorStateList.valueOf(color),content,null);}

    static void prepareWindow(Activity a){
        resolvePalette(a);
        try{
            a.getWindow().setStatusBarColor(BG);a.getWindow().setNavigationBarColor(CARD);
            int flags=0;
            if(!dark && Build.VERSION.SDK_INT>=23)flags|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if(!dark && Build.VERSION.SDK_INT>=26)flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            a.getWindow().getDecorView().setSystemUiVisibility(flags);
        }catch(Throwable ignored){}
    }

    static LinearLayout page(Activity a,String title){
        prepareWindow(a);
        int side=isWide(a)?dp(a,48):dp(a,18);
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(side,dp(a,10),side,dp(a,26));GradientDrawable pageBg=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{BG,blend(SURFACE_ALT,BG,.58f)});root.setBackground(pageBg);
        root.setOnApplyWindowInsetsListener((v,insets)->{int top=insets.getSystemWindowInsetTop(),bottom=insets.getSystemWindowInsetBottom();v.setPadding(side,dp(a,10)+top,side,dp(a,26)+bottom);return insets;});
        LinearLayout head=new LinearLayout(a);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(0,dp(a,5),0,dp(a,13));
        TextView eyebrow=new TextView(a);eyebrow.setText("QRINT PRINT");eyebrow.setTextSize(10);eyebrow.setLetterSpacing(.18f);eyebrow.setTextColor(PRIMARY);eyebrow.setTypeface(Typeface.DEFAULT,Typeface.BOLD);head.addView(eyebrow);
        TextView t=new TextView(a);t.setText(title);t.setTextSize(isWide(a)?30:27);t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));t.setTextColor(TEXT);t.setPadding(0,dp(a,2),0,0);head.addView(t);
        root.addView(head);
        // Children are normally added synchronously by build(); tint the complete hierarchy next loop.
        root.post(()->tintTree(a,root));
        if(motionEnabled(a)){root.setAlpha(0f);root.setTranslationY(dp(a,10));root.animate().alpha(1f).translationY(0).setDuration(260).setInterpolator(new DecelerateInterpolator()).start();}
        return root;
    }

    static TextView section(Activity a,String title,String subtitle){
        TextView t=new TextView(a);t.setPadding(0,dp(a,20),0,dp(a,8));t.setLineSpacing(dp(a,2),1.08f);
        SpannableStringBuilder s=new SpannableStringBuilder(title);
        s.setSpan(new StyleSpan(Typeface.BOLD),0,title.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new ForegroundColorSpan(TEXT),0,title.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if(subtitle!=null&&!subtitle.isEmpty()){
            int start=s.length();s.append("\n").append(subtitle);int body=start+1;
            s.setSpan(new RelativeSizeSpan(.72f),body,s.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.setSpan(new ForegroundColorSpan(MUTED),body,s.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        t.setText(s);t.setTextSize(18);return t;
    }

    static LinearLayout card(Activity a){
        LinearLayout c=new LinearLayout(a);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(a,16),dp(a,14),dp(a,16),dp(a,14));c.setBackground(ripple(a,0x145B5CE2,roundedStroke(CARD,BORDER,dp(a,CARD_RADIUS),dp(a,1))));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(a,6),0,dp(a,8));c.setLayoutParams(p);c.setElevation(dp(a,CARD_ELEVATION));return c;
    }

    /** Paper-like preview chrome shared by every live preview. */
    static FrameLayout previewFrame(Activity a,View content,String label){
        FrameLayout outer=new FrameLayout(a);outer.setPadding(dp(a,12),dp(a,34),dp(a,12),dp(a,12));outer.setBackground(roundedStroke(SURFACE_ALT,BORDER,dp(a,22),dp(a,1)));
        TextView tag=new TextView(a);String shown=label==null?"打印预览":label;if(content instanceof ZoomablePreviewView && shown.indexOf("缩放")<0)shown += " · 双指缩放 / 拖动";tag.setText(shown);tag.setTextSize(12);tag.setTextColor(MUTED);tag.setTypeface(Typeface.DEFAULT,Typeface.BOLD);FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-2,dp(a,26),Gravity.TOP|Gravity.START);tp.leftMargin=dp(a,12);tp.topMargin=dp(a,5);tag.setGravity(Gravity.CENTER_VERTICAL);outer.addView(tag,tp);
        FrameLayout paper=new FrameLayout(a);paper.setClipToOutline(false);paper.setBackground(rounded(Color.WHITE,dp(a,12)));paper.setElevation(dp(a,dark?1:3));
        FrameLayout.LayoutParams paperLp=new FrameLayout.LayoutParams(-1,-1);paperLp.setMargins(dp(a,2),dp(a,2),dp(a,2),dp(a,2));paper.addView(content,new FrameLayout.LayoutParams(-1,-1));outer.addView(paper,paperLp);
        return outer;
    }

    static Button button(Activity a,String text){return styledButton(a,text,false);}
    static Button primaryButton(Activity a,String text){return styledButton(a,text,true);}
    static Button styledButton(Activity a,String text,boolean primary){
        Button b=new Button(a);b.setText(text);b.setAllCaps(false);b.setTextSize(15);b.setGravity(Gravity.CENTER);b.setMinHeight(0);b.setMinWidth(0);b.setPadding(dp(a,14),0,dp(a,14),0);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setStateListAnimator(null);
        if(primary){b.setTextColor(Color.WHITE);b.setBackground(ripple(a,0x25FFFFFF,rounded(PRIMARY,dp(a,BUTTON_RADIUS))));b.setElevation(dp(a,dark?0:2));}
        else{b.setTextColor(TEXT);b.setBackground(ripple(a,0x165B5CE2,roundedStroke(CARD,BORDER,dp(a,BUTTON_RADIUS),dp(a,1))));}
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(a,52));p.setMargins(0,dp(a,5),0,dp(a,5));b.setLayoutParams(p);pressMotion(b);return b;
    }

    static Button compactButton(Activity a,String text){Button b=button(a,text);b.setTextSize(13);return b;}

    static TextView hint(Activity a,String text){TextView v=new TextView(a);v.setText(text);v.setTextSize(13);v.setTextColor(MUTED);v.setLineSpacing(dp(a,2),1.08f);v.setPadding(0,dp(a,3),0,dp(a,8));return v;}

    static TextView pill(Activity a,String text,int color){TextView t=new TextView(a);t.setText(text);t.setTextSize(12);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setTextColor(color);t.setGravity(Gravity.CENTER);t.setPadding(dp(a,10),dp(a,5),dp(a,10),dp(a,5));int bg=blend(color,CARD,dark?.82f:.88f);t.setBackground(rounded(bg,dp(a,999)));return t;}

    static LinearLayout featureCard(Activity a,String mark,String title,String subtitle,View.OnClickListener click){
        LinearLayout c=card(a);c.setPadding(dp(a,14),dp(a,14),dp(a,14),dp(a,14));c.setOnClickListener(v->openFromCard(a,v,()->{if(click!=null)click.onClick(v);}));c.setMinimumHeight(dp(a,isWide(a)?150:144));
        LinearLayout top=new LinearLayout(a);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon=new TextView(a);icon.setText(mark);icon.setTextSize(mark.length()>2?13:18);icon.setTextColor(PRIMARY);icon.setGravity(Gravity.CENTER);icon.setTypeface(Typeface.DEFAULT,Typeface.BOLD);icon.setBackground(rounded(SOFT,dp(a,13)));top.addView(icon,new LinearLayout.LayoutParams(dp(a,42),dp(a,42)));
        Space spacer=new Space(a);top.addView(spacer,new LinearLayout.LayoutParams(0,1,1));TextView arrow=new TextView(a);arrow.setText("↗");arrow.setTextSize(16);arrow.setTextColor(MUTED);top.addView(arrow);c.addView(top);
        TextView t=new TextView(a);t.setText(title);t.setTextSize(16);t.setTextColor(TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(a,12),0,dp(a,4));t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);c.addView(t,new LinearLayout.LayoutParams(-1,dp(a,34)));
        TextView s=new TextView(a);s.setText(subtitle);s.setTextSize(12);s.setTextColor(MUTED);s.setMinLines(2);s.setMaxLines(2);s.setEllipsize(android.text.TextUtils.TruncateAt.END);s.setGravity(Gravity.TOP);c.addView(s,new LinearLayout.LayoutParams(-1,dp(a,46)));return c;
    }

    static View bottomNav(Activity a,Class<?> current){
        LinearLayout bar=new LinearLayout(a);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setGravity(Gravity.CENTER);int baseBottom=dp(a,8);bar.setPadding(dp(a,10),dp(a,6),dp(a,10),baseBottom);bar.setBackgroundColor(CARD);bar.setElevation(dp(a,10));
        bar.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(dp(a,10),dp(a,6),dp(a,10),baseBottom+insets.getSystemWindowInsetBottom());return insets;});
        Class<?>[] classes=new Class<?>[]{MainActivity.class,TemplateActivity.class,HistoryActivity.class,SettingsActivity.class};String[] labels={"首页","模板","记录","设置"};String[] marks={"⌂","▦","◷","⚙"};
        for(int i=0;i<classes.length;i++){final Class<?> target=classes[i];boolean selected=current==target;LinearLayout item=new LinearLayout(a);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setPadding(dp(a,4),dp(a,4),dp(a,4),dp(a,4));if(selected)item.setBackground(rounded(SOFT,dp(a,15)));TextView icon=new TextView(a);icon.setText(marks[i]);icon.setTextSize(16);icon.setGravity(Gravity.CENTER);icon.setTextColor(selected?PRIMARY:MUTED);item.addView(icon,new LinearLayout.LayoutParams(-1,dp(a,24)));TextView label=new TextView(a);label.setText(labels[i]);label.setTextSize(11);label.setGravity(Gravity.CENTER);label.setTypeface(Typeface.DEFAULT,selected?Typeface.BOLD:Typeface.NORMAL);label.setTextColor(selected?PRIMARY:MUTED);item.addView(label,new LinearLayout.LayoutParams(-1,dp(a,20)));if(!selected){item.setOnClickListener(v->navigateTab(a,current,target));pressMotion(item);}bar.addView(item,new LinearLayout.LayoutParams(0,dp(a,56),1));}
        return bar;
    }

    private static final WeakHashMap<Activity,SwipeState> TAB_SWIPES=new WeakHashMap<>();
    private static final class SwipeState{float x,y;boolean tracking,multi;}

    static void openFromCard(Activity a,View source,Runnable action){
        if(a==null||action==null)return;
        haptic(source);
        if(source==null||!motionEnabled(a)){action.run();try{a.overridePendingTransition(R.anim.zoom_enter,R.anim.fade_exit);}catch(Throwable ignored){}return;}
        source.animate().cancel();
        source.animate().scaleX(1.045f).scaleY(1.045f).alpha(.88f).setDuration(105).setInterpolator(new DecelerateInterpolator()).withEndAction(()->{
            action.run();
            try{a.overridePendingTransition(R.anim.zoom_enter,R.anim.fade_exit);}catch(Throwable ignored){}
            source.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120).start();
        }).start();
    }

    static boolean handleTabSwipe(Activity a,Class<?> current,MotionEvent e){
        if(a==null||e==null||!motionEnabled(a))return false;
        SwipeState st=TAB_SWIPES.get(a);if(st==null){st=new SwipeState();TAB_SWIPES.put(a,st);}
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){st.x=e.getRawX();st.y=e.getRawY();st.tracking=true;st.multi=false;return false;}
        if(e.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN){st.multi=true;return false;}
        if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){st.tracking=false;return false;}
        if(e.getActionMasked()!=MotionEvent.ACTION_UP||!st.tracking)return false;
        st.tracking=false;if(st.multi)return false;float dx=e.getRawX()-st.x,dy=e.getRawY()-st.y;
        if(Math.abs(dx)<dp(a,120)||Math.abs(dx)<Math.abs(dy)*1.65f)return false;
        Class<?>[] tabs={MainActivity.class,TemplateActivity.class,HistoryActivity.class,SettingsActivity.class};int idx=-1;for(int i=0;i<tabs.length;i++)if(tabs[i]==current){idx=i;break;}if(idx<0)return false;
        int next=dx<0?idx+1:idx-1;if(next<0||next>=tabs.length)return false;
        navigateTab(a,current,tabs[next]);return true;
    }

    private static void navigateTab(Activity a,Class<?> current,Class<?> target){
        if(a==null||target==null||current==target)return;
        Class<?>[] tabs={MainActivity.class,TemplateActivity.class,HistoryActivity.class,SettingsActivity.class};int from=0,to=0;for(int i=0;i<tabs.length;i++){if(tabs[i]==current)from=i;if(tabs[i]==target)to=i;}
        Intent in=new Intent(a,target);in.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);a.startActivity(in);
        try{if(to>from)a.overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);else a.overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);}catch(Throwable ignored){}
    }

    static LinearLayout emptyState(Activity a,String mark,String title,String message,String action,View.OnClickListener click){
        LinearLayout c=card(a);c.setGravity(Gravity.CENTER_HORIZONTAL);c.setPadding(dp(a,20),dp(a,24),dp(a,20),dp(a,24));
        TextView icon=pill(a,mark,PRIMARY);icon.setTextSize(18);c.addView(icon);
        TextView h=new TextView(a);h.setText(title);h.setTextSize(17);h.setTextColor(TEXT);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setGravity(Gravity.CENTER);h.setPadding(0,dp(a,12),0,dp(a,5));c.addView(h);
        TextView m=hint(a,message);m.setGravity(Gravity.CENTER);c.addView(m);
        if(action!=null&&click!=null){Button b=button(a,action);b.setOnClickListener(click);c.addView(b);}
        return c;
    }

    static void haptic(View v){
        if(v==null||!v.isEnabled())return;
        Context c=v.getContext();
        try{if(!c.getSharedPreferences("settings",0).getBoolean("haptic",true))return;}catch(Throwable ignored){}
        boolean performed=false;
        try{performed=v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);}catch(Throwable ignored){}
        if(!performed){
            try{
                Vibrator vib=(Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);
                if(vib!=null&&vib.hasVibrator()){if(Build.VERSION.SDK_INT>=26)vib.vibrate(VibrationEffect.createOneShot(28,150));else vib.vibrate(28);}
            }catch(Throwable ignored){}
        }
    }

    static void pressMotion(View v){
        if(v==null)return;
        final boolean animate=motionEnabled(v.getContext());
        v.setOnTouchListener((x,e)->{switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:if(animate)x.animate().scaleX(.975f).scaleY(.975f).setDuration(70).start();break;case MotionEvent.ACTION_UP:if(animate)x.animate().scaleX(1f).scaleY(1f).setDuration(120).start();haptic(x);break;case MotionEvent.ACTION_CANCEL:if(animate)x.animate().scaleX(1f).scaleY(1f).setDuration(120).start();break;}return false;});
    }

    static void pulse(View v){if(v==null||!motionEnabled(v.getContext()))return;ObjectAnimator a=ObjectAnimator.ofFloat(v,"alpha",.58f,1f);a.setDuration(260);a.start();}

    static void toast(Activity a,String s){if(a==null)return;a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())Toast.makeText(a.getApplicationContext(),s,Toast.LENGTH_SHORT).show();});}

    static void showPrintPreview(Activity a,RasterEncoder.Raster raster,int count,Runnable confirm,Runnable cancel){
        showPrintPreview(a,raster,count,false,copies->{if(confirm!=null)confirm.run();},cancel);
    }

    /** Final preview gate: the dialog itself is scrollable and exposes the parameters that affect real output. */
    static void showPrintPreview(Activity a,RasterEncoder.Raster rawRaster,int count,boolean editableCopies,PrintConfirm confirm,Runnable cancel){
        if(a==null||a.isFinishing()||a.isDestroyed()||rawRaster==null){if(cancel!=null)cancel.run();return;}
        a.runOnUiThread(()->{
            if(a.isFinishing()||a.isDestroyed()){if(cancel!=null)cancel.run();return;}
            try{
                resolvePalette(a);
                final android.content.SharedPreferences sp=a.getSharedPreferences("settings",0);
                final int initialCopies=Math.max(1,Math.min(20,count));
                ScrollView dialogScroll=new ScrollView(a);dialogScroll.setFillViewport(false);dialogScroll.setClipToPadding(false);dialogScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
                LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(a,18),dp(a,12),dp(a,18),dp(a,18));box.setBackgroundColor(CARD);dialogScroll.addView(box,new ScrollView.LayoutParams(-1,-2));

                TextView title=new TextView(a);title.setText("确认最终打印效果");title.setTextSize(23);title.setTextColor(TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);box.addView(title);
                TextView meta=hint(a,"");box.addView(meta);
                ZoomablePreviewView image=new ZoomablePreviewView(a);image.setBackgroundColor(Color.WHITE);
                FrameLayout frame=previewFrame(a,image,"最终点阵 · 双指缩放 / 拖动 / 双击复位");box.addView(frame,new LinearLayout.LayoutParams(-1,dp(a,isWide(a)?420:320)));
                final Bitmap[] previewBmp={null};
                Runnable refresh=()->refreshFinalPreview(a,rawRaster,image,meta,previewBmp);
                refresh.run();

                LinearLayout tools=new LinearLayout(a);tools.setOrientation(LinearLayout.HORIZONTAL);tools.setGravity(Gravity.END);Button fit=compactButton(a,"复位预览");fit.setOnClickListener(v->{image.reset();pulse(frame);});tools.addView(fit,new LinearLayout.LayoutParams(dp(a,118),dp(a,44)));box.addView(tools);

                PrinterSnapshot snap=PrinterManager.get(a).snapshot();float initialPaperMm=PrinterProfile.clampPaperWidthMm(sp.getInt("paper_width_tenths_mm",570)/10f);int initialPaperDots=PrinterProfile.usableDotsForPaper(initialPaperMm);HorizontalScrollView chipScroll=new HorizontalScrollView(a);chipScroll.setHorizontalScrollBarEnabled(false);LinearLayout chips=new LinearLayout(a);chips.setOrientation(LinearLayout.HORIZONTAL);TextView c1=pill(a,"SPP",PRIMARY),c2=pill(a,String.format(java.util.Locale.US,"%.1fmm · 203DPI · %d/384 dots",initialPaperMm,initialPaperDots),SUCCESS),c3=pill(a,editableCopies?initialCopies+" 份":count+" 个任务",WARNING),c4=pill(a,snap.connected?"设备在线":"未连接",snap.connected?SUCCESS:WARNING);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,-2);cp.setMargins(0,dp(a,8),dp(a,8),0);chips.addView(c1,cp);chips.addView(c2,cp);chips.addView(c3,cp);chips.addView(c4,cp);chipScroll.addView(chips);box.addView(chipScroll);

                box.addView(section(a,"打印参数","这里的调整会直接用于最终输出；页面可以上下滑动，不再隐藏下面的设置。"));
                LinearLayout settings=card(a);settings.setPadding(dp(a,14),dp(a,10),dp(a,14),dp(a,14));

                int currentDensity=sp.getInt("density",1);TextView densityLabel=hint(a,"打印浓度："+currentDensity+" · 由打印机硬件执行");settings.addView(densityLabel);SeekBar density=new SeekBar(a);density.setMax(5);density.setProgress(Math.max(0,Math.min(5,currentDensity)));settings.addView(density);density.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean from){densityLabel.setText("打印浓度："+p+" · 由打印机硬件执行");sp.edit().putInt("density",p).apply();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});

                final int[] selectedCopies={initialCopies};
                if(editableCopies){TextView copiesLabel=hint(a,"打印份数："+initialCopies);settings.addView(copiesLabel);SeekBar copiesBar=new SeekBar(a);copiesBar.setMax(19);copiesBar.setProgress(initialCopies-1);settings.addView(copiesBar);copiesBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean from){selectedCopies[0]=p+1;copiesLabel.setText("打印份数："+selectedCopies[0]);c3.setText(selectedCopies[0]+" 份");}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});}

                int scaleNow=Math.max(25,Math.min(200,sp.getInt("content_scale_percent",100)));TextView scaleLabel=hint(a,"内容二次缩放："+scaleNow+"%");settings.addView(scaleLabel);SeekBar scale=new SeekBar(a);scale.setMax(175);scale.setProgress(scaleNow-25);settings.addView(scale);scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean from){int v=25+p;scaleLabel.setText("内容二次缩放："+v+"%");if(from)sp.edit().putInt("content_scale_percent",v).apply();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){refresh.run();}});

                int xNow=Math.max(-96,Math.min(96,sp.getInt("x_offset",0)));TextView xLabel=hint(a,"水平校准 X："+xNow+" dots（正数向右）");settings.addView(xLabel);SeekBar xBar=new SeekBar(a);xBar.setMax(192);xBar.setProgress(xNow+96);settings.addView(xBar);xBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean from){int v=p-96;xLabel.setText("水平校准 X："+v+" dots（正数向右）");if(from)sp.edit().putInt("x_offset",v).apply();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){refresh.run();}});

                int yNow=Math.max(-1000,Math.min(1000,sp.getInt("y_offset",0)));TextView yLabel=hint(a,"垂直位置 Y："+yNow+" dots（正数向下）");settings.addView(yLabel);SeekBar yBar=new SeekBar(a);yBar.setMax(2000);yBar.setProgress(yNow+1000);settings.addView(yBar);yBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean from){int v=p-1000;yLabel.setText("垂直位置 Y："+v+" dots（正数向下）");if(from)sp.edit().putInt("y_offset",v).apply();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){refresh.run();}});

                Spinner media=new Spinner(a);media.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,new String[]{"连续纸：长度由内容决定","标签纸：固定宽度和长度"}));media.setSelection("label".equals(sp.getString("media_mode","continuous"))?1:0);settings.addView(media);media.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){boolean first=true;public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(first){first=false;return;}sp.edit().putString("media_mode",pos==1?"label":"continuous").apply();refresh.run();}});

                int pwNow=Math.max(10,Math.min(57,Math.round(sp.getInt("paper_width_tenths_mm",570)/10f)));TextView pwLabel=hint(a,"纸张 / 标签宽度："+pwNow+" mm（由用户声明，不是传感器测量）");settings.addView(pwLabel);SeekBar pwBar=new SeekBar(a);pwBar.setMax(47);pwBar.setProgress(pwNow-10);settings.addView(pwBar);pwBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean from){int v=10+p;pwLabel.setText("纸张 / 标签宽度："+v+" mm（可用 "+PrinterProfile.usableDotsForPaper(v)+" dots）");if(from){sp.edit().putInt("paper_width_tenths_mm",v*10).apply();c2.setText(v+"mm · 203DPI · "+PrinterProfile.usableDotsForPaper(v)+"/384 dots");}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){refresh.run();}});

                Spinner widthMode=new Spinner(a);widthMode.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,new String[]{"内容宽度：自动按有效内容","内容宽度：铺满纸张可打印区","内容宽度：自定义毫米"}));widthMode.setSelection(Math.max(0,Math.min(2,sp.getInt("content_width_mode",0))));settings.addView(widthMode);widthMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){boolean first=true;public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(first){first=false;return;}sp.edit().putInt("content_width_mode",pos).apply();refresh.run();}});

                Spinner verticalAlign=new Spinner(a);verticalAlign.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,new String[]{"标签内容垂直：顶部","标签内容垂直：居中","标签内容垂直：底部"}));verticalAlign.setSelection(Math.max(0,Math.min(2,sp.getInt("content_vertical_alignment",0))));settings.addView(verticalAlign);verticalAlign.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){boolean first=true;public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(first){first=false;return;}sp.edit().putInt("content_vertical_alignment",pos).apply();refresh.run();}});

                int cwNow=Math.max(5,Math.min(48,Math.round(sp.getInt("content_width_tenths_mm",480)/10f)));TextView cwLabel=hint(a,"自定义内容宽度："+cwNow+" mm");settings.addView(cwLabel);SeekBar cwBar=new SeekBar(a);cwBar.setMax(43);cwBar.setProgress(cwNow-5);settings.addView(cwBar);cwBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean from){int v=5+p;cwLabel.setText("自定义内容宽度："+v+" mm");if(from)sp.edit().putInt("content_width_tenths_mm",v*10).apply();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){refresh.run();}});

                int ltNow=Math.max(5,Math.min(300,Math.round(sp.getInt("label_length_tenths_mm",300)/10f)));TextView hLabel=hint(a,"标签长度："+ltNow+" mm（仅标签纸模式生效）");settings.addView(hLabel);SeekBar hBar=new SeekBar(a);hBar.setMax(295);hBar.setProgress(ltNow-5);settings.addView(hBar);hBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean from){int v=5+p;hLabel.setText("标签长度："+v+" mm（仅标签纸模式生效）");if(from)sp.edit().putInt("label_length_tenths_mm",v*10).putInt("paper_height_mm",v).apply();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){refresh.run();}});

                int preNow=Math.max(0,Math.min(2000,sp.getInt("feed_before",10)));TextView preLabel=hint(a,"打印前走纸："+preNow+" dots");settings.addView(preLabel);SeekBar preBar=new SeekBar(a);preBar.setMax(2000);preBar.setProgress(preNow);settings.addView(preBar);preBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean from){preLabel.setText("打印前走纸："+p+" dots");if(from)sp.edit().putInt("feed_before",p).apply();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});

                int postNow=Math.max(0,Math.min(4000,sp.getInt("feed_after",100)));TextView postLabel=hint(a,"打印后走纸："+postNow+" dots");settings.addView(postLabel);SeekBar postBar=new SeekBar(a);postBar.setMax(4000);postBar.setProgress(postNow);settings.addView(postBar);postBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean from){postLabel.setText("打印后走纸："+p+" dots");if(from)sp.edit().putInt("feed_after",p).apply();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});

                CheckBox rotate=new CheckBox(a);rotate.setText("旋转 180° 打印");rotate.setChecked(sp.getInt("print_direction",0)==180);settings.addView(rotate);rotate.setOnCheckedChangeListener((v,checked)->{sp.edit().putInt("print_direction",checked?180:0).apply();refresh.run();});
                TextView explain=hint(a,"上面参数会直接写入本地打印设置，并在确认时真实应用到点阵。若需要逐元素拖动、字体、字号、粗细、旋转和图层，请返回使用“自定义打印”。");explain.setPadding(0,dp(a,8),0,0);settings.addView(explain);
                Button layoutSettings=button(a,"更多纸张 / 设备参数");settings.addView(layoutSettings);box.addView(settings);tintTree(a,box);

                AlertDialog d=new AlertDialog.Builder(a).setView(dialogScroll).setNegativeButton("返回调整",(x,w)->{if(cancel!=null)cancel.run();}).setPositiveButton("确认打印",null).create();
                layoutSettings.setOnClickListener(v->{haptic(v);d.dismiss();if(cancel!=null)cancel.run();try{a.startActivity(new Intent(a,PaperSettingsActivity.class));}catch(Throwable e){toast(a,"无法打开纸张设置");}});
                d.setOnShowListener(x->{Button p=d.getButton(AlertDialog.BUTTON_POSITIVE);p.setTextColor(PRIMARY);p.setTypeface(Typeface.DEFAULT,Typeface.BOLD);p.setOnClickListener(v->{haptic(v);PrintSound.play(a);d.dismiss();if(confirm!=null)confirm.confirm(selectedCopies[0]);});Button n=d.getButton(AlertDialog.BUTTON_NEGATIVE);if(n!=null)n.setTextColor(MUTED);});
                d.setOnCancelListener(x->{if(cancel!=null)cancel.run();});
                d.setOnDismissListener(x->{try{image.setImageDrawable(null);if(previewBmp[0]!=null&&!previewBmp[0].isRecycled())previewBmp[0].recycle();previewBmp[0]=null;}catch(Throwable ignored){}});
                d.show();
                if(d.getWindow()!=null){try{d.getWindow().setDimAmount(.5f);d.getWindow().setBackgroundDrawable(rounded(CARD,dp(a,24)));android.util.DisplayMetrics dm=a.getResources().getDisplayMetrics();int w=Math.min(dm.widthPixels-dp(a,22),dp(a,680));int h=(int)(dm.heightPixels*.91f);d.getWindow().setLayout(w,h);}catch(Throwable ignored){}}
                if(motionEnabled(a)){dialogScroll.setScaleX(.965f);dialogScroll.setScaleY(.965f);dialogScroll.setAlpha(0f);dialogScroll.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(240).setInterpolator(new DecelerateInterpolator()).start();}
            }catch(OutOfMemoryError e){toast(a,"预览内容过大，当前设备内存不足");if(cancel!=null)cancel.run();}
            catch(Throwable e){toast(a,"打开打印预览失败："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));if(cancel!=null)cancel.run();}
        });
    }

    private static void refreshFinalPreview(Activity a,RasterEncoder.Raster raw,ZoomablePreviewView image,TextView meta,Bitmap[] holder){
        try{
            RasterEncoder.Raster shown=PrinterManager.get(a).previewRaster(raw);Bitmap next=RasterEncoder.toBitmap(shown,2200);Bitmap old=holder[0];holder[0]=next;image.setImageBitmap(next);image.reset();if(old!=null&&old!=next&&!old.isRecycled())old.recycle();
            PrinterSnapshot snap=PrinterManager.get(a).snapshot();String state=snap.connected?"打印机已连接":"打印机未连接";String battery=snap.battery==null?"":(" · 电量 "+snap.battery+"%");android.content.SharedPreferences sp=a.getSharedPreferences("settings",0);float mm=PrinterProfile.dotsToMm(shown.height);float paperWidth=PrinterProfile.clampPaperWidthMm(sp.getInt("paper_width_tenths_mm",570)/10f);int paperDots=PrinterProfile.usableDotsForPaper(paperWidth),scale=sp.getInt("content_scale_percent",100),xo=sp.getInt("x_offset",0),yo=sp.getInt("y_offset",0),dir=sp.getInt("print_direction",0),mode=Math.max(0,Math.min(2,sp.getInt("content_width_mode",0)));boolean label="label".equals(sp.getString("media_mode","continuous"));int blackW=RasterEncoder.blackContentWidth(shown),vertical=Math.max(0,Math.min(2,sp.getInt("content_vertical_alignment",0)));String widthText=mode==0?"自动按内容":mode==1?"铺满纸张":"自定义宽度",verticalText=vertical==1?"垂直居中":vertical==2?"垂直底部":"垂直顶部";meta.setText("下图就是确认后实际发送的最终 384-dot 点阵。\n约 "+String.format(java.util.Locale.US,"%.1f",mm)+" mm 高 · 用户纸宽 "+String.format(java.util.Locale.US,"%.1f",paperWidth)+" mm / 可用 "+paperDots+" dots · 实际黑色内容约 "+blackW+" dots（"+String.format(java.util.Locale.US,"%.1f",PrinterProfile.dotsToMm(blackW))+"mm） · "+widthText+" · "+scale+"% · "+(label?("固定标签 / "+verticalText):"连续纸自动内容长度")+" · X/Y "+xo+"/"+yo+" · "+dir+"° · "+state+battery);
            pulse(image);
        }catch(Throwable e){meta.setText("预览刷新失败："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));}
    }

    /** Apply palette to platform widgets that otherwise inherit the light platform theme. */
    static void tintTree(Activity a,View v){
        if(v==null)return;
        try{
            if(v instanceof EditText){EditText e=(EditText)v;e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setBackgroundTintList(ColorStateList.valueOf(PRIMARY));}
            else if(v instanceof CheckBox){CheckBox c=(CheckBox)v;c.setTextColor(TEXT);c.setButtonTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{PRIMARY,MUTED}));}
            else if(v instanceof RadioButton){RadioButton c=(RadioButton)v;c.setTextColor(TEXT);c.setButtonTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{PRIMARY,MUTED}));}
            else if(v instanceof Spinner){v.setBackgroundTintList(ColorStateList.valueOf(MUTED));}
            else if(v instanceof SeekBar){SeekBar s=(SeekBar)v;s.setProgressTintList(ColorStateList.valueOf(PRIMARY));s.setThumbTintList(ColorStateList.valueOf(PRIMARY));}
            else if(v instanceof TextView && !(v instanceof Button)){TextView t=(TextView)v;if(t.getCurrentTextColor()==Color.BLACK || t.getCurrentTextColor()==0xff212121)t.setTextColor(TEXT);}
            if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)tintTree(a,g.getChildAt(i));}
        }catch(Throwable ignored){}
    }

    static int blend(int a,int b,float towardB){float t=Math.max(0,Math.min(1,towardB));int r=(int)(Color.red(a)*(1-t)+Color.red(b)*t),g=(int)(Color.green(a)*(1-t)+Color.green(b)*t),bl=(int)(Color.blue(a)*(1-t)+Color.blue(b)*t);return Color.rgb(r,g,bl);}
}
