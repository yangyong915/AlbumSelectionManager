package com.example.yy.albumselectionmanager;

import android.os.Environment;

/**
 * Created by yy on 2018/3/1.
 * 描述：
 */

public class Constant {
    public static final String CACHE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/mio";
    //视频缓存目录
    public static final String VIDEO_CACHE = CACHE_DIR + "/cache/video/";
    //图片缓存目录
    public static final String IMAGE_CACHE = CACHE_DIR + "/cache/image/";
    //文件下载目录
    public static final String DOWNLOAD_FILE = CACHE_DIR + "/download/";
}
