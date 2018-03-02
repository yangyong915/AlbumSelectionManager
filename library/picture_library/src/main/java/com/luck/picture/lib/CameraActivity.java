package com.luck.picture.lib;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;

import com.luck.picture.lib.camera.JCameraView;
import com.luck.picture.lib.camera.lisenter.JCameraLisenter;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.tools.Constant;
import com.luck.picture.lib.tools.DebugUtil;
import com.luck.picture.lib.tools.PictureFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


/**
 * 相机拍照和拍摄视频
 * yy
 */
public class CameraActivity extends PictureBaseActivity {

    private static final String TAG = CameraActivity.class.getSimpleName();

    private JCameraView cameraView;
    private RelativeLayout activityCamera;
    private List<LocalMedia> mediaList = new ArrayList<>();
    private LocalMedia media = null;
    private Disposable disposable = null;


    private void initCameraView() {

        cameraView = (JCameraView) findViewById(R.id.cameraView);
        activityCamera = (RelativeLayout) findViewById(R.id.activity_camera);

        cameraView.setSaveVideoPath(Constant.VIDEO_CACHE);
        cameraView.setJCameraLisenter(new JCameraLisenter() {
            @Override
            public void captureSuccess(Bitmap bitmap) {
                DebugUtil.i("capture bitmap:with" + bitmap.getWidth() + ", height:" + bitmap.getHeight());
                String url = "";
                handleJCameraListenerResult(url, bitmap);
            }

            @Override
            public void recordSuccess(final String url, Bitmap firstFrame) {
                DebugUtil.i("record bitmap:url = " + url + ", height:" + firstFrame.getHeight());
                handleJCameraListenerResult(url, firstFrame);
            }

            @Override
            public void quit() {
                finish();
            }
        });
    }


    private void handleJCameraListenerResult(final String url, final Bitmap bitmap) {
        disposable = Observable.just(url)
                .map(new Function<String, List<LocalMedia>>() {
                    @Override
                    public List<LocalMedia> apply(String url) throws Exception {
                        mediaList.clear();
                        if (TextUtils.isEmpty(url)) {
                            File cameraImageFile = PictureFileUtils.createCameraFileNew(CameraActivity.this,
                                    mimeType == PictureConfig.TYPE_ALL ? PictureConfig.TYPE_IMAGE : mimeType,
                                    outputCameraPath);

                            PictureFileUtils.saveBitmapFile(bitmap, cameraImageFile);
                            media.setPath(cameraImageFile.getAbsolutePath());

                        } else {
                            media.setPath(url);
                        }
                        mediaList.add(media);
                        return mediaList;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<LocalMedia>>() {
                    @Override
                    public void accept(List<LocalMedia> localMedias) throws Exception {
                        DebugUtil.i("TANHQ====> finish()");

                        Intent intent = PictureSelector.putIntentResult(localMedias);
                        setResult(RESULT_OK, intent);

                        finish();
                    }
                });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);

        initCameraView();

        getIntentData();
    }


    private void getIntentData() {
        int type = getIntent().getIntExtra(Constant.TYPE_CAMERA, PictureConfig.TYPE_ALL);

        //init LocalMedia
        media = new LocalMedia();
        media.setMimeType(type);

        switch (type) {
            case PictureConfig.TYPE_ALL:
                cameraView.setFeatures(JCameraView.BUTTON_STATE_BOTH);
                cameraView.setTip(mContext.getString(R.string.label_camera_all));
                break;
            case PictureConfig.TYPE_IMAGE:
                cameraView.setFeatures(JCameraView.BUTTON_STATE_ONLY_CAPTURE);
                cameraView.setTip(mContext.getString(R.string.label_camera_picture_only));
                media.setPictureType("image/jpeg");
                break;
            case PictureConfig.TYPE_VIDEO:
                cameraView.setFeatures(JCameraView.BUTTON_STATE_ONLY_RECORDER);
                cameraView.setTip(mContext.getString(R.string.label_camera_video_only));
                media.setPictureType("video/mp4");
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //全屏显示
        if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(option);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.onPause();
    }


    @Override
    protected void onDestroy() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }

        super.onDestroy();
        DebugUtil.e(TAG, "onDestroy");
    }

}
