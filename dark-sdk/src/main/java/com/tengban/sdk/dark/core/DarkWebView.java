package com.tengban.sdk.dark.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class DarkWebView extends WebView {

    private String mJsUrl;

    public DarkWebView(Context context) {
        this(context, null);
    }

    public DarkWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DarkWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setBackgroundColor(Color.WHITE);
    }

    @SuppressLint({"SetJavaScriptEnabled", "ObsoleteSdkInt"})
    void init(String jsUrl) {
        mJsUrl = jsUrl;

        setWebViewClient(mWebViewClient);
        setWebChromeClient(mWebChromeClient);

        final WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowUniversalAccessFromFileURLs(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.setMediaPlaybackRequiresUserGesture(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Enable DOM storage
        settings.setDomStorageEnabled(true);

        // Enable built-in geolocation
        settings.setGeolocationEnabled(true);
    }

    private final WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            Log.d("Dark", "onPageStarted: " + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            Log.d("Dark", "onPageFinished: " + url);

            if(!TextUtils.isEmpty(mJsUrl)) {
                try {
                    view.loadUrl("javascript:(function(){" +
                            "var script=document.createElement('script');" +
                            "script.src='" + mJsUrl + "';" +
                            "document.head.appendChild(script);" +
                            "})();");
                } catch (Throwable t) {
                    // Eat
                }
                mJsUrl = null;
            }
        }
    };

    private final WebChromeClient mWebChromeClient = new WebChromeClient() {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            return true;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            try {
                callback.invoke(origin, true, false);
            } catch (Throwable t) {
                // Eat
            }
        }
    };
}
