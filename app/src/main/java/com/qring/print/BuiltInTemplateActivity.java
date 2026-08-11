package com.qring.print;

import android.app.*;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Local, editable-ready template gallery. No vendor cloud/template assets are used. */
public class BuiltInTemplateActivity extends Activity {
    private static final String[] NAMES={
            "服饰价签","商超价格牌","食品日期标签","珠宝标签","物流地址标签","电力线缆标签","玩具母婴标签",
            "药品信息标签","货架/仓储标签","资产管理标签","错题记录","待办清单","周计划","快递备注贴","姓名贴",
            "单词卡","公式/知识点卡","错题复习计划","文件档案标签","线材端口标签","实验样本标签","维修巡检标签",
            "日期封口贴","促销价格牌","空白自由标签","课程表","阅读摘录卡","背诵打卡","考试倒计时","学习目标卡",
            "入库标签","出库标签","库存盘点卡","保质期提醒","冰箱冷冻标签","厨房备餐标签","线缆双端对贴","维修工单",
            "设备状态牌","访客临时证"
    };
    private static final String[] SUB={
            "品牌 / 品名 / 尺码 / 价格","商品 / 会员价 / 条码","日期 / 保质期 / 批次","材质 / 重量 / 价格","收件信息 / 地址 / 备注",
            "规格 / 起终点 / 编号","型号 / 年龄 / 批次","规格 / 批号 / 有效期","库位 / 物料 / 数量","资产编号 / 部门 / 二维码",
            "题目 / 错因 / 思路 / 知识点","勾选式任务列表","周一到周日计划","快递单号 / 易碎 / 备注","姓名 / 班级 / 联系方式",
            "单词 / 音标 / 释义 / 例句","公式 / 条件 / 结论 / 易错点","复习日期 / 掌握度 / 再练","档案号 / 分类 / 日期 / 责任人",
            "端口 / 来源 / 去向 / 颜色","样本号 / 项目 / 时间 / 操作员","设备 / 检查项 / 结果 / 日期","生产 / 开封 / 到期 / 批次",
            "活动价 / 原价 / 卖点 / 条码","384 点空白纸张 · 可保存后继续编辑","星期 / 节次 / 科目 / 教室","书名 / 页码 / 摘录 / 感想",
            "日期 / 内容 / 完成勾选 / 连续天数","考试 / 日期 / 剩余天数 / 今日任务","本周目标 / 关键结果 / 完成度","供应商 / 物料 / 数量 / 批次",
            "领用人 / 物料 / 数量 / 去向","库位 / 账面 / 实盘 / 差异","品名 / 开封 / 到期 / 提醒","品名 / 冷冻日期 / 建议食用期",
            "餐品 / 制作时间 / 保存条件 / 到期","A 端 / B 端 / 线号 / 颜色","设备 / 故障 / 处理 / 签名","设备名 / 状态 / 时间 / 负责人",
            "姓名 / 单位 / 事由 / 有效时间"
    };
    private final ArrayList<Bitmap> thumbs=new ArrayList<>();
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());}

    private View build(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);LinearLayout root=Ui.page(this,"内置行业模板");sc.addView(root);
        LinearLayout intro=Ui.card(this);intro.addView(Ui.pill(this,NAMES.length+" 张",Ui.SUCCESS),new LinearLayout.LayoutParams(Ui.dp(this,72),Ui.dp(this,36)));intro.addView(Ui.hint(this,"全部模板由本应用本地生成，不使用厂商云模板或商标素材。每张模板都直接按 384 点打印头宽度绘制，可先缩放/拖动检查，再保存或打印。"));root.addView(intro);
        root.addView(Ui.section(this,"模板画廊","零售、物流、仓储、学习、办公和日常标签。缩略卡高度统一，避免列表一大一小。"));
        for(int i=0;i<NAMES.length;i++)root.addView(templateCard(i));
        return sc;
    }

    private View templateCard(int i){
        LinearLayout card=Ui.card(this);LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon=Ui.pill(this,Integer.toString(i+1),Ui.PRIMARY);head.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,38)));LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);copy.setPadding(Ui.dp(this,12),Ui.dp(this,2),0,Ui.dp(this,4));TextView t=new TextView(this);t.setText(NAMES[i]);t.setTextSize(17);t.setTextColor(Ui.TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(t,new LinearLayout.LayoutParams(-1,Ui.dp(this,30)));TextView sub=Ui.hint(this,SUB[i]);sub.setPadding(0,0,0,0);sub.setMinLines(2);sub.setMaxLines(2);sub.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(sub,new LinearLayout.LayoutParams(-1,Ui.dp(this,42)));head.addView(copy,new LinearLayout.LayoutParams(0,Ui.dp(this,76),1));card.addView(head,new LinearLayout.LayoutParams(-1,Ui.dp(this,80)));
        try{
            Bitmap full=makeTemplate(i);int h=Math.max(100,Math.round(full.getHeight()*192f/full.getWidth()));Bitmap thumb=Bitmap.createScaledBitmap(full,192,h,true);thumbs.add(thumb);full.recycle();
            ImageView iv=new ZoomablePreviewView(this);iv.setImageBitmap(thumb);iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);iv.setBackgroundColor(Color.WHITE);
            card.addView(Ui.previewFrame(this,iv,"384 点模板缩略 · 双指缩放 / 拖动"),new LinearLayout.LayoutParams(-1,Ui.dp(this,196)));
        }catch(Throwable e){card.addView(Ui.hint(this,"缩略图生成失败，可点下方打开模板。"));}
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button preview=Ui.button(this,"大图预览"),save=Ui.button(this,"保存模板"),print=Ui.primaryButton(this,"参数 / 打印");preview.setOnClickListener(v->showTemplate(i));save.setOnClickListener(v->saveTemplate(i));print.setOnClickListener(v->printFresh(i));row.addView(preview,new LinearLayout.LayoutParams(0,Ui.dp(this,56),1));row.addView(save,new LinearLayout.LayoutParams(0,Ui.dp(this,56),1));row.addView(print,new LinearLayout.LayoutParams(0,Ui.dp(this,56),1));card.addView(row);return card;
    }

    private void showTemplate(int i){
        Bitmap b=makeTemplate(i);ImageView iv=new ZoomablePreviewView(this);iv.setAdjustViewBounds(true);iv.setScaleType(ImageView.ScaleType.FIT_CENTER);iv.setBackgroundColor(Color.WHITE);iv.setImageBitmap(b);FrameLayout frame=Ui.previewFrame(this,iv,"完整模板 · 384 dots");
        AlertDialog d=new AlertDialog.Builder(this).setTitle(NAMES[i]).setView(frame).setPositiveButton("参数 / 打印",(x,w)->print(i,b)).setNeutralButton("保存到我的模板",(x,w)->{try{TemplateStore.save(this,NAMES[i],b);Ui.toast(this,"模板已保存");}catch(Throwable e){Ui.toast(this,"保存失败："+e.getMessage());}}).setNegativeButton("关闭",null).create();
        d.setOnDismissListener(x->{try{iv.setImageDrawable(null);if(!b.isRecycled())b.recycle();}catch(Throwable ignored){}});d.show();
    }
    private void saveTemplate(int i){Bitmap b=null;try{b=makeTemplate(i);TemplateStore.save(this,NAMES[i],b);Ui.toast(this,"模板已保存");}catch(Throwable e){Ui.toast(this,"保存失败："+e.getMessage());}finally{if(b!=null&&!b.isRecycled())b.recycle();}}
    private void printFresh(int i){Bitmap b=null;try{b=makeTemplate(i);print(i,b);}catch(Throwable e){Ui.toast(this,"模板生成失败："+e.getMessage());}finally{if(b!=null&&!b.isRecycled())b.recycle();}}
    private void print(int i,Bitmap b){RasterEncoder.Raster r=RasterEncoder.encode(b,180,RasterEncoder.Dither.THRESHOLD);PrinterManager.get(this).print(this,new PrinterManager.BitmapHolder(r),(ok,msg)->{if(!"已取消打印".equals(msg))Ui.toast(this,msg);if(ok)HistoryStore.addRaster(this,"内置模板",NAMES[i],r);});}
    static int templateCount(){return NAMES.length;}
    static String templateName(int i){return i>=0&&i<NAMES.length?NAMES[i]:"行业模板";}

    static Bitmap makeTemplate(int kind){
        kind=Math.max(0,Math.min(NAMES.length-1,kind));int h=templateHeight(kind);Bitmap b=Bitmap.createBitmap(QringProtocol.WIDTH_DOTS,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);p.setStrokeWidth(2);p.setStyle(Paint.Style.STROKE);c.drawRect(3,3,QringProtocol.WIDTH_DOTS-4,h-4,p);p.setStyle(Paint.Style.FILL);p.setTextSize(28);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(NAMES[kind],18,42,p);
        p.setTypeface(Typeface.DEFAULT);p.setTextSize(20);
        switch(kind){
            case 0: c.drawText("品牌：________________",18,92,p);c.drawText("品名：________________",18,138,p);c.drawText("尺码：______  颜色：______",18,184,p);price(c,p,260);barcodeBox(c,p,340);break;
            case 1: c.drawText("商品名称",18,98,p);price(c,p,180);c.drawText("会员价：¥ __________",18,270,p);barcodeBox(c,p,340);break;
            case 2: c.drawText("品名：________________",18,95,p);c.drawText("生产日期：____-__-__",18,150,p);c.drawText("保质期：________ 天",18,205,p);c.drawText("批次：______________",18,260,p);barcodeBox(c,p,340);break;
            case 3: c.drawText("品名：____________",18,95,p);c.drawText("材质：____________",18,145,p);c.drawText("重量：____________",18,195,p);price(c,p,280);smallBox(c,p,285,350,70,110);break;
            case 4: address(c,p);break;
            case 5: c.drawText("线缆/设备：______________",18,95,p);c.drawText("规格：__________________",18,150,p);c.drawText("起点：________  终点：________",18,205,p);c.drawText("编号：__________________",18,260,p);barcodeBox(c,p,340);break;
            case 6: c.drawText("品名：________________",18,95,p);c.drawText("型号/年龄：____________",18,150,p);c.drawText("批次：________________",18,205,p);price(c,p,280);barcodeBox(c,p,360);break;
            case 7: c.drawText("药品：________________",18,92,p);c.drawText("规格：________________",18,140,p);c.drawText("批号：________________",18,188,p);c.drawText("有效期：____-__-__",18,236,p);c.drawText("用法：________________",18,284,p);barcodeBox(c,p,360);break;
            case 8: c.drawText("库位：________________",18,100,p);c.drawText("物料：________________",18,155,p);c.drawText("数量：________________",18,210,p);c.drawText("批次：________________",18,265,p);barcodeBox(c,p,350);break;
            case 9: c.drawText("资产名称：____________",18,95,p);c.drawText("资产编号：____________",18,145,p);c.drawText("部门：________________",18,195,p);c.drawRect(112,245,272,405,p);c.drawText("二维码区域",145,330,p);break;
            case 10: wrongQuestion(c,p);break;
            case 11: todo(c,p);break;
            case 12: weekly(c,p);break;
            case 13: c.drawText("运单号：________________",18,95,p);c.drawText("□ 易碎  □ 防水  □ 勿压",18,150,p);c.drawText("备注：",18,205,p);c.drawRect(18,225,366,420,p);barcodeBox(c,p,445);break;
            case 14: p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(46);c.drawText("姓名：____________",28,140,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(22);c.drawText("班级：____________",30,215,p);c.drawText("电话：____________",30,270,p);barcodeBox(c,p,345);break;
            case 15: p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(40);c.drawText("WORD",24,110,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(21);c.drawText("音标：/____________/",24,160,p);c.drawText("释义：________________",24,220,p);c.drawText("例句：",24,282,p);c.drawRect(24,300,360,500,p);break;
            case 16: c.drawText("公式 / 定理：",18,92,p);c.drawRect(18,110,366,225,p);c.drawText("适用条件：",18,270,p);c.drawRect(18,288,366,385,p);c.drawText("易错点：________________",18,435,p);c.drawText("结论：__________________",18,490,p);break;
            case 17: reviewPlan(c,p);break;
            case 18: c.drawText("档案号：________________",18,95,p);c.drawText("分类：__________________",18,150,p);c.drawText("日期：____-__-__",18,205,p);c.drawText("责任人：________________",18,260,p);barcodeBox(c,p,340);break;
            case 19: c.drawText("端口：__________________",18,95,p);c.drawText("来源：__________________",18,150,p);c.drawText("去向：__________________",18,205,p);c.drawText("颜色：________  编号：____",18,260,p);barcodeBox(c,p,340);break;
            case 20: c.drawText("样本号：________________",18,95,p);c.drawText("项目：__________________",18,150,p);c.drawText("采集：____-__-__ __:__",18,205,p);c.drawText("操作员：________________",18,260,p);barcodeBox(c,p,340);break;
            case 21: c.drawText("设备：__________________",18,90,p);String[] checks={"□ 外观","□ 电源","□ 连接","□ 打印","□ 清洁"};for(int j=0;j<checks.length;j++)c.drawText(checks[j],22,145+j*48,p);c.drawText("日期：____-__-__",18,420,p);c.drawText("签名：____________",18,470,p);break;
            case 22: c.drawText("生产：____-__-__ __:__",18,105,p);c.drawText("开封：____-__-__ __:__",18,175,p);c.drawText("到期：____-__-__ __:__",18,245,p);c.drawText("批次：________________",18,315,p);c.drawLine(18,365,366,365,p);c.drawText("封口后请保持干燥",88,420,p);break;
            case 23: p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(34);c.drawText("限时活动价",18,95,p);price(c,p,175);p.setTypeface(Typeface.DEFAULT);p.setTextSize(20);c.drawText("原价：¥ __________",18,245,p);c.drawText("卖点：________________",18,300,p);barcodeBox(c,p,360);break;
            case 24: c.drawText("空白自由标签",18,95,p);p.setTextSize(18);c.drawText("保存到“我的模板”后，可继续作为自定义标签底稿。",18,145,p);c.drawLine(18,180,366,180,p);break;
            case 25: courseTable(c,p);break;
            case 26: c.drawText("书名：____________________",18,92,p);c.drawText("页码：______",18,140,p);c.drawText("摘录：",18,190,p);box(c,p,18,210,366,400);c.drawText("我的想法：",18,448,p);box(c,p,18,468,366,610);break;
            case 27: c.drawText("背诵内容：________________",18,92,p);c.drawText("开始日期：____-__-__",18,142,p);c.drawText("连续：______ 天",18,192,p);for(int r=0;r<6;r++){c.drawText("□ 第 "+(r+1)+" 次   日期：____-__-__   熟练度：☆ ☆ ☆",22,255+r*62,p);}break;
            case 28: p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(30);c.drawText("考试：________________",18,95,p);p.setTextSize(68);c.drawText("还有 ____ 天",40,205,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(20);c.drawText("考试日期：____-__-__",18,270,p);c.drawText("今日任务：",18,325,p);box(c,p,18,345,366,515);break;
            case 29: c.drawText("本周目标",18,92,p);for(int r=0;r<4;r++){c.drawText("□ 目标 "+(r+1)+"：________________",22,150+r*78,p);c.drawLine(42,172+r*78,354,172+r*78,p);}c.drawText("完成度：  ○20%  ○40%  ○60%  ○80%  ○100%",18,490,p);break;
            case 30: c.drawText("供应商：________________",18,92,p);c.drawText("物料：__________________",18,142,p);c.drawText("数量：________  单位：____",18,192,p);c.drawText("批次：__________________",18,242,p);c.drawText("入库：____-__-__ __:__",18,292,p);barcodeBox(c,p,350);break;
            case 31: c.drawText("领用人：________________",18,92,p);c.drawText("物料：__________________",18,142,p);c.drawText("数量：________  去向：____",18,192,p);c.drawText("出库：____-__-__ __:__",18,242,p);c.drawText("签字：__________________",18,292,p);barcodeBox(c,p,350);break;
            case 32: c.drawText("库位：__________________",18,92,p);c.drawText("物料：__________________",18,142,p);c.drawText("账面：________",18,202,p);c.drawText("实盘：________",200,202,p);c.drawText("差异：________",18,262,p);c.drawText("盘点人：______________",200,262,p);c.drawText("日期：____-__-__",18,322,p);barcodeBox(c,p,380);break;
            case 33: c.drawText("品名：__________________",18,92,p);c.drawText("开封：____-__-__ __:__",18,152,p);c.drawText("到期：____-__-__ __:__",18,212,p);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(32);c.drawText("到期前 ____ 天提醒",40,292,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(20);c.drawText("保存条件：______________",18,352,p);barcodeBox(c,p,410);break;
            case 34: p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(32);c.drawText("冷冻保存",18,92,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(20);c.drawText("品名：__________________",18,150,p);c.drawText("冷冻：____-__-__",18,205,p);c.drawText("建议食用：____-__-__ 前",18,260,p);c.drawText("份量：________",18,315,p);c.drawText("备注：__________________",18,370,p);break;
            case 35: c.drawText("餐品：__________________",18,92,p);c.drawText("制作：____-__-__ __:__",18,148,p);c.drawText("保存：□ 冷藏  □ 常温  □ 冷冻",18,204,p);c.drawText("到期：____-__-__ __:__",18,260,p);c.drawText("操作员：________________",18,316,p);barcodeBox(c,p,380);break;
            case 36: dualCable(c,p);break;
            case 37: c.drawText("设备：__________________",18,90,p);c.drawText("故障：",18,145,p);box(c,p,18,165,366,285);c.drawText("处理：",18,330,p);box(c,p,18,350,366,470);c.drawText("工程师：____________  日期：____-__-__",18,520,p);break;
            case 38: p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(34);c.drawText("设备状态",18,92,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(21);c.drawText("设备：________________",18,150,p);c.drawText("状态：□ 运行  □ 停机  □ 维修",18,215,p);c.drawText("时间：____-__-__ __:__",18,280,p);c.drawText("负责人：________________",18,345,p);c.drawText("备注：__________________",18,410,p);break;
            case 39: p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(36);c.drawText("访客证",130,90,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(21);c.drawText("姓名：__________________",18,155,p);c.drawText("单位：__________________",18,215,p);c.drawText("事由：__________________",18,275,p);c.drawText("有效：____-__-__ __:__ 至 __:__",18,335,p);c.drawText("接待人：________________",18,395,p);c.drawRect(270,430,360,520,p);c.drawText("照片",296,482,p);break;
            default: address(c,p);
        }
        return b;
    }

    private static int templateHeight(int kind){switch(kind){case 10:return 690;case 12:return 760;case 16:return 620;case 17:return 720;case 25:return 760;case 26:return 650;case 27:return 680;case 37:return 590;default:return 560;}}
    private static void box(Canvas c,Paint p,int l,int t,int r,int b){Paint old=new Paint(p);p.setStyle(Paint.Style.STROKE);c.drawRect(l,t,r,b,p);p.set(old);}
    private static void courseTable(Canvas c,Paint p){int left=18,top=92,right=366,bottom=720;Paint old=new Paint(p);p.setStyle(Paint.Style.STROKE);c.drawRect(left,top,right,bottom,p);int cols=6;for(int i=1;i<cols;i++){float x=left+(right-left)*i/(float)cols;c.drawLine(x,top,x,bottom,p);}int rows=8;for(int r=1;r<rows;r++){float y=top+(bottom-top)*r/(float)rows;c.drawLine(left,y,right,y,p);}p.setStyle(Paint.Style.FILL);p.setTextSize(16);String[] ds={"节","一","二","三","四","五"};for(int i=0;i<ds.length;i++)c.drawText(ds[i],left+8+(right-left)*i/(float)cols,top+24,p);p.set(old);}
    private static void dualCable(Canvas c,Paint p){Paint old=new Paint(p);for(int side=0;side<2;side++){int y=78+side*245;p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(24);c.drawText(side==0?"A 端":"B 端",18,y,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(18);c.drawText("线号：__________________",18,y+48,p);c.drawText("设备：__________________",18,y+88,p);c.drawText("端口：________  颜色：____",18,y+128,p);p.setStyle(Paint.Style.STROKE);c.drawRect(285,y+25,360,y+105,p);p.setStyle(Paint.Style.FILL);c.drawText("码",314,y+72,p);if(side==0)c.drawLine(18,y+185,366,y+185,p);}p.set(old);}
    private static void price(Canvas c,Paint p,int y){Paint old=new Paint(p);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(42);c.drawText("¥ ______.__",28,y,p);p.set(old);}
    private static void barcodeBox(Canvas c,Paint p,int y){
        Paint old=new Paint(p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRect(20,y,364,y+120,p);
        p.setStyle(Paint.Style.FILL);
        // 用真实“条纹示意”代替整块黑色占位，预览和热敏输出都不会再出现大黑块。
        int x=42;int[] widths={2,1,3,1,2,2,1,3,2,1,1,3,1,2,3,1,2,1,3,2,2,1,3,1,2,2,1,3,1,2,3,1,1,2,3,2,1,2,1,3};
        for(int i=0;i<widths.length&&x<342;i++){int w=widths[i]*2;if((i&1)==0)c.drawRect(x,y+18,x+w,y+78,p);x+=w+1;}
        p.setTextSize(14);p.setTypeface(Typeface.create("monospace",Typeface.NORMAL));c.drawText("1234567890",132,y+102,p);
        p.set(old);
    }
    private static void smallBox(Canvas c,Paint p,int x,int y,int w,int h){Paint old=new Paint(p);p.setStyle(Paint.Style.STROKE);c.drawRect(x,y,x+w,y+h,p);p.set(old);}
    private static void address(Canvas c,Paint p){c.drawText("收件人：________________",20,92,p);c.drawText("电话：__________________",20,140,p);c.drawText("地址：",20,198,p);Paint old=new Paint(p);p.setStyle(Paint.Style.STROKE);c.drawRect(20,215,364,380,p);p.set(old);c.drawText("备注：__________________",20,440,p);}
    private static void wrongQuestion(Canvas c,Paint p){String[] labs={"题目：","错误原因：","正确思路：","知识点："};int[] ys={88,250,420,575};for(int j=0;j<labs.length;j++){c.drawText(labs[j],18,ys[j],p);int bottom=(j==labs.length-1?650:ys[j]+125);Paint old=new Paint(p);p.setStyle(Paint.Style.STROKE);c.drawRect(18,ys[j]+15,366,bottom,p);p.set(old);}}
    private static void todo(Canvas c,Paint p){Paint old=new Paint(p);p.setStyle(Paint.Style.STROKE);for(int y=90;y<520;y+=58){c.drawRect(20,y-18,38,y,p);c.drawLine(58,y,360,y,p);}p.set(old);}
    private static void weekly(Canvas c,Paint p){String[] days={"一","二","三","四","五","六","日"};int top=78,row=88;Paint old=new Paint(p);for(int r=0;r<7;r++){p.setStyle(Paint.Style.FILL);c.drawText("周"+days[r],18,top+r*row+28,p);p.setStyle(Paint.Style.STROKE);c.drawRect(72,top+r*row,365,top+r*row+60,p);}p.set(old);}
    private static void reviewPlan(Canvas c,Paint p){String[] days={"当天","第 1 天","第 3 天","第 7 天","第 14 天","第 30 天"};int y=105;for(String d:days){c.drawText("□ "+d+"   掌握度：☆ ☆ ☆ ☆ ☆",22,y,p);c.drawLine(22,y+18,362,y+18,p);y+=86;}c.drawText("下次重点：________________________",22,650,p);}

    @Override protected void onDestroy(){for(Bitmap b:thumbs)try{if(b!=null&&!b.isRecycled())b.recycle();}catch(Throwable ignored){}thumbs.clear();super.onDestroy();}
}
