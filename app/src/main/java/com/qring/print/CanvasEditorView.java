package com.qring.print;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import android.view.ScaleGestureDetector;
import android.view.HapticFeedbackConstants;
import java.util.*;

final class CanvasEditorView extends View {
    static final int KIND_TEXT=0,KIND_IMAGE=1,KIND_CODE=2;
    static final class Element {Bitmap bmp;float x,y,w,h;int kind;boolean locked;Element(Bitmap b,float x,float y,float w,float h,int k){this(b,x,y,w,h,k,false);}Element(Bitmap b,float x,float y,float w,float h,int k,boolean l){this.bmp=b;this.x=x;this.y=y;this.w=w;this.h=h;this.kind=k;this.locked=l;}}
    private final ArrayList<Element> elements=new ArrayList<>();
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG),border=new Paint(Paint.ANTI_ALIAS_FLAG),guide=new Paint(Paint.ANTI_ALIAS_FLAG);
    private Element selected;private final LinkedHashSet<Element> multiSelected=new LinkedHashSet<>();private float lastX,lastY;private boolean resizing,snap=true,gestureSaved,scaling,multiMode;private int logicalHeight=620;private int fixedHeight=0;private final ScaleGestureDetector scaleDetector;
    private static final int HISTORY_LIMIT=8;
    private final ArrayDeque<State> undoStack=new ArrayDeque<>(),redoStack=new ArrayDeque<>();
    private static final class ElementState{final Bitmap bmp;final float x,y,w,h;final int kind;final boolean locked;ElementState(Element e){bmp=e.bmp;x=e.x;y=e.y;w=e.w;h=e.h;kind=e.kind;locked=e.locked;}Element make(){return new Element(bmp,x,y,w,h,kind,locked);}}
    private static final class State{final ArrayList<ElementState> items=new ArrayList<>();final int selectedIndex,logicalHeight;State(ArrayList<Element> src,Element sel,int lh){for(Element e:src)items.add(new ElementState(e));selectedIndex=sel==null?-1:src.indexOf(sel);logicalHeight=lh;}}

    CanvasEditorView(Context c){super(c);setBackgroundColor(Color.rgb(240,241,246));border.setStyle(Paint.Style.STROKE);border.setStrokeWidth(2);border.setColor(Ui.PRIMARY);guide.setStrokeWidth(1);guide.setColor(Color.rgb(222,225,234));scaleDetector=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScaleBegin(ScaleGestureDetector d){if(selected==null||selected.locked)return false;pushUndo();scaling=true;return true;}@Override public boolean onScale(ScaleGestureDetector d){if(selected==null)return false;float f=Math.max(.82f,Math.min(1.18f,d.getScaleFactor()));float nw=Math.max(24,Math.min(QringProtocol.WIDTH_DOTS,selected.w*f));float nh=Math.max(20,selected.h*f);float cx=selected.x+selected.w/2f,cy=selected.y+selected.h/2f;selected.w=nw;selected.h=nh;selected.x=clamp(cx-nw/2f,0,QringProtocol.WIDTH_DOTS-nw);selected.y=Math.max(0,cy-nh/2f);if(fixedHeight>0)selected.y=clamp(selected.y,0,Math.max(0,fixedHeight-nh));else logicalHeight=Math.max(logicalHeight,(int)(selected.y+nh+60));requestLayout();invalidate();return true;}@Override public void onScaleEnd(ScaleGestureDetector d){scaling=false;}});}
    void add(Bitmap b,float w,float h){add(b,w,h,KIND_TEXT);}
    void add(Bitmap b,float w,float h,int kind){if(b==null)return;pushUndo();float x=(QringProtocol.WIDTH_DOTS-w)/2f,y=Math.min(contentBottom()+16,pageHeight()-h);Element e=new Element(b,clamp(x,0,QringProtocol.WIDTH_DOTS-w),Math.max(0,y),w,h,kind);elements.add(e);selected=e;logicalHeight=Math.max(logicalHeight,(int)(e.y+h+60));requestLayout();invalidate();}
    void deleteSelected(){if(multiMode&&!multiSelected.isEmpty()){pushUndo();elements.removeAll(multiSelected);multiSelected.clear();selected=null;invalidate();return;}if(selected!=null){pushUndo();elements.remove(selected);selected=null;invalidate();}}
    void clearAll(){if(elements.isEmpty())return;pushUndo();elements.clear();multiSelected.clear();selected=null;logicalHeight=fixedHeight>0?fixedHeight:620;requestLayout();invalidate();}
    void duplicateSelected(){if(selected==null)return;pushUndo();Element e=new Element(selected.bmp,clamp(selected.x+16,0,QringProtocol.WIDTH_DOTS-selected.w),selected.y+16,selected.w,selected.h,selected.kind);elements.add(e);selected=e;logicalHeight=Math.max(logicalHeight,(int)(e.y+e.h+60));requestLayout();invalidate();}
    void bringFront(){if(selected==null||elements.indexOf(selected)==elements.size()-1)return;pushUndo();elements.remove(selected);elements.add(selected);invalidate();}
    void sendBack(){if(selected==null||elements.indexOf(selected)==0)return;pushUndo();elements.remove(selected);elements.add(0,selected);invalidate();}
    void rotateSelected(){if(selected==null)return;try{pushUndo();Matrix m=new Matrix();m.postRotate(90);Bitmap r=Bitmap.createBitmap(selected.bmp,0,0,selected.bmp.getWidth(),selected.bmp.getHeight(),m,true);selected.bmp=r;float old=selected.w;selected.w=Math.min(QringProtocol.WIDTH_DOTS-selected.x,selected.h);selected.h=old;selected.x=clamp(selected.x,0,QringProtocol.WIDTH_DOTS-selected.w);invalidate();}catch(Throwable ignored){}}
    boolean canUndo(){return !undoStack.isEmpty();}
    boolean canRedo(){return !redoStack.isEmpty();}
    void undo(){if(undoStack.isEmpty())return;redoStack.push(capture());restore(undoStack.pop());}
    void redo(){if(redoStack.isEmpty())return;undoStack.push(capture());trim(undoStack);restore(redoStack.pop());}
    private State capture(){return new State(elements,selected,logicalHeight);}
    private void pushUndo(){undoStack.push(capture());trim(undoStack);redoStack.clear();}
    private void trim(ArrayDeque<State> q){while(q.size()>HISTORY_LIMIT)q.removeLast();}
    private void restore(State state){elements.clear();for(ElementState es:state.items)elements.add(es.make());selected=state.selectedIndex>=0&&state.selectedIndex<elements.size()?elements.get(state.selectedIndex):null;multiSelected.clear();logicalHeight=fixedHeight>0?fixedHeight:state.logicalHeight;requestLayout();invalidate();}
    void setSnap(boolean v){snap=v;invalidate();}
    boolean isSnap(){return snap;}
    void setMultiMode(boolean v){multiMode=v;if(!v)multiSelected.clear();invalidate();}
    boolean isMultiMode(){return multiMode;}
    int multiCount(){return multiSelected.size();}
    boolean hasSelection(){return selected!=null;}
    float[] selectedBounds(){return selected==null?null:new float[]{selected.x,selected.y,selected.w,selected.h};}
    void setSelectedBounds(float x,float y,float w,float h){if(selected==null||selected.locked)return;pushUndo();w=Math.max(24,Math.min(QringProtocol.WIDTH_DOTS,w));h=Math.max(20,h);x=clamp(x,0,QringProtocol.WIDTH_DOTS-w);y=Math.max(0,y);if(fixedHeight>0){h=Math.min(h,fixedHeight);y=clamp(y,0,Math.max(0,fixedHeight-h));}selected.x=x;selected.y=y;selected.w=w;selected.h=h;if(fixedHeight==0)logicalHeight=Math.max(logicalHeight,(int)(y+h+60));requestLayout();invalidate();}
    void scaleSelected(float factor){if(selected==null||selected.locked)return;factor=Math.max(.25f,Math.min(4f,factor));float cx=selected.x+selected.w/2f,cy=selected.y+selected.h/2f;float nw=Math.max(24,Math.min(QringProtocol.WIDTH_DOTS,selected.w*factor)),nh=Math.max(20,selected.h*factor);setSelectedBounds(cx-nw/2f,cy-nh/2f,nw,nh);}
    boolean toggleLockSelected(){if(selected==null)return false;pushUndo();selected.locked=!selected.locked;invalidate();return selected.locked;}
    void mirrorSelected(){if(selected==null||selected.locked)return;try{pushUndo();Matrix m=new Matrix();m.setScale(-1,1);m.postTranslate(selected.bmp.getWidth(),0);Bitmap r=Bitmap.createBitmap(selected.bmp.getWidth(),selected.bmp.getHeight(),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(r);c.drawColor(Color.WHITE);c.drawBitmap(selected.bmp,m,p);selected.bmp=r;invalidate();}catch(Throwable ignored){}}
    boolean selectedLocked(){return selected!=null&&selected.locked;}
    void alignMulti(int mode){if(multiSelected.size()<2)return;pushUndo();float target;if(mode==0){target=Float.MAX_VALUE;for(Element e:multiSelected)target=Math.min(target,e.x);for(Element e:multiSelected)if(!e.locked)e.x=target;}else if(mode==2){target=-Float.MAX_VALUE;for(Element e:multiSelected)target=Math.max(target,e.x+e.w);for(Element e:multiSelected)if(!e.locked)e.x=target-e.w;}else{target=0;for(Element e:multiSelected)target+=e.x+e.w/2f;target/=multiSelected.size();for(Element e:multiSelected)if(!e.locked)e.x=clamp(target-e.w/2f,0,QringProtocol.WIDTH_DOTS-e.w);}invalidate();}
    void selectAll(){multiMode=true;multiSelected.clear();multiSelected.addAll(elements);selected=elements.isEmpty()?null:elements.get(elements.size()-1);invalidate();}
    void setFixedHeightDots(int dots){fixedHeight=Math.max(0,dots);if(fixedHeight>0)logicalHeight=fixedHeight;else logicalHeight=Math.max(620,(int)(contentBottom()+60));requestLayout();invalidate();}
    int getPageHeightDots(){return pageHeight();}
    private int pageHeight(){return fixedHeight>0?fixedHeight:logicalHeight;}
    private float contentBottom(){float v=16;for(Element e:elements)v=Math.max(v,e.y+e.h);return v;}

    @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws);float scale=w/(float)QringProtocol.WIDTH_DOTS;int want=(int)(pageHeight()*scale);setMeasuredDimension(w,Math.max(want,MeasureSpec.getSize(hs)));}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float s=getWidth()/(float)QringProtocol.WIDTH_DOTS;c.save();c.scale(s,s);p.setColor(Color.WHITE);c.drawRect(0,0,QringProtocol.WIDTH_DOTS,pageHeight(),p);if(snap){for(int x=48;x<QringProtocol.WIDTH_DOTS;x+=48)c.drawLine(x,0,x,pageHeight(),guide);for(int y=48;y<pageHeight();y+=48)c.drawLine(0,y,QringProtocol.WIDTH_DOTS,y,guide);c.drawLine(QringProtocol.WIDTH_DOTS/2f,0,QringProtocol.WIDTH_DOTS/2f,pageHeight(),guide);}for(Element e:elements){RectF dst=new RectF(e.x,e.y,e.x+e.w,e.y+e.h);c.drawBitmap(e.bmp,null,dst,p);boolean marked=e==selected||multiSelected.contains(e);if(marked){border.setColor(e.locked?Ui.WARNING:Ui.PRIMARY);border.setPathEffect(e.locked?new DashPathEffect(new float[]{8,6},0):null);c.drawRect(dst,border);p.setColor(e.locked?Ui.WARNING:Ui.PRIMARY);c.drawCircle(e.x+e.w,e.y+e.h,8,p);}}border.setPathEffect(null);c.restore();}

    @Override public boolean onTouchEvent(MotionEvent ev){scaleDetector.onTouchEvent(ev);if(scaling||ev.getPointerCount()>1)return true;float s=getWidth()/(float)QringProtocol.WIDTH_DOTS,x=ev.getX()/s,y=ev.getY()/s;switch(ev.getActionMasked()){
        case MotionEvent.ACTION_DOWN:Element before=selected;Element hit=find(x,y);if(multiMode&&hit!=null){multiSelected.add(hit);selected=hit;}else selected=hit;gestureSaved=false;if(selected!=null){if(selected!=before)Ui.haptic(this);try{getParent().requestDisallowInterceptTouchEvent(true);}catch(Throwable ignored){}resizing=!selected.locked&&Math.hypot(x-(selected.x+selected.w),y-(selected.y+selected.h))<24;lastX=x;lastY=y;invalidate();return true;}invalidate();break;
        case MotionEvent.ACTION_MOVE:if(selected!=null){if(selected.locked){lastX=x;lastY=y;return true;}float dx=x-lastX,dy=y-lastY;if(Math.abs(dx)+Math.abs(dy)>0.5f&&!gestureSaved){pushUndo();gestureSaved=true;}if(resizing){selected.w=Math.max(24,Math.min(QringProtocol.WIDTH_DOTS-selected.x,selected.w+dx));selected.h=Math.max(20,selected.h+dy);}else if(multiMode&&multiSelected.contains(selected)&&multiSelected.size()>1){for(Element e:multiSelected){if(e.locked)continue;e.x=clamp(e.x+dx,0,QringProtocol.WIDTH_DOTS-e.w);e.y=clamp(e.y+dy,0,Math.max(0,pageHeight()-e.h));}}else{selected.x=clamp(selected.x+dx,0,QringProtocol.WIDTH_DOTS-selected.w);selected.y=clamp(selected.y+dy,0,Math.max(0,pageHeight()-selected.h));if(snap){selected.x=snapX(selected.x,selected.w);selected.y=snap8(selected.y);}}lastX=x;lastY=y;if(fixedHeight==0)logicalHeight=Math.max(logicalHeight,(int)(selected.y+selected.h+60));requestLayout();invalidate();return true;}break;
        case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:resizing=false;gestureSaved=false;try{getParent().requestDisallowInterceptTouchEvent(false);}catch(Throwable ignored){}return selected!=null;}
        return true;}
    private float snapX(float x,float w){float[] targets={0,48,96,144,192-w/2f,192,240,288,336,QringProtocol.WIDTH_DOTS-w};float best=x,dist=7;for(float t:targets){float d=Math.abs(x-t);if(d<dist){dist=d;best=t;}}float center=x+w/2f;if(Math.abs(center-192)<7)best=192-w/2f;return clamp(best,0,QringProtocol.WIDTH_DOTS-w);}
    private float snap8(float v){float t=Math.round(v/8f)*8f;return Math.abs(t-v)<5?t:v;}
    private Element find(float x,float y){for(int i=elements.size()-1;i>=0;i--){Element e=elements.get(i);if(x>=e.x&&x<=e.x+e.w&&y>=e.y&&y<=e.y+e.h)return e;}return null;}
    private float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
    RasterEncoder.Raster compositeRaster(){
        int h=fixedHeight>0?fixedHeight:Math.max(80,(int)Math.ceil(contentBottom()+16));
        h=Math.min(8000,h);byte[] out=new byte[h*QringProtocol.WIDTH_BYTES];boolean keepMargins=false;
        for(Element e:elements){
            if(e.kind==KIND_CODE)keepMargins=true;
            int x0=Math.round(e.x),y0=Math.round(e.y),w=Math.max(1,Math.round(e.w)),eh=Math.max(1,Math.round(e.h));
            RasterEncoder.Dither mode=e.kind==KIND_IMAGE?RasterEncoder.Dither.FLOYD:RasterEncoder.Dither.THRESHOLD;
            int threshold=e.kind==KIND_CODE?128:(e.kind==KIND_TEXT?212:180);
            RasterEncoder.ElementRaster er=RasterEncoder.encodeElement(e.bmp,w,eh,threshold,mode);
            for(int yy=0;yy<er.height;yy++){
                int ty=y0+yy;if(ty<0||ty>=h)continue;
                int row=ty*QringProtocol.WIDTH_BYTES;
                for(int xx=0;xx<er.width;xx++){
                    if(!er.black(xx,yy))continue;int tx=x0+xx;if(tx<0||tx>=QringProtocol.WIDTH_DOTS)continue;
                    out[row+(tx>>3)]|=(byte)(0x80>>(tx&7));
                }
            }
        }
        return new RasterEncoder.Raster(h,out,keepMargins);
    }
    Bitmap compositeBinaryBitmap(){return bitmapFromRaster(compositeRaster());}
    Bitmap bitmapFromRaster(RasterEncoder.Raster r){
        if(r==null)return null;
        Bitmap b=Bitmap.createBitmap(QringProtocol.WIDTH_DOTS,r.height,Bitmap.Config.RGB_565);
        int[] row=new int[QringProtocol.WIDTH_DOTS];
        for(int y=0;y<r.height;y++){
            int base=y*QringProtocol.WIDTH_BYTES;
            for(int x=0;x<QringProtocol.WIDTH_DOTS;x++)row[x]=((r.data[base+(x>>3)]&(0x80>>(x&7)))!=0)?Color.BLACK:Color.WHITE;
            b.setPixels(row,0,QringProtocol.WIDTH_DOTS,0,y,QringProtocol.WIDTH_DOTS,1);
        }
        return b;
    }
    Bitmap composite(){int h=fixedHeight>0?fixedHeight:Math.max(80,(int)Math.ceil(contentBottom()+16));Bitmap out=Bitmap.createBitmap(QringProtocol.WIDTH_DOTS,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(out);c.drawColor(Color.WHITE);for(Element e:elements)c.drawBitmap(e.bmp,null,new RectF(e.x,e.y,e.x+e.w,e.y+e.h),p);return out;}
}
