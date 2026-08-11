package com.qring.print;

import android.app.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class WebPrintActivity extends Activity {
    private EditText url;private WebView web;private TextView state;private ImageView preview;private Bitmap cached,thermalPreview;private String pendingLocalUrl;
    @Override public void onCreate(Bundle b){super.onCreate(b);try{WebView.enableSlowWholeDocumentDraw();setContentView(build());}catch(Throwable e){LinearLayout root=Ui.page(this,"网页打印");root.addView(Ui.hint(this,"当前系统没有可用的 Android System WebView，网页打印不可用，但其它打印功能可继续使用。"));setContentView(root);}}
    private View build(){
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setBackgroundColor(Ui.BG);LinearLayout root=Ui.page(this,"网页打印");shell.addView(root,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout bar=Ui.card(this);LinearLayout address=new LinearLayout(this);address.setOrientation(LinearLayout.HORIZONTAL);url=new EditText(this);url.setHint("https://example.com");url.setSingleLine(true);url.setHorizontallyScrolling(true);url.setEllipsize(TextUtils.TruncateAt.END);url.setTextDirection(View.TEXT_DIRECTION_LTR);url.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);url.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);url.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);url.setTypeface(Typeface.MONOSPACE);url.setTextLocale(Locale.US);url.setTextSize(16);Button go=Ui.primaryButton(this,"打开");address.addView(url,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));address.addView(go,new LinearLayout.LayoutParams(Ui.dp(this,90),Ui.dp(this,52)));bar.addView(address);state=Ui.hint(this,"网页加载后可先生成热敏打印预览。超长网页最多截取 8000 点。局域网页会在未来系统要求时按需申请本地网络权限。");bar.addView(state);root.addView(bar);
        web=new WebView(this);web.setBackgroundColor(Color.WHITE);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setSupportZoom(true);web.getSettings().setBuiltInZoomControls(true);web.getSettings().setDisplayZoomControls(false);web.getSettings().setUseWideViewPort(true);web.getSettings().setLoadWithOverviewMode(true);web.setWebViewClient(new WebViewClient(){@Override public void onPageStarted(WebView v,String u,Bitmap f){state.setText("正在加载网页…");Bitmap old=cached;cached=null;if(old!=null&&!old.isRecycled())old.recycle();}@Override public void onPageFinished(WebView v,String u){state.setText("网页已加载 · 点击下方“生成热敏预览”");}@Override public void onReceivedError(WebView view,int errorCode,String description,String failingUrl){state.setText("网页加载失败："+(description==null?"未知错误":description));}});FrameLayout wf=Ui.previewFrame(this,web,"网页内容");root.addView(wf,new LinearLayout.LayoutParams(-1,Ui.dp(this,300)));
        preview=new ZoomablePreviewView(this);preview.setAdjustViewBounds(true);preview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);preview.setBackgroundColor(Color.WHITE);root.addView(Ui.previewFrame(this,preview,"热敏打印预览"),new LinearLayout.LayoutParams(-1,Ui.dp(this,280)));
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button make=Ui.button(this,"生成热敏预览"),print=Ui.primaryButton(this,"预览确认并打印");make.setOnClickListener(v->makePreview());print.setOnClickListener(v->print());actions.addView(make,new LinearLayout.LayoutParams(0,Ui.dp(this,54),1));actions.addView(print,new LinearLayout.LayoutParams(0,Ui.dp(this,54),1));root.addView(actions);go.setOnClickListener(v->{String s=url.getText().toString().trim();if(s.isEmpty()){Ui.toast(this,"请输入网页地址");return;}if(!s.startsWith("http://")&&!s.startsWith("https://"))s="https://"+s;openUrl(s);});return shell;
    }
    private void openUrl(String s){
        try{
            if(isLocalNetworkUrl(s)&&!Compat.hasLocalNetworkPermission(this)){
                pendingLocalUrl=s;state.setText("当前系统访问局域网页需要“本地网络/附近设备”权限");Compat.requestLocalNetworkPermission(this);return;
            }
            pendingLocalUrl=null;url.setText(s);url.setSelection(url.length());web.loadUrl(s);
        }catch(Throwable e){state.setText("网页打开失败："+e.getMessage());}
    }
    private static boolean isLocalNetworkUrl(String s){
        try{
            Uri u=Uri.parse(s);String h=u.getHost();if(h==null)return false;h=h.toLowerCase(Locale.ROOT);
            if(h.equals("localhost")||h.endsWith(".local")||h.equals("127.0.0.1")||h.equals("::1"))return true;
            if(h.startsWith("10.")||h.startsWith("192.168."))return true;
            if(h.startsWith("172.")){
                String[] p=h.split("\\.");if(p.length>1){int n=Integer.parseInt(p[1]);if(n>=16&&n<=31)return true;}
            }
            if(h.startsWith("169.254."))return true;
            // IPv6 unique-local (fc00::/7) / link-local (fe80::/10)
            return h.startsWith("fc")||h.startsWith("fd")||h.startsWith("fe8")||h.startsWith("fe9")||h.startsWith("fea")||h.startsWith("feb");
        }catch(Throwable ignored){return false;}
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==Compat.REQ_LOCAL_NETWORK){
            String s=pendingLocalUrl;pendingLocalUrl=null;
            boolean granted=Compat.hasLocalNetworkPermission(this)||(grantResults!=null&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED);
            if(granted&&s!=null)openUrl(s);else state.setText("未授予局域网权限；普通互联网网页仍可打印。可在系统设置中重新授权。");
        }
    }
    private Bitmap capture(){if(web==null)return null;Picture pic=web.capturePicture();if(pic==null||pic.getWidth()<=0||pic.getHeight()<=0)return null;int w=384,h=Math.max(1,Math.round(pic.getHeight()*w/(float)pic.getWidth()));if(h>8000)h=8000;Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);float scale=w/(float)pic.getWidth();c.scale(scale,scale);pic.draw(c);return b;}
    private void makePreview(){if(web==null){Ui.toast(this,"当前系统没有可用 WebView");return;}try{Bitmap b=capture();if(b==null){Ui.toast(this,"网页还没有可预览内容");return;}Bitmap oldCapture=cached;cached=b;if(oldCapture!=null&&oldCapture!=b&&!oldCapture.isRecycled())oldCapture.recycle();RasterEncoder.Raster r=RasterEncoder.encode(b,190,RasterEncoder.Dither.FLOYD);r=PrinterManager.get(this).previewRaster(r);Bitmap next=RasterEncoder.toBitmap(r,1800);Bitmap oldPreview=thermalPreview;thermalPreview=next;preview.setImageBitmap(next);if(oldPreview!=null&&oldPreview!=next&&!oldPreview.isRecycled())oldPreview.recycle();state.setText("热敏预览已生成 · "+b.getHeight()+" dots 高"+(b.getHeight()>=8000?" · 已截取前 8000 点":""));Ui.pulse(preview);}catch(OutOfMemoryError e){Ui.toast(this,"网页太长，当前设备内存不足");}catch(Throwable e){Ui.toast(this,"预览失败："+e.getMessage());}}
    private void print(){if(cached==null)makePreview();if(cached==null)return;try{Bitmap b=cached;RasterEncoder.Raster r=RasterEncoder.encode(b,190,RasterEncoder.Dither.FLOYD);PrinterManager.get(this).print(this,new PrinterManager.BitmapHolder(r),(ok,msg)->{if(!"已取消打印".equals(msg))Ui.toast(this,msg);if(ok)HistoryStore.addRaster(this,"网页",url.getText().toString(),r);});}catch(OutOfMemoryError e){Ui.toast(this,"网页太长，无法在当前设备内存中生成打印图");}catch(Throwable e){Ui.toast(this,"网页打印失败："+e.getMessage());}}
    @Override protected void onDestroy(){try{if(web!=null){web.stopLoading();web.destroy();}}catch(Throwable ignored){}if(cached!=null&&!cached.isRecycled())cached.recycle();if(thermalPreview!=null&&!thermalPreview.isRecycled())thermalPreview.recycle();cached=null;thermalPreview=null;super.onDestroy();}
}
