package com.qring.print;

import com.google.zxing.BarcodeFormat;
import org.junit.Test;
import static org.junit.Assert.*;

public class BarcodeUtilTest {
    @Test public void numericSamplesPassPreValidation() throws Exception {
        assertEquals("690123456789",BarcodeUtil.prepare(BarcodeUtil.sample(BarcodeFormat.EAN_13),BarcodeFormat.EAN_13));
        assertEquals("1234567",BarcodeUtil.prepare(BarcodeUtil.sample(BarcodeFormat.EAN_8),BarcodeFormat.EAN_8));
        assertEquals("01234567890",BarcodeUtil.prepare(BarcodeUtil.sample(BarcodeFormat.UPC_A),BarcodeFormat.UPC_A));
        assertEquals("1234567",BarcodeUtil.prepare(BarcodeUtil.sample(BarcodeFormat.UPC_E),BarcodeFormat.UPC_E));
        assertEquals("12345678",BarcodeUtil.prepare(BarcodeUtil.sample(BarcodeFormat.ITF),BarcodeFormat.ITF));
    }

    @Test public void itfRejectsOddLengthBeforeEncoder(){
        try { BarcodeUtil.prepare("1234567",BarcodeFormat.ITF); fail("expected validation error"); }
        catch(Exception e){ assertTrue(e.getMessage().contains("偶数位")); }
    }

    @Test public void eanAndUpcRejectWrongLengthBeforeEncoder(){
        try { BarcodeUtil.prepare("123",BarcodeFormat.EAN_13); fail("expected validation error"); }
        catch(Exception e){ assertTrue(e.getMessage().contains("12 或 13")); }
        try { BarcodeUtil.prepare("1234567890",BarcodeFormat.UPC_A); fail("expected validation error"); }
        catch(Exception e){ assertTrue(e.getMessage().contains("11 或 12")); }
    }
}
