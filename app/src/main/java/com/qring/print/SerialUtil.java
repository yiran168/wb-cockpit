package com.qring.print;

import java.util.Locale;
import java.math.BigInteger;

final class SerialUtil {
    static final int BIN=0, DEC=1, HEX=2, BASE26_ZERO=3, BASE26_ONE=4, BASE36=5;
    private SerialUtil(){}

    static String format(long value,int mode,int width){
        final String raw;
        switch(mode){
            case BIN: raw=signedRadix(value,2); break;
            case HEX: raw=signedRadix(value,16); break;
            case BASE26_ZERO: raw=base26Zero(value); break;
            case BASE26_ONE: raw=base26One(value); break;
            case BASE36: raw=signedRadix(value,36); break;
            default: raw=Long.toString(value);
        }
        return zeroPad(raw,Math.max(0,Math.min(64,width)));
    }

    private static String signedRadix(long value,int radix){
        if(value<0){
            // Long.MIN_VALUE cannot be negated safely; Java's formatter already handles its sign.
            return Long.toString(value,radix).toUpperCase(Locale.ROOT);
        }
        return Long.toString(value,radix).toUpperCase(Locale.ROOT);
    }

    /** 真正的 26 进制：0=A, 25=Z, 26=BA。 */
    static String base26Zero(long value){
        BigInteger v=BigInteger.valueOf(value);boolean neg=v.signum()<0;if(neg)v=v.negate();
        String body=base26ZeroPositive(v);return neg?"-"+body:body;
    }
    private static String base26ZeroPositive(BigInteger value){
        if(value.signum()==0)return "A";
        BigInteger base=BigInteger.valueOf(26);StringBuilder b=new StringBuilder();BigInteger v=value;
        while(v.signum()>0){BigInteger[] qr=v.divideAndRemainder(base);b.append((char)('A'+qr[1].intValue()));v=qr[0];}
        return b.reverse().toString();
    }

    /** Excel/标签常见字母序列：1=A, 26=Z, 27=AA。0 显示 A。 */
    static String base26One(long value){
        BigInteger v=BigInteger.valueOf(value);boolean neg=v.signum()<0;if(neg)v=v.negate();
        if(v.signum()==0)return "A";
        BigInteger base=BigInteger.valueOf(26),one=BigInteger.ONE;StringBuilder b=new StringBuilder();
        while(v.signum()>0){v=v.subtract(one);BigInteger[] qr=v.divideAndRemainder(base);b.append((char)('A'+qr[1].intValue()));v=qr[0];}
        String body=b.reverse().toString();return neg?"-"+body:body;
    }

    static String zeroPad(String value,int width){
        if(value==null)value="";
        boolean neg=value.startsWith("-");
        String body=neg?value.substring(1):value;
        StringBuilder b=new StringBuilder();
        if(neg)b.append('-');
        for(int i=body.length();i<width;i++)b.append('0');
        return b.append(body).toString();
    }
}
