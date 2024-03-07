package com.tengban.sdk.base.utils;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

public final class ThreadUtil {

    public static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    public static void execute(Runnable runnable) {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(runnable);
    }

    public static void executeOnMainThread(Runnable runnable) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            runnable.run();
        } else {
            sMainHandler.post(runnable);
        }
    }
}
