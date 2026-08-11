package com.qring.print;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;

final class DoodleView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final Paint cursor=new Paint(Paint.ANTI_ALIAS_FLAG);private final ArrayList<Path> paths=new ArrayList<>();private final ArrayList<Float> widths=new ArrayList<>();private Path current;private float brush=5f,lastX=-1,lastY=-1;
    DoodleView(Context c){super(c);setBackgroundColor(Color.WHITE);paint.setColor(Color.BLACK);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);cursor.setColor(0x805B5CE2);cursor.setStyle(Paint.Style.STROKE);cursor.setStrokeWidth(2);}
    void setBrush(float w){brush=Math.max(1,Math.min(28,w));invalidate();}
    float getBrush(){return brush;}
    void undo(){if(!paths.isEmpty()){paths.remove(paths.size()-1);widths.remove(widths.size()-1);invalidate();}}
    void clear(){paths.clear();widths.clear();current=null;invalidate();}
    boolean empty(){return paths.isEmpty();}
    Bitmap exportBitmap(){int w=Math.max(1,getWidth()),h=Math.max(1,getHeight());Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);drawPaths(c);return b;}
    private void drawPaths(Canvas c){for(int i=0;i<paths.size();i++){paint.setStrokeWidth(widths.get(i));c.drawPath(paths.get(i),paint);}}
    @Override protected void onDraw(Canvas c){super.onDraw(c);drawPaths(c);if(lastX>=0){c.drawCircle(lastX,lastY,Math.max(7,brush*1.4f),cursor);}}
    @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX(),y=e.getY();switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:current=new Path();current.moveTo(x,y);paths.add(current);widths.add(brush);lastX=x;lastY=y;invalidate();return true;case MotionEvent.ACTION_MOVE:if(current!=null){for(int i=0;i<e.getHistorySize();i++)current.lineTo(e.getHistoricalX(i),e.getHistoricalY(i));current.lineTo(x,y);}lastX=x;lastY=y;invalidate();return true;case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:if(current!=null)current.lineTo(x,y);current=null;lastX=-1;lastY=-1;invalidate();return true;}return true;}
}
