package com.tengban.sdk.base.http;

import android.text.TextUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class HttpRequestFormBody implements HttpRequestBody {

    private final List<String> mEncodedNames;
    private final List<String> mEncodedValues;

    public HttpRequestFormBody() {
        mEncodedNames = new ArrayList<>(4);
        mEncodedValues = new ArrayList<>(4);
    }

    public HttpRequestFormBody add(String name, String value) {
        if(!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
            mEncodedNames.add(HttpEncoderAndDecoder.encode(name));
            mEncodedValues.add(HttpEncoderAndDecoder.encode(value));
        }

        return this;
    }

    public HttpRequestFormBody addEncoded(String name, String value) {
        if(!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
            mEncodedNames.add(name);
            mEncodedValues.add(value);
        }

        return this;
    }

    @Override
    public String contentType() {
        return "application/x-www-form-urlencoded";
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        final byte[] formData = toData();

        if(formData != null) {
            out.write(formData);
        }
    }

    private byte[] toData() {
        if(mEncodedNames != null && !mEncodedNames.isEmpty()) {
            final StringBuilder sb = new StringBuilder(64);

            for (int i = 0, size = mEncodedNames.size(); i < size; i++) {
                if (i > 0) sb.append('&');

                sb.append(mEncodedNames.get(i));
                sb.append('=');
                sb.append(mEncodedValues.get(i));
            }

            return sb.toString().getBytes();
        }

        return null;
    }
}
