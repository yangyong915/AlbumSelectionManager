package com.luck.picture.lib.camera.lisenter;

/**
 * create by CJT2325
 * 445263848@qq.com.
 */

public interface CaptureLisenter {
    void takePictures();

    void recordShort(long time);

    void recordStart();

    void recordLoding(int time);

    void recordEnd(long time);

    void recordZoom(float zoom);

    void recordError();
}
