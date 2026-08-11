package com.qring.print;

import org.junit.Test;
import static org.junit.Assert.*;

public class SerialUtilTest {
    @Test public void binaryDecimalHexAndBase36Work(){
        assertEquals("101", SerialUtil.format(5, SerialUtil.BIN, 0));
        assertEquals("42", SerialUtil.format(42, SerialUtil.DEC, 0));
        assertEquals("FF", SerialUtil.format(255, SerialUtil.HEX, 0));
        assertEquals("Z", SerialUtil.format(35, SerialUtil.BASE36, 0));
        assertEquals("10", SerialUtil.format(36, SerialUtil.BASE36, 0));
    }

    @Test public void base26VariantsHaveDocumentedSemantics(){
        assertEquals("A", SerialUtil.base26Zero(0));
        assertEquals("Z", SerialUtil.base26Zero(25));
        assertEquals("BA", SerialUtil.base26Zero(26));
        assertEquals("A", SerialUtil.base26One(1));
        assertEquals("Z", SerialUtil.base26One(26));
        assertEquals("AA", SerialUtil.base26One(27));
    }

    @Test public void zeroPaddingPreservesNegativeSign(){
        assertEquals("0007", SerialUtil.zeroPad("7",4));
        assertEquals("-007", SerialUtil.zeroPad("-7",3));
    }
}
