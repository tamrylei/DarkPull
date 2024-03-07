package com.tengban.sdk.base.http;

import android.text.TextUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/* package */ class EmptyHostnameVerifier implements HostnameVerifier {

    @Override
    public boolean verify(String s, SSLSession sslSession) {
        return !TextUtils.isEmpty(s);
    }
}
