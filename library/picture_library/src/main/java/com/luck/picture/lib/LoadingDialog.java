package com.luck.picture.lib;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * Created on 2016/12/12.
 * Author：qdq
 * Description:数据加载对话框
 */
public class LoadingDialog extends Dialog {
    private TextView content_tv;
    private ImageView load_iv;
    private RotateAnimation mAnim;
    public LoadingDialog(Context context) {
        super(context, R.style.dialog_style);
        init(context);
    }

    public LoadingDialog(Context context, int themeResId) {
        super(context,themeResId);
        init(context);
    }
    private void init(Context context){
        View view= LayoutInflater.from(context).inflate(R.layout.load_dialog,null);
        content_tv= (TextView) view.findViewById(R.id.content_tv);
        load_iv= (ImageView) view.findViewById(R.id.load_iv);
        initAnim();
        load_iv.startAnimation(mAnim);
        setContentView(view);
    }
    public void setContent(String content){
        if(!TextUtils.isEmpty(content)){
            content_tv.setText(content);
        }
    }
    private void initAnim() {
        mAnim = new RotateAnimation(0, 360, Animation.RESTART, 0.5f, Animation.RESTART, 0.5f);
        mAnim.setDuration(1000);
        mAnim.setRepeatCount(Animation.INFINITE);
        mAnim.setRepeatMode(Animation.RESTART);
        mAnim.setStartTime(Animation.START_ON_FIRST_FRAME);
    }
}
