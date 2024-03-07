package com.tengban.sdk.base.http;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;

public class HttpRequestJSONBody implements HttpRequestBody {

    private final JSONObject mJSONObject;

    public HttpRequestJSONBody(JSONObject jsonObject) {
        mJSONObject = jsonObject;
    }

    @Override
    public String contentType() {
        return "application/json";
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        final byte[] jsonData = toData();

        if(jsonData != null) {
            out.write(jsonData);
        }
    }

    private byte[] toData() {
        if(mJSONObject != null) {
            final String jsonStr = mJSONObject.toString();

            if(jsonStr != null) {
                return jsonStr.getBytes();
            }
        }

        return null;
    }
}
