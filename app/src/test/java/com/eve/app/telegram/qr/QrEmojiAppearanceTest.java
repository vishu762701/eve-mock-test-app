package com.eve.app.telegram.qr;

import static org.junit.Assert.*;
import org.junit.Test;

public class QrEmojiAppearanceTest {
    @Test public void lateFrameStartsItsOwnClockOnlyAfterOpenAndReadiness() {
        QrEmojiAppearance state = new QrEmojiAppearance(1,false,.4f);
        assertEquals(0f,state.progress(0,true,.3f,1000,false,false,false),0f);
        assertEquals(0f,state.progress(0,false,.3f,5000,true,false,false),0f);
        assertEquals(0f,state.progress(0,true,.3f,7000,true,false,false),0f);
        long delay=(long)Math.ceil(state.delay(.3f));
        float half=state.progress(0,true,.3f,7000+delay+190,true,false,false);
        assertEquals(.5f,half,.003f);
        assertEquals(1f,state.progress(0,true,.3f,8000,true,false,false),0f);
    }

    @Test public void sharingDoesNotSeedOrAdvanceLiveAppearanceAndEachNodeKeepsItsDelay() {
        QrEmojiAppearance state = new QrEmojiAppearance(1,false,.4f);
        assertEquals(1f,state.progress(0,true,.3f,9000,false,true,false),0f);
        assertEquals(0f,state.progress(0,true,.3f,12000,true,false,false),0f);
        float first=state.progress(0,true,.1f,12300,true,false,false);
        float second=state.progress(0,true,.8f,12300,true,false,false);
        assertNotEquals(first,second,0f);
        assertEquals(state.delay(.1f),state.delay(.1f),0f);
        assertTrue(state.delay(.1f)>=0 && state.delay(.1f)<300);
    }

    @Test public void pickedLaterUsesOnlySlotSwapPopAndNeverInventsUnreadyFrames() {
        QrEmojiAppearance picked = new QrEmojiAppearance(6,true,.2f);
        assertEquals(0f,picked.progress(5,false,.8f,1000,true,false,false),0f);
        assertEquals(1f,picked.progress(5,true,.8f,1000,true,false,false),0f);
        picked.resetForOpen();
        assertEquals(0f,picked.progress(5,true,.8f,2000,false,false,false),0f);
    }

    @Test public void currentAndOutgoingPatternsRetainSeparateAppearanceOnRapidSelection() {
        QrEmojiAppearance old = new QrEmojiAppearance(1,false,.2f);
        old.progress(0,true,.3f,1000,true,false,false);
        QrEmojiAppearance next = new QrEmojiAppearance(1,true,.2f);
        assertEquals(1f,next.progress(0,true,.3f,1100,true,false,false),0f);
        assertTrue(old.progress(0,true,.3f,1100,true,false,false)<1f);
        assertEquals(0f,old.progress(-1,true,.3f,1100,true,false,false),0f);
        assertEquals(0f,old.progress(6,true,.3f,1100,true,false,false),0f);
    }

    @Test public void optionalFirstFrameAndOriginalBackOutHaveCorrectEndpointsAndOvershoot() {
        QrEmojiAppearance state = new QrEmojiAppearance(1,false,.2f);
        assertEquals(1f,state.progress(0,true,.3f,1000,false,false,true),0f);
        assertEquals(1f,state.progress(0,true,.3f,2000,false,false,false),0f);
        assertEquals(0f,QrEmojiAppearance.scale(0f),0f);
        assertTrue(QrEmojiAppearance.scale(.7f)>1f);
        assertEquals(1f,QrEmojiAppearance.scale(1f),0f);
    }
}
