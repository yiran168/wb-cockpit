package com.qring.print;

import android.graphics.*;

final class RasterEncoder {
    enum Dither { THRESHOLD, FLOYD, ATKINSON, JARVIS, SIERRA_LITE, STUCKI, ORDERED, BAYER }

    static final class Raster {
        final int height;
        final byte[] data;
        final boolean preserveMargins;
        Raster(int h, byte[] d) { this(h,d,false); }
        Raster(int h, byte[] d, boolean keepMargins) { height=h; data=d; preserveMargins=keepMargins; }
    }
    static final class ElementRaster {
        final int width,height,rowBytes; final byte[] data;
        ElementRaster(int w,int h,int rb,byte[] d){width=w;height=h;rowBytes=rb;data=d;}
        boolean black(int x,int y){return x>=0&&x<width&&y>=0&&y<height&&(data[y*rowBytes+(x>>3)]&(0x80>>(x&7)))!=0;}
    }

    private RasterEncoder(){}

    static Bitmap fitToWidth(Bitmap src) {
        int h = Math.max(1, Math.min(8000, Math.round(src.getHeight() * QringProtocol.WIDTH_DOTS / (float)Math.max(1,src.getWidth()))));
        if (src.getWidth() == QringProtocol.WIDTH_DOTS && src.getHeight()==h) return src.copy(Bitmap.Config.ARGB_8888, false);
        return Bitmap.createScaledBitmap(src, QringProtocol.WIDTH_DOTS, h, true);
    }

    static Raster encode(Bitmap source, int threshold, Dither mode) {
        Bitmap bmp = fitToWidth(source);
        int w = QringProtocol.WIDTH_DOTS, h = bmp.getHeight();
        byte[] out;
        if (isErrorDiffusion(mode)) out=diffuse(bmp,w,h,threshold,mode,QringProtocol.WIDTH_BYTES);
        else out=orderedOrThreshold(bmp,w,h,threshold,mode,QringProtocol.WIDTH_BYTES);
        if(bmp!=source&&!bmp.isRecycled())bmp.recycle();
        return new Raster(h,out);
    }

    /** Barcodes/QR require their quiet zones; global blank-edge trimming must not remove them. */
    static Raster encodePreserveMargins(Bitmap source,int threshold,Dither mode){
        Raster r=encode(source,threshold,mode);return new Raster(r.height,r.data,true);
    }

    static ElementRaster encodeElement(Bitmap source,int targetWidth,int targetHeight,int threshold,Dither mode){
        int w=Math.max(1,Math.min(QringProtocol.WIDTH_DOTS,targetWidth));
        int h=Math.max(1,Math.min(8000,targetHeight));
        Bitmap bmp=(source.getWidth()==w&&source.getHeight()==h)?source:Bitmap.createScaledBitmap(source,w,h,true);
        int rb=(w+7)/8;
        byte[] out=isErrorDiffusion(mode)?diffuse(bmp,w,h,threshold,mode,rb):orderedOrThreshold(bmp,w,h,threshold,mode,rb);
        if(bmp!=source&&!bmp.isRecycled())bmp.recycle();
        return new ElementRaster(w,h,rb,out);
    }

    private static boolean isErrorDiffusion(Dither mode){
        return mode==Dither.FLOYD||mode==Dither.ATKINSON||mode==Dither.JARVIS||mode==Dither.SIERRA_LITE||mode==Dither.STUCKI;
    }

    /**
     * Error-diffusion family with a three-row rolling error buffer.  This keeps memory bounded
     * even for very long 57mm receipts while supporting kernels that reach two rows down.
     */
    private static byte[] diffuse(Bitmap bmp,int w,int h,int threshold,Dither mode,int rowBytes){
        byte[] out=new byte[h*rowBytes];
        int[] px=new int[w];
        float[][] err=new float[][]{new float[w+4],new float[w+4],new float[w+4]};
        int[][] kernel;float divisor;
        switch(mode){
            case ATKINSON:
                kernel=new int[][]{{1,0,1},{2,0,1},{-1,1,1},{0,1,1},{1,1,1},{0,2,1}};divisor=8f;break;
            case JARVIS:
                kernel=new int[][]{{1,0,7},{2,0,5},{-2,1,3},{-1,1,5},{0,1,7},{1,1,5},{2,1,3},{-2,2,1},{-1,2,3},{0,2,5},{1,2,3},{2,2,1}};divisor=48f;break;
            case SIERRA_LITE:
                kernel=new int[][]{{1,0,2},{-1,1,1},{0,1,1}};divisor=4f;break;
            case STUCKI:
                kernel=new int[][]{{1,0,8},{2,0,4},{-2,1,2},{-1,1,4},{0,1,8},{1,1,4},{2,1,2},{-2,2,1},{-1,2,2},{0,2,4},{1,2,2},{2,2,1}};divisor=42f;break;
            case FLOYD:
            default:
                kernel=new int[][]{{1,0,7},{-1,1,3},{0,1,5},{1,1,1}};divisor=16f;break;
        }
        for(int y=0;y<h;y++){
            bmp.getPixels(px,0,w,0,y,w,1);int base=y*rowBytes;
            for(int x=0;x<w;x++){
                float old=clampf(gray(px[x])+err[0][x+2],0f,255f);
                float nv=old<threshold?0f:255f;
                if(nv==0f)out[base+(x>>3)]|=(byte)(0x80>>(x&7));
                float e=old-nv;
                for(int[] k:kernel){int tx=x+k[0],ty=k[1];if(tx>=0&&tx<w&&ty>=0&&ty<3)err[ty][tx+2]+=e*k[2]/divisor;}
            }
            float[] first=err[0];err[0]=err[1];err[1]=err[2];err[2]=first;java.util.Arrays.fill(err[2],0f);
        }
        return out;
    }

    private static byte[] orderedOrThreshold(Bitmap bmp,int w,int h,int threshold,Dither mode,int rowBytes){
        byte[] out=new byte[h*rowBytes];int[] row=new int[w];
        int[][] ordered={{0,32,8,40,2,34,10,42},{48,16,56,24,50,18,58,26},{12,44,4,36,14,46,6,38},{60,28,52,20,62,30,54,22},{3,35,11,43,1,33,9,41},{51,19,59,27,49,17,57,25},{15,47,7,39,13,45,5,37},{63,31,55,23,61,29,53,21}};
        int[][] bayer={{0,8,2,10},{12,4,14,6},{3,11,1,9},{15,7,13,5}};
        for(int y=0;y<h;y++){
            bmp.getPixels(row,0,w,0,y,w,1);int base=y*rowBytes;
            for(int x=0;x<w;x++){
                int lt=threshold;
                if(mode==Dither.ORDERED)lt=clamp(threshold+(((ordered[y&7][x&7]*4)-126)/2),0,255);
                else if(mode==Dither.BAYER)lt=clamp(threshold+(((bayer[y&3][x&3]*16)-128)/2),0,255);
                if(gray(row[x])<lt)out[base+(x>>3)]|=(byte)(0x80>>(x&7));
            }
        }
        return out;
    }

    private static float clampf(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}

    private static void loadGrayRowSized(Bitmap bmp,int y,int[] px,float[] dst,int w){
        bmp.getPixels(px,0,w,0,y,w,1);for(int x=0;x<w;x++)dst[x]=gray(px[x]);
    }

    /** 将打印光栅裁切/补白到固定高度；0 或负数表示保持自动长度。 */
    static Raster fitHeight(Raster src,int targetHeight){
        if(src==null||targetHeight<=0)return src;
        int h=Math.max(1,Math.min(8000,targetHeight));
        if(src.height==h)return src;
        byte[] out=new byte[h*QringProtocol.WIDTH_BYTES];
        int rows=Math.min(src.height,h);
        System.arraycopy(src.data,0,out,0,rows*QringProtocol.WIDTH_BYTES);
        return new Raster(h,out);
    }

    /** 180° 打印方向：逐行倒序 + 每字节位反转，保持 384 点宽度不变。 */
    static Raster rotate180(Raster src){
        if(src==null)return null;
        byte[] out=new byte[src.data.length];
        int rb=QringProtocol.WIDTH_BYTES;
        for(int y=0;y<src.height;y++){
            int from=y*rb,to=(src.height-1-y)*rb;
            for(int b=0;b<rb;b++)out[to+(rb-1-b)]=reverseBits(src.data[from+b]);
        }
        return new Raster(src.height,out);
    }

    private static byte reverseBits(byte value){
        int v=value&0xff;v=((v&0x55)<<1)|((v>>>1)&0x55);v=((v&0x33)<<2)|((v>>>2)&0x33);v=((v&0x0f)<<4)|((v>>>4)&0x0f);return (byte)v;
    }

    /** 垂直偏移，保持总标签高度不变；正数向出纸方向后移，负数向前移。 */
    static Raster shiftY(Raster src,int offset){
        if(src==null||offset==0)return src;
        offset=Math.max(-1000,Math.min(1000,offset));
        byte[] out=new byte[src.data.length];int rb=QringProtocol.WIDTH_BYTES;
        for(int y=0;y<src.height;y++){int ty=y+offset;if(ty<0||ty>=src.height)continue;System.arraycopy(src.data,y*rb,out,ty*rb,rb);}
        return new Raster(src.height,out);
    }

    static Raster shiftX(Raster src,int offset){
        if(src==null||offset==0)return src;
        offset=Math.max(-96,Math.min(96,offset));
        byte[] out=new byte[src.data.length];
        for(int y=0;y<src.height;y++){
            int row=y*QringProtocol.WIDTH_BYTES;
            for(int x=0;x<QringProtocol.WIDTH_DOTS;x++){
                int b=src.data[row+(x>>3)]&0xff;
                if((b&(0x80>>(x&7)))==0)continue;
                int tx=x+offset;
                if(tx>=0&&tx<QringProtocol.WIDTH_DOTS)out[row+(tx>>3)]|=(byte)(0x80>>(tx&7));
            }
        }
        return new Raster(src.height,out);
    }

    /** 全局内容缩放：保持 384-dot 纸宽，围绕纸张中心缩放；小于 100% 留白，大于 100% 居中裁切。 */
    static Raster scaleCentered(Raster src,int percent){
        if(src==null||percent==100)return src;
        percent=Math.max(50,Math.min(200,percent));float f=percent/100f;
        int outH=Math.max(1,Math.min(8000,Math.round(src.height*f)));byte[] out=new byte[outH*QringProtocol.WIDTH_BYTES];
        float cx=(QringProtocol.WIDTH_DOTS-1)/2f;
        for(int y=0;y<outH;y++){
            int sy=Math.max(0,Math.min(src.height-1,Math.round((y+.5f)/f-.5f)));int srcRow=sy*QringProtocol.WIDTH_BYTES;int outRow=y*QringProtocol.WIDTH_BYTES;
            for(int x=0;x<QringProtocol.WIDTH_DOTS;x++){
                float fx=(x-cx)/f+cx;int sx=Math.round(fx);if(sx<0||sx>=QringProtocol.WIDTH_DOTS)continue;
                if((src.data[srcRow+(sx>>3)]&(0x80>>(sx&7)))!=0)out[outRow+(x>>3)]|=(byte)(0x80>>(x&7));
            }
        }
        return new Raster(outH,out);
    }

    /**
     * 将已经生成的 384-dot 黑白内容重新排版到“用户声明的纸宽/标签宽”内。
     *
     * 机器没有纸宽传感器，因此 paperWidthDots 是用户校准值：窄标签会只使用热敏头的一部分；
     * 57mm 卷纸最多仍只有 384 个真实打印点。contentWidthMode: 0=按内容自动，1=铺满可用纸宽，
     * 2=自定义内容宽度。paperAlign/contentAlign: 0=居中，1=左，2=右。
     *
     * 该函数同时用于最终预览和真正发送，保证“预览是什么，打印点阵就是什么”。
     */
    static Raster layoutToMedia(Raster src,int paperWidthDots,int contentWidthMode,int customContentWidthDots,
                                int paperAlign,int contentAlign,int verticalAlign,int percent,boolean trimSides,
                                boolean trimBottom,int fixedHeightDots){
        if(src==null)return null;
        paperWidthDots=Math.max(1,Math.min(QringProtocol.WIDTH_DOTS,paperWidthDots));
        percent=Math.max(25,Math.min(200,percent));
        if(src.preserveMargins){trimSides=false;trimBottom=false;}
        int[] b=blackBounds(src);
        int fixed=fixedHeightDots<=0?0:Math.max(1,Math.min(8000,fixedHeightDots));
        if(b==null)return new Raster(fixed>0?fixed:Math.max(1,Math.min(8000,src.height)),new byte[(fixed>0?fixed:Math.max(1,Math.min(8000,src.height)))*QringProtocol.WIDTH_BYTES]);

        int sx0=trimSides?b[0]:0, sx1=trimSides?b[1]:QringProtocol.WIDTH_DOTS-1;
        int sy0=trimBottom?b[2]:0, sy1=trimBottom?b[3]:src.height-1;
        int sourceW=Math.max(1,sx1-sx0+1), sourceH=Math.max(1,sy1-sy0+1);

        int wanted;
        if(contentWidthMode==1)wanted=paperWidthDots;
        else if(contentWidthMode==2)wanted=Math.max(1,Math.min(paperWidthDots,customContentWidthDots));
        else wanted=Math.min(paperWidthDots,sourceW);
        wanted=Math.max(1,Math.min(paperWidthDots,Math.round(wanted*(percent/100f))));

        int scaledH=Math.max(1,Math.min(8000,Math.round(sourceH*(wanted/(float)sourceW))));
        // 固定标签长度下绝不把内容悄悄裁掉；若太高则继续等比例缩小，直到能完整放入标签。
        if(fixed>0 && scaledH>fixed){
            float fit=fixed/(float)scaledH;
            wanted=Math.max(1,Math.min(paperWidthDots,Math.round(wanted*fit)));
            scaledH=Math.max(1,Math.min(fixed,Math.round(sourceH*(wanted/(float)sourceW))));
        }

        int outH=fixed>0?fixed:scaledH;
        byte[] out=new byte[outH*QringProtocol.WIDTH_BYTES];
        int paperX=alignStart(QringProtocol.WIDTH_DOTS,paperWidthDots,paperAlign);
        int contentX=paperX+alignStart(paperWidthDots,wanted,contentAlign);
        int drawH=Math.min(outH,scaledH);
        int contentY=fixed>0?alignStart(outH,drawH,verticalAlign):0;
        for(int y=0;y<drawH;y++){
            int sy=sy0+Math.max(0,Math.min(sourceH-1,(int)(((y+.5f)*sourceH)/scaledH)));
            int dstRow=(contentY+y)*QringProtocol.WIDTH_BYTES, srcRow=sy*QringProtocol.WIDTH_BYTES;
            for(int x=0;x<wanted;x++){
                int sx=sx0+Math.max(0,Math.min(sourceW-1,(int)(((x+.5f)*sourceW)/wanted)));
                if((src.data[srcRow+(sx>>3)]&(0x80>>(sx&7)))==0)continue;
                int dx=contentX+x;if(dx>=0&&dx<QringProtocol.WIDTH_DOTS)out[dstRow+(dx>>3)]|=(byte)(0x80>>(dx&7));
            }
        }
        return new Raster(outH,out);
    }

    static int blackContentWidth(Raster src){int[] b=blackBounds(src);return b==null?0:b[1]-b[0]+1;}
    static int blackContentHeight(Raster src){int[] b=blackBounds(src);return b==null?0:b[3]-b[2]+1;}

    private static int alignStart(int outer,int inner,int align){
        inner=Math.max(1,Math.min(outer,inner));
        if(align==1)return 0;
        if(align==2)return outer-inner;
        return Math.max(0,(outer-inner)/2);
    }

    private static int[] blackBounds(Raster src){
        if(src==null||src.height<=0)return null;
        int minX=QringProtocol.WIDTH_DOTS,maxX=-1,minY=src.height,maxY=-1;
        for(int y=0;y<src.height;y++){
            int row=y*QringProtocol.WIDTH_BYTES;
            for(int bx=0;bx<QringProtocol.WIDTH_BYTES;bx++){
                int v=src.data[row+bx]&0xff;if(v==0)continue;
                for(int bit=0;bit<8;bit++){if((v&(0x80>>bit))==0)continue;int x=(bx<<3)+bit;if(x>=QringProtocol.WIDTH_DOTS)continue;if(x<minX)minX=x;if(x>maxX)maxX=x;if(y<minY)minY=y;if(y>maxY)maxY=y;}
            }
        }
        return maxX<minX||maxY<minY?null:new int[]{minX,maxX,minY,maxY};
    }

    private static void loadGrayRow(Bitmap bmp,int y,int[] px,float[] dst){
        int w=QringProtocol.WIDTH_DOTS;
        bmp.getPixels(px,0,w,0,y,w,1);
        for(int x=0;x<w;x++)dst[x]=gray(px[x]);
    }

    private static float gray(int c){
        int a=Color.alpha(c),r=Color.red(c),g=Color.green(c),b=Color.blue(c);
        float y=0.299f*r+0.587f*g+0.114f*b;
        return 255f-(255f-y)*(a/255f);
    }

    private static int clamp(int v,int lo,int hi){ return Math.max(lo,Math.min(hi,v)); }

    static Bitmap textBitmap(String text, float textSizePx, boolean bold, int align, int lineGap) {
        return textBitmap(text,textSizePx,bold,false,false,align,lineGap);
    }

    static Bitmap textBitmap(String text, float textSizePx, boolean bold, boolean italic, boolean underline, int align, int lineGap) {
        return textBitmap(text,textSizePx,bold,italic,underline,align,lineGap,0,"sans-serif",1f);
    }

    static Bitmap textBitmap(String text, float textSizePx, boolean bold, boolean italic, boolean underline, int align, int lineGap, int charGap, String family) {
        return textBitmap(text,textSizePx,bold,italic,underline,align,lineGap,charGap,family,1f);
    }

    static Bitmap textBitmap(String text, float textSizePx, boolean bold, boolean italic, boolean underline, int align, int lineGap, int charGap, String family, float stretch) {
        return textBitmap(text,textSizePx,bold,italic,underline,align,lineGap,charGap,family,stretch,0f);
    }

    static Bitmap textBitmap(String text, float textSizePx, boolean bold, boolean italic, boolean underline, int align, int lineGap, int charGap, String family, float stretch, float strokeWidthPx) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
        p.setColor(Color.BLACK);
        p.setTextSize(textSizePx);
        p.setTextScaleX(Math.max(0.5f,Math.min(2.0f,stretch)));
        int style=(bold?Typeface.BOLD:Typeface.NORMAL) | (italic?Typeface.ITALIC:Typeface.NORMAL);
        p.setTypeface(Typeface.create((family==null||family.isEmpty())?"sans-serif":family,style));
        p.setUnderlineText(underline);
        if(strokeWidthPx>0.01f){p.setStyle(Paint.Style.FILL_AND_STROKE);p.setStrokeWidth(Math.max(0f,Math.min(8f,strokeWidthPx)));}else p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.LEFT);

        int width=QringProtocol.WIDTH_DOTS, pad=12;
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        String[] paras = text.replace("\r","").split("\n",-1);
        for(String para: paras){
            if(para.length()==0){ lines.add(""); continue; }
            wrapParagraph(lines,p,para,charGap,width-pad*2);
        }
        Paint.FontMetrics fm=p.getFontMetrics();
        int lh=(int)Math.ceil(fm.descent-fm.ascent)+lineGap;
        int maxLines=Math.max(1,(8000-pad*2)/Math.max(1,lh));
        if(lines.size()>maxLines){lines=new java.util.ArrayList<>(lines.subList(0,maxLines));String mark="…内容过长，已截断";lines.set(lines.size()-1,mark);}
        int height=Math.max(80, pad*2+lh*lines.size());
        Bitmap bmp=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bmp);
        c.drawColor(Color.WHITE);
        float y=pad-fm.ascent;
        for(String s:lines){
            float sw=measureWithGap(p,s,charGap);
            float x=pad;
            if(align==1) x=(width-sw)/2f;
            else if(align==2) x=width-pad-sw;
            drawSpaced(c,p,s,x,y,charGap);
            y+=lh;
        }
        return bmp;
    }
    private static void wrapParagraph(java.util.ArrayList<String> out,Paint p,String para,int gap,float max){
        // 英文优先按单词换行；中文/无空格内容退回逐字符换行。
        if(para.indexOf(' ')>=0 || para.indexOf('\t')>=0){
            String[] words=para.trim().split("\\s+");StringBuilder line=new StringBuilder();
            for(String word:words){
                if(word.isEmpty())continue;String candidate=line.length()==0?word:line+" "+word;
                if(measureWithGap(p,candidate,gap)<=max){line.setLength(0);line.append(candidate);continue;}
                if(line.length()>0){out.add(line.toString());line.setLength(0);}
                if(measureWithGap(p,word,gap)<=max){line.append(word);}else{appendCharWrapped(out,p,word,gap,max,line);}
            }
            if(line.length()>0)out.add(line.toString());
        }else{
            StringBuilder line=new StringBuilder();appendCharWrapped(out,p,para,gap,max,line);if(line.length()>0)out.add(line.toString());
        }
    }
    private static void appendCharWrapped(java.util.ArrayList<String> out,Paint p,String text,int gap,float max,StringBuilder line){
        for(int i=0;i<text.length();i++){char ch=text.charAt(i);String next=line.toString()+ch;if(measureWithGap(p,next,gap)>max&&line.length()>0){out.add(line.toString());line.setLength(0);}line.append(ch);}
    }

    private static float measureWithGap(Paint p,String s,int gap){
        if(s==null||s.isEmpty())return 0f;
        return p.measureText(s)+Math.max(0,s.length()-1)*Math.max(0,gap);
    }
    private static void drawSpaced(Canvas c,Paint p,String s,float x,float y,int gap){
        if(gap<=0){c.drawText(s,x,y,p);return;}
        float at=x;for(int i=0;i<s.length();i++){String ch=String.valueOf(s.charAt(i));c.drawText(ch,at,y,p);at+=p.measureText(ch)+gap;}
    }

    static Bitmap toBitmap(Raster r,int maxHeight){
        if(r==null||r.height<=0)return null;
        int srcH=r.height;
        int outH=Math.max(1,Math.min(srcH,Math.max(1,maxHeight)));
        float step=srcH/(float)outH;
        Bitmap b=Bitmap.createBitmap(QringProtocol.WIDTH_DOTS,outH,Bitmap.Config.RGB_565);
        int[] row=new int[QringProtocol.WIDTH_DOTS];
        for(int oy=0;oy<outH;oy++){
            int y=Math.min(srcH-1,(int)(oy*step));int base=y*QringProtocol.WIDTH_BYTES;
            for(int x=0;x<QringProtocol.WIDTH_DOTS;x++)row[x]=((r.data[base+(x>>3)]&(0x80>>(x&7)))!=0)?Color.BLACK:Color.WHITE;
            b.setPixels(row,0,QringProtocol.WIDTH_DOTS,0,oy,QringProtocol.WIDTH_DOTS,1);
        }
        return b;
    }

}
