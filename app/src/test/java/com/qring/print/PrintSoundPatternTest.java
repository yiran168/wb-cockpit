package com.qring.print;

import org.junit.Test;
import static org.junit.Assert.*;

public class PrintSoundPatternTest {
    @Test public void tenPresetsProduceShortNonSilentPcm(){
        for(int i=0;i<10;i++){
            short[] p=PrintSoundPattern.preset(i);
            assertTrue("preset length",p.length>1000 && p.length<44100);
            boolean nonzero=false;for(short v:p)if(v!=0){nonzero=true;break;}
            assertTrue("preset must not be silent",nonzero);
        }
    }
    @Test public void randomGeneratedPatternIsBounded(){
        short[] p=PrintSoundPattern.randomPattern();
        assertTrue(p.length>1000);assertTrue(p.length<44100);
    }
}
