# ScratchCard
http://www.jianshu.com/p/619652237d7c
跟支付宝那个刮刮卡差不多，体验可能更好点（个人以为）
##先看图：
这是刮开的效果图
![刮开.gif](http://upload-images.jianshu.io/upload_images/3414806-a15dd91c1f1a72e8.gif?imageMogr2/auto-orient/strip%7CimageView2/2/w/400)
看起来还可以吧
当刮开面积超过70%的时候，显示全部底图
![显示奖励.gif](http://upload-images.jianshu.io/upload_images/3414806-e9bdc4780ba3542a.gif?imageMogr2/auto-orient/strip%7CimageView2/2/w/400)
##使用
xml
```
<com.riverlet.scratchcard.ScratchCardView
            android:id="@+id/scratch_card_01"
            android:layout_width="match_parent"
            android:layout_height="150dp"
            android:layout_centerInParent="true"
            android:layout_marginTop="5dp" />
```
activity
```
        ScratchCardView scratchCardView = findViewById(R.id.scratch_card_01);
        scratchCardView.setDownLayer(R.drawable.down);//设置谜底
        scratchCardView.setUpLayer(R.drawable.up);//设置遮盖层
      //这是刮开谜底完成的回调
        scratchCardView.setOnCompleteListener(new ScratchCardView.OnCompleteListener() {
            @Override
            public void onComplete() {
                Log.d(TAG,"完成00");
           }
        });
```
setDownLayer和setUpLayer还支持文字、颜色和Bitmap
##---------------------只使用看到这里就可以了----------------------------------



#Coding
###绘制
```
 @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (rectF != null) {
            //绘制下面的图层
            drawDownLayer(canvas);

            //绘制上面的图层
            if (!isComplete) {
                canvas.drawBitmap(canvasBitmap, null, rectF, null);
            }
        }
    }

```
直接在dispatchDraw方法中绘制即可，不过我这里为了方便多种类型的绘制，做了一下封装，根据文字，颜色和图片分类绘制
###绘制下面的图层比较简单
```
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
```
###绘制上面的图层
绘制上面的额图层就比较麻烦了，先创建一个跟View相同大小的Bitmap,使用它作为Canvas操作的对象,先把上面的图（遮盖层）绘制到上面，
```
        Bitmap canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);//创建一个Bitmp
        Canvas scratchCanvas = new Canvas(canvasBitmap);//初始化画布
        RectF rectF = new RectF(0, 0, width, height);//初始化画布和遮盖层绘制的位置
        scratchCanvas.drawBitmap(upLayer.bitmap, null, rectF, null);//把还在遮盖绘制到画布上
```
初始化一个Path记录手机移动的位置，初始化Path的Paint
``` //初始化path的Paint
        pathPaint = new Paint();
        pathPaint.setColor(Color.WHITE);//颜色随意，只要不是透明
        pathPaint.setAntiAlias(true);
        pathPaint.setDither(true);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeJoin(Paint.Join.ROUND); // 圆角
        pathPaint.setStrokeCap(Paint.Cap.ROUND); // 圆角
        pathPaint.setStrokeWidth(50);
        pathPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));//这里是重点
        //初始化path
        path = new Path();

```
关于Xfermode，这里配上经典说明图一张
![Xfermode.jpg](http://upload-images.jianshu.io/upload_images/3414806-dcf18bc72399b44f.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
也就是path的Paint的Xfermode类型是DST_OUT的情况，path所绘制的区域都会变成透明
######重写onTouchEvent，记录使用path记录手指轨迹，并绘制在我们创建的画布上（scratchCanvas ）
```
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

 private void drawPath() {
        if (scratchCanvas != null) {
            scratchCanvas.drawPath(path, pathPaint);
        }
  }
```
###计算刮开的面积
即就死按画布上透明像素所占比例，这里思路源于洋神，洋神无敌
```
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

                if (percent > 70 && !isComplete) {
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
```
描述能力有限，看不懂的小伙伴，直接看代码：[代码地址](https://github.com/JianRiverlet/ScratchCard)
##-----------------------------------------------------------------------------------------------------------
###暴富镇楼
![暴富小时候.jpg](http://upload-images.jianshu.io/upload_images/3414806-db2641fff4f22ca8.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/500)


