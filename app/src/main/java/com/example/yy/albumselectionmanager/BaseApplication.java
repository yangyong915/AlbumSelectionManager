package com.example.yy.albumselectionmanager;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

/**
 * Created by yy on 2018/3/1.
 * 描述：
 */

public class BaseApplication extends MultiDexApplication {

    static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    /**
     * 获得当前上下文对象
     */
    public static Context getAppContext() {
        return context;
    }
}
