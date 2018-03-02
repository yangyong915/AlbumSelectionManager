package com.mabeijianxi.smallvideorecord2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;

import com.mabeijianxi.smallvideorecord2.model.LocalMediaConfig;
import com.mabeijianxi.smallvideorecord2.model.MediaObject;
import com.mabeijianxi.smallvideorecord2.model.OnlyCompressOverBean;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;


/**
 * Created by jianxi on 2017/4/1.
 * https://github.com/mabeijianxi
 * mabeijianxi@gmail.com
 */

public class LocalMediaCompress extends MediaRecorderBase {

    private static final String TAG = LocalMediaCompress.class.getSimpleName();

    private final String mNeedCompressVideo;
    private final OnlyCompressOverBean mOnlyCompressOverBean;
    private final LocalMediaConfig localMediaConfig;
    protected String scaleWH = "";

    //add by tan
    private int videoWidth = 0;
    private int videoHeight = 0;
    private static final String ROTATION_0 = "0";
    private static final String ROTATION_90 = "90";
    private static final String ROTATION_180 = "180";
    private static final String ROTATION_270 = "270";
    private static final String ROTATION_360 = "360";
    //end

    @Override
    public MediaObject.MediaPart startRecord() {
        return null;
    }

    public LocalMediaCompress(LocalMediaConfig localMediaConfig) {
        this.localMediaConfig = localMediaConfig;
        compressConfig = localMediaConfig.getCompressConfig();
        CAPTURE_THUMBNAILS_TIME = localMediaConfig.getCaptureThumbnailsTime();
        if (localMediaConfig.getFrameRate() > 0) {
            setTranscodingFrameRate(localMediaConfig.getFrameRate());
        }
        mNeedCompressVideo = localMediaConfig.getVideoPath();
        mOnlyCompressOverBean = new OnlyCompressOverBean();
        mOnlyCompressOverBean.setVideoPath(mNeedCompressVideo);

    }

    private String getScaleWH(String videoPath, float scale) {
        try {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(videoPath);
            String rotation = "0";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                rotation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            }
            String videoW = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String videoH = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);

            //System.out.println("TANHQ===>rotation: " + rotation + ", videoW: " + videoW + ", videoH: " + videoH);

            //videoW/videoH is null on Samsung ROM, so should add double check.
//            final float innerScale = scale;
            int srcW = 0;
            int srcH = 0;

            if (videoW == null || TextUtils.isEmpty(videoW)
                    || videoH == null || TextUtils.isEmpty(videoH)
                    || rotation == null || TextUtils.isEmpty(rotation)) {
                //videoWidth and videoHeight is from PictureEditAudioActivity.
                srcW = videoWidth;
                srcH = videoHeight;
                rotation = (srcW < srcH) ? "0" : "90";

                //System.out.println("TANHQ===>srcW-> Error: " + srcW + ", srcH: " + srcH + ", rotation: " + rotation);
            } else {
                srcW = Integer.valueOf(videoW);
                srcH = Integer.valueOf(videoH);

                //System.out.println("TANHQ===>srcW-> OKOK: " + srcW + ", srcH: " + srcH+ ", rotation: " + rotation);
            }

            int newsrcW = (int) (srcW / scale);
            int newsrcH = (int) (srcH / scale);
            if (newsrcH % 2 != 0) {
                newsrcH += 1;
            }
            if (newsrcW % 2 != 0) {
                newsrcW += 1;
            }
            if (ROTATION_90.equals(rotation) || ROTATION_270.equals(rotation)) {
                return String.format("%dx%d", newsrcH,newsrcW);

            } else if (ROTATION_0.equals(rotation) || ROTATION_180.equals(rotation) || ROTATION_360.equals(rotation)) {
                return String.format("%dx%d", newsrcW, newsrcH);
            }else {
                return "";
            }

        } catch (Exception exp) {
            exp.printStackTrace();
            return "";
        }

    }



    private void correcAttribute(String videoPath, String picPath) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(videoPath);
        String s = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        String videoW = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String videoH = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);

        if (s.equals("90") || s.equals("270")) {
            SMALL_VIDEO_WIDTH = Integer.valueOf(videoW);
            SMALL_VIDEO_HEIGHT = Integer.valueOf(videoH);
            String newPicPath = checkPicRotaing(Integer.valueOf(s), picPath);
            if (!TextUtils.isEmpty(newPicPath)) {
                mOnlyCompressOverBean.setPicPath(newPicPath);
            }

        } else if (s.equals("0") || s.equals("180") || s.equals("360")) {
            SMALL_VIDEO_HEIGHT = Integer.valueOf(videoW);
            SMALL_VIDEO_WIDTH = Integer.valueOf(videoH);
        }

    }

    @Override
    public String getScaleWH() {
        return scaleWH;
    }

    private String checkPicRotaing(int angle, String picPath) {
        Bitmap bitmap = rotaingImageView(angle, BitmapFactory.decodeFile(picPath));
        return savePhoto(bitmap);
    }

    private Bitmap rotaingImageView(int angle, Bitmap bitmap) {

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizedBitmap;
    }

    private String savePhoto(Bitmap bitmap) {

        FileOutputStream fileOutputStream = null;

        String fileName = UUID.randomUUID().toString() + ".jpg";
        File f = new File(mMediaObject.getOutputDirectory(), fileName);
        try {
            fileOutputStream = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);

        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return null;
        } finally {
            try {
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return f.toString();
    }

    public OnlyCompressOverBean startCompress() {

        if (TextUtils.isEmpty(mNeedCompressVideo)) {
            return mOnlyCompressOverBean;
        }

        File f = new File(JianXiCamera.getVideoCachePath());
        if (!FileUtils.checkFile(f)) {
            f.mkdirs();
        }
        String key = String.valueOf(System.currentTimeMillis());
        mMediaObject = setOutputDirectory(key,
                JianXiCamera.getVideoCachePath() + key);

        mMediaObject.setOutputTempVideoPath(mNeedCompressVideo);

        float scale = localMediaConfig.getScale();
        if (scale > 1) {
            scaleWH = getScaleWH(mNeedCompressVideo, scale);
        }

        boolean b = doCompress(true);
        mOnlyCompressOverBean.setSucceed(b);

        if (b) {
            mOnlyCompressOverBean.setVideoPath(mMediaObject.getOutputTempTranscodingVideoPath());
            mOnlyCompressOverBean.setPicPath(mMediaObject.getOutputVideoThumbPath());
            correcAttribute(mMediaObject.getOutputTempTranscodingVideoPath(), mMediaObject.getOutputVideoThumbPath());
        }

        return mOnlyCompressOverBean;
    }


    public OnlyCompressOverBean startCropVideo() {

        if (TextUtils.isEmpty(mNeedCompressVideo)) {
            return mOnlyCompressOverBean;
        }

        File f = new File(JianXiCamera.getVideoCachePath());
        if (!FileUtils.checkFile(f)) {
            f.mkdirs();
        }
        String key = String.valueOf(System.currentTimeMillis());
        mMediaObject = setOutputDirectory(key, JianXiCamera.getVideoCachePath() + key);

        mMediaObject.setOutputTempVideoPath(mNeedCompressVideo);

        float scale = localMediaConfig.getScale();
        if (scale > 1) {
            scaleWH = getScaleWH(mNeedCompressVideo, scale);
        }

        boolean b = doCompress(true);
        mOnlyCompressOverBean.setSucceed(b);

        if (b) {
            mOnlyCompressOverBean.setVideoPath(mMediaObject.getOutputTempTranscodingVideoPath());
            mOnlyCompressOverBean.setPicPath(mMediaObject.getOutputVideoThumbPath());
            correcAttribute(mMediaObject.getOutputTempTranscodingVideoPath(), mMediaObject.getOutputVideoThumbPath());
        }

        return mOnlyCompressOverBean;
    }


    public void setVideoOriginalWitdhHeight(int width, int height) {
        videoHeight = height;
        videoWidth = width;
    }

}
