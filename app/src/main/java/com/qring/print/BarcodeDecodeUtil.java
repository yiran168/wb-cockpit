package com.qring.print;

import android.graphics.Bitmap;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;

final class BarcodeDecodeUtil {
    private BarcodeDecodeUtil(){}
    static Result decode(Bitmap b) throws Exception{
        if(b==null)throw new Exception("图片为空");
        Bitmap safe=b.getConfig()==Bitmap.Config.ARGB_8888?b:b.copy(Bitmap.Config.ARGB_8888,false);
        try{
            int w=safe.getWidth(),h=safe.getHeight();
            if((long)w*h>2_000_000L){
                float scale=(float)Math.sqrt(2_000_000d/((double)w*h));
                Bitmap scaled=Bitmap.createScaledBitmap(safe,Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)),true);
                if(safe!=b&&!safe.isRecycled())safe.recycle();
                safe=scaled;w=safe.getWidth();h=safe.getHeight();
            }
            int[] px=new int[w*h];safe.getPixels(px,0,w,0,0,w,h);
            RGBLuminanceSource src=new RGBLuminanceSource(w,h,px);
            MultiFormatReader r=new MultiFormatReader();
            try{return r.decode(new BinaryBitmap(new HybridBinarizer(src)));}
            finally{r.reset();}
        }finally{if(safe!=b&&!safe.isRecycled())safe.recycle();}
    }
}
