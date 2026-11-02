package com.eve.app.telegram.qr;

import static org.junit.Assert.*;
import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;
import androidx.core.graphics.ColorUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, application=Application.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class QrCameraBackdropTest {
    @Test public void closingFrameTracksActualNestedControlCoordinatesAndReleasesOwnership() {
        try (org.robolectric.android.controller.ActivityController<Activity> controller =
                Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            FrameLayout root = new FrameLayout(activity);
            View camera = new View(activity), owner = new View(activity);
            root.addView(camera); root.addView(owner);
            activity.setContentView(root);
            root.layout(0, 0, 200, 240);
            camera.layout(12, 18, 92, 138);
            owner.layout(62, 88, 82, 108);
            Bitmap small = Bitmap.createBitmap(8, 12, Bitmap.Config.ARGB_8888);
            small.eraseColor(Color.RED);
            for (int y=6; y<12; y++) for (int x=4; x<8; x++) small.setPixel(x,y,Color.BLUE);
            QrCameraBackdrop backdrop = new QrCameraBackdrop();
            backdrop.setSnapshotBlur(small);
            Bitmap target = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);
            assertTrue(backdrop.draw(new Canvas(target), owner, camera, true));
            assertEquals(Color.BLUE, target.getPixel(10,10));
            assertFalse(backdrop.draw(new Canvas(target), owner, camera, false));
            Bitmap replacement = Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);
            replacement.eraseColor(Color.GREEN);
            backdrop.setSnapshotBlur(replacement);
            assertTrue(small.isRecycled());
            backdrop.dispose();
            backdrop.dispose();
            assertTrue(replacement.isRecycled());
            assertFalse(backdrop.draw(new Canvas(target), owner, camera, true));
            target.recycle();
        }
    }

    @Test public void sourceFlatShadeDiffersFromDisabledBlurAndLiveFramesNeverReadBackOnSoftware() {
        QrCameraBackdrop backdrop = new QrCameraBackdrop();
        Bitmap target = Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        assertSame(canvas, backdrop.beginFrame(canvas,100,100,1f,true));
        backdrop.endFrame(canvas,100,100); // no recording on software
        int white = (int)(255 * Math.max(0f,Math.min(1f,RimTune.v[RimTune.FILL]/100f)));
        for (boolean allowed : new boolean[]{true,false}) {
            // The real scanner has an opaque camera/black backing. Avoid
            // low-alpha unpremultiplication quantization of a transparent fixture.
            target.eraseColor(Color.BLACK);
            QrScannerGlass glass = new QrScannerGlass(10f,null,()->allowed);
            glass.setBounds(0,0,100,100);
            glass.draw(canvas);
            int expected=ColorUtils.compositeColors((white<<24)|0xFFFFFF,
                    allowed ? 0x29000000 : 0x80000000);
            int visible = ColorUtils.compositeColors(expected, Color.BLACK);
            assertEquals(Color.alpha(visible),Color.alpha(target.getPixel(50,80)));
            assertEquals(Color.red(visible),Color.red(target.getPixel(50,80)),1);
            assertEquals(Color.BLACK,target.getPixel(0,0));
        }
        backdrop.dispose(); target.recycle();
    }

    @Test public void actualGlassCompositesCameraBeforeWhiteFillAndClipsOutsideControls() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        View owner = new View(activity);
        QrScannerGlass glass = new QrScannerGlass(20f,(canvas,view)-> {
            assertSame(owner,view);
            canvas.drawColor(Color.GREEN);
            return true;
        },()->true);
        glass.setCallback(owner);
        glass.setBounds(0,0,100,100);
        Bitmap target=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);
        glass.draw(new Canvas(target));
        int white=(int)(255*Math.max(0f,Math.min(1f,RimTune.v[RimTune.FILL]/100f)));
        int expected=ColorUtils.compositeColors((white<<24)|0xFFFFFF,Color.GREEN);
        assertEquals(expected,target.getPixel(50,80));
        assertEquals(0,Color.alpha(target.getPixel(0,0)));
        glass.setAlpha(0);
        target.eraseColor(Color.TRANSPARENT);
        glass.draw(new Canvas(target));
        assertEquals(0,target.getPixel(50,80));
        activity.finish(); target.recycle();
    }
}
