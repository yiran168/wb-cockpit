package com.qring.print;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.google.zxing.BarcodeFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CanvasEditorActivity extends Activity {
    private static final int PICK_IMAGE=601;
    private CanvasEditorView editor;private TextView modeHint;
    @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(build());}

    private View build(){
        LinearLayout root=Ui.page(this,"自定义打印");
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.HORIZONTAL);info.setGravity(Gravity.CENTER_VERTICAL);TextView canvasPill=Ui.pill(this,"203DPI · 384-dot 画布",Ui.PRIMARY);info.addView(canvasPill);modeHint=Ui.hint(this,"  拖动移动 · 右下角/双指缩放 · 精确坐标");modeHint.setPadding(Ui.dp(this,8),0,0,0);info.addView(modeHint,new LinearLayout.LayoutParams(0,-2,1));root.addView(info);

        HorizontalScrollView actionsScroll=new HorizontalScrollView(this);actionsScroll.setHorizontalScrollBarEnabled(false);LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);actionsScroll.addView(actions);
        String[] actionNames={"撤销","恢复","旋转","镜像","复制","锁定","多选","全选","左齐","居中","右齐","置顶","置底","删除","清空","吸附","精确位置","缩小10%","放大10%"};Button[] a=new Button[actionNames.length];for(int i=0;i<a.length;i++){a[i]=tool(actionNames[i]);actions.addView(a[i]);}root.addView(actionsScroll);

        editor=new CanvasEditorView(this);android.content.SharedPreferences ps=getSharedPreferences("settings",0);boolean label="label".equals(ps.getString("media_mode","continuous"));float mm=ps.getInt("label_length_tenths_mm",ps.getInt("paper_height_mm",0)*10)/10f;if(label&&mm>0)editor.setFixedHeightDots(Math.max(80,PrinterProfile.mmToDots(mm)));ScrollView canvasScroll=new ScrollView(this);canvasScroll.setFillViewport(false);canvasScroll.addView(editor,new ScrollView.LayoutParams(-1,Ui.dp(this,980)));FrameLayout frame=Ui.previewFrame(this,canvasScroll,"标签画布 · 所见即所得");root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));

        HorizontalScrollView insertScroll=new HorizontalScrollView(this);insertScroll.setHorizontalScrollBarEnabled(false);LinearLayout insert=new LinearLayout(this);insert.setOrientation(LinearLayout.HORIZONTAL);insertScroll.addView(insert);String[] insertNames={"+文字","+图片","+二维码","+图标","+线条","+涂鸦","+矩形","+圆形","+三角","+菱形","+日期","+表格","+条码"};Button[] b=new Button[insertNames.length];for(int i=0;i<b.length;i++){b[i]=tool(insertNames[i]);insert.addView(b[i]);}root.addView(insertScroll);
        String unit=getSharedPreferences("settings",0).getString("paper_unit","mm");String paperText=mm>0?("inch".equals(unit)?String.format(Locale.US,"固定标签 %.2f inch · 约 %d dots",mm/25.4,editor.getPageHeightDots()):("固定标签 "+mm+" mm · 约 "+editor.getPageHeightDots()+" dots")):"标签长度随内容自动增长";root.addView(Ui.hint(this,paperText));
        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.HORIZONTAL);Button save=Ui.button(this,"保存模板"),print=Ui.primaryButton(this,"预览确认并打印");bottom.addView(save,new LinearLayout.LayoutParams(0,Ui.dp(this,54),1));bottom.addView(print,new LinearLayout.LayoutParams(0,Ui.dp(this,54),1));root.addView(bottom);

        a[0].setOnClickListener(v->{if(editor.canUndo())editor.undo();else Ui.toast(this,"没有可撤销的操作");});a[1].setOnClickListener(v->{if(editor.canRedo())editor.redo();else Ui.toast(this,"没有可恢复的操作");});a[2].setOnClickListener(v->editor.rotateSelected());a[3].setOnClickListener(v->editor.mirrorSelected());a[4].setOnClickListener(v->editor.duplicateSelected());a[5].setOnClickListener(v->{if(!editor.hasSelection()){Ui.toast(this,"请先选择元素");return;}boolean locked=editor.toggleLockSelected();Ui.toast(this,locked?"元素已锁定":"元素已解锁");});a[6].setOnClickListener(v->{editor.setMultiMode(!editor.isMultiMode());modeHint.setText(editor.isMultiMode()?"多选模式 · 点击多个元素后可批量移动/对齐":"拖动移动 · 右下角/双指缩放 · 精确坐标");});a[7].setOnClickListener(v->{editor.selectAll();modeHint.setText("已全选 · 可批量移动/对齐");});a[8].setOnClickListener(v->editor.alignMulti(0));a[9].setOnClickListener(v->editor.alignMulti(1));a[10].setOnClickListener(v->editor.alignMulti(2));a[11].setOnClickListener(v->editor.bringFront());a[12].setOnClickListener(v->editor.sendBack());a[13].setOnClickListener(v->editor.deleteSelected());a[14].setOnClickListener(v->editor.clearAll());a[15].setOnClickListener(v->{editor.setSnap(!editor.isSnap());Ui.toast(this,editor.isSnap()?"智能吸附已开启":"智能吸附已关闭");});a[16].setOnClickListener(v->editBounds());a[17].setOnClickListener(v->{if(editor.hasSelection())editor.scaleSelected(.9f);else Ui.toast(this,"请先选择元素");});a[18].setOnClickListener(v->{if(editor.hasSelection())editor.scaleSelected(1.1f);else Ui.toast(this,"请先选择元素");});
        b[0].setOnClickListener(v->addText());b[1].setOnClickListener(v->pickImage());b[2].setOnClickListener(v->addCode());b[3].setOnClickListener(v->addIcon());b[4].setOnClickListener(v->addLine());b[5].setOnClickListener(v->addDoodle());b[6].setOnClickListener(v->addShape(0));b[7].setOnClickListener(v->addShape(1));b[8].setOnClickListener(v->addShape(2));b[9].setOnClickListener(v->addShape(3));b[10].setOnClickListener(v->addDate());b[11].setOnClickListener(v->addTable());b[12].setOnClickListener(v->addBarcode());save.setOnClickListener(v->saveTemplate());print.setOnClickListener(v->print());return root;
    }
    private Button tool(String s){Button b=Ui.button(this,s);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(Ui.dp(this,88),Ui.dp(this,44));p.setMargins(0,0,Ui.dp(this,6),Ui.dp(this,6));b.setLayoutParams(p);b.setTextSize(12);return b;}

    private void addText(){
        final LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),0);
        final EditText e=new EditText(this);e.setHint("文字内容");e.setMinLines(2);box.addView(e);
        final Spinner font=new Spinner(this);font.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"无衬线","衬线","等宽","圆体","窄体"}));box.addView(Ui.hint(this,"字体样式"));box.addView(font);
        final Spinner align=new Spinner(this);align.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"左对齐","居中","右对齐"}));box.addView(Ui.hint(this,"文字对齐"));box.addView(align);
        final TextView sizeLabel=Ui.hint(this,"字号 28sp");box.addView(sizeLabel);final SeekBar size=new SeekBar(this);size.setMax(60);size.setProgress(16);size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){sizeLabel.setText("字号 "+(12+p)+"sp");}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});box.addView(size);
        final TextView inkLabel=Ui.hint(this,"笔画粗细：标准");box.addView(inkLabel);final SeekBar ink=new SeekBar(this);ink.setMax(8);ink.setProgress(0);ink.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){inkLabel.setText("笔画粗细："+(p==0?"标准":"增强 "+p));}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});box.addView(ink);
        LinearLayout styles=new LinearLayout(this);styles.setOrientation(LinearLayout.HORIZONTAL);final CheckBox bold=new CheckBox(this);bold.setText("加粗");final CheckBox italic=new CheckBox(this);italic.setText("斜体");final CheckBox underline=new CheckBox(this);underline.setText("下划线");styles.addView(bold);styles.addView(italic);styles.addView(underline);box.addView(styles);
        new AlertDialog.Builder(this).setTitle("添加文字").setView(box).setPositiveButton("添加",(d,w)->{String text=e.getText().toString();if(text.trim().isEmpty())return;String family=font.getSelectedItemPosition()==1?"serif":font.getSelectedItemPosition()==2?"monospace":font.getSelectedItemPosition()==3?"sans-serif-rounded":font.getSelectedItemPosition()==4?"sans-serif-condensed":"sans-serif";int sp=12+size.getProgress();float px=sp*getResources().getDisplayMetrics().scaledDensity;Bitmap bmp=RasterEncoder.textBitmap(text,px,bold.isChecked(),italic.isChecked(),underline.isChecked(),align.getSelectedItemPosition(),4,0,family,1f,ink.getProgress()*.55f);int bw=Math.min(340,bmp.getWidth()),bh=Math.min(420,bmp.getHeight());editor.add(bmp,bw,bh,CanvasEditorView.KIND_TEXT);Ui.haptic(editor);}).setNegativeButton("取消",null).show();
    }

    private void editBounds(){
        float[] b=editor.selectedBounds();if(b==null){Ui.toast(this,"请先选择一个元素");return;}
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),0);
        EditText x=num("X 坐标 0..383",Math.round(b[0])),y=num("Y 坐标",Math.round(b[1])),w=num("宽度 dots",Math.round(b[2])),h=num("高度 dots",Math.round(b[3]));box.addView(x);box.addView(y);box.addView(w);box.addView(h);
        box.addView(Ui.hint(this,"也可以直接在画布上拖动；右下角手柄或双指手势缩放。这里用于精确输入坐标和尺寸。"));
        new AlertDialog.Builder(this).setTitle("精确位置 / 尺寸").setView(box).setPositiveButton("应用",(d,which)->{try{editor.setSelectedBounds(Float.parseFloat(x.getText().toString()),Float.parseFloat(y.getText().toString()),Float.parseFloat(w.getText().toString()),Float.parseFloat(h.getText().toString()));Ui.haptic(editor);}catch(Throwable e){Ui.toast(this,"请输入有效数字");}}).setNegativeButton("取消",null).show();
    }
    private EditText num(String hint,int value){EditText e=new EditText(this);e.setHint(hint);e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);e.setText(Integer.toString(value));e.setTextDirection(View.TEXT_DIRECTION_LTR);return e;}

    private void pickImage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");try{startActivityForResult(i,PICK_IMAGE);}catch(Throwable e){Ui.toast(this,"无法打开图片选择器");}}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==PICK_IMAGE&&result==RESULT_OK&&data!=null&&data.getData()!=null)loadImage(data.getData());}
    private void loadImage(Uri u){try{Bitmap b=ImageLoader.load(this,u);float w=260,h=w*b.getHeight()/(float)b.getWidth();editor.add(b,w,Math.max(30,Math.min(600,h)),CanvasEditorView.KIND_IMAGE);}catch(OutOfMemoryError e){Ui.toast(this,"图片过大，当前设备内存不足");}catch(Throwable e){Ui.toast(this,"图片读取失败："+e.getMessage());}}
    private void addCode(){final EditText e=new EditText(this);e.setHint("二维码内容");new AlertDialog.Builder(this).setTitle("添加二维码").setView(e).setPositiveButton("添加",(d,w)->{try{Bitmap b=BarcodeUtil.make(e.getText().toString(),BarcodeFormat.QR_CODE,240,240);editor.add(b,160,160,CanvasEditorView.KIND_CODE);}catch(Throwable x){Ui.toast(this,"二维码生成失败："+x.getMessage());}}).setNegativeButton("取消",null).show();}


    private void addBarcode(){
        final LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),0);
        final Spinner format=new Spinner(this);final String[] labels={"Code128","Code39","Code93","EAN-13","EAN-8","UPC-A","UPC-E","ITF","Codabar"};
        final BarcodeFormat[] formats={BarcodeFormat.CODE_128,BarcodeFormat.CODE_39,BarcodeFormat.CODE_93,BarcodeFormat.EAN_13,BarcodeFormat.EAN_8,BarcodeFormat.UPC_A,BarcodeFormat.UPC_E,BarcodeFormat.ITF,BarcodeFormat.CODABAR};
        format.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));box.addView(Ui.hint(this,"一维条码制式"));box.addView(format);
        final EditText e=new EditText(this);e.setHint("输入条码内容");e.setSingleLine(true);e.setTextDirection(View.TEXT_DIRECTION_LTR);box.addView(e);
        final TextView rule=Ui.hint(this,BarcodeUtil.rule(formats[0])+" · 示例："+BarcodeUtil.sample(formats[0]));box.addView(rule);
        format.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){BarcodeFormat f=formats[Math.max(0,Math.min(formats.length-1,pos))];rule.setText(BarcodeUtil.rule(f)+" · 示例："+BarcodeUtil.sample(f));}public void onNothingSelected(AdapterView<?> p){}});
        new AlertDialog.Builder(this).setTitle("添加一维条码").setView(box).setPositiveButton("添加",(d,w)->{try{BarcodeFormat f=formats[format.getSelectedItemPosition()];String prepared=BarcodeUtil.prepare(e.getText().toString(),f);Bitmap code=BarcodeUtil.make(prepared,f,340,112);Bitmap withText=BarcodeUtil.withCaption(code,prepared,18*getResources().getDisplayMetrics().scaledDensity);if(!code.isRecycled())code.recycle();editor.add(withText,340,Math.min(180,withText.getHeight()),CanvasEditorView.KIND_CODE);Ui.haptic(editor);}catch(Throwable x){Ui.toast(this,"条码生成失败："+BarcodeUtil.friendly(x));}}).setNegativeButton("取消",null).show();
    }

    private void addIcon(){
        String[] names={"★ 星标","♥ 爱心","✓ 对勾","! 提醒","→ 箭头","☎ 电话","⌂ 地址","￥ 价格","● 圆点","□ 方框"};
        new AlertDialog.Builder(this).setTitle("本地图标库").setItems(names,(d,which)->{Bitmap b=makeIcon(which);editor.add(b,96,96,CanvasEditorView.KIND_IMAGE);}).setNegativeButton("取消",null).show();
    }
    private Bitmap makeIcon(int which){
        Bitmap b=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(which==6?72:86);String[] glyph={"★","♥","✓","!","→","☎","⌂","￥","●","□"};Paint.FontMetrics fm=p.getFontMetrics();float y=64-(fm.ascent+fm.descent)/2f;c.drawText(glyph[Math.max(0,Math.min(glyph.length-1,which))],64,y,p);return b;
    }

    private void addLine(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);Spinner style=new Spinner(this);style.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"实线","虚线","点线"}));SeekBar width=new SeekBar(this);width.setMax(10);width.setProgress(2);box.addView(Ui.hint(this,"线型"));box.addView(style);box.addView(Ui.hint(this,"粗细"));box.addView(width);new AlertDialog.Builder(this).setTitle("添加线条").setView(box).setPositiveButton("添加",(d,w)->{Bitmap b=Bitmap.createBitmap(340,18,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1+width.getProgress());if(style.getSelectedItemPosition()==1)p.setPathEffect(new DashPathEffect(new float[]{18,10},0));else if(style.getSelectedItemPosition()==2)p.setPathEffect(new DashPathEffect(new float[]{3,9},0));c.drawLine(2,9,338,9,p);editor.add(b,300,18);}).setNegativeButton("取消",null).show();}

    private void addDoodle(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(Ui.dp(this,10),0,Ui.dp(this,10),0);
        DoodleView draw=new DoodleView(this);box.addView(draw,new LinearLayout.LayoutParams(-1,Ui.dp(this,300)));
        TextView label=Ui.hint(this,"画笔 6");box.addView(label);SeekBar brush=new SeekBar(this);brush.setMax(24);brush.setProgress(5);brush.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){draw.setBrush(1+p);label.setText("画笔 "+(1+p));}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});box.addView(brush);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button undo=Ui.button(this,"撤销一笔"),clear=Ui.button(this,"清空");undo.setOnClickListener(v->draw.undo());clear.setOnClickListener(v->draw.clear());actions.addView(undo,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));actions.addView(clear,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));box.addView(actions);
        new AlertDialog.Builder(this).setTitle("图片涂鸦").setView(box).setPositiveButton("添加到画布",(d,w)->{if(draw.empty()){Ui.toast(this,"还没有绘制内容");return;}Bitmap b=draw.exportBitmap();editor.add(b,300,220,CanvasEditorView.KIND_IMAGE);}).setNegativeButton("取消",null).show();
    }
    private void addShape(int type){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(Ui.dp(this,8),0,Ui.dp(this,8),0);
        TextView thickLabel=Ui.hint(this,"边框粗细：5 dots");box.addView(thickLabel);SeekBar thick=new SeekBar(this);thick.setMax(15);thick.setProgress(4);thick.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){thickLabel.setText("边框粗细："+(p+1)+" dots");}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});box.addView(thick);
        CheckBox fill=new CheckBox(this);fill.setText("实心填充");box.addView(fill);
        String[] names={"矩形","圆形","三角形","菱形"};
        new AlertDialog.Builder(this).setTitle("添加"+names[Math.max(0,Math.min(3,type))]).setView(box).setPositiveButton("添加",(d,w)->{
            int stroke=1+thick.getProgress();Bitmap b=Bitmap.createBitmap(240,140,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);p.setStyle(fill.isChecked()?Paint.Style.FILL:Paint.Style.STROKE);p.setStrokeWidth(stroke);float inset=Math.max(3,stroke/2f+2);
            if(type==0)c.drawRect(inset,inset,240-inset,140-inset,p);else if(type==1)c.drawOval(new RectF(inset,inset,240-inset,140-inset),p);else{Path path=new Path();if(type==2){path.moveTo(120,inset);path.lineTo(240-inset,140-inset);path.lineTo(inset,140-inset);}else{path.moveTo(120,inset);path.lineTo(240-inset,70);path.lineTo(120,140-inset);path.lineTo(inset,70);}path.close();c.drawPath(path,p);}editor.add(b,180,100);Ui.haptic(editor);
        }).setNegativeButton("取消",null).show();
    }
    private void addTable(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        EditText rows=new EditText(this);rows.setHint("行数 1-10");rows.setInputType(2);rows.setText("3");
        EditText cols=new EditText(this);cols.setHint("列数 1-6");cols.setInputType(2);cols.setText("2");box.addView(rows);box.addView(cols);
        new AlertDialog.Builder(this).setTitle("添加表格").setView(box).setPositiveButton("添加",(d,w)->{
            int r=3,c=2;try{r=Integer.parseInt(rows.getText().toString());}catch(Exception ignored){}try{c=Integer.parseInt(cols.getText().toString());}catch(Exception ignored){}
            r=Math.max(1,Math.min(10,r));c=Math.max(1,Math.min(6,c));int bw=340,bh=Math.max(60,r*48);
            Bitmap b=Bitmap.createBitmap(bw,bh,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(Color.WHITE);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
            for(int y=0;y<=r;y++){float yy=y*bh/(float)r;canvas.drawLine(1,yy,bw-1,yy,p);}for(int x=0;x<=c;x++){float xx=x*bw/(float)c;canvas.drawLine(xx,1,xx,bh-1,p);}editor.add(b,330,Math.min(520,bh));
        }).setNegativeButton("取消",null).show();
    }
    private void addDate(){
        final Spinner fmt=new Spinner(this);
        fmt.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{
                "yyyy-MM-dd","yyyy/MM/dd","yyyy.MM.dd","dd-MM-yyyy","dd/MM/yyyy","MM-dd-yyyy","MM/dd/yyyy",
                "yyyy年MM月dd日","MM月dd日","yyyy-MM-dd HH:mm","yyyy/MM/dd HH:mm","MM-dd HH:mm","HH:mm","HH:mm:ss"}));
        final EditText specified=new EditText(this);specified.setHint("指定时间（可空）：yyyy-MM-dd HH:mm");
        final EditText dayOffset=new EditText(this);dayOffset.setHint("偏移天数，例如 0 / 1 / -1");dayOffset.setText("0");
        final EditText minuteOffset=new EditText(this);minuteOffset.setHint("偏移分钟，例如 0 / 30 / -15");minuteOffset.setText("0");
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        box.addView(fmt);box.addView(specified);box.addView(dayOffset);box.addView(minuteOffset);
        new AlertDialog.Builder(this).setTitle("日期 / 时间").setView(box).setPositiveButton("添加",(d,w)->{
            try{
                Calendar cal=Calendar.getInstance();
                String base=specified.getText().toString().trim();
                if(!base.isEmpty()){
                    SimpleDateFormat input=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault());
                    input.setLenient(false);
                    java.util.Date parsed=input.parse(base);
                    if(parsed==null)throw new IllegalArgumentException("指定时间格式错误");
                    cal.setTime(parsed);
                }
                int days=0,mins=0;
                try{days=Integer.parseInt(dayOffset.getText().toString().trim());}catch(Exception ignored){}
                try{mins=Integer.parseInt(minuteOffset.getText().toString().trim());}catch(Exception ignored){}
                cal.add(Calendar.DAY_OF_YEAR,Math.max(-36500,Math.min(36500,days)));
                cal.add(Calendar.MINUTE,Math.max(-5256000,Math.min(5256000,mins)));
                String pattern=(String)fmt.getSelectedItem();
                String text=new SimpleDateFormat(pattern,Locale.getDefault()).format(cal.getTime());
                Bitmap bmp=RasterEncoder.textBitmap(text,25*getResources().getDisplayMetrics().scaledDensity,false,1,4);
                editor.add(bmp,Math.min(340,bmp.getWidth()),Math.min(120,bmp.getHeight()));
            }catch(Throwable x){Ui.toast(this,"日期设置失败："+x.getMessage());}
        }).setNegativeButton("取消",null).show();
    }
    private void saveTemplate(){Bitmap b=editor.compositeBinaryBitmap();final EditText e=new EditText(this);e.setHint("模板名称");e.setText("标签 "+HistoryStore.formatTime(System.currentTimeMillis()));new AlertDialog.Builder(this).setTitle("保存模板").setView(e).setPositiveButton("保存",(d,w)->{try{TemplateStore.save(this,e.getText().toString(),b);Ui.toast(this,"模板已保存");}catch(Throwable x){Ui.toast(this,"保存失败："+x.getMessage());}}).setNegativeButton("取消",null).show();}
    private void print(){try{RasterEncoder.Raster r=editor.compositeRaster();PrinterManager.get(this).print(this,new PrinterManager.BitmapHolder(r),(ok,msg)->{Ui.toast(this,msg);if(ok)HistoryStore.addRaster(this,"自定义画布","自定义标签",r);});}catch(OutOfMemoryError e){Ui.toast(this,"标签过长，当前设备内存不足，请缩短后再打印");}catch(Throwable e){Ui.toast(this,"画布生成失败："+e.getMessage());}}
}
