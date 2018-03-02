package com.luck.picture.lib.camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.luck.picture.lib.camera.lisenter.CaptureLisenter;
import com.luck.picture.lib.camera.util.CheckPermission;

/**
 * =====================================
 * 作    者: 陈嘉桐 445263848@qq.com
 * 版    本：1.0.4
 * 创建日期：2017/4/25
 * 描    述：拍照按钮
 * =====================================
 */
public class CaptureButton extends View {
    //    private static final String TAG = "CJT";


    //按钮可执行的功能状态
    private int button_state;

    //空状态
    public static final int STATE_NULL = 0x000;
    //点击后松开时候的状态
    public static final int STATE_UNPRESS_CLICK = 0X002;
    //点击按下时候的状态
    public static final int STATE_PRESS_CLICK = 0X001;
    //长按按下时候的状态
    public static final int STATE_PRESS_LONG_CLICK = 0x003;
    //长按后松开时候的状态
    public static final int STATE_UNPRESS_LONG_CLICK = 0x004;
    //长按后处理的逻辑Runnable
    private LongPressRunnable longPressRunnable;
    //录制视频的Runnable
    private RecordRunnable recordRunnable;
    //录视频进度条动画
    private ValueAnimator record_anim = ValueAnimator.ofFloat(0, 362);

    //当前按钮状态
    private int state;


    private Paint mPaint;
    //进度条宽度
    private float strokeWidth;
    //长按外圆半径变大的Size
    private int outside_add_size;
    //长安内圆缩小的Size
    private int inside_reduce_size;

    //中心坐标
    private float center_X;
    private float center_Y;

    //按钮半径
    private float button_radius;

    //外圆半径
    private float button_outside_radius;
    //内圆半径
    private float button_inside_radius;

    //按钮大小
    private int button_size;
    //录制视频的进度
    private float progress;
    private RectF rectF;
    //录制视频最大时间长度
    private int duration;

    //按钮回调接口
    private CaptureLisenter captureLisenter;

//    private boolean hasWindowFocus = true;

    public CaptureButton(Context context) {
        super(context);
    }

    //customize construction method
    public CaptureButton(Context context, int size) {
        super(context);
        this.button_size = size;
        button_radius = size / 2.0f;

        button_outside_radius = button_radius;
        button_inside_radius = button_radius * 0.71f;

        strokeWidth = size / 15;
        outside_add_size = size / 7;// 外圆增大半径
        inside_reduce_size = size / 8; //内圆减小半径

        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        progress = 0;

        //init longPress runnable
        longPressRunnable = new LongPressRunnable();
        recordRunnable = new RecordRunnable();
        //set default state;
        this.state = STATE_NULL;

        this.button_state = JCameraView.BUTTON_STATE_BOTH;

        //set max record duration,default 10*1000
        duration = 10 * 1000;
        center_X = (button_size + outside_add_size * 2) / 2;
        center_Y = (button_size + outside_add_size * 2) / 2;

        rectF = new RectF(
                center_X - (button_radius + outside_add_size - strokeWidth / 2),
                center_Y - (button_radius + outside_add_size - strokeWidth / 2),
                center_X + (button_radius + outside_add_size - strokeWidth / 2),
                center_Y + (button_radius + outside_add_size - strokeWidth / 2));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(button_size + outside_add_size * 2, button_size + outside_add_size * 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setStyle(Paint.Style.FILL);
        //外圆弧（半透明）
        mPaint.setColor(JCameraView.COLOR_CAPTURE_BUTTON_OUT_CIRCLE);
//        mPaint.setStrokeWidth(strokeWidth);
        canvas.drawCircle(center_X, center_Y, button_outside_radius, mPaint);

        //内圆（白色）
//        mPaint.setColor(0x00000000);
//        canvas.drawCircle(center_X, center_Y, button_inside_radius, mPaint);
        mPaint.setColor(JCameraView.COLOR_CAPTURE_BUTTON_INSIDE_CIRCLE);
        canvas.drawCircle(center_X, center_Y, button_inside_radius, mPaint);

        //如果状态为按钮长按按下的状态，则绘制录制进度条
        if (state == STATE_PRESS_LONG_CLICK) {
            mPaint.setAntiAlias(true);
            mPaint.setColor(JCameraView.COLOR_CAPTURE_BUTTON_RECORDING_PROGRESS);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(strokeWidth);
            canvas.drawArc(rectF, -90, progress, false, mPaint);
        }
    }

    //Touch_Event_Down时候记录的Y值
    float event_Y;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            //
            case MotionEvent.ACTION_DOWN:
                if (event.getPointerCount() > 1) {
                    break;
                }
                //记录Y值
                event_Y = event.getY();
                //修改当前状态为点击按下
                state = STATE_PRESS_CLICK;
                //当前状态能否录制
                //判断按钮状态是否为可录制状态
                if (!isRecorder &&
                        (button_state == JCameraView.BUTTON_STATE_ONLY_RECORDER ||
                                button_state == JCameraView.BUTTON_STATE_BOTH)) {
                    //同时延长100启动长按后处理的逻辑Runnable
                    postDelayed(longPressRunnable, 100);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (captureLisenter != null
                        && state == STATE_PRESS_LONG_CLICK
                        && (button_state == JCameraView.BUTTON_STATE_ONLY_RECORDER || button_state == JCameraView.BUTTON_STATE_BOTH)) {
                    //记录当前Y值与按下时候Y值的差值，调用缩放回调接口
                    captureLisenter.recordZoom(event_Y - event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
                //根据当前按钮的状态进行相应的处理
                handlerUnpressByState();
                break;
        }
        return true;
    }

    //当手指松开按钮时候处理的逻辑
    private void handlerUnpressByState() {
        //移除长按逻辑的Runnable
        removeCallbacks(longPressRunnable);
        //根据当前状态处理
        switch (state) {
            //当前是点击按下
            case STATE_PRESS_CLICK:
                if (captureLisenter != null &&
                        (button_state == JCameraView.BUTTON_STATE_ONLY_CAPTURE ||
                                button_state == JCameraView.BUTTON_STATE_BOTH)) {
                    //回调拍照接口
                    captureLisenter.takePictures();
                }
                break;
            //当前是长按按下
            case STATE_PRESS_LONG_CLICK:
                state = STATE_UNPRESS_LONG_CLICK;
                //移除录制视频的Runnable
                removeCallbacks(recordRunnable);
                //录制结束
                recordEnd(false);
                break;
        }
        //制空当前状态
        this.state = STATE_NULL;
    }

    private boolean isRecorder = false;

    /**
     * 当前能否录视频
     */
    public void isRecord(boolean record) {
        isRecorder = record;
    }

    /**
     * LongPressRunnable
     */
    private class LongPressRunnable implements Runnable {
        @Override
        public void run() {
            //如果按下后经过100毫秒则会修改当前状态为长按状态
            state = STATE_PRESS_LONG_CLICK;
            //启动按钮动画，外圆变大，内圆缩小
            if (CheckPermission.getRecordState() != CheckPermission.STATE_SUCCESS) {
                if (captureLisenter != null) {
                    captureLisenter.recordError();
                    state = STATE_NULL;
                    return;
                }
            }
            startAnimation(
                    button_outside_radius,
                    button_outside_radius + outside_add_size,
                    button_inside_radius,
                    button_inside_radius - inside_reduce_size
            );
        }
    }

    /**
     * record runnable
     */
    private class RecordRunnable implements Runnable {
        @Override
        public void run() {
            record_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (state == STATE_PRESS_LONG_CLICK) {
                        //更新录制进度
                        progress = (float) animation.getAnimatedValue();
//                        int p = (int) progress / 36;
//                        captureLisenter.recordLoding(p);
                    }
                    invalidate();
                }
            });
            //如果一直长按到结束，则自动回调录制结束接口
            record_anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (state == STATE_PRESS_LONG_CLICK) {
                        recordEnd(true);
                    }
                }
            });
            record_anim.setInterpolator(new LinearInterpolator());
            record_anim.setDuration(duration);
            record_anim.start();
        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            handlerUnpressByState();
        }
    }

    /**
     * 录制结束
     *
     * @param finish 是否录制满时间
     */
    private void recordEnd(boolean finish) {
        state = STATE_UNPRESS_LONG_CLICK;
        if (captureLisenter != null) {
            //录制时间小于一秒时候则提示录制时间过短
            if (record_anim.getCurrentPlayTime() < 1100 && !finish) {
                captureLisenter.recordShort(record_anim.getCurrentPlayTime());
            } else {
                if (finish) {
                    captureLisenter.recordEnd(duration);
                } else {
                    captureLisenter.recordEnd(record_anim.getCurrentPlayTime());
                }
            }
        }
        resetRecordAnim();
    }

    private void resetRecordAnim() {
        //取消动画
        record_anim.cancel();
        //重制进度
        progress = 0;
        invalidate();
        //还原按钮初始状态动画
        startAnimation(
                button_outside_radius,
                button_radius,
                button_inside_radius,
                button_radius * 0.75f
        );
    }

    //capture button outside and inside resize animation
    //钮动画，外圆变大，内圆缩小
    private void startAnimation(float outside_start, float outside_end, float inside_start, float inside_end) {
        ValueAnimator outside_anim = ValueAnimator.ofFloat(outside_start, outside_end);
        ValueAnimator inside_anim = ValueAnimator.ofFloat(inside_start, inside_end);
        //外圆
        outside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                button_outside_radius = (float) animation.getAnimatedValue();
                invalidate();
            }

        });
        //内圆
        inside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                button_inside_radius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        //当动画结束后启动录像Runnable并且回调录像开始接口
        outside_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (state == STATE_PRESS_LONG_CLICK) {
                    if (captureLisenter != null) {
                        captureLisenter.recordStart();
                    }
                    post(recordRunnable);
                }
            }
        });
        outside_anim.setDuration(100);
        inside_anim.setDuration(100);
        outside_anim.start();
        inside_anim.start();
    }

    /**
     * set record duration
     * 设置最长录制时间
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    //设置回调接口
    public void setCaptureLisenter(CaptureLisenter captureLisenter) {
        this.captureLisenter = captureLisenter;
    }

    //设置按钮功能（拍照和录像）
    public void setButtonFeatures(int state) {
        this.button_state = state;
    }
}
