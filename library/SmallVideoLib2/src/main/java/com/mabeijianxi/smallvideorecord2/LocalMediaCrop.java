package com.mabeijianxi.smallvideorecord2;

import android.text.TextUtils;

import com.mabeijianxi.smallvideorecord2.model.MediaObject;
import com.mabeijianxi.smallvideorecord2.model.OnlyCompressOverBean;


/**
 * Created by jianxi on 2017/4/1.
 * https://github.com/mabeijianxi
 * mabeijianxi@gmail.com
 */

public class LocalMediaCrop extends MediaRecorderBase {

    private static final String TAG = LocalMediaCrop.class.getSimpleName();

    private String mSrcVideoVideo;
    private String mDecVideoPath;
    private final int mStart;
    private final int mDuration;
    private final OnlyCompressOverBean mOnlyCompressOverBean;


    @Override
    public MediaObject.MediaPart startRecord() {
        return null;
    }

    public LocalMediaCrop(String srcVideoPath, String decVideoPath, int startS, int duration) {

        mSrcVideoVideo = srcVideoPath;
        mDecVideoPath = decVideoPath;
        mStart = startS;
        mDuration = duration;
        mOnlyCompressOverBean = new OnlyCompressOverBean();
        mOnlyCompressOverBean.setVideoPath(mSrcVideoVideo);

    }


    public OnlyCompressOverBean startCropVideo() {

        if (TextUtils.isEmpty(mSrcVideoVideo)) {
            return mOnlyCompressOverBean;
        }

//        File f = new File(JianXiCamera.getVideoCachePath());
//        if (!FileUtils.checkFile(f)) {
//            f.mkdirs();
//        }
//        String key = String.valueOf(System.currentTimeMillis());
//        mMediaObject = setOutputDirectory(key, JianXiCamera.getVideoCachePath() + key);
//
//        mMediaObject.setOutputTempVideoPath(mNeedCompressVideo);
//
//        float scale = localMediaConfig.getScale();
//        if (scale > 1) {
//            scaleWH = getScaleWH(mNeedCompressVideo, scale);
//        }

//        System.out.println("TANHQ===>startCropVideo: mSrcVideoVideo = " + mSrcVideoVideo
//        + ",  mDecVideoPath = " + mDecVideoPath
//        + ",  mStart = " + mStart
//        + ",  mDuration = " + mDuration);

        boolean b = doCrop(mSrcVideoVideo, mDecVideoPath, mStart, mDuration);
        mOnlyCompressOverBean.setSucceed(b);

        if (b) {
//            mOnlyCompressOverBean.setVideoPath(mMediaObject.getOutputTempTranscodingVideoPath());
//            mOnlyCompressOverBean.setPicPath(mMediaObject.getOutputVideoThumbPath());

            mOnlyCompressOverBean.setVideoPath(mDecVideoPath);
//            mOnlyCompressOverBean.setPicPath(mMediaObject.getOutputVideoThumbPath());
        }

        return mOnlyCompressOverBean;
    }

    public OnlyCompressOverBean startScaleVideo() {

//        mSrcVideoVideo = srcVideo;
//        mDecVideoPath = decVideo;

        if (TextUtils.isEmpty(mSrcVideoVideo)) {
//            mOnlyCompressOverBean.setVideoPath(mSrcVideoVideo);
            return mOnlyCompressOverBean;
        }

//        System.out.println("TANHQ===>startScaleVideo: mSrcVideoVideo = " + mSrcVideoVideo
//                + ",  mDecVideoPath = " + mDecVideoPath);

        boolean b = doScale(mSrcVideoVideo, mDecVideoPath);
        mOnlyCompressOverBean.setSucceed(b);

        if (b) {
//            mOnlyCompressOverBean.setVideoPath(mMediaObject.getOutputTempTranscodingVideoPath());
//            mOnlyCompressOverBean.setPicPath(mMediaObject.getOutputVideoThumbPath());

            mOnlyCompressOverBean.setVideoPath(mDecVideoPath);
//            mOnlyCompressOverBean.setPicPath(mMediaObject.getOutputVideoThumbPath());
        }

        return mOnlyCompressOverBean;
    }


}
