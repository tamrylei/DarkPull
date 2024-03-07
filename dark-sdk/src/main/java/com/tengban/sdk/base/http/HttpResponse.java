package com.tengban.sdk.base.http;

import android.text.TextUtils;

import com.tengban.sdk.base.utils.IOUtil;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public final class HttpResponse {

    private JSONObject jsonData;

    public final int code;

    public final String contentType;
    public final String charset;
    public final long contentLength;

    final InputStream stream;
    final Exception error;

    final HttpHeader header;

    /* package */ HttpResponse(int code, String contentType, String charset, long contentLength, InputStream stream, Exception error) {
        this.code = code;
        this.contentType = contentType;
        this.charset = charset;
        this.contentLength = contentLength;

        this.stream = stream;

        if(error != null) {
            this.error = error;
        } else if(code < 200 || code >= 300) {
            this.error = new Exception("Http failed: " + code);
        } else {
            this.error = null;
        }

        this.header = new HttpHeader();
    }

    public boolean isSuccessful() {
        return error == null;
    }

    public String getHeader(String name) {
        return header.get(name);
    }

    public List<String> getHeaders(String name) {
        return header.getAll(name);
    }

    public Map<String, String> allHeaders() {
        return header.all();
    }

    public Exception error() {
        return error;
    }

    public InputStream stream() {
        return stream;
    }

    public byte[] data() {
        try {
            if(stream != null) {
                int limit = Integer.MAX_VALUE;

                if(contentLength > 0) {
                    limit = (int)contentLength;
                }

                return IOUtil.toBytes(stream, limit);
            }
        } catch (Exception e) {
            return null;
        } finally {
            IOUtil.closeQuietly(stream);
        }

        return null;
    }

    public String string() {
        final byte[] data = data();

        return ((data != null && data.length > 0) ? new String(data) : null);
    }

    public JSONObject json() {
        if(jsonData == null) {
            final String string = string();

            if (string != null) {
                try {
                    jsonData = new JSONObject(string);
                } catch (Exception e) {
                    // Eat
                }
            }
        }

        return jsonData;
    }
}
