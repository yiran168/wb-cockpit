package com.qring.print;

import android.graphics.*;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.util.*;

final class BarcodeUtil {
    private BarcodeUtil(){}

    static Bitmap make(String text, BarcodeFormat format, int width, int height) throws Exception{
        return make(text,format,width,height,null);
    }

    static Bitmap make(String text, BarcodeFormat format, int width, int height, ErrorCorrectionLevel qrLevel) throws Exception{
        if(text==null||text.trim().isEmpty())throw new Exception("内容为空");
        Map<EncodeHintType,Object> hints=new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN,format==BarcodeFormat.QR_CODE?4:(isOneDimensional(format)?10:2));
        hints.put(EncodeHintType.CHARACTER_SET,"UTF-8");
        if(format==BarcodeFormat.QR_CODE && qrLevel!=null)hints.put(EncodeHintType.ERROR_CORRECTION,qrLevel);
        BitMatrix m=new MultiFormatWriter().encode(text,format,width,height,hints);
        Bitmap b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        int[] px=new int[width*height];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)px[y*width+x]=m.get(x,y)?Color.BLACK:Color.WHITE;
        b.setPixels(px,0,width,0,0,width,height);return b;
    }

    /** Validate and normalize user input before ZXing. Error messages are deliberately Chinese/user-facing. */
    static String prepare(String input,BarcodeFormat format) throws Exception{
        String s=input==null?"":input.trim();if(s.isEmpty())throw new Exception("请输入编码内容");
        switch(format){
            case EAN_13: digits(s,"EAN-13"); length(s,"EAN-13",12,13); return s;
            case EAN_8: digits(s,"EAN-8"); length(s,"EAN-8",7,8); return s;
            case UPC_A: digits(s,"UPC-A"); length(s,"UPC-A",11,12); return s;
            case UPC_E: digits(s,"UPC-E"); length(s,"UPC-E",7,8); return s;
            case ITF:
                digits(s,"ITF");if((s.length()&1)!=0)throw new Exception("ITF 必须是偶数位数字，例如 12345678");if(s.length()<2||s.length()>80)throw new Exception("ITF 建议 2–80 位偶数长度数字");return s;
            case CODE_39:
                s=s.toUpperCase(Locale.ROOT);if(!s.matches("[0-9A-Z .\\-$/+%]+"))throw new Exception("Code 39 只支持 0-9、A-Z、空格和 . - $ / + %");return s;
            case CODE_93:
                ascii(s,"Code 93");return s;
            case CODE_128:
                latin1(s,"Code 128");return s;
            case CODABAR:
                s=s.toUpperCase(Locale.ROOT);if(!s.matches("[0-9\\-\\$:/.+ABCDTN*E]+"))throw new Exception("Codabar 仅支持数字、- $ : / . + 和起止符 A-D/T/N/*/E");
                if(!isCodabarGuard(s.charAt(0)))s="A"+s;if(!isCodabarGuard(s.charAt(s.length()-1)))s=s+"A";return s;
            case DATA_MATRIX:
            case PDF_417:
            case AZTEC:
            case QR_CODE:
            default:return s;
        }
    }

    static String friendly(Throwable e){
        String m=e==null?"未知错误":String.valueOf(e.getMessage());
        if(m.contains("digits 0-9"))return "该制式只能输入数字 0-9";
        if(m.contains("Requested contents should be"))return "输入位数不符合当前条码规则";
        if(m.toLowerCase(Locale.ROOT).contains("checksum"))return "校验位不正确；可以只输入不含校验位的基础数字，让编码器自动计算";
        if(m.toLowerCase(Locale.ROOT).contains("too large"))return "内容太长，当前条码尺寸放不下";
        if(m.toLowerCase(Locale.ROOT).contains("contents length"))return "内容长度不符合当前条码规则";
        return m;
    }

    static String rule(BarcodeFormat format){
        switch(format){
            case EAN_13:return "EAN-13：12 或 13 位数字；输入 12 位时自动计算校验位";
            case EAN_8:return "EAN-8：7 或 8 位数字；输入 7 位时自动计算校验位";
            case UPC_A:return "UPC-A：11 或 12 位数字；输入 11 位时自动计算校验位";
            case UPC_E:return "UPC-E：7 或 8 位数字";
            case ITF:return "ITF：仅数字，而且必须是偶数位";
            case CODE_39:return "Code 39：0-9 / A-Z 和有限符号，输入会自动转大写";
            case CODE_93:return "Code 93：适合短英文、数字和 ASCII 符号";
            case CODE_128:return "Code 128：高密度一维码，适合英文、数字和常用符号";
            case CODABAR:return "Codabar：数字为主；缺少起止符时 APP 自动补 A…A";
            case DATA_MATRIX:return "Data Matrix：二维矩阵码，可输入文本、网址、编号";
            case PDF_417:return "PDF417：堆叠式二维条码，适合较长文本";
            case AZTEC:return "Aztec：二维矩阵码，可输入文本、网址、编号";
            default:return "请输入内容后实时生成";
        }
    }

    static String sample(BarcodeFormat format){
        switch(format){
            case EAN_13:return "690123456789";
            case EAN_8:return "1234567";
            case UPC_A:return "01234567890";
            case UPC_E:return "1234567";
            case ITF:return "12345678";
            case CODE_39:return "ITEM-2026";
            case CODE_93:return "ITEM2026";
            case CODE_128:return "ORDER-2026-001";
            case CODABAR:return "12345678";
            case DATA_MATRIX:return "https://example.com";
            case PDF_417:return "DOC-2026-001";
            case AZTEC:return "ASSET-2026-001";
            default:return "";
        }
    }

    private static void digits(String s,String name)throws Exception{if(!s.matches("[0-9]+"))throw new Exception(name+" 只能输入数字 0-9");}
    private static void length(String s,String name,int a,int b)throws Exception{if(s.length()!=a&&s.length()!=b)throw new Exception(name+" 需要 "+a+" 或 "+b+" 位数字，当前是 "+s.length()+" 位");}
    private static void ascii(String s,String name)throws Exception{for(int i=0;i<s.length();i++)if(s.charAt(i)>127)throw new Exception(name+" 当前实现只接受 ASCII 英文/数字/符号；中文请使用二维码、Data Matrix、PDF417 或 Aztec");}
    private static void latin1(String s,String name)throws Exception{for(int i=0;i<s.length();i++)if(s.charAt(i)>255)throw new Exception(name+" 不支持当前字符；中文请使用二维码或二维矩阵码");}
    private static boolean isCodabarGuard(char c){return "ABCDTN*E".indexOf(c)>=0;}
    private static boolean isOneDimensional(BarcodeFormat f){return f==BarcodeFormat.CODE_128||f==BarcodeFormat.CODE_39||f==BarcodeFormat.CODE_93||f==BarcodeFormat.EAN_13||f==BarcodeFormat.EAN_8||f==BarcodeFormat.UPC_A||f==BarcodeFormat.UPC_E||f==BarcodeFormat.ITF||f==BarcodeFormat.CODABAR;}


    static Bitmap withCaption(Bitmap code,String text,float textPx){
        Bitmap caption=RasterEncoder.textBitmap(text,textPx,false,1,4);
        int h=code.getHeight()+caption.getHeight()+8;
        Bitmap out=Bitmap.createBitmap(QringProtocol.WIDTH_DOTS,h,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(out);c.drawColor(Color.WHITE);
        float x=(QringProtocol.WIDTH_DOTS-code.getWidth())/2f;
        c.drawBitmap(code,x,0,null);c.drawBitmap(caption,0,code.getHeight()+8,null);
        if(!caption.isRecycled())caption.recycle();
        return out;
    }
}
