package com.eve.app.telegram.qr;

import static org.junit.Assert.*;
import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, application=Application.class)
public class QrScanMorphTest {
    @Test public void sourceButtonAndFullScreenEndpoints() {
        QrScanMorph m = new QrScanMorph();
        assertEquals(390L, QrScanMorph.OPEN_MS);
        assertEquals(330L, QrScanMorph.CLOSE_MS);
        m.sample(180,600,228,648,400,800,0f,false,20);
        assertEquals(180f,m.left,.001f);
        assertEquals(600f,m.top,.001f);
        assertEquals(24f,m.radius,.001f);
        assertEquals(0f,m.contentAlpha,.001f);
        m.sample(180,600,228,648,400,800,1f,false,20);
        assertEquals(0f,m.left,.001f);
        assertEquals(800f,m.bottom,.001f);
        assertEquals(20f,m.radius,.001f);
        assertEquals(1f,m.scale,.001f);
        m.sample(180,600,228,648,400,800,0f,true,20);
        assertEquals(228f,m.right,.001f);
    }
    @Test public void sourceInterruptionsAndNonFiniteFractionRemainBounded() {
        float previous=0f;
        for(int i=0;i<=100;i++) {
            float p=i/100f;
            float open=QrScanMorph.stretch(p,false);
            float close=QrScanMorph.stretch(p,true);
            assertTrue(open >= previous-.00001f);
            assertTrue(open>=0f && open<=1f);
            assertTrue(close>=0f && close<=1f);
            previous=open;
        }
        QrScanMorph m=new QrScanMorph();
        m.sample(0,0,40,40,400,800,Float.NaN,false,0);
        assertTrue(Float.isFinite(m.scale));
    }
}
