package com.qring.print;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

/** Backward-compatible entry point from older builds. QR and barcodes are now deliberately separated. */
public class CodePrintActivity extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());}
    private View build(){
        LinearLayout root=Ui.page(this,"编码打印");
        root.addView(Ui.section(this,"请选择类型","二维码与条形码使用不同的输入规则和尺寸逻辑，现已拆成两个独立入口。"));
        LinearLayout card=Ui.card(this);
        Button qr=Ui.primaryButton(this,"二维码 · 文字 / 网址 / 中文");qr.setOnClickListener(v->startActivity(new Intent(this,QrCodeActivity.class)));card.addView(qr);
        Button bar=Ui.button(this,"条形码 · 一维 / 二维 / GS1");bar.setOnClickListener(v->startActivity(new Intent(this,BarcodePrintActivity.class)));card.addView(bar);
        root.addView(card);
        return root;
    }
}
