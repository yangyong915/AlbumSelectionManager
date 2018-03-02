package com.luck.picture.lib.tools;

import android.os.Environment;

/**
 * author：luck
 * project：PictureSelector
 * package：com.luck.picture.lib.widget
 * email：893855882@qq.com
 * data：2017/3/21
 */

public class Constant {

    public final static boolean IS_LOG_ENABLED = true;

    public final static String ACTION_AC_FINISH = "app.activity.finish";

    public final static String ACTION_AC_REFRESH_DATA = "app.action.refresh.data";

    public final static String ACTION_CROP_DATA = "app.action.crop_data";

    public final static String ACTION_AC_SINGE_UCROP = "app.activity.singe.ucrop.finish";

    // SD卡写入权限 Flag
    public static final int WRITE_EXTERNAL_STORAGE = 0x01;

    public static final int AutoVBRMode_CAMERA = 25;
    public static final int AutoVBRMode = 28;
    //帧率
    public static final int FRAMERATE_20 = 20;
    public static final int FRAMERATE_15 = 15;
    //缩放大小
    public static final float SCALE_10 = 1.0f;
    public static final float SCALE_15 = 1.5f;
    public static final float SCALE_20 = 2.0f;

    public static final String CACHE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/mio";

    //视频缓存目录
    public static final String VIDEO_CACHE = CACHE_DIR + "/cache/video/";

    //图片缓存目录
    public static final String IMAGE_CACHE = CACHE_DIR + "/cache/image/";

    //文件下载目录
    public static final String DOWNLOAD_FILE = CACHE_DIR + "/download/";

    //视频裁剪文件
    public static final String VIDEO_CROP_TEMP_FILE = VIDEO_CACHE + "crop_";

    //启动app/post/CameraActivity.java intent type
    public static final int POST_NORMAL = 10;
    public static final int POST_IMAGE = 11;
    public static final int POST_VIDEO = 12;
    public static final String ACTION_START_CAMERA = "com.yanyu.mio.intent.action.START_CAMERA";
    public static final String TYPE_CAMERA = "type";

    //进入发帖界面 不同的intent type
    public static final String TYPE_INTENT = "type";
    public static final int TYPE_INTENT_DEFAULT_VALUE = 0x0;
    public static final int TYPE_POST_PICTURE = 1024;
    public static final int TYPE_POST_VIDEO = 1025;

    //CameraActivity直接跳转到PostTopicActivity
    public static final int TYPE_RETURN_FROM_CAMERA = 1026;

    //附带CameraMedia 到PostTopicActivity
    public static final String TYPE_INTENT_PICTURE_CONFIG_TYPE = "picture_config_type";
    public static final String TYPE_INTENT_PICTURE_CONFIG_URL = "video_url";
    public static final String TYPE_INTENT_PICTURE_CONFIG_DATA = "bitmap_data";

    //筛选视频裁剪 大小 3M = 3 * 1024 * 1024 = 3145728
    public static final int VIDEO_LENGTH_BYTE = 3 * 1024 * 1024;

}
