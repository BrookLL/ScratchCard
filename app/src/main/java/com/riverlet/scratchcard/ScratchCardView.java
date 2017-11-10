package com.riverlet.scratchcard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Author liujian
 * Email: riverlet.liu@mopo.com
 * Date: 2017/11/3.
 * Despribe: 整个View的绘制分为两层，下层是谜底（downLayer），上册是可以刮开的部分（canvasBitmap = upLayer+path）
 */

public class ScratchCardView extends View {
    private static final String TAG = "ScratchCardView";
    private static final int TYPE_BITMAP = 100;
    private static final int TYPE_COLOR = 101;
    private static final int TYPE_TEXT = 102;
    private OnCompleteListener onCompleteListener;
    private Paint pathPaint;
    private Path path;
    private Bitmap canvasBitmap;
    private Canvas scratchCanvas;
    private RectF rectF;
    private int completeWithPercentage = 70;
    private int pointWidth = 50;

    private LayerData upLayer;
    private LayerData downLayer;

    private volatile boolean isComplete;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public ScratchCardView(Context context) {
        this(context, null);
    }

    public ScratchCardView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScratchCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        //初始化path的Paint
        pathPaint = new Paint();
        pathPaint.setColor(Color.WHITE);//颜色随意，只要不是透明
        pathPaint.setAntiAlias(true);
        pathPaint.setDither(true);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeJoin(Paint.Join.ROUND); // 圆角
        pathPaint.setStrokeCap(Paint.Cap.ROUND); // 圆角
        pathPaint.setStrokeWidth(pointWidth);
        pathPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        //初始化path
        path = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initCanvas(w, h);
    }

    /**
     * 在可以获取View长款或者长款改变的时候调用，初始化画布、计算文字的绘画位置
     *
     * @param width
     * @param height
     */
    private void initCanvas(int width, int height) {
        //初始化画布
        canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        scratchCanvas = new Canvas(canvasBitmap);
        rectF = new RectF(0, 0, width, height);
        if (downLayer.type == TYPE_TEXT) {
            measureDownText();
        }
        drawUpLayer();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        initCanvas(width, height);
    }


    public void setDownLayer(@DrawableRes int resId) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        setDownLayer(bitmap);
    }

    public void setDownLayer(Bitmap bitmap) {
        downLayer = new LayerData();
        downLayer.type = TYPE_BITMAP;
        downLayer.bitmap = bitmap;
    }

    public void setDownLayer(String text) {
        setDownLayer(Color.WHITE, text);
    }

    public void setDownLayerColor(int color) {
        downLayer = new LayerData();
        downLayer.type = TYPE_COLOR;
        downLayer.color = color;
    }

    public void setDownLayer(int color, String text) {
        downLayer = new LayerData();
        downLayer.type = TYPE_TEXT;
        downLayer.color = color;
        downLayer.text = text;
        measureDownText();
    }


    public void setUpLayer(@DrawableRes int resId) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        setUpLayer(bitmap);
    }

    public void setUpLayer(Bitmap bitmap) {
        upLayer = new LayerData();
        upLayer.type = TYPE_BITMAP;
        upLayer.bitmap = bitmap;
        drawUpLayer();
    }

    public void setUpLayer(String text) {
        setUpLayer(Color.WHITE, text);
    }

    public void setUpLayerColor(int color) {
        upLayer = new LayerData();
        upLayer.type = TYPE_COLOR;
        upLayer.color = color;
        drawUpLayer();
    }

    public void setUpLayer(int color, String text) {
        upLayer = new LayerData();
        upLayer.type = TYPE_TEXT;
        upLayer.color = color;
        upLayer.text = text;
        drawUpLayer();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (rectF != null) {
            drawDownLayer(canvas);
            if (!isComplete) {
                canvas.drawBitmap(canvasBitmap, null, rectF, null);
            }
        }
    }

    private void drawUpLayer() {
        if (upLayer != null && scratchCanvas != null) {
            switch (upLayer.type) {
                case TYPE_BITMAP:
                    scratchCanvas.drawBitmap(upLayer.bitmap, null, rectF, null);
                    break;
                case TYPE_COLOR:
                    scratchCanvas.drawColor(upLayer.color);
                    break;
                case TYPE_TEXT:
                    measureUpText();
                    scratchCanvas.drawColor(upLayer.color);
                    scratchCanvas.drawText(upLayer.text, upLayer.textStartX, upLayer.textStartY, upLayer.paint);
                    break;
            }
            invalidate();
        }
    }

    private void drawDownLayer(Canvas canvas) {
        if (downLayer != null) {
            switch (downLayer.type) {
                case TYPE_BITMAP:
                    canvas.drawBitmap(downLayer.bitmap, null, rectF, null);
                    break;
                case TYPE_COLOR:
                    canvas.drawColor(downLayer.color);
                    break;
                case TYPE_TEXT:
                    canvas.drawColor(downLayer.color);
                    canvas.drawText(downLayer.text, downLayer.textStartX, downLayer.textStartY, downLayer.paint);
                    break;
            }
        }
    }

    private void drawPath() {
        if (scratchCanvas != null) {
            scratchCanvas.drawPath(path, pathPaint);
        }
    }

    private void measureDownText() {
        if (rectF != null) {
            if (downLayer.paint == null) {
                Paint paint = new Paint();
                paint.setTextSize(30);
                paint.setColor(0xff4a4a4a);
                paint.setAntiAlias(true);
                downLayer.paint = paint;
            }
            int textWidth = 0;
            if (downLayer.text != null && downLayer.text.length() > 0) {
                int len = downLayer.text.length();
                float[] widths = new float[len];
                downLayer.paint.getTextWidths(downLayer.text, widths);
                for (int j = 0; j < len; j++) {
                    textWidth += (int) Math.ceil(widths[j]);
                }
            }
            downLayer.textStartX = (int) (rectF.right / 2 - textWidth / 2);
            downLayer.textStartY = (int) (rectF.bottom / 2 + downLayer.paint.getTextSize() / 2);
        }
    }

    private void measureUpText() {
        if (rectF != null) {
            if (upLayer.paint == null) {
                Paint paint = new Paint();
                paint.setTextSize(30);
                paint.setColor(0xff4a4a4a);
                paint.setAntiAlias(true);
                upLayer.paint = paint;
            }
            int textWidth = 0;
            if (upLayer.text != null && upLayer.text.length() > 0) {
                int len = upLayer.text.length();
                float[] widths = new float[len];
                upLayer.paint.getTextWidths(upLayer.text, widths);
                for (int j = 0; j < len; j++) {
                    textWidth += (int) Math.ceil(widths[j]);
                }
            }
            upLayer.textStartX = (int) (rectF.right / 2 - textWidth / 2);
            upLayer.textStartY = (int) (rectF.bottom / 2 + upLayer.paint.getTextSize() / 2);
        }
    }

    private int mLastX;
    private int mLastY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) (event.getX());
        int y = (int) (event.getY());
        int offsetX = (int) (rectF != null ? rectF.left : 0);
        int offsetY = (int) (rectF != null ? rectF.top : 0);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = x;
                mLastY = y;
                path.moveTo(x - offsetX, y - offsetY);
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = Math.abs(x - mLastX);
                int dy = Math.abs(y - mLastY);

                if (dx > 3 || dy > 3) {
                    //贝塞尔曲线
                    path.quadTo(mLastX - offsetX, mLastY - offsetY, (x + mLastX) / 2 - offsetX, (y + mLastY) / 2 - offsetY);
                }
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                computeScratchArea();
                break;
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                computeScratchArea();
                break;

        }
        drawPath();
        invalidate();
        return true;
    }

    public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    public void setCompleteWithPercentage(int completeWithPercentage) {
        this.completeWithPercentage = completeWithPercentage;
    }

    public void setPointWidth(int pointWidth) {
        this.pointWidth = pointWidth;
    }

    private void computeScratchArea() {
        if (!isComplete) {
            executorService.execute(runnable);
        } else {
            executorService.shutdownNow();
        }
    }


    //在子线程计算
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isComplete) {
                return;
            }
            Bitmap bitmap = canvasBitmap;
            int w = getWidth();
            int h = getHeight();
            int[] pixels = new int[w * h];

            float wipeArea = 0;
            float totalArea = w * h;
            //获取像素数据
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            //遍历色素值为0的像素，也就是透明区域
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    int index = i + j * w;
                    if (pixels[index] == 0) {
                        wipeArea++;
                    }
                }
            }

            if (wipeArea > 0 && totalArea > 0) {
                int percent = (int) (wipeArea * 100 / totalArea);
                Log.e("TAG", percent + "");

                if (percent >= completeWithPercentage && !isComplete) {
                    isComplete = true;
                    postInvalidate();
                    Log.d(TAG, "........." + isComplete);
                    if (onCompleteListener != null) {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                onCompleteListener.onComplete();
                            }
                        });
                    }
                }
            }
        }

    };

    //层次数据
    static final class LayerData {
        int type;
        Bitmap bitmap;
        String text;
        int color;
        Paint paint;
        int textStartX;
        int textStartY;
    }

    public interface OnCompleteListener {
        void onComplete();
    }
}
