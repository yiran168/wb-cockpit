package com.qring.print;

import org.junit.Test;
import static org.junit.Assert.*;

public class PrinterProfileTest {
    @Test public void physicalHeadCapsWidePaperAt384Dots(){
        assertEquals(384,PrinterProfile.usableDotsForPaper(57f));
        assertEquals(384,PrinterProfile.HEAD_DOTS);
        assertEquals(48,PrinterProfile.HEAD_BYTES);
    }

    @Test public void narrowLabelUsesOnlyRequestedHeadRegion(){
        assertEquals(240,PrinterProfile.usableDotsForPaper(30f));
        assertEquals(160,PrinterProfile.usableDotsForPaper(20f));
    }

    @Test public void dotMillimeterRoundTripMatches203Dpi(){
        assertEquals(48.05f,PrinterProfile.printableWidthMm(),0.06f);
        assertEquals(203,PrinterProfile.DPI);
    }
}
