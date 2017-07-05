
[博客地址文章地址](http://blog.csdn.net/sw5131899/article/details/74390640)

![image](https://github.com/SingleShu/VideoRecord/raw/master/img/GIF2.gif)

制作的gif图有噪点没办法啊。还是看看截图吧


![image](https://github.com/SingleShu/VideoRecord/raw/master/img/a.jpg)


![image](https://github.com/SingleShu/VideoRecord/raw/master/img/b.jpg)

![image](https://github.com/SingleShu/VideoRecord/raw/master/img/c.jpg)

好吧，都是恍恍惚惚红红火火~。不过这些都是小事儿，我最后会把代码放在git上，需要的朋友自己去拉下来跑一下就知道效果了。

![image](https://github.com/SingleShu/VideoRecord/raw/master/img/d.png)

一共就是这些类。当然最重要还是VideoRecordSurface这个类里面的逻辑，对摄像头做了各种初始化，设置还有录制的视频质量，时间等相关设置。

![image](https://github.com/SingleShu/VideoRecord/raw/master/img/e.png)

视频存放的默认地址，在外面设置，这样不用每次都去修改里面的代码嘛。相关的最小录制时间，最大录制时间，我们老板说了，微信只能录6秒，那咱们也要录6秒。时间短了就算没录上。好吧，加了两个限制参数就ok了。当然还要能手动取消录制啊。这个需要在onTouch的事件中做处理了。当然还要录制视频的第一帧做显示，还要有个进度条，为了让用户知道自己录制了多少嘛。好吧，这些都easy。
问题一个一个来解决：


1、如何拿到视频第一帧？


![image](https://github.com/SingleShu/VideoRecord/raw/master/img/f.png)

谷歌提供的媒体相关api中有个 MediaMetadataRetriever，设置资源来源之后。可以获取该资源相关的信息。ok了。


2、如何展示进度条，自定义进度条。


  自定义一个不就Ok了，一条线，主要的还是如何更新，把更新相关设置暴露给调用层。需要几秒，就设置几秒，开启一个线程，然后每一秒回调更新UI就搞定了，这里要调用postInvalidate();
```
public class VideoProgressView extends View {
    private Paint mProgressPaint;//进度条的画笔
    private int mProgress = 0;//进度条
    private int mHeight;
    private int mWidth;
    private Handler mHandler;

    public VideoProgressView(Context context) {
        this(context, null);
    }

    public VideoProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public VideoProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(Color.TRANSPARENT);
        mHandler = new Handler(Looper.getMainLooper());
    }


    private Paint getDefaultPaint() {
        if (mProgressPaint == null) {
            mProgressPaint = new Paint();
            mProgressPaint.setAntiAlias(true);
            mProgressPaint.setColor(Color.GREEN);
            mProgressPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mProgressPaint.setStrokeWidth(mHeight * 2);
        }
        return mProgressPaint;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = measureWidth(widthMeasureSpec);
        mHeight = measureHeight(heightMeasureSpec);
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    private int measureWidth(int widthMeasureSpec) {
        int width;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            width = widthSize;
        } else {
            width = 200;
            if (widthMode == MeasureSpec.AT_MOST) {
                width = Math.min(width, widthSize);
            }
        }
        return width;
    }

    private int measureHeight(int heightMeasureSpec) {
        int height;
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (heightMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            height = heightSize;
        } else {
            height = 50;
            if (heightMode == MeasureSpec.AT_MOST) {
                height = Math.min(heightMeasureSpec, height);
            }
        }
        return height;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float startX = mProgress;
        float startY = mHeight;
        float stopX = mWidth - mProgress;
        float stopY = mHeight;
        canvas.drawLine(startX, startY, stopX, stopY, getDefaultPaint());
    }

    /**
     * 设置进度条
     *
     * @param second 秒
     */
    public void startProgress(final int second) {
        isProgress = true;
        setVisibility(VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count = mWidth / 2;
                int sleepTime = second * 1000 / count;
                for (int i = 0; i < count; i++) {

                    if ((i == count - 1) || !isProgress) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setVisibility(INVISIBLE);
                            }
                        });
                        break;
                    } else {
                        mProgress = i;
                        postInvalidate();
                        SystemClock.sleep(sleepTime);
                    }


                }
            }
        }).start();
    }

    private boolean isProgress;

    public void stopProgress() {
        isProgress = false;
    }

}
```


3、如何实现手动取消呢？


这个需要在录制的Activity里做文章了，自定义的Surface保证功能单一，只是摄像头视频录制相关设置。我们首先要分析点击录制那个按钮有几个状态。第一，点下去，是开始录制视频。第二，一直按着，滑动到按钮范围外取消。第三、抬起。抬起时需要判断抬起位置和按住的时间长短是否达到了最短时间。这就是onTouch的三个状态。那么就设置button的onTouch了。

```
btnStart.setOnTouchListener(new View.OnTouchListener() {
            private float moveY;
            private float moveX;
            Rect rect = new Rect();
            boolean isInner = true;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        //按住事件发生后执行代码的区域
                        tvTips.setVisibility(View.VISIBLE);
                        videoRecordSurface.record(VedioRecordActivity.this,listener.getOrientationHintDegrees());
                        videoProgressView.startProgress(videoRecordSurface.mRecordMaxTime);
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        //移动事件发生后执行代码的区域
                        if (rect.right == 0 && rect.bottom == 0) {
                            btnStart.getFocusedRect(rect);
                        }
                        moveX = event.getX();
                        moveY = event.getY();
                        if (moveY > 0 && moveX > 0 && moveX <= rect.right && moveY <= rect.bottom) {
                            //内
                            isInner = true;
                            if (!"移开取消".equals(tvTips.getText().toString().trim())) {
                                tvTips.setBackgroundColor(Color.TRANSPARENT);
                                tvTips.setTextColor(getResources().getColor(R.color.video_green));
                                tvTips.setText("移开取消");
                            }
                            btnStart.setVisibility(View.INVISIBLE);
                        } else {
                            //外
                            isInner = false;
                            if (!"松开取消".equals(tvTips.getText().toString().trim())) {
                                tvTips.setBackgroundColor(Color.RED);//getResources().getColor(android.R.color.holo_red_dark));
                                tvTips.setTextColor(Color.WHITE);
                                tvTips.setText("松开取消");
                            }
                        }
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        //松开事件发生后执行代码的区域
                        tvTips.setVisibility(View.INVISIBLE);
                        videoProgressView.stopProgress();
                        if (iTime <= videoRecordSurface.mRecordMiniTime || (iTime < videoRecordSurface.mRecordMaxTime && !isInner)) {
                            if (isInner) {
                                Toast.makeText(VedioRecordActivity.this, "录制时间太短", Toast.LENGTH_SHORT).show();
                            } else {
                                //
                            }
                            videoRecordSurface.stopRecord();
                            videoRecordSurface.repCamera();
                            btnStart.setVisibility(View.VISIBLE);
                        } else if(iTime < videoRecordSurface.mRecordMaxTime){
                            videoRecordSurface.stop();
                        }
                        break;
                    }
                    default:
                        break;
                }
                return false;
            }
        });
```
好啦。录制，保存，取消，时间长短，进度条都解决了。代码就不都贴出来了，上传到git上吧，有需要的自己拉下来瞅瞅。亲测有效。觉得有用给个star，谢谢了 。