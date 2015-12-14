package org.akvo.flow.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SignatureView extends View {
    private static final int BACKGROUND_COLOR = Color.WHITE;
    private static final int STROKE_COLOR = Color.BLACK;
    private static final float STROKE_WIDTH = 6f;
    private static final float TOUCH_TOLERANCE = 4f;

    private float mX, mY;

    private Path mPath;
    private Paint mPaint;
    private Paint mBitmapPaint;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private boolean mIsEmpty;

    public SignatureView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPath = new Path();
        mPaint = new Paint();
        mPaint.setColor(STROKE_COLOR);
        mPaint.setStrokeWidth(STROKE_WIDTH);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
    }

    public void resetBitmap() {
        mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mCanvas.drawColor(BACKGROUND_COLOR);
        mIsEmpty = true;
    }

    public void clear() {
        mPath.reset();
        resetBitmap();
        invalidate();
    }

    public boolean isEmpty() {
        return mIsEmpty;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resetBitmap();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(mPath, mPaint);
    }

    private void touchDown(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp() {
        mPath.lineTo(mX, mY);
        mCanvas.drawPath(mPath, mPaint);
        mPath.reset();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDown(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
                touchUp();
                break;
            default:
                return false;
        }

        mIsEmpty = false;
        invalidate();
        return true;
    }

}
