package com.tengban.sdk.base.http;

import android.text.TextUtils;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* package */ final class HttpHeader {

    private final List<String> mEncodedNames;
    private final List<String> mEncodedValues;

    public HttpHeader() {
        mEncodedNames = new ArrayList<>(4);
        mEncodedValues = new ArrayList<>(4);
    }

    public HttpHeader add(String name, String value) {
        if(!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
            addEncoded(HttpEncoderAndDecoder.encode(name), HttpEncoderAndDecoder.encode(value));
        }

        return this;
    }

    public HttpHeader add(Map<String, String> headers) {
        if(headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                add(header.getKey(), header.getValue());
            }
        }

        return this;
    }

    public HttpHeader addEncoded(String name, String value) {
        if(!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
            mEncodedNames.add(name);
            mEncodedValues.add(value);
        }

        return this;
    }

    public HttpHeader set(String name, String value) {
        if(!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
            setEncoded(HttpEncoderAndDecoder.encode(name), HttpEncoderAndDecoder.encode(value));
        }

        return this;
    }

    public HttpHeader set(Map<String, String> headers) {
        if(headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                set(header.getKey(), header.getValue());
            }
        }

        return this;
    }

    public HttpHeader setEncoded(String name, String value) {
        if(!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
            boolean existed = false;

            if(!mEncodedNames.isEmpty()) {
                for (int i = 0, size = mEncodedNames.size(); i < size; i++) {
                    if(name.equalsIgnoreCase(mEncodedNames.get(i))) {
                        existed = true;

                        // 已经存在的就替换
                        mEncodedValues.set(i, value);
                    }
                }
            }

            if(!existed) {
                mEncodedNames.add(name);
                mEncodedValues.add(value);
            }
        }

        return this;
    }

    public String get(String name) {
        if(!TextUtils.isEmpty(name) && !mEncodedNames.isEmpty()) {
            for (int i = 0, size = mEncodedNames.size(); i < size; i++) {
                if(name.equalsIgnoreCase(mEncodedNames.get(i))) {
                    return mEncodedValues.get(i);
                }
            }
        }

        return null;
    }

    public List<String> getAll(String name) {
        ArrayList<String> values = null;

        if(!TextUtils.isEmpty(name) && !mEncodedNames.isEmpty()) {
            values = new ArrayList<>();

            for (int i = 0, size = mEncodedNames.size(); i < size; i++) {
                if(name.equalsIgnoreCase(mEncodedNames.get(i))) {
                    values.add(mEncodedValues.get(i));
                }
            }
        }

        return values;
    }

    public Map<String, String> all() {
        Map<String, String> all = null;

        if(!mEncodedNames.isEmpty()) {
            all = new HashMap<>();

            for (int i = 0, size = mEncodedNames.size(); i < size; i++) {
                all.put(mEncodedNames.get(i), mEncodedValues.get(i));
            }
        }

        return all;
    }

    public void writeTo(HttpURLConnection conn) {
        if(!mEncodedNames.isEmpty()) {
            for (int i = 0, size = mEncodedNames.size(); i < size; i++) {
                conn.addRequestProperty(mEncodedNames.get(i), mEncodedValues.get(i));
            }
        }
    }
}
