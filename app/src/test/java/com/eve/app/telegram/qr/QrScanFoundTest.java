package com.eve.app.telegram.qr;

import static org.junit.Assert.*;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, application=Application.class)
public class QrScanFoundTest {
    private static final float[] FOUR = {30,30,90,30,90,90,30,90};

    @Test public void drawOriginalYellowFourPointCornersAndFadeWhiteFrame() {
        QrScanFound outline = new QrScanFound(1f);
        assertTrue(outline.show(FOUR, 120, 120));
        assertEquals(0f, outline.progress(), 0f);
        assertTrue(outline.advance());
        assertEquals(.3f, outline.progress(), .00001f);
        for (int i=0;i<40;i++) outline.advance();
        assertEquals(1f, outline.progress(), .005f);
        Bitmap bmp=Bitmap.createBitmap(120,120,Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas=new Canvas(bmp);
            outline.draw(canvas);
            // Source's 1.1x centroid scale moves (30,30) to about (27,27).
            assertTrue(Color.red(bmp.getPixel(27,27)) > 150);
            assertEquals(Color.TRANSPARENT, bmp.getPixel(60,60));
            outline.clear();
            assertEquals(0f, outline.progress(), 0f);
        } finally { bmp.recycle(); }
        assertEquals(450L, QrScanFound.FOUND_DELAY_MS);
    }

    @Test public void neverFabricateThreeFinderPointsOrInvalidCoordinates() {
        QrScanFound o=new QrScanFound(2f);
        assertFalse(o.show(new float[]{1,1,2,2,3,3}, 120,120));
        assertFalse(o.show(new float[]{1,1,2,2,3,3,4,Float.NaN},120,120));
        assertFalse(o.show(new float[]{1,1,2,2,3,3,121,5},120,120));
        assertEquals(0f,o.progress(),0f);
        assertFalse(o.advance());
    }

    @Test public void returnRealZxingFourPointsFromEveryRotatedDecodeToBuffer() {
        float[] orig={1,1,7,1,7,4,1,4};
        int w=10,h=6;
        for (int rotation=0;rotation<4;rotation++) {
            float[] rotated=new float[8];
            for(int i=0;i<8;i+=2) {
                float x=orig[i],y=orig[i+1];
                switch(rotation) {
                    case 1: rotated[i]=y;rotated[i+1]=w-1-x;break;
                    case 2: rotated[i]=w-1-x;rotated[i+1]=h-1-y;break;
                    case 3: rotated[i]=h-1-y;rotated[i+1]=x;break;
                    default: rotated[i]=x;rotated[i+1]=y;
                }
            }
            assertArrayEquals(orig,QrScanFound.toUnrotated(rotated,w,h,rotation),.0001f);
        }
        assertNull(QrScanFound.toUnrotated(orig,10,6,4));
    }
}
