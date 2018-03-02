package com.luck.picture.lib;

import android.animation.ValueAnimator;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.luck.picture.lib.adapter.VideoEditAdapter;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.entity.EventEntity;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.rxbus2.RxBus;
import com.luck.picture.lib.tools.Constant;
import com.luck.picture.lib.tools.DebugUtil;
import com.luck.picture.lib.tools.ExtractVideoInfoUtil;
import com.luck.picture.lib.tools.PictureUtils;
import com.luck.picture.lib.tools.TrimVideoUtils;
import com.luck.picture.lib.tools.UIUtil;
import com.luck.picture.lib.tools.VideoEditInfo;
import com.luck.picture.lib.widget.RangeSeekBar;
import com.mabeijianxi.smallvideorecord2.LocalMediaCompress;
import com.mabeijianxi.smallvideorecord2.LocalMediaCrop;
import com.mabeijianxi.smallvideorecord2.Log;
import com.mabeijianxi.smallvideorecord2.model.AutoVBRMode;
import com.mabeijianxi.smallvideorecord2.model.LocalMediaConfig;
import com.mabeijianxi.smallvideorecord2.model.OnlyCompressOverBean;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

import static com.luck.picture.lib.tools.Constant.VIDEO_CROP_TEMP_FILE;
import static com.luck.picture.lib.tools.Constant.VIDEO_LENGTH_BYTE;

/**
 * 视频编辑页面
 */
public class PictureEditAudioActivity extends PictureBaseActivity implements View.OnClickListener {

    private static final long MIN_CUT_DURATION = 1000L;// 最小剪辑时间1s
    private static final long MAX_CUT_DURATION = 10 * 1000L;//视频最多剪切多长时间
    private static final int MAX_COUNT_RANGE = 10;//seekBar的区域内一共有多少张图片

    private static final String TAG = PictureEditAudioActivity.class.getSimpleName();

    private ExtractVideoInfoUtil mExtractVideoInfoUtil;
    private int mMaxWidth;
    private long duration;
    private RangeSeekBar seekBar;
    private VideoView mVideoView;
    private LinearLayout seekBarLayout;
    private RecyclerView mRecyclerView;
    private ImageView positionIcon;
    private VideoEditAdapter videoEditAdapter;
    private float averageMsPx;//每毫秒所占的px
    private float averagePxMs;//每px所占用的ms毫秒
    private String OutPutFileDirPath;
    private String OutMoviePath;
    private ExtractFrameWorkThread mExtractFrameWorkThread;
    private long leftProgress, rightProgress;
    private long scrollPos = 0;
    private int mScaledTouchSlop;
    private int lastScrollX;
    private boolean isSeeking;
    private String path;  //传递进来的视频路劲
    private TextView exit, edit_ok;
    private LocalMedia media = new LocalMedia();

    private int videoWidth = 0;
    private int videoHeight = 0;

    private long startCropTime = 0L;
    private long startCompressTime = 0L;

    //add by tanhaiqin
    private int thumbnailsCount;

    private CompositeDisposable compositeDisposable;
    //end

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_edit_audio);

        compositeDisposable = new CompositeDisposable();

        getMyIntent();
        initData();
        initView();
        initEditVideo();
        initPlay();
    }

    private void getMyIntent() {
        media = (LocalMedia) getIntent().getSerializableExtra(PictureConfig.EXTRA_MEDIA);
        if (media.isCompressed()) {
            // 压缩过,或者裁剪同时压缩过,以最终压缩过图片为准
            path = media.getCompressPath();
        } else {
            path = media.getPath();
        }
        Log.i(TAG, "TANHQ===> getMyIntent() video_path:" + path);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.edit_exit) {
            finish();
        }

        if (view.getId() == R.id.edit_finish) {
            startCropTime = System.currentTimeMillis();
            //add by tanhaiqin 重构 裁剪-压缩代码
            edit_ok.setEnabled(false);//完成 按钮不可用
            showPleaseDialog("处理中...");
            //end
            crop_video();
//            cropVideo();
//            cropVideoWithRx();
        }
    }

    private void initData() {
        if (TextUtils.isEmpty(path) || !new File(path).exists()) {
            Toast.makeText(this, "视频文件不存在", Toast.LENGTH_LONG).show();
            finish();
        }
        mExtractVideoInfoUtil = new ExtractVideoInfoUtil(path);
        duration = Long.valueOf(mExtractVideoInfoUtil.getVideoLength());

        mMaxWidth = UIUtil.getScreenWidth(this) - UIUtil.dip2px(this, 70);
        mScaledTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
    }

    private void initView() {
        exit = (TextView) findViewById(R.id.edit_exit);
        exit.setOnClickListener(this);
        edit_ok = (TextView) findViewById(R.id.edit_finish);
        edit_ok.setOnClickListener(this);
        seekBarLayout = (LinearLayout) findViewById(R.id.id_seekBarLayout);
        mVideoView = (VideoView) findViewById(R.id.uVideoView);
        positionIcon = (ImageView) findViewById(R.id.positionIcon);
        mRecyclerView = (RecyclerView) findViewById(R.id.id_rv_id);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        videoEditAdapter = new VideoEditAdapter(this,
                (UIUtil.getScreenWidth(this) - UIUtil.dip2px(this, 70)) / 10);
        mRecyclerView.setAdapter(videoEditAdapter);
        mRecyclerView.addOnScrollListener(mOnScrollListener);

    }

    private void initEditVideo() {
        //for video edit
        long startPosition = 0;
        long endPosition = duration;
        int rangeWidth;
        boolean isOver_10_s;
        if (endPosition <= MAX_CUT_DURATION) {
            isOver_10_s = false;
            thumbnailsCount = MAX_COUNT_RANGE;
            rangeWidth = mMaxWidth;
        } else {
            isOver_10_s = true;
            thumbnailsCount = (int) (endPosition * 1.0f / (MAX_CUT_DURATION * 1.0f) * MAX_COUNT_RANGE);
            rangeWidth = mMaxWidth / MAX_COUNT_RANGE * thumbnailsCount;
        }
        mRecyclerView.addItemDecoration(new EditSpacingItemDecoration(UIUtil.dip2px(this, 35), thumbnailsCount));

        //init seekBar
        if (isOver_10_s) {
            seekBar = new RangeSeekBar(this, 0L, MAX_CUT_DURATION);
            seekBar.setSelectedMinValue(0L);
            seekBar.setSelectedMaxValue(MAX_CUT_DURATION);
        } else {
            seekBar = new RangeSeekBar(this, 0L, endPosition);
            seekBar.setSelectedMinValue(0L);
            seekBar.setSelectedMaxValue(endPosition);
        }
        seekBar.setMin_cut_time(MIN_CUT_DURATION);//设置最小裁剪时间
        seekBar.setNotifyWhileDragging(true);
        seekBar.setOnRangeSeekBarChangeListener(mOnRangeSeekBarChangeListener);
        seekBarLayout.addView(seekBar);

        Log.d(TAG, "-------thumbnailsCount--->>>>" + thumbnailsCount);
        averageMsPx = duration * 1.0f / rangeWidth * 1.0f;
        Log.d(TAG, "-------rangeWidth--->>>>" + rangeWidth);
        Log.d(TAG, "-------localMedia.getDuration()--->>>>" + duration);
        Log.d(TAG, "-------averageMsPx--->>>>" + averageMsPx);
        OutPutFileDirPath = PictureUtils.getSaveEditThumbnailDir(this);
        int extractW = (UIUtil.getScreenWidth(this) - UIUtil.dip2px(this, 70)) / MAX_COUNT_RANGE;
        int extractH = UIUtil.dip2px(this, 55);
        mExtractFrameWorkThread = new ExtractFrameWorkThread(extractW, extractH, mUIHandler, path, OutPutFileDirPath, startPosition, endPosition, thumbnailsCount);
        mExtractFrameWorkThread.start();

        //init pos icon start
        leftProgress = 0;
        if (isOver_10_s) {
            rightProgress = MAX_CUT_DURATION;
        } else {
            rightProgress = endPosition;
        }
        averagePxMs = (mMaxWidth * 1.0f / (rightProgress - leftProgress));
        Log.d(TAG, "------averagePxMs----:>>>>>" + averagePxMs);

        //add by tanhaiqin thumbnailsCount 传递到Recycler Adapter中 用于更新 "完成"按钮状态
        if (videoEditAdapter != null) {
            videoEditAdapter.setThumbnailsCount(thumbnailsCount);
            Log.d(TAG, "videoEditAdapter.setListener");
            videoEditAdapter.setListener(new VideoEditAdapter.EditAdapterListener() {
                @Override
                public void enable(boolean enable) {
                    if (edit_ok == null) return;
                    edit_ok.setEnabled(enable);//完成按钮 可用
                }
            });
        }
        //end
    }

    private void initPlay() {
        mVideoView.setVideoPath(path);
        //设置videoview的OnPrepared监听
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                //得到视频宽高信息
                videoWidth = mp.getVideoWidth();
                videoHeight = mp.getVideoHeight();
                //Log.e(TAG, " TANHQ===> videoWidth: " + videoWidth + ", videoHeight: " + videoHeight);

                //设置MediaPlayer的OnSeekComplete监听
                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        Log.d(TAG, "------ok----real---start-----");
                        Log.d(TAG, "------isSeeking-----" + isSeeking);
                        if (!isSeeking) {
                            videoStart();
                        }
                    }
                });
            }
        });
        //first
        videoStart();
    }

    private void videoStart() {
        Log.d(TAG, "----videoStart----->>>>>>>");
        mVideoView.start();
        positionIcon.clearAnimation();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        anim();
        handler.removeCallbacks(run);
        handler.post(run);
    }

    private void cropVideoWithRx() {
        // 保存的路径文件名称
        OutMoviePath = VIDEO_CROP_TEMP_FILE + "result.mp4";
        //String.valueOf(System.currentTimeMillis())

        // 获取开始时间
        final int startS = (int) leftProgress / 1000;

        // 获取结束时间
        final int endS = (int) rightProgress / 1000;

        startCropTime = System.currentTimeMillis();

        Log.e(TAG, "TANHQ===> cropVideoWithRx()... path = " + path);
        Log.e(TAG, "TANHQ===> starts = " + startS  + ",  ends = " + endS);

        Observable.just(
                new LocalMediaCrop(path, VIDEO_CROP_TEMP_FILE + "temp.mp4", startS, endS - startS).startCropVideo())

                .filter(new Predicate<OnlyCompressOverBean>() {
                    @Override
                    public boolean test(OnlyCompressOverBean onlyCompressOverBean) throws Exception {

                        Log.e(TAG, "TANHQ===> ffmpeg裁剪花时间 1111： " + (System.currentTimeMillis() - startCropTime)
                                + ",  getVideoPath() = " + onlyCompressOverBean.getVideoPath());

                        String videoPath = onlyCompressOverBean.getVideoPath();
                        Log.e(TAG, "TANHQ===> video path: " + videoPath);
                        return videoPath != null && !TextUtils.isEmpty(videoPath);
                    }
                })
                .map(new Function<OnlyCompressOverBean, OnlyCompressOverBean>() {
                    @Override
                    public OnlyCompressOverBean apply(OnlyCompressOverBean onlyCompressOverBean) throws Exception {

                        Log.e(TAG, "TANHQ===> 即将开始缩小视频");

                        long start1 = System.currentTimeMillis();

//                        final OnlyCompressOverBean onlyCompressOverBeanTemp =
//                                new LocalMediaCrop(onlyCompressOverBean.getVideoPath(), OutMoviePath, startS, endS - startS).startScaleVideo();

                        //压缩本地视频
                        LocalMediaConfig.Buidler builder = new LocalMediaConfig.Buidler();
                        LocalMediaConfig config = builder
                                .setVideoPath(onlyCompressOverBean.getVideoPath())
                                .captureThumbnailsTime(1)
                                .doH264Compress(new AutoVBRMode(Constant.AutoVBRMode))
//                                .setFramerate(Constant.FRAMERATE_15)
                                .setScale(Constant.SCALE_20)
                                .build();

                        LocalMediaCompress localMediaCompress = new LocalMediaCompress(config);
                        localMediaCompress.setVideoOriginalWitdhHeight(videoWidth, videoHeight);
                        final OnlyCompressOverBean onlyCompressOverBeanResult = localMediaCompress.startCompress();


                        Log.e(TAG, "TANHQ===> ffmpeg缩放花时间 2222： " + (System.currentTimeMillis() - start1)
                                + ",  getVideoPath() = " + onlyCompressOverBean.getVideoPath());

                        return onlyCompressOverBeanResult;

                        //return onlyCompressOverBeanTemp;
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        Log.e(TAG, "TANHQ===> ffmpeg doOnSubscribe!!" );
                        showPleaseDialog("处理中");
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<OnlyCompressOverBean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.e(TAG, "TANHQ===> ffmpeg onSubscribe!!" );
                    }

                    @Override
                    public void onNext(OnlyCompressOverBean onlyCompressOverBean) {
                        Log.e(TAG, "TANHQ===> ffmpeg onNext!!" );

                        dismissDialog();
                        media.setPath(onlyCompressOverBean.getVideoPath());
                        //media.setCompressPath(onlyCompressOverBean.getPicPath());

                        Log.e(TAG, "TANHQ===> Run ： media.path = " + media.getPath()
                                + ",  media.CompressPath = " + media.getCompressPath());

                        List<LocalMedia> images = new ArrayList<>();
                        images.add(media);
                        RxBus.getDefault().post(new EventEntity(PictureConfig.CROP_VIDEO, images));
                        finish();
                        overridePendingTransition(0, R.anim.a3);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "TANHQ===> ffmpeg onError!!" );
                    }

                    @Override
                    public void onComplete() {
                        Log.e(TAG, "TANHQ===> ffmpeg onComplete!!" );
                    }
                });


    }

    private void cropVideo() {
        // 保存的路径文件名称
        OutMoviePath = VIDEO_CROP_TEMP_FILE + "110.mp4";
        //String.valueOf(System.currentTimeMillis())

        // 获取开始时间
        final int startS = (int) leftProgress / 1000;

        // 获取结束时间
        final int endS = (int) rightProgress / 1000;

        //Log.e(TAG, "TANHQ===> cropVideo: OutMoviePath = " + OutMoviePath + ", startS = " + startS + ",  ends = " + endS);
        //final long startTime = System.currentTimeMillis();

        showPleaseDialog("裁剪中");

        // ffmpeg 进行裁剪
        new Thread(new Runnable() {
            @Override
            public void run() {
                //裁剪本地视频信息 构建

                Log.e(TAG, "TANHQ===> 即将裁剪！");

                //add by tanhaiqin
                LocalMediaCrop localMediaCrop = new LocalMediaCrop(path, OutMoviePath, startS, endS - startS);
                final OnlyCompressOverBean onlyCompressOverBean = localMediaCrop.startCropVideo();
                //end tanhaiqin
                Log.e(TAG, "TANHQ===> ffmpeg裁剪 花时间： " + (System.currentTimeMillis() - startCropTime)
                        + ",  getVideoPath() = " + onlyCompressOverBean.getVideoPath());

//                startCompressTime = System.currentTimeMillis();
//                if (onlyCompressOverBean.getVideoPath() == null || TextUtils.isEmpty(onlyCompressOverBean.getVideoPath())) {
//                    return;
//                }

//                //压缩本地视频
//                LocalMediaConfig.Buidler builder = new LocalMediaConfig.Buidler();
//                LocalMediaConfig config = builder
//                        .setVideoPath(onlyCompressOverBean.getVideoPath())
//                        .captureThumbnailsTime(1)
//                        .doH264Compress(new AutoVBRMode(Constant.AutoVBRMode))
//                        .setFramerate(Constant.FRAMERATE)
//                        .setScale(Constant.SCALE)
//                        .build();
//
//
//                LocalMediaCompress localMediaCompress = new LocalMediaCompress(config);
//                final OnlyCompressOverBean onlyCompressOverBean11 = localMediaCompress.startCompress();
//
//                Log.e(TAG, "TANHQ===> ffmpeg 压缩花时间： " + (System.currentTimeMillis() - startCompressTime));


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissDialog();
                        media.setPath(onlyCompressOverBean.getVideoPath());
                        //media.setCompressPath(onlyCompressOverBean.getPicPath());

                        Log.e(TAG, "TANHQ===> Run ： media.path = " + media.getPath()
                                + ",  media.CompressPath = " + media.getCompressPath());

                        List<LocalMedia> images = new ArrayList<>();
                        images.add(media);
                        RxBus.getDefault().post(new EventEntity(PictureConfig.CROP_VIDEO, images));
                        finish();
                        overridePendingTransition(0, R.anim.a3);
                    }
                });
            }
        }).start();

    }

    /**
     * 传统方法，使用isomp4parser 处理裁剪，然后 ffmpeg压缩
     * 需要根据 裁剪后的文件大小，判断是否需要进一步压缩
     */
    private void crop_video() {
        TrimVideoUtils trimVideoUtils = TrimVideoUtils.getInstance();
        trimVideoUtils.setTrimCallBack(new TrimVideoUtils.TrimFileCallBack() {
            @Override
            public void trimError(int eType) {
                Message msg = new Message();
                msg.what = TrimVideoUtils.TRIM_FAIL;
                switch (eType) {
                    case TrimVideoUtils.FILE_NOT_EXISTS: // 文件不存在
                        msg.obj = "视频文件不存在";
                        break;
                    case TrimVideoUtils.TRIM_STOP: // 手动停止裁剪
                        msg.obj = "停止裁剪";
                        break;
                    case TrimVideoUtils.TRIM_FAIL:
                    default: // 裁剪失败
                        msg.obj = "裁剪失败";
                        break;
                }
                cutHandler.sendMessage(msg);
            }

            @Override
            public void trimCallback(boolean isNew, int startS, int endS, int vTotal, File file, File trimFile) {
                /**
                 * 裁剪回调
                 * @param isNew 是否新剪辑
                 * @param starts 开始时间(秒)
                 * @param ends 结束时间(秒)
                 * @param vTime 视频长度
                 * @param file 需要裁剪的文件路径
                 * @param trimFile 裁剪后保存的文件路径
                 */
                // ===========
                Log.i(TAG, "isNew : " + isNew);
                Log.i(TAG, "startS : " + startS);
                Log.i(TAG, "endS : " + endS);
                Log.i(TAG, "vTotal : " + vTotal);
                Log.i(TAG, "file : " + file.getAbsolutePath());
                Log.i(TAG, "trimFile : " + trimFile.getAbsolutePath());


                media.setPath(trimFile.getAbsolutePath());  //改变了储存目录
                media.setDuration(vTotal);
                cutHandler.sendEmptyMessage(TrimVideoUtils.TRIM_SUCCESS);
            }
        });
        // 保存的路径
        //OutMoviePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/mio/cache/video/cut.mp4";
        OutMoviePath = VIDEO_CROP_TEMP_FILE  + System.currentTimeMillis() + ".mp4";

        // ==
        final File file = new File(path); // 视频文件地址
        final File trimFile = new File(OutMoviePath);// 裁剪文件保存地址
        final int startS = (int) leftProgress / 1000; // 获取开始时间
        final int endS = (int) rightProgress / 1000; // 获取结束时间

        //showPleaseDialog("处理中...");
        // 进行裁剪
        new Thread(new Runnable() {
            @Override
            public void run() {
                try { // 开始裁剪
                    TrimVideoUtils.getInstance().startTrim(true, startS, endS, file, trimFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    // 设置回调为null
                    TrimVideoUtils.getInstance().setTrimCallBack(null);
                }
            }
        }).start();
    }

    /**
     * 裁剪处理 Handler
     */
    private Handler cutHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            switch (msg.what) {
                case TrimVideoUtils.TRIM_FAIL: // 裁剪失败
                    Toast.makeText(PictureEditAudioActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    // --
                    edit_ok.setEnabled(true);
                    dismissDialog();
                    break;

                case TrimVideoUtils.TRIM_SUCCESS: // 裁剪成功
                    handleCompressVideo();
                    break;
            }
        }
    };

    private void handleCompressVideo() {
        //Toast.makeText(PictureEditAudioActivity.this, "裁剪成功", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "TANHQ===> isoView裁剪花的时间： " + (System.currentTimeMillis() - startCropTime));
        startCompressTime = System.currentTimeMillis();
        // --
        //edit_ok.setEnabled(true);
        //dismissDialog();

        //add by tanhaiqin
        // 需要判断裁剪后的视频是大于 3M， 还是小于 3M
        String videoPath = media.getPath();
        Log.d("TANHQ===> crop video path = " + videoPath);

        Observable.just(videoPath)
                .map(new Function<String, List<LocalMedia>>() {
                    @Override
                    public List<LocalMedia> apply(String path) throws Exception {
                        return getVideoLocalMedias(path, media);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<LocalMedia>>() {
                    @Override
                    public void onSubscribe(Disposable disposable) {
                        if (compositeDisposable != null) {
                            compositeDisposable.add(disposable);
                        }
                        DebugUtil.i(TAG, "TANHQ===> onSubscribe...");
                        //showPleaseDialog("处理中...");
                    }

                    @Override
                    public void onNext(List<LocalMedia> localMediaList) {
                        dismissDialog();
                        RxBus.getDefault().post(new EventEntity(PictureConfig.CROP_VIDEO, localMediaList));
                        finish();
                        overridePendingTransition(0, R.anim.a3);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        DebugUtil.i(TAG, "TANHQ===> onError...");
                        dismissDialog();
                    }

                    @Override
                    public void onComplete() {
                    }
                });//end
    }

    /**
     * 获得传入视频对应的 LocalMedia对象，用于传递到PostTopicActivity.java
     * @param path
     * @param media
     * @return
     */
    @NonNull
    private List<LocalMedia> getVideoLocalMedias(String path, LocalMedia media) {
        File file = new File(path);

        List<LocalMedia> localMediaList = new ArrayList<>();
        localMediaList.add(media);

        if (file.exists() && file.isFile()) {
            DebugUtil.debug("TANHQ===> file length = " + file.length() + ", is > 3M : " + (file.length() > VIDEO_LENGTH_BYTE));

            if (file.length() > VIDEO_LENGTH_BYTE) {//3M, 需要压缩
                //压缩本地视频
                LocalMediaConfig.Buidler buidler = new LocalMediaConfig.Buidler();
                LocalMediaConfig config = buidler
                        .setVideoPath(path)
                        .captureThumbnailsTime(1)
                        .doH264Compress(new AutoVBRMode(Constant.AutoVBRMode))
                        .setFramerate(Constant.FRAMERATE_15)
                        .setScale(Constant.SCALE_15)
                        .build();

                LocalMediaCompress localMediaCompress = new LocalMediaCompress(config);
                localMediaCompress.setVideoOriginalWitdhHeight(videoWidth, videoHeight);
                final OnlyCompressOverBean onlyCompressOverBean = localMediaCompress.startCompress();
                localMediaList.get(0).setPath(onlyCompressOverBean.getVideoPath());
                localMediaList.get(0).setCompressPath(onlyCompressOverBean.getPicPath());

            } else {// <= 3M 不压缩
                localMediaList.get(0).setPath(path);
            }
        }

        return localMediaList;
    }

    private boolean isOverScaledTouchSlop;

    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            Log.d(TAG, "-------newState:>>>>>" + newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                isSeeking = false;
//                videoStart();
            } else {
                isSeeking = true;
                if (isOverScaledTouchSlop && mVideoView != null && mVideoView.isPlaying()) {
                    videoPause();
                }
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            isSeeking = false;
            int scrollX = getScrollXDistance();
            //达不到滑动的距离
            if (Math.abs(lastScrollX - scrollX) < mScaledTouchSlop) {
                isOverScaledTouchSlop = false;
                return;
            }
            isOverScaledTouchSlop = true;
            Log.d(TAG, "-------scrollX:>>>>>" + scrollX);
            //初始状态,why ? 因为默认的时候有35dp的空白！
            if (scrollX == -UIUtil.dip2px(PictureEditAudioActivity.this, 35)) {
                scrollPos = 0;
            } else {
                // why 在这里处理一下,因为onScrollStateChanged早于onScrolled回调
                if (mVideoView != null && mVideoView.isPlaying()) {
                    videoPause();
                }
                isSeeking = true;
                scrollPos = (long) (averageMsPx * (UIUtil.dip2px(PictureEditAudioActivity.this, 35) + scrollX));
                Log.d(TAG, "-------scrollPos:>>>>>" + scrollPos);
                leftProgress = seekBar.getSelectedMinValue() + scrollPos;
                rightProgress = seekBar.getSelectedMaxValue() + scrollPos;
                Log.d(TAG, "-------leftProgress:>>>>>" + leftProgress);
                mVideoView.seekTo((int) leftProgress);
            }
            lastScrollX = scrollX;
        }
    };

    /**
     * 水平滑动了多少px
     *
     * @return int px
     */
    private int getScrollXDistance() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        int position = layoutManager.findFirstVisibleItemPosition();
        View firstVisibleChildView = layoutManager.findViewByPosition(position);
        int itemWidth = firstVisibleChildView.getWidth();
        return (position) * itemWidth - firstVisibleChildView.getLeft();
    }

    private ValueAnimator animator;

    private void anim() {
        Log.d(TAG, "--anim--onProgressUpdate---->>>>>>>" + mVideoView.getCurrentPosition());
        if (positionIcon.getVisibility() == View.GONE) {
            positionIcon.setVisibility(View.VISIBLE);
        }
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) positionIcon.getLayoutParams();
        int start = (int) (UIUtil.dip2px(this, 35) + (leftProgress/*mVideoView.getCurrentPosition()*/ - scrollPos) * averagePxMs);
        int end = (int) (UIUtil.dip2px(this, 35) + (rightProgress - scrollPos) * averagePxMs);
        animator = ValueAnimator
                .ofInt(start, end)
                .setDuration((rightProgress - scrollPos) - (leftProgress/*mVideoView.getCurrentPosition()*/ - scrollPos));
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.leftMargin = (int) animation.getAnimatedValue();
                positionIcon.setLayoutParams(params);
            }
        });
        animator.start();
    }

    private final MainHandler mUIHandler = new MainHandler(this);

    private static class MainHandler extends Handler {
        private final WeakReference<PictureEditAudioActivity> mActivity;

        MainHandler(PictureEditAudioActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            PictureEditAudioActivity activity = mActivity.get();
            if (activity != null) {
                if (msg.what == ExtractFrameWorkThread.MSG_SAVE_SUCCESS) {
                    if (activity.videoEditAdapter != null) {
                        VideoEditInfo info = (VideoEditInfo) msg.obj;
                        activity.videoEditAdapter.addItemVideoInfo(info);
                    }
                }
            }
        }
    }

    private final RangeSeekBar.OnRangeSeekBarChangeListener mOnRangeSeekBarChangeListener = new RangeSeekBar.OnRangeSeekBarChangeListener() {
        @Override
        public void onRangeSeekBarValuesChanged(RangeSeekBar bar, long minValue, long maxValue, int action, boolean isMin, RangeSeekBar.Thumb pressedThumb) {
            Log.d(TAG, "-----minValue----->>>>>>" + minValue);
            Log.d(TAG, "-----maxValue----->>>>>>" + maxValue);
            leftProgress = minValue + scrollPos;
            rightProgress = maxValue + scrollPos;
            Log.d(TAG, "-----leftProgress----->>>>>>" + leftProgress);
            Log.d(TAG, "-----rightProgress----->>>>>>" + rightProgress);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    Log.d(TAG, "-----ACTION_DOWN---->>>>>>");
                    isSeeking = false;
                    videoPause();
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.d(TAG, "-----ACTION_MOVE---->>>>>>");
                    isSeeking = true;
                    mVideoView.seekTo((int) (pressedThumb == RangeSeekBar.Thumb.MIN ?
                            leftProgress : rightProgress));
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d(TAG, "-----ACTION_UP--leftProgress--->>>>>>" + leftProgress);
                    isSeeking = false;
                    //从minValue开始播
                    mVideoView.seekTo((int) leftProgress);
//                    videoStart();
                    break;
                default:
                    break;
            }
        }
    };

    private void videoProgressUpdate() {
        long currentPosition = mVideoView.getCurrentPosition();
        Log.d(TAG, "----onProgressUpdate-cp---->>>>>>>" + currentPosition);
        if (currentPosition >= (rightProgress)) {
            mVideoView.seekTo((int) leftProgress);
            positionIcon.clearAnimation();
            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
            anim();
        }
    }

    private void videoPause() {
        isSeeking = false;
        if (mVideoView != null && mVideoView.isPlaying()) {
            mVideoView.pause();
            handler.removeCallbacks(run);
        }
        Log.d(TAG, "----videoPause----->>>>>>>");
        if (positionIcon.getVisibility() == View.VISIBLE) {
            positionIcon.setVisibility(View.GONE);
        }
        positionIcon.clearAnimation();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null) {
            mVideoView.seekTo((int) leftProgress);
//            videoStart();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null && mVideoView.isPlaying()) {
            videoPause();
        }
    }

    private Handler handler = new Handler();
    private Runnable run = new Runnable() {

        @Override
        public void run() {
            videoProgressUpdate();
            handler.postDelayed(run, 1000);
        }
    };

    @Override
    protected void onDestroy() {

        if (animator != null) {
            animator.cancel();
        }
        if (mVideoView != null) {
            mVideoView.stopPlayback();
        }
        if (mExtractVideoInfoUtil != null) {
            mExtractVideoInfoUtil.release();
        }
        mRecyclerView.removeOnScrollListener(mOnScrollListener);
        if (mExtractFrameWorkThread != null) {
            mExtractFrameWorkThread.stopExtract();
        }
        mUIHandler.removeCallbacksAndMessages(null);
        handler.removeCallbacksAndMessages(null);
        if (!TextUtils.isEmpty(OutPutFileDirPath)) {
            PictureUtils.deleteFile(new File(OutPutFileDirPath));
        }

        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }

        super.onDestroy();
    }
}
