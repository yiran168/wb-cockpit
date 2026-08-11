package com.qring.print;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** In-app release notes, usage notes and open-source attribution. */
public class ReleaseInfoActivity extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());}

    private View build(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);LinearLayout root=Ui.page(this,"版本说明与开源致谢");sc.addView(root);

        LinearLayout thanks=Ui.card(this);thanks.setPadding(Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,18),Ui.dp(this,18));
        thanks.addView(Ui.pill(this,"特别感谢 · 开源参考",Ui.SUCCESS));
        TextView title=new TextView(this);title.setText("Thisko / QrintPrint");title.setTextSize(22);title.setTextColor(Ui.TEXT);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setPadding(0,Ui.dp(this,12),0,Ui.dp(this,6));thanks.addView(title);
        TextView body=Ui.hint(this,"感谢 Thisko 大佬公开 QrintPrint。这个 Android 版本把该项目作为重要开源参考来源之一，尤其参考其公开实现与说明中的 Qring / BeePrt 经典蓝牙 SPP、384 dots（48 bytes/行）光栅、1024-byte 分包和相关协议思路。Android UI、纸张/标签排版算法、二维码与条码工作流、模板、文档处理、画布编辑、兼容层及交互均在本项目中继续实现和扩展。\n\n开源参考不代表原作者、设备厂商与本应用存在官方合作、授权或背书；使用时请遵守上游许可证。应用内其它说明统一采用“使用说明/功能说明”，不使用品牌比较或官方化表述。");body.setTextSize(14);thanks.addView(body);
        Button repo=Ui.primaryButton(this,"打开 Thisko / QrintPrint 开源仓库");repo.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Thisko/QrintPrint")));}catch(Throwable e){Ui.toast(this,"无法打开浏览器");}});thanks.addView(repo);root.addView(thanks);

        root.addView(Ui.section(this,"纸张尺寸为什么不能自动识别","这是硬件能力边界，不在 APP 中伪造自动检测。"));
        LinearLayout media=Ui.card(this);media.addView(Ui.hint(this,
                "这类机器的红外传感器只可靠判断“有纸 / 缺纸”，不会返回纸宽、标签宽、标签长度，也没有摄像头或边缘阵列可以测量介质尺寸。\n\n"+
                "因此 v1.5.1 使用“用户声明尺寸 + 203DPI 点数换算 + 内容边界分析”的方式：\n"+
                "• 纸张/标签宽度：10–57mm 手动设置；窄标签只使用 384-dot 打印头的一部分。\n"+
                "• 连续纸：可按黑色有效内容自动裁左右空白并在最后一行内容后结束。\n"+
                "• 标签纸：宽度和长度均手动设置；内容过大时等比例缩小以完整放入固定标签。\n"+
                "• 内容宽度：自动按有效内容 / 铺满可打印区 / 自定义毫米三种模式。\n"+
                "• 对齐与校准：纸张装载位置、内容对齐、X/Y 偏移、前后走纸都可校准。\n\n"+
                "57mm 是耗材物理宽度；384 dots 在 203DPI 下约为 48.05mm，这是当前打印头真正能成像的最大宽度。APP 不会把 57mm 虚构成超过打印头能力的点数。"));root.addView(media);

        root.addView(Ui.section(this,"预览 = 实际发送点阵","最终确认页与真正打印共用同一套排版函数。"));
        LinearLayout wysiwyg=Ui.card(this);wysiwyg.addView(Ui.hint(this,
                "文字、图片、PDF、Office、网页、二维码、条码、模板、批量任务等在真正发送前都进入最终确认页。最终确认页调用与打印发送完全相同的纸宽、内容宽度、标签长度、旋转和 X/Y 校准算法，显示的就是即将发送的 384-dot 黑白点阵。\n\n编辑过程中的彩色/源文件预览用于调整内容；“确认最终打印效果”中的黑白点阵才是打印输出的权威预览。"));root.addView(wysiwyg);

        root.addView(Ui.section(this,"二维码与条码已经分开","不再把 QR、EAN、UPC、ITF、PDF417 等混在同一个入口里。"));
        LinearLayout codes=Ui.card(this);codes.addView(Ui.hint(this,
                "二维码：独立页面，支持 UTF-8 中文、网址、文本以及 L/M/Q/H 容错级别。\n\n"+
                "条形码：独立页面，再分为“一维条码”和“二维条码”。一维包含 Code 128 / 39 / 93、EAN-13 / EAN-8、UPC-A / UPC-E、ITF、Codabar、GS1-128；二维条码包含 Data Matrix、PDF417、Aztec。\n\n"+
                "v1.5.1 会先按制式校验输入：EAN/UPC 的位数、ITF 偶数位、Code39 字符集等都在生成前给出中文提示，并提供每种制式的合法示例。空输入不再拿一个错误默认字符串去硬生成，因此不会再出现之前截图里 ITF / UPC / EAN / PDF417 的空白预览连锁报错。二维码和条码使用保留静区的点阵路径，避免自动裁白边破坏可扫描性。"));root.addView(codes);

        root.addView(Ui.section(this,"v1.5.1 使用说明","本版在真实纸张几何、所见即所得和编码可靠性的基础上，补充 40 张本地模板和可配置打印点击音效。"));
        LinearLayout use=Ui.card(this);use.addView(Ui.hint(this,
                "• 图片：8 种黑白/抖动算法，可缩放预览；最终宽度由全局纸张规则决定。\n"+
                "• 文本：字体、字号、粗细、斜体、下划线、字距、行距、拉伸和对齐。\n"+
                "• 自定义打印：元素拖动、双指缩放、精确 XYWH、旋转、图层和吸附。\n"+
                "• 模板/标签：40 张本地模板；先设置实际标签宽度和长度，再按最终点阵预览确认。\n"+
                "• 打印音效：10 种内置短音，可固定选择、随机使用 10 种，或每次随机生成；只在最终确认打印时播放，不写入打印数据。\n"+
                "• 打印：经典蓝牙 SPP 直连；打印前读取设备状态，最终发送固定 384 dots/行的黑白光栅。"));root.addView(use);

        TextView foot=Ui.hint(this,"第三方本地打印工具 · 版本 1.5.1 · 独立应用 ID com.yiran168.cuotiprint");foot.setGravity(Gravity.CENTER);foot.setPadding(0,Ui.dp(this,16),0,Ui.dp(this,12));root.addView(foot);
        return sc;
    }
}
