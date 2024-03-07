package com.tamry.test;

import android.app.Application;

import com.tengban.sdk.dark.DarkManager;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        DarkManager.start(this);
    }
}
