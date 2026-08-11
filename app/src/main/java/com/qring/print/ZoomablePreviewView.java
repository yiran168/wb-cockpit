package com.qring.print;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

/** Lightweight, dependency-free pinch-to-zoom preview used by the final print confirmation. */
final class ZoomablePreviewView extends ImageView {
    private final Matrix matrix = new Matrix();
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private float scale = 1f;
    private float lastX, lastY;
    private boolean dragging;

    ZoomablePreviewView(Context context) {
        super(context);
        super.setScaleType(ScaleType.MATRIX);
        setAdjustViewBounds(false);
        setClickable(true);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                float next = clamp(scale * detector.getScaleFactor(), 0.55f, 8f);
                float factor = next / scale;
                scale = next;
                matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
                constrain();
                setImageMatrix(matrix);
                return true;
            }
        });
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDoubleTap(MotionEvent e) { Ui.haptic(ZoomablePreviewView.this); reset(); return true; }
        });
    }


    @Override public void setScaleType(ScaleType scaleType) {
        super.setScaleType(ScaleType.MATRIX);
    }

    @Override public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        post(this::reset);
    }

    void reset() {
        Drawable d = getDrawable();
        if (d == null || getWidth() <= 0 || getHeight() <= 0) return;
        matrix.reset();
        float dw = Math.max(1, d.getIntrinsicWidth());
        float dh = Math.max(1, d.getIntrinsicHeight());
        float fit = Math.min(getWidth() / dw, getHeight() / dh);
        if (Float.isNaN(fit) || Float.isInfinite(fit) || fit <= 0f) fit = 1f;
        float tx = (getWidth() - dw * fit) / 2f;
        float ty = (getHeight() - dh * fit) / 2f;
        matrix.postScale(fit, fit);
        matrix.postTranslate(tx, ty);
        scale = 1f;
        setImageMatrix(matrix);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) post(this::reset);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX(); lastY = event.getY(); dragging = true; return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging && !scaleDetector.isInProgress() && Math.abs(scale-1f) > .001f) {
                    float dx = event.getX() - lastX, dy = event.getY() - lastY;
                    matrix.postTranslate(dx, dy); constrain(); setImageMatrix(matrix);
                }
                lastX = event.getX(); lastY = event.getY(); return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false; return true;
        }
        return true;
    }

    private void constrain() {
        Drawable d = getDrawable();
        if (d == null) return;
        float[] v = new float[9]; matrix.getValues(v);
        float sx = v[Matrix.MSCALE_X], sy = v[Matrix.MSCALE_Y];
        float tx = v[Matrix.MTRANS_X], ty = v[Matrix.MTRANS_Y];
        float w = Math.max(1, d.getIntrinsicWidth()) * sx;
        float h = Math.max(1, d.getIntrinsicHeight()) * sy;
        float minX = w <= getWidth() ? (getWidth() - w) / 2f : getWidth() - w;
        float maxX = w <= getWidth() ? minX : 0f;
        float minY = h <= getHeight() ? (getHeight() - h) / 2f : getHeight() - h;
        float maxY = h <= getHeight() ? minY : 0f;
        float ntx = clamp(tx, minX, maxX), nty = clamp(ty, minY, maxY);
        matrix.postTranslate(ntx - tx, nty - ty);
    }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
}
