package com.luck.picture.lib.camera;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.VideoView;

import com.luck.picture.lib.LoadingDialog;
import com.luck.picture.lib.R;
import com.luck.picture.lib.camera.lisenter.CaptureLisenter;
import com.luck.picture.lib.camera.lisenter.ErrorLisenter;
import com.luck.picture.lib.camera.lisenter.FirstFoucsLisenter;
import com.luck.picture.lib.camera.lisenter.JCameraLisenter;
import com.luck.picture.lib.camera.lisenter.ReturnLisenter;
import com.luck.picture.lib.camera.lisenter.TypeLisenter;
import com.luck.picture.lib.tools.Constant;
import com.luck.picture.lib.tools.DebugUtil;
import com.mabeijianxi.smallvideorecord2.LocalMediaCompress;
import com.mabeijianxi.smallvideorecord2.model.AutoVBRMode;
import com.mabeijianxi.smallvideorecord2.model.LocalMediaConfig;
import com.mabeijianxi.smallvideorecord2.model.OnlyCompressOverBean;

import java.io.File;
import java.io.IOException;


/**
 * =====================================
 * 作    者: 陈嘉桐
 * 版    本：1.0.4
 * 创建日期：2017/4/25
 * 描    述：拍照和录像功能，由yy根据公司需求进行了订制
 * =====================================
 */
public class JCameraView extends FrameLayout implements CameraInterface.CamOpenOverCallback, SurfaceHolder.Callback {
    private static final String TAG = "MIO";

    //拍照浏览时候的类型
    private static final int TYPE_PICTURE = 0x001;
    private static final int TYPE_VIDEO = 0x002;

    //录制视频比特率
    public static final int MEDIA_QUALITY_HIGH = 40 * 100000;
    public static final int MEDIA_QUALITY_MIDDLE = 25 * 100000;
    public static final int MEDIA_QUALITY_LOW = 12 * 100000;
    public static final int MEDIA_QUALITY_POOR = 8 * 100000;
    public static final int MEDIA_QUALITY_FUNNY = 4 * 100000;
    public static final int MEDIA_QUALITY_DESPAIR = 2 * 100000;
    public static final int MEDIA_QUALITY_SORRY = 80000;

    //界面上所有控件颜色 集中管理
    public static final int COLOR_CAPTURE_BUTTON_OUT_CIRCLE = 0x88FFFFFF;//半透明白
    public static final int COLOR_CAPTURE_BUTTON_INSIDE_CIRCLE = 0xFFFFFFFF;//全白
    public static final int COLOR_CAPTURE_BUTTON_RECORDING_PROGRESS = 0xFFFDC300;//黄
    public static final int COLOR_FOCUS_VIEW = 0xFFFDC300;//黄

    public static final int COLOR_TYPE_BUTTON_CANCEL_BACKGROUND = 0x88FFFFFF;//半透明白
    public static final int COLOR_TYPE_BUTTON_CANCEL_OBJECT = 0xFF4A4A4A;//灰黑

    public static final int COLOR_TYPE_BUTTON_CONFIRM_BACKGROUND = 0xFFFFFFFF;//半透明白
    public static final int COLOR_TYPE_BUTTON_CONFIRM_OBJECT = 0xFFFDC300;//黄


    //只能拍照
    public static final int BUTTON_STATE_ONLY_CAPTURE = 0x101;
    //只能录像
    public static final int BUTTON_STATE_ONLY_RECORDER = 0x102;
    //两者都可以
    public static final int BUTTON_STATE_BOTH = 0x103;

    //回调监听
    private JCameraLisenter jCameraLisenter;


    private Context mContext;
    private VideoView mVideoView;
    private ImageView mPhoto;
    private ImageView mSwitchCamera;  //摄像头前后切换
    private CaptureLayout mCaptureLayout; // 底部布局控制器
    //private ImageView mBack;   //返回
    private FoucsView mFoucsView;
    private MediaPlayer mMediaPlayer;
    LoadingDialog loadingDialog;

    private int layout_width;
    private int fouce_size;
    private float screenProp;

    //拍照的图片
    private Bitmap captureBitmap;
    //第一帧图片
    private Bitmap firstFrame;
    //视频URL
    private String videoUrl;
    private int type = -1;
    private boolean onlyPause = false;

    private int CAMERA_STATE = -1;
    private static final int STATE_IDLE = 0x010;
    private static final int STATE_RUNNING = 0x020;
    private static final int STATE_WAIT = 0x030;

    private boolean stopping = false;
    private boolean isBorrow = false;
    private boolean takePictureing = false;
    private boolean forbiddenSwitch = false;

    /**
     * switch buttom param
     */
    private int iconSize = 0;
    private int iconMargin = 0;
    private int iconSrc = 0;
    private int duration = 0;

    /**
     * constructor
     */
    public JCameraView(Context context) {
        this(context, null);
    }

    /**
     * constructor
     */
    public JCameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * constructor
     */
    public JCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        //get AttributeSet
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.JCameraView, defStyleAttr, 0);
        iconSize = a.getDimensionPixelSize(R.styleable.JCameraView_iconSize, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 35, getResources().getDisplayMetrics()));
        iconMargin = a.getDimensionPixelSize(R.styleable.JCameraView_iconMargin, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 15, getResources().getDisplayMetrics()));
        iconSrc = a.getResourceId(R.styleable.JCameraView_iconSrc, R.drawable.ic_sync_black_24dp);
        duration = a.getInteger(R.styleable.JCameraView_duration_max, 10 * 1000);
        a.recycle();
        initData();
        initView();
    }

    private void initData() {
        WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        layout_width = outMetrics.widthPixels;
        fouce_size = layout_width / 4;
        CAMERA_STATE = STATE_IDLE;
    }


    private void initView() {
        setWillNotDraw(false);
        this.setBackgroundColor(0xff000000);
        //VideoView
        mVideoView = new VideoView(mContext);
        LayoutParams videoViewParam = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mVideoView.setLayoutParams(videoViewParam);

        //mPhoto
        mPhoto = new ImageView(mContext);
        LayoutParams photoParam = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams
                .MATCH_PARENT);
        mPhoto.setLayoutParams(photoParam);
        mPhoto.setBackgroundColor(0xff000000);
        mPhoto.setVisibility(INVISIBLE);

        //back
        //mBack = new ImageView(mContext);
        iconSize = 3 * iconSize / 4;
        LayoutParams backParam = new LayoutParams(iconSize + 2 * iconMargin, iconSize + 3 * iconMargin);
//        mBack.setPadding(iconMargin, 3 * iconMargin / 2, iconMargin, iconMargin);
//        mBack.setLayoutParams(backParam);
//        mBack.setImageResource(R.drawable.back);
//        mBack.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (jCameraLisenter != null && !takePictureing) {
//                    jCameraLisenter.quit();
//                }
//            }
//        });

        //switchCamera
        mSwitchCamera = new ImageView(mContext);
        LayoutParams imageViewParam = new LayoutParams(iconSize + 2 * iconMargin, iconSize + 3 * iconMargin);
        imageViewParam.gravity = Gravity.RIGHT;
        mSwitchCamera.setPadding(iconMargin, 3 * iconMargin / 2, iconMargin, iconMargin);
        mSwitchCamera.setLayoutParams(imageViewParam);
        mSwitchCamera.setImageResource(iconSrc);
        mSwitchCamera.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBorrow || switching || forbiddenSwitch) {
                    return;
                }
                switching = true;
                new Thread() {
                    /**
                     * switch camera
                     */
                    @Override
                    public void run() {
                        CameraInterface.getInstance().switchCamera(JCameraView.this);
                    }
                }.start();
            }
        });
        //CaptureLayout
        mCaptureLayout = new CaptureLayout(mContext);
        LayoutParams layout_param = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layout_param.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        mCaptureLayout.setLayoutParams(layout_param);
        mCaptureLayout.setDuration(duration);

        //mFoucsView
        mFoucsView = new FoucsView(mContext, fouce_size);
        LayoutParams foucs_param = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        foucs_param.gravity = Gravity.CENTER;
        mFoucsView.setLayoutParams(foucs_param);
        mFoucsView.setVisibility(INVISIBLE);

        //add view to ParentLayout
        this.addView(mVideoView);
        this.addView(mPhoto);
        this.addView(mSwitchCamera);
        //this.addView(mBack);
        this.addView(mCaptureLayout);
        this.addView(mFoucsView);
        //START >>>>>>> captureLayout lisenter callback
        mCaptureLayout.setCaptureLisenter(new CaptureLisenter() {
            @Override
            public void takePictures() {
                if (CAMERA_STATE != STATE_IDLE || takePictureing) {
                    return;
                }
                CAMERA_STATE = STATE_RUNNING;
                takePictureing = true;
                mFoucsView.setVisibility(INVISIBLE);
                CameraInterface.getInstance().takePicture(new CameraInterface.TakePictureCallback() {
                    @Override
                    public void captureResult(Bitmap bitmap, boolean isVertical) {
                        captureBitmap = bitmap;
                        CameraInterface.getInstance().doStopCamera();
                        type = TYPE_PICTURE;
                        isBorrow = true;
                        CAMERA_STATE = STATE_WAIT;
                        if (isVertical) {
                            mPhoto.setScaleType(ImageView.ScaleType.FIT_XY);
                        } else {
                            mPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        }
                        mPhoto.setImageBitmap(bitmap);
                        mPhoto.setVisibility(VISIBLE);
                        mCaptureLayout.startAlphaAnimation();
                        mCaptureLayout.startTypeBtnAnimator();
                        takePictureing = false;
                        mSwitchCamera.setVisibility(INVISIBLE);
                        //mBack.setVisibility(INVISIBLE);
                        CameraInterface.getInstance().doOpenCamera(JCameraView.this);
                    }
                });
            }

            @Override
            public void recordShort(long time) {
                if (CAMERA_STATE != STATE_RUNNING && stopping) {
                    return;
                }
                stopping = true;
                mCaptureLayout.setTip("录制时间过短");
                mSwitchCamera.setRotation(0);
                mSwitchCamera.setVisibility(VISIBLE);
                //mBack.setVisibility(VISIBLE);
                CameraInterface.getInstance().setSwitchView(mSwitchCamera);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CameraInterface.getInstance().stopRecord(true, new
                                CameraInterface.StopRecordCallback() {
                                    @Override
                                    public void recordResult(String url, Bitmap firstFrame) {
                                        DebugUtil.i(TAG, "Record Stopping ...");
                                        mCaptureLayout.isRecord(false);
                                        CAMERA_STATE = STATE_IDLE;
                                        stopping = false;
                                        isBorrow = false;
                                    }
                                });
                    }
                }, 1100 - time);
            }

            @Override
            public void recordStart() {
                if (CAMERA_STATE != STATE_IDLE && stopping) {
                    return;
                }

                mSwitchCamera.setVisibility(GONE);
                //mBack.setVisibility(GONE);
                mCaptureLayout.isRecord(true);
                isBorrow = true;
                CAMERA_STATE = STATE_RUNNING;
                mFoucsView.setVisibility(INVISIBLE);
                CameraInterface.getInstance().startRecord(mVideoView.getHolder().getSurface(), new CameraInterface
                        .ErrorCallback() {
                    @Override
                    public void onError() {
                        DebugUtil.i("CJT", "startRecorder error");
                        mCaptureLayout.isRecord(false);
                        CAMERA_STATE = STATE_WAIT;
                        stopping = false;
                        isBorrow = false;
                    }
                });
            }

            @Override
            public void recordLoding(int time) {
            }

            @Override
            public void recordEnd(long time) {
                CameraInterface.getInstance().stopRecord(false, new CameraInterface.StopRecordCallback() {
                    @Override
                    public void recordResult(final String url, Bitmap firstFrame) {
                        CAMERA_STATE = STATE_WAIT;
                        videoUrl = url;
                        type = TYPE_VIDEO;
                        JCameraView.this.firstFrame = firstFrame;
                        //播放视频
                        new Thread(new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                            @Override
                            public void run() {
                                try {
                                    if (mMediaPlayer == null) {
                                        mMediaPlayer = new MediaPlayer();
                                    } else {
                                        mMediaPlayer.reset();
                                    }
                                    DebugUtil.i(TAG, "URL = " + url);
                                    mMediaPlayer.setDataSource(url);
                                    mMediaPlayer.setSurface(mVideoView.getHolder().getSurface());
                                    mMediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                    mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer
                                            .OnVideoSizeChangedListener() {
                                        @Override
                                        public void
                                        onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                                            updateVideoViewSize(mMediaPlayer.getVideoWidth(), mMediaPlayer
                                                    .getVideoHeight());
                                        }
                                    });
                                    mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                        @Override
                                        public void onPrepared(MediaPlayer mp) {
                                            mMediaPlayer.start();
                                        }
                                    });
                                    mMediaPlayer.setLooping(true);
                                    mMediaPlayer.prepare();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                });
            }

            @Override
            public void recordZoom(float zoom) {
                CameraInterface.getInstance().setZoom(zoom, CameraInterface.TYPE_RECORDER);
            }

            @Override
            public void recordError() {
                //错误回调
                if (errorLisenter != null) {
                    errorLisenter.AudioPermissionError();
                }
            }
        });
        mCaptureLayout.setTypeLisenter(new TypeLisenter() {
            @Override
            public void cancel() {
                if (CAMERA_STATE == STATE_WAIT) {
                    if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                        mMediaPlayer.stop();
                        mMediaPlayer.release();
                        mMediaPlayer = null;
                    }
                    handlerPictureOrVideo(type, false);
                }
            }

            @Override
            public void confirm() {
                if (CAMERA_STATE == STATE_WAIT) {
                    if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                        mMediaPlayer.stop();
                        mMediaPlayer.release();
                        mMediaPlayer = null;
                    }

                    handlerPictureOrVideo(type, true);
                    //handleCompressVideo();
                }
            }
        });
        mCaptureLayout.setReturnLisenter(new ReturnLisenter() {
            @Override
            public void onReturn() {
                if (jCameraLisenter != null && !takePictureing) {
                    jCameraLisenter.quit();
                }
            }
        });
        //END >>>>>>> captureLayout lisenter callback
        mVideoView.getHolder().addCallback(this);
    }

    /**
     * 视频拍摄后，压缩处理方法
     */
    private void handleCompressVideo() {

        //拍照后直接进入发帖页面，不需要进度提示
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(mContext, R.style.dialog_style);
            loadingDialog.setContent("处理中...");
            loadingDialog.show();
        }

        //视频不需要压缩
        new Thread(new Runnable() {
            @Override
            public void run() {
                //压缩本地视频
                LocalMediaConfig.Buidler buidler = new LocalMediaConfig.Buidler();
                LocalMediaConfig config = buidler
                        .setVideoPath(videoUrl)
                        .captureThumbnailsTime(1)
                        .doH264Compress(new AutoVBRMode(Constant.AutoVBRMode_CAMERA))
                        .setFramerate(Constant.FRAMERATE_20)
                        .setScale(Constant.SCALE_10)
                        .build();
                OnlyCompressOverBean onlyCompressOverBean = new LocalMediaCompress(config).startCompress();
                videoUrl = onlyCompressOverBean.getVideoPath();
                mCaptureLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.dismiss();
                        handlerPictureOrVideo(type, true);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        float widthSize = MeasureSpec.getSize(widthMeasureSpec);
        float heightSize = MeasureSpec.getSize(heightMeasureSpec);
        screenProp = heightSize / widthSize;
    }

    @Override
    public void cameraHasOpened() {
        CameraInterface.getInstance().doStartPreview(mVideoView.getHolder(), screenProp, new FirstFoucsLisenter() {
            @Override
            public void onFouce() {
                JCameraView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        setFocusViewWidthAnimation(getWidth() / 2, getHeight() / 2);
                    }
                });
            }
        });
    }

    private boolean switching = false;

    @Override
    public void cameraSwitchSuccess() {
        switching = false;
    }

    /**
     * start preview
     */
    public void onResume() {
        CameraInterface.getInstance().registerSensorManager(mContext);
        CameraInterface.getInstance().setSwitchView(mSwitchCamera);
        if (onlyPause) {
//            if (isBorrow && type == TYPE_VIDEO) {
//                new Thread(new Runnable() {
//                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
//                    @Override
//                    public void run() {
//                        try {
//                            if (mMediaPlayer == null) {
//                                mMediaPlayer = new MediaPlayer();
//                            } else {
//                                mMediaPlayer.reset();
//                            }
//                            Log.i("CJT", "URL = " + videoUrl);
//                            mMediaPlayer.setDataSource(videoUrl);
//                            mMediaPlayer.setSurface(mVideoView.getHolder().getSurface());
//                            mMediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
//                            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//                            mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer
//                                    .OnVideoSizeChangedListener() {
//                                @Override
//                                public void
//                                onVideoSizeChanged(MediaPlayer mp, int width, int height) {
//                                    updateVideoViewSize(mMediaPlayer.getVideoWidth(), mMediaPlayer
//                                            .getVideoHeight());
//                                }
//                            });
//                            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//                                @Override
//                                public void onPrepared(MediaPlayer mp) {
//                                    mMediaPlayer.start();
//                                }
//                            });
//                            mMediaPlayer.setLooping(true);
//                            mMediaPlayer.prepare();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }).start();
//            } else {
            new Thread() {
                @Override
                public void run() {
                    CameraInterface.getInstance().doOpenCamera(JCameraView.this);
                }
            }.start();
            mFoucsView.setVisibility(INVISIBLE);
//            }
        }
    }

    /**
     * stop preview
     */
    public void onPause() {
        onlyPause = true;
        CameraInterface.getInstance().unregisterSensorManager(mContext);
        CameraInterface.getInstance().doStopCamera();
    }

    private boolean firstTouch = true;
    private float firstTouchLength = 0;
    private int zoomScale = 0;

    /**
     * handler touch focus
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getPointerCount() == 1) {
                    //显示对焦指示器
                    setFocusViewWidthAnimation(event.getX(), event.getY());
                }
                if (event.getPointerCount() == 2) {
                    DebugUtil.i(TAG, "ACTION_DOWN = " + 2);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1) {
                    firstTouch = true;
                }
                if (event.getPointerCount() == 2) {
                    //第一个点
                    float point_1_X = event.getX(0);
                    float point_1_Y = event.getY(0);
                    //第二个点
                    float point_2_X = event.getX(1);
                    float point_2_Y = event.getY(1);

                    float result = (float) Math.sqrt(Math.pow(point_1_X - point_2_X, 2) + Math.pow(point_1_Y -
                            point_2_Y, 2));

                    if (firstTouch) {
                        firstTouchLength = result;
                        firstTouch = false;
                    }
                    if ((int) (result - firstTouchLength) / 40 != 0) {
                        firstTouch = true;
                        CameraInterface.getInstance().setZoom(result - firstTouchLength, CameraInterface.TYPE_CAPTURE);
                    }
                    DebugUtil.i(TAG, "result = " + (result - firstTouchLength));
                }
                break;
            case MotionEvent.ACTION_UP:
                firstTouch = true;
                break;
        }
        return true;
    }

    /**
     * focusview animation
     */
    private void setFocusViewWidthAnimation(float x, float y) {
        if (isBorrow) {
            return;
        }
        if (y > mCaptureLayout.getTop()) {
            return;
        }
        mFoucsView.setVisibility(VISIBLE);
        if (x < mFoucsView.getWidth() / 2) {
            x = mFoucsView.getWidth() / 2;
        }
        if (x > layout_width - mFoucsView.getWidth() / 2) {
            x = layout_width - mFoucsView.getWidth() / 2;
        }
        if (y < mFoucsView.getWidth() / 2) {
            y = mFoucsView.getWidth() / 2;
        }
        if (y > mCaptureLayout.getTop() - mFoucsView.getWidth() / 2) {
            y = mCaptureLayout.getTop() - mFoucsView.getWidth() / 2;
        }
        CameraInterface.getInstance().handleFocus(mContext, x, y, new CameraInterface.FocusCallback() {
            @Override
            public void focusSuccess() {
                mFoucsView.setVisibility(INVISIBLE);
            }
        });

        mFoucsView.setX(x - mFoucsView.getWidth() / 2);
        mFoucsView.setY(y - mFoucsView.getHeight() / 2);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mFoucsView, "scaleX", 1, 0.6f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mFoucsView, "scaleY", 1, 0.6f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(mFoucsView, "alpha", 1f, 0.3f, 1f, 0.3f, 1f, 0.3f, 1f);
        AnimatorSet animSet = new AnimatorSet();
        animSet.play(scaleX).with(scaleY).before(alpha);
        animSet.setDuration(400);
        animSet.start();
    }

    public void setJCameraLisenter(JCameraLisenter jCameraLisenter) {
        this.jCameraLisenter = jCameraLisenter;
    }

    /**
     * 处理拍照 & 拍视频
     * @param type
     * @param confirm
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void handlerPictureOrVideo(int type, boolean confirm) {
        if (jCameraLisenter == null || type == -1) {
            return;
        }
        switch (type) {
            case TYPE_PICTURE:
                mPhoto.setVisibility(INVISIBLE);
                if (confirm && captureBitmap != null) {
                    jCameraLisenter.captureSuccess(captureBitmap);
                } else {
                    if (captureBitmap != null) {
                        captureBitmap.recycle();
                    }
                    captureBitmap = null;
                }
                break;

            case TYPE_VIDEO:
                if (confirm) {
                    //回调录像成功后的URL
                    jCameraLisenter.recordSuccess(videoUrl, firstFrame);
                } else {
                    //删除视频
                    File file = new File(videoUrl);
                    if (file.exists()) {
                        file.delete();
                    }
                }
                mCaptureLayout.isRecord(false);
                LayoutParams videoViewParam = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                mVideoView.setLayoutParams(videoViewParam);
                CameraInterface.getInstance().doOpenCamera(JCameraView.this);
                mSwitchCamera.setRotation(0);
                CameraInterface.getInstance().setSwitchView(mSwitchCamera);
                break;
        }
        isBorrow = false;
        mSwitchCamera.setVisibility(VISIBLE);
        //mBack.setVisibility(VISIBLE);
        CAMERA_STATE = STATE_IDLE;
        mFoucsView.setVisibility(VISIBLE);
        setFocusViewWidthAnimation(getWidth() / 2, getHeight() / 2);

    }

    public void setSaveVideoPath(String path) {
        CameraInterface.getInstance().setSaveVideoPath(path);
    }

    /**
     * TextureView resize
     */
    public void updateVideoViewSize(float videoWidth, float videoHeight) {
        if (videoWidth > videoHeight) {
            LayoutParams videoViewParam;
            int height = (int) ((videoHeight / videoWidth) * getWidth());
            videoViewParam = new LayoutParams(LayoutParams.MATCH_PARENT,
                    height);
            videoViewParam.gravity = Gravity.CENTER;
//            videoViewParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            mVideoView.setLayoutParams(videoViewParam);
        }
    }

    /**
     * forbidden audio
     */
    public void enableshutterSound(boolean enable) {
    }

    public void forbiddenSwitchCamera(boolean forbiddenSwitch) {
        this.forbiddenSwitch = forbiddenSwitch;
    }

    private ErrorLisenter errorLisenter;

    //启动Camera错误回调
    public void setErrorLisenter(ErrorLisenter errorLisenter) {
        this.errorLisenter = errorLisenter;
        CameraInterface.getInstance().setErrorLinsenter(errorLisenter);
    }

    //设置CaptureButton功能（拍照和录像）
    public void setFeatures(int state) {
        this.mCaptureLayout.setButtonFeatures(state);
    }

    //设置录制质量
    public void setMediaQuality(int quality) {
        CameraInterface.getInstance().setMediaQuality(quality);
    }

    public void setTip(String tip) {
        mCaptureLayout.setTip(tip);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        DebugUtil.i(TAG, "surfaceCreated");
        new Thread() {
            @Override
            public void run() {
                CameraInterface.getInstance().doOpenCamera(JCameraView.this);
            }
        }.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        onlyPause = false;
        DebugUtil.i(TAG, "surfaceDestroyed");
        CameraInterface.getInstance().doDestroyCamera();
    }
}
