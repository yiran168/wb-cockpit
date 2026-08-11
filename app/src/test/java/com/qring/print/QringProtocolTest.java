package com.qring.print;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class QringProtocolTest {
    @Test public void rasterGeometryIs384Dots(){
        assertEquals(384,QringProtocol.WIDTH_DOTS);
        assertEquals(48,QringProtocol.WIDTH_BYTES);
        assertEquals(1024,QringProtocol.CHUNK_SIZE);
        assertEquals(1,QringProtocol.CHUNK_DELAY_MS);
    }

    @Test public void rasterHeaderUses48BytesAndLittleEndianHeight(){
        byte[] h=QringProtocol.rasterHeader(300);
        assertArrayEquals(new byte[]{0x1D,0x76,0x30,0x00,0x30,0x00,0x2C,0x01},h);
    }

    @Test public void feedSplitsAt255Dots(){
        List<byte[]> packets=QringProtocol.feed(600);
        assertEquals(3,packets.size());
        assertArrayEquals(new byte[]{0x1B,0x4A,(byte)255},packets.get(0));
        assertArrayEquals(new byte[]{0x1B,0x4A,(byte)255},packets.get(1));
        assertArrayEquals(new byte[]{0x1B,0x4A,90},packets.get(2));
    }

    @Test public void statusBitsDecodeIndependently(){
        QringProtocol.Status s=QringProtocol.parseStatus(0x1F);
        assertTrue(s.printing); assertTrue(s.coverOpen); assertTrue(s.noPaper);
        assertTrue(s.lowBattery); assertTrue(s.overheat);
        assertEquals("上盖未合",s.label());
    }

    @Test public void densityCommandKeepsProtocolPrefix(){
        assertArrayEquals(new byte[]{0x10,(byte)0xFF,0x10,0x00,0x03},QringProtocol.thickness(3));
    }
    @Test public void shutdownCommandUsesBigEndianSeconds(){
        assertArrayEquals(new byte[]{0x10,(byte)0xFF,0x12,0x01,0x2C},QringProtocol.shutdownTime(300));
    }

    @Test public void faultLabelsMatchKnownPrinterFrames(){
        assertEquals("缺纸",QringProtocol.faultLabel(0x01));
        assertEquals("上盖未合",QringProtocol.faultLabel(0x02));
        assertEquals("打印机过热",QringProtocol.faultLabel(0x03));
        assertEquals("低电量",QringProtocol.faultLabel(0x04));
    }

}
