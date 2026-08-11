package com.qring.print;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.os.Build;
import java.io.*;

final class ImageLoader {
    private static final int MAX_SIDE=2048;
    private ImageLoader(){}

    static Bitmap load(Context c, Uri uri) throws Exception {
        if(uri==null) throw new IOException("图片地址为空");
        if(Build.VERSION.SDK_INT>=28)return Api28.load(c,uri);
        return loadLegacy(c,uri);
    }

    private static Bitmap loadLegacy(Context c,Uri uri)throws Exception{
        BitmapFactory.Options probe=new BitmapFactory.Options();probe.inJustDecodeBounds=true;
        try(InputStream in=c.getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,probe);}
        if(probe.outWidth<=0||probe.outHeight<=0)throw new IOException("无法读取图片尺寸");
        int sample=1;while(Math.max(probe.outWidth/sample,probe.outHeight/sample)>MAX_SIDE)sample*=2;
        BitmapFactory.Options opt=new BitmapFactory.Options();opt.inSampleSize=sample;opt.inPreferredConfig=Bitmap.Config.ARGB_8888;
        try(InputStream in=c.getContentResolver().openInputStream(uri)){
            Bitmap b=BitmapFactory.decodeStream(in,null,opt);if(b==null)throw new IOException("图片解码失败");return b;
        }
    }

    @android.annotation.TargetApi(28)
    private static final class Api28{
        static Bitmap load(Context c,Uri uri)throws IOException{
            ImageDecoder.Source source=ImageDecoder.createSource(c.getContentResolver(),uri);
            return ImageDecoder.decodeBitmap(source,(decoder,info,src)->{
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                int w=info.getSize().getWidth(),h=info.getSize().getHeight();
                float scale=Math.min(1f,MAX_SIDE/(float)Math.max(w,h));
                if(scale<1f)decoder.setTargetSize(Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)));
            });
        }
    }
}
