package com.qring.print;

import java.util.ArrayList;
import java.util.List;

final class QringProtocol {
    static final int WIDTH_DOTS = 384;
    static final int WIDTH_BYTES = 48;
    static final int CHUNK_SIZE = 1024;
    static final int CHUNK_DELAY_MS = 1;

    static final byte[] CMD_ENABLE = b(0x10,0xFF,0xF1,0x02);
    static final byte[] CMD_ENABLE2 = b(0x1F,0xB2,0x10);
    static final byte[] CMD_STOP = b(0x10,0xFF,0xF1,0x45);
    static final byte[] CMD_WAKEUP = new byte[12];
    static final byte[] CMD_STATUS = b(0x10,0xFF,0x40);
    static final byte[] CMD_BATTERY = b(0x10,0xFF,0x50,0xF1);
    static final byte[] CMD_MODEL = b(0x10,0xFF,0x20,0xF0);
    static final byte[] CMD_FW_VERSION = b(0x10,0xFF,0x20,0xF1);
    static final byte[] CMD_SN = b(0x10,0xFF,0x20,0xF2);
    static final byte[] CMD_BT_NAME = b(0x10,0xFF,0x30,0x11);

    static final int ACK_PRINT_DONE = 0xAA;
    static final int FAULT_FRAME_HEAD = 0xFF;

    static final int ST_PRINTING = 0x01;
    static final int ST_COVER_OPEN = 0x02;
    static final int ST_NO_PAPER = 0x04;
    static final int ST_LOW_BATTERY = 0x08;
    static final int ST_OVERHEAT = 0x10;

    private QringProtocol(){}

    static byte[] b(int... a) {
        byte[] r = new byte[a.length];
        for (int i=0;i<a.length;i++) r[i]=(byte)(a[i]&0xff);
        return r;
    }

    static byte[] thickness(int level) {
        return b(0x10,0xFF,0x10,0x00, level);
    }

    static byte[] shutdownTime(int seconds) {
        seconds = Math.max(0, Math.min(65535, seconds));
        return b(0x10,0xFF,0x12,(seconds>>8)&0xff,seconds&0xff);
    }

    static List<byte[]> feed(int dots) {
        ArrayList<byte[]> out = new ArrayList<>();
        while (dots > 0) {
            int n = Math.min(255, dots);
            out.add(b(0x1B,0x4A,n));
            dots -= n;
        }
        return out;
    }

    static byte[] rasterHeader(int height) {
        int w = WIDTH_BYTES;
        return b(0x1D,0x76,0x30,0x00,
                w & 0xff, (w >> 8) & 0xff,
                height & 0xff, (height >> 8) & 0xff);
    }

    static Status parseStatus(int raw) { return new Status(raw & 0xff); }

    static String faultLabel(int code) {
        switch(code){
            case 0x01: return "缺纸";
            case 0x02: return "上盖未合";
            case 0x03: return "打印机过热";
            case 0x04: return "低电量";
            default: return "未知故障 0x"+Integer.toHexString(code);
        }
    }

    static final class Status {
        final int raw;
        final boolean printing, coverOpen, noPaper, lowBattery, overheat;
        Status(int raw){
            this.raw=raw;
            printing=(raw&ST_PRINTING)!=0;
            coverOpen=(raw&ST_COVER_OPEN)!=0;
            noPaper=(raw&ST_NO_PAPER)!=0;
            lowBattery=(raw&ST_LOW_BATTERY)!=0;
            overheat=(raw&ST_OVERHEAT)!=0;
        }
        boolean healthy(){ return raw==0; }
        String faultMessage(){
            if(coverOpen) return "机器未合盖，请检查机器";
            if(noPaper) return "机器缺纸，请检查纸张装配";
            if(overheat) return "机器过热，请稍候再尝试打印";
            return null;
        }
        String label(){
            if(coverOpen) return "上盖未合";
            if(noPaper) return "缺纸";
            if(overheat) return "过热";
            if(lowBattery) return "低电量";
            if(printing) return "打印中";
            return "正常";
        }
    }
}
