package com.luck.picture.lib.camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.luck.picture.lib.camera.lisenter.CaptureLisenter;
import com.luck.picture.lib.camera.lisenter.ReturnLisenter;
import com.luck.picture.lib.camera.lisenter.TypeLisenter;

/**
 * =====================================
 * 作    者: 陈嘉桐 445263848@qq.com
 * 版    本：1.0.4
 * 创建日期：2017/4/26
 * 描    述：集成各个控件的布局
 * =====================================
 */

public class CaptureLayout extends FrameLayout {
    //拍照按钮监听
    private CaptureLisenter captureLisenter;
    //拍照或录制后接结果按钮监听
    private TypeLisenter typeLisenter;
    //退出按钮监听
    private ReturnLisenter returnLisenter;

    public void setTypeLisenter(TypeLisenter typeLisenter) {
        this.typeLisenter = typeLisenter;
    }

    public void setCaptureLisenter(CaptureLisenter captureLisenter) {
        this.captureLisenter = captureLisenter;
    }

    public void setReturnLisenter(ReturnLisenter returnLisenter) {
        this.returnLisenter = returnLisenter;
    }

    private CaptureButton btn_capture;
    private TypeButton btn_confirm;
    private TypeButton btn_cancel;
    private ReturnButton btn_return;
    private TextView txt_tip;
//    private TextView txt_time;

    private int layout_width;
    private int layout_height;
    private int button_size;

    public CaptureLayout(Context context) {
        this(context, null);
    }

    public CaptureLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptureLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //get width
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);

        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            layout_width = outMetrics.widthPixels;
        } else {
            layout_width = outMetrics.widthPixels / 2;
        }
        button_size = (int) (layout_width / 5.1f);
        layout_height = button_size + (button_size / 5) * 2 + 100;

        initView();
        initEvent();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(layout_width, layout_height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        canvas.drawColor(0xffff0000);
        super.onDraw(canvas);
    }

    public void initEvent() {
        //默认Typebutton为隐藏
        btn_cancel.setVisibility(INVISIBLE);
        btn_confirm.setVisibility(INVISIBLE);
    }

    public void startTypeBtnAnimator() {
        //拍照录制结果后的动画
        btn_capture.setVisibility(INVISIBLE);
        btn_return.setVisibility(INVISIBLE);
        btn_cancel.setVisibility(VISIBLE);
        btn_confirm.setVisibility(VISIBLE);
        btn_cancel.setClickable(false);
        btn_confirm.setClickable(false);
        ObjectAnimator animator_cancel = ObjectAnimator.ofFloat(btn_cancel, "translationX", layout_width / 4, 0);
        animator_cancel.setDuration(200);
        animator_cancel.start();
        ObjectAnimator animator_confirm = ObjectAnimator.ofFloat(btn_confirm, "translationX", -layout_width / 4, 0);
        animator_confirm.setDuration(200);
        animator_confirm.start();
        animator_confirm.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                btn_cancel.setClickable(true);
                btn_confirm.setClickable(true);
            }
        });
    }

    private boolean isFirst = true;

    public void startAlphaAnimation() {
        if (isFirst) {
            ObjectAnimator animator_txt_tip = ObjectAnimator.ofFloat(txt_tip, "alpha", 1f, 0f);
            animator_txt_tip.setDuration(100);
            animator_txt_tip.start();
            isFirst = false;
        }
    }

    private void initView() {
        setWillNotDraw(false);
        //btn_capture
        btn_capture = new CaptureButton(getContext(), button_size);
        LayoutParams btn_capture_param = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
//        btn_capture_param.addRule(CENTER_IN_PARENT, TRUE);
        btn_capture_param.gravity = Gravity.CENTER;
        //btn_capture_param.setMargins(0, 0, 0, (int)(button_size * 0.57f)); //拍照View的位置
        btn_capture.setLayoutParams(btn_capture_param);
        btn_capture.setDuration(10 * 1000);
        btn_capture.setCaptureLisenter(new CaptureLisenter() {
            @Override
            public void takePictures() {
                if (captureLisenter != null) {
                    captureLisenter.takePictures();
                }
            }

            @Override
            public void recordShort(long time) {
                if (captureLisenter != null) {
                    captureLisenter.recordShort(time);
                }
                startAlphaAnimation();
            }

            @Override
            public void recordStart() {
//                txt_time.setVisibility(VISIBLE);
                if (captureLisenter != null) {
                    captureLisenter.recordStart();
                }
                startAlphaAnimation();
            }

            @Override
            public void recordLoding(int time) {
//                if (txt_time != null) {
//                    Log.i("CaptureLayout", "camera_progress:" + time);
//                    txt_time.setText("00:0" + time);
//                }
            }

            @Override
            public void recordEnd(long time) {
                if (captureLisenter != null) {
                    captureLisenter.recordEnd(time);
                }
                startAlphaAnimation();
                startTypeBtnAnimator();
            }

            @Override
            public void recordZoom(float zoom) {
                if (captureLisenter != null) {
                    captureLisenter.recordZoom(zoom);
                }
            }

            @Override
            public void recordError() {
                if (captureLisenter != null) {
                    captureLisenter.recordError();
                }
            }
        });

        //btn_cancel

        btn_cancel = new TypeButton(getContext(), TypeButton.TYPE_CANCEL, button_size);
        final LayoutParams btn_cancel_param = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
//        btn_cancel_param.addRule(CENTER_VERTICAL, TRUE);
//        btn_cancel_param.addRule(ALIGN_PARENT_LEFT, TRUE);
        btn_cancel_param.gravity = Gravity.CENTER_VERTICAL;
        btn_cancel_param.setMargins((layout_width / 4) - button_size / 2, 0, 0, 0);
        btn_cancel.setLayoutParams(btn_cancel_param);
        btn_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (typeLisenter != null) {
                    typeLisenter.cancel();
                }
                startAlphaAnimation();
                btn_cancel.setVisibility(INVISIBLE);
                btn_confirm.setVisibility(INVISIBLE);
                btn_capture.setVisibility(VISIBLE);
                btn_return.setVisibility(VISIBLE);
            }
        });

        //btn_confirm

        btn_confirm = new TypeButton(getContext(), TypeButton.TYPE_CONFIRM, button_size);
        LayoutParams btn_confirm_param = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
//        btn_confirm_param.addRule(CENTER_VERTICAL, TRUE);
//        btn_confirm_param.addRule(ALIGN_PARENT_RIGHT, TRUE);
        btn_confirm_param.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
        btn_confirm_param.setMargins(0, 0, (layout_width / 4) - button_size / 2, 0);
        btn_confirm.setLayoutParams(btn_confirm_param);
        btn_confirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (typeLisenter != null) {
                    typeLisenter.confirm();
                }
                startAlphaAnimation();
                btn_cancel.setVisibility(INVISIBLE);
                btn_confirm.setVisibility(INVISIBLE);
                btn_capture.setVisibility(VISIBLE);
                btn_return.setVisibility(VISIBLE);
            }
        });

        //btn_return
        btn_return = new ReturnButton(getContext(), button_size / 3); //返回按钮大小缩小为 1/3
        LayoutParams btn_return_param = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//        btn_return_param.addRule(CENTER_VERTICAL, TRUE);
        btn_return_param.gravity = Gravity.CENTER_VERTICAL;
        btn_return_param.setMargins(layout_width / 9, button_size / 4 , 0, 0); //返回按钮的位置 左下角位置
        btn_return.setLayoutParams(btn_return_param);
        btn_return.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (captureLisenter != null) {
                    if (returnLisenter != null) {
                        returnLisenter.onReturn();
                    }
                }
            }
        });


        //txt_tip
        txt_tip = new TextView(getContext());
        LayoutParams txt_param = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        txt_param.gravity = Gravity.CENTER_HORIZONTAL;

        // add by tan 轻按拍照，按住摄像 文字对应的TextView 距离100px
        txt_param.setMargins(0, 0, 0, 100);
        txt_tip.setTextColor(0xFFFFFFFF);
        txt_tip.setGravity(Gravity.CENTER);
        txt_tip.setLayoutParams(txt_param);

//        txt_time = new TextView(getContext());
//        txt_time.setTextColor(0xFFFFFFFF);
//        txt_time.setGravity(Gravity.CENTER);
//        txt_time.setLayoutParams(txt_param);

        this.addView(btn_capture);
        this.addView(btn_cancel);
        this.addView(btn_confirm);
        this.addView(btn_return);
        this.addView(txt_tip);

    }

    public void setTextWithAnimation(String tip) {
        txt_tip.setText(tip);
        ObjectAnimator animator_txt_tip = ObjectAnimator.ofFloat(txt_tip, "alpha", 0f, 1f, 1f, 0f);
        animator_txt_tip.setDuration(2500);
        animator_txt_tip.start();
    }

    public void setDuration(int duration) {
        btn_capture.setDuration(duration);
    }

    public void isRecord(boolean record) {
        btn_capture.isRecord(record);
    }

    public void setButtonFeatures(int state) {
        btn_capture.setButtonFeatures(state);
    }

    public void setTip(String tip) {
        txt_tip.setText(tip);
    }
}
