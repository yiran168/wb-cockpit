package com.qring.print;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class BackupActivity extends Activity {
    private static final int CREATE=1701,OPEN=1702; private TextView status,summary;private ProgressBar progress;
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());refreshSummary();}
    @Override protected void onResume(){super.onResume();refreshSummary();}
    private View build(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);LinearLayout root=Ui.page(this,"备份与迁移");sc.addView(root);
        root.addView(Ui.section(this,"备份内容预览","导出前先确认会带走哪些本地数据；蓝牙系统配对不会写入备份。"));
        LinearLayout manifest=Ui.card(this);LinearLayout h=new LinearLayout(this);h.setOrientation(LinearLayout.HORIZONTAL);h.setGravity(Gravity.CENTER_VERTICAL);h.addView(Ui.pill(this,"ZIP",Ui.PRIMARY));summary=Ui.hint(this,"正在统计本地数据…");summary.setPadding(Ui.dp(this,12),0,0,0);h.addView(summary,new LinearLayout.LayoutParams(0,-2,1));manifest.addView(h);root.addView(manifest);

        LinearLayout card=Ui.card(this);card.addView(Ui.hint(this,"本地备份用于替代必须依赖厂商云端的同步：包含打印设置、外观偏好、模板、打印记录和商品信息库。备份格式有版本校验与 ZIP 路径安全检查。"));
        Button export=Ui.primaryButton(this,"导出完整本地备份 ZIP");export.setOnClickListener(v->exportBackup());card.addView(export);
        Button restore=Ui.button(this,"导入并恢复备份 ZIP");restore.setOnClickListener(v->confirmRestore());card.addView(restore);root.addView(card);

        root.addView(Ui.section(this,"操作状态","长任务在后台执行，页面保持可响应。"));
        LinearLayout state=Ui.card(this);LinearLayout sr=new LinearLayout(this);sr.setOrientation(LinearLayout.HORIZONTAL);sr.setGravity(Gravity.CENTER_VERTICAL);sr.addView(Ui.pill(this,"LOCAL",Ui.SUCCESS));status=Ui.hint(this,"等待操作");status.setPadding(Ui.dp(this,12),0,0,0);sr.addView(status,new LinearLayout.LayoutParams(0,-2,1));progress=new ProgressBar(this);progress.setIndeterminate(true);progress.setVisibility(View.GONE);sr.addView(progress,new LinearLayout.LayoutParams(Ui.dp(this,28),Ui.dp(this,28)));state.addView(sr);root.addView(state);
        return sc;
    }
    private void refreshSummary(){if(summary==null)return;try{int templates=TemplateStore.list(this).size(),history=HistoryStore.list(this).size(),products=0;if(ProductStore.exists(this)){try{products=ProductStore.load(this).rows.size();}catch(Throwable ignored){}}summary.setText("设置与外观 · "+templates+" 个模板 · "+history+" 条记录 · "+products+" 条商品数据");}catch(Throwable e){summary.setText("设置、模板、历史和商品库");}}
    private void exportBackup(){
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/zip");
        String d=new SimpleDateFormat("yyyyMMdd-HHmm",Locale.US).format(new Date());i.putExtra(Intent.EXTRA_TITLE,"QrintPrint-backup-"+d+".zip");
        try{startActivityForResult(i,CREATE);}catch(Throwable e){Ui.toast(this,"无法打开文件保存器");}
    }
    private void confirmRestore(){
        new AlertDialog.Builder(this).setTitle("恢复备份").setMessage("恢复会替换当前本地设置、模板、打印记录和商品库。建议先导出当前备份。是否继续？")
                .setPositiveButton("继续",(d,w)->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/zip");try{startActivityForResult(i,OPEN);}catch(Throwable e){Ui.toast(this,"无法打开文件选择器");}})
                .setNegativeButton("取消",null).show();
    }
    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null||data.getData()==null)return;Uri u=data.getData();
        progress.setVisibility(View.VISIBLE);status.setText(req==CREATE?"正在导出完整备份…":"正在校验并恢复备份…");
        new Thread(()->{try{
            if(req==CREATE){BackupUtil.exportBackup(this,u);postStatus("备份已导出",true);}
            else if(req==OPEN){String msg=BackupUtil.importBackup(this,u);postStatus(msg,true);}
        }catch(Throwable e){postStatus("操作失败："+e.getMessage(),false);}},"QrintBackup").start();
    }
    private void postStatus(String text,boolean ok){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&status!=null){progress.setVisibility(View.GONE);status.setText(text);status.setTextColor(ok?Ui.SUCCESS:Ui.WARNING);refreshSummary();}});}
}
