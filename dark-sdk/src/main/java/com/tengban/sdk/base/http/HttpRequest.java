package com.tengban.sdk.base.http;

import android.net.Uri;
import android.text.TextUtils;

import java.util.Map;

public final class HttpRequest {

    final String method;
    final HttpRequestBody body;
    final int timeout;

    final HttpHeader header;

    private final String mBaseUrl;
    private Uri.Builder mBuilder;

    private HttpRequest(String url, String method, HttpRequestBody body, int timeout) {
        this.method = method;
        this.body = body;
        this.timeout = timeout;

        this.header = new HttpHeader();

        mBaseUrl = url;
    }

    public static HttpRequest get(String url) {
        return get(url, 30000);
    }

    public static HttpRequest get(String url, int timeout) {
        return new HttpRequest(url, "GET", null, timeout);
    }

    public static HttpRequest post(String url, HttpRequestBody body) {
        return post(url, body, 30000);
    }

    public static HttpRequest post(String url, HttpRequestBody body, int timeout) {
        return new HttpRequest(url, "POST", body, timeout);
    }

    public String getUrl() {
        if(mBuilder != null) {
            return mBuilder.build().toString();
        }

        return mBaseUrl;
    }

    public HttpRequest addHeader(String name, String value) {
        header.set(name, value);

        return this;
    }

    public HttpRequest addEncodedHeader(String name, String value) {
        header.setEncoded(name, value);

        return this;
    }

    public HttpRequest addHeaders(Map<String, String> headers) {
        header.set(headers);

        return this;
    }

    public HttpRequest addPath(String pathSegment) {
        if(mBuilder == null) {
            mBuilder = Uri.parse(mBaseUrl).buildUpon();
        }

        if(!TextUtils.isEmpty(pathSegment)) {
            final String [] pathList = pathSegment.split("/");

            if(pathList != null && pathList.length > 0) {
                for(String path : pathList) {
                    if(!TextUtils.isEmpty(path)) {
                        mBuilder.appendPath(path);
                    }
                }
            }
        }

        return this;
    }

    public HttpRequest addQueryParameter(String name, String value) {
        if(mBuilder == null) {
            mBuilder = Uri.parse(mBaseUrl).buildUpon();
        }

        mBuilder.appendQueryParameter(name, value);

        return this;
    }
}
