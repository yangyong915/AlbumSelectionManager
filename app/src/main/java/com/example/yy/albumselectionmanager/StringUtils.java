package com.example.yy.albumselectionmanager;

import android.text.TextUtils;

import java.io.File;

/**
 * Created by yy on 2018/3/1.
 * 描述：
 */

public class StringUtils {
    /**
     * 判断文件是否存在
     *
     * @param strFile
     * @return
     */
    public static boolean fileIsExists(String strFile) {
        if (TextUtils.isEmpty(strFile)) return false;

        try {
            File f = new File(strFile);
            if (!f.exists()) {
                return false;
            }

        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
