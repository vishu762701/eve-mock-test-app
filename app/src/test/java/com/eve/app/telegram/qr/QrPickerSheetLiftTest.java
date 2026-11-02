package com.eve.app.telegram.qr;

import static org.junit.Assert.*;
import android.app.Application;
import android.graphics.Color;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, application=Application.class)
public class QrPickerSheetLiftTest {
    @Test public void sourceGlassHasSeparateOneEighthBlurAndQuantizedSteps() {
        assertEquals(8, QrPickerSheetLift.DOWNSAMPLE);
        assertEquals(0f, QrPickerSheetLift.quantized(-1f), 0f);
        assertEquals(0.52f, QrPickerSheetLift.quantized(0.519f), 0.0001f);
        assertEquals(1f, QrPickerSheetLift.quantized(2f), 0f);
        assertEquals(0f, QrPickerSheetLift.quantized(Float.NaN), 0f);
        assertTrue(QrPickerSheetLift.blurRadius(1f) >
            QrPickerSheetLift.blurRadius(0f));
    }

    @Test public void sourceLiftMatrixUsesRealLuminanceAndProtectsLightScenes() {
        float[] idle = QrPickerSheetLift.colorMatrix(Color.BLACK, 0f);
        assertEquals(20, idle.length);
        assertEquals(1f, idle[0], 0f);
        assertEquals(1f, idle[6], 0f);
        assertEquals(0f, idle[4], 0f);
        assertEquals(1f, idle[18], 0f);
        float[] dark = QrPickerSheetLift.colorMatrix(Color.BLACK, 1f);
        float[] light = QrPickerSheetLift.colorMatrix(Color.WHITE, 1f);
        assertEquals(1f + RimTune.v[RimTune.SH_CONTRAST] / 100f,
            dark[0], .0001f);
        assertTrue("brightening dark must exceed white", dark[4] > light[4]);
        assertEquals(dark[0], dark[6], 0f);
        assertEquals(dark[0], dark[12], 0f);
    }
}
