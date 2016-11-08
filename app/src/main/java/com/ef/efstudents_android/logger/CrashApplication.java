package com.ef.efstudents_android.logger;

import android.app.Application;
import android.content.ComponentName;

/**
 * Created by Ezreal on 2016/10/12 0012.
 */

public class CrashApplication extends Application {
    public static ComponentName getContext() {
        return CrashApplication.getContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext(),"/sdcard/crash/","http://cnshhq-e1dev106:9200/");
    }
}
