package com.eve.app.telegram.qr;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * ADAPTED original Java camera-layer recording from Telegram CameraView.java:1012-1035,1096-1106
 * and QrScanOverlay.java:517-546,804-879 at 3de37c8cfb039452be7398909461da6288babf15.
 * Copyright Telegram contributors; root GPLv2 and EVE release-compliance gate apply.
 * EVE records only the CameraX PreviewView child, never controls or a window screenshot.
 * UI-thread ownership; no CPU readback during live preview. New framework types stay in QrRender.
 */
public final class QrCameraBackdrop {
    private Object cameraNode, blurNode;
    private boolean recording, ready;
    private Bitmap snapshotBlur;
    private final Paint snapshotPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Matrix snapshotMatrix = new Matrix();
    private final RectF viewport = new RectF();
    private final int[] cameraLocation = new int[2], ownerLocation = new int[2];

    /** Called around the real preview child's draw, after the opening morph has settled. */
    public Canvas beginFrame(Canvas canvas, int width, int height, float density, boolean allowed) {
        if (!allowed || !QrRender.hasEffects() || !canvas.isHardwareAccelerated() ||
                width <= 0 || height <= 0) {
            discardLive();
            return canvas;
        }
        if (cameraNode == null) {
            cameraNode = QrRender.createNode("EveCameraViewRenderNode");
            blurNode = QrRender.createNode("EveCameraViewRenderNodeBlur");
            QrRender.setBlur(blurNode, 32f * density, Shader.TileMode.DECAL);
        }
        recording = true;
        return QrRender.beginRecording(cameraNode, width, height);
    }

    public void endFrame(Canvas canvas, int width, int height) {
        if (!QrRender.hasEffects() || !recording) return;
        recording = false;
        QrRender.endRecording(cameraNode);
        QrRender.draw(canvas, cameraNode);
        Canvas blurred = QrRender.beginRecording(blurNode, width, height);
        QrRender.draw(blurred, cameraNode);
        QrRender.endRecording(blurNode);
        ready = true;
    }

    /** Takes ownership of the small, already stack-blurred closing frame; never persists it. */
    public void setSnapshotBlur(Bitmap bitmap) {
        clearSnapshot();
        discardLive();
        if (bitmap != null && !bitmap.isRecycled()) {
            snapshotBlur = bitmap;
            snapshotPaint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        }
    }

    /** Draw in the control's local coordinates, including nested/inset view offsets. */
    public boolean draw(Canvas canvas, View owner, View camera, boolean allowed) {
        if (!allowed || camera.getWidth() <= 0 || camera.getHeight() <= 0) return false;
        camera.getLocationOnScreen(cameraLocation);
        owner.getLocationOnScreen(ownerLocation);
        final float x = cameraLocation[0] - ownerLocation[0];
        final float y = cameraLocation[1] - ownerLocation[1];
        if (snapshotBlur != null && !snapshotBlur.isRecycled()) {
            snapshotMatrix.setScale((float) camera.getWidth() / snapshotBlur.getWidth(),
                    (float) camera.getHeight() / snapshotBlur.getHeight());
            snapshotMatrix.postTranslate(x, y);
            snapshotPaint.getShader().setLocalMatrix(snapshotMatrix);
            viewport.set(x, y, x + camera.getWidth(), y + camera.getHeight());
            canvas.drawRect(viewport, snapshotPaint);
            return true;
        }
        if (!QrRender.hasEffects() || !canvas.isHardwareAccelerated() || !ready ||
                !QrRender.hasDisplayList(blurNode)) return false;
        final int save = canvas.save();
        canvas.translate(x, y);
        QrRender.draw(canvas, blurNode);
        canvas.restoreToCount(save);
        return true;
    }

    public void discardLive() {
        ready = recording = false;
        if (QrRender.hasNodes()) {
            QrRender.discardNode(blurNode);
            QrRender.discardNode(cameraNode);
        }
        blurNode = cameraNode = null;
    }

    private void clearSnapshot() {
        snapshotPaint.setShader(null);
        if (snapshotBlur != null && !snapshotBlur.isRecycled()) snapshotBlur.recycle();
        snapshotBlur = null;
    }

    public void dispose() {
        discardLive();
        clearSnapshot();
    }
}
