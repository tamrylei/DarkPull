package com.tengban.sdk.base.http;

import android.text.TextUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;

/* package */ class HttpEncoderAndDecoder {

    private static final String ENCODING = "UTF-8";

    public static String encode(String string) {
        if(TextUtils.isEmpty(string)) {
            return "";
        }

        try {
            return URLEncoder.encode(string, ENCODING);
        } catch (Exception e) {
            return string;
        }
    }

    public static String decode(String string) {
        if(TextUtils.isEmpty(string)) {
            return "";
        }

        try {
            return URLDecoder.decode(string, ENCODING);
        } catch (Exception e) {
            return string;
        }
    }
}
