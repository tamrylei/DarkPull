package com.tengban.sdk.base.http;

import android.text.TextUtils;

import com.tengban.sdk.base.utils.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HttpRequestMultipartBody implements HttpRequestBody {

    private static final byte[] DASH_DASH = { '-', '-' };
    private static final byte[] CRLF = { '\r', '\n' };

    private static final byte[] CONTENT_DISPOSITION = "Content-Disposition: form-data; name=\"".getBytes();
    private static final byte[] CONTENT_DISPOSITION_FILENAME = "; filename=\"".getBytes();
    private static final byte QUOTE = '\"';

    private static final byte[] CONTENT_TYPE = "Content-Type: application/octet-stream".getBytes();

    private final List<String> mEncodedNames;
    private final List<Object> mEncodedValues;

    private final String mBoundary;

    public HttpRequestMultipartBody() {
        mEncodedNames = new ArrayList<>();
        mEncodedValues = new ArrayList<>();

        mBoundary = UUID.randomUUID().toString();
    }

    public HttpRequestMultipartBody add(String name, String value) {
        if(!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
            mEncodedNames.add(HttpEncoderAndDecoder.encode(name));
            mEncodedValues.add(value);
        }

        return this;
    }

    public HttpRequestMultipartBody add(String name, byte[] data) {
        if(!TextUtils.isEmpty(name) && data != null && data.length > 0) {
            mEncodedNames.add(HttpEncoderAndDecoder.encode(name));
            mEncodedValues.add(data);
        }

        return this;
    }

    public HttpRequestMultipartBody add(String name, File file) {
        if(!TextUtils.isEmpty(name) && file != null) {
            mEncodedNames.add(HttpEncoderAndDecoder.encode(name));
            mEncodedValues.add(file);
        }

        return this;
    }

    @Override
    public String contentType() {
        return "multipart/form-data; boundary=" + mBoundary;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        if(!mEncodedNames.isEmpty()) {
            String name = null;
            Object value = null;

            final byte[] boundaryData = mBoundary.getBytes();

            for (int i = 0, size = mEncodedNames.size(); i < size; i++) {
                name = mEncodedNames.get(i);
                value = mEncodedValues.get(i);

                out.write(DASH_DASH);
                out.write(boundaryData);
                out.write(CRLF);

                if(value instanceof String) {
                    out.write(CONTENT_DISPOSITION);
                    out.write(name.getBytes());
                    out.write(QUOTE);
                    out.write(CRLF);

                    out.write(CRLF);
                    out.write(((String)value).getBytes());
                } else if(value instanceof byte[]) {
                    final byte[] nameData = name.getBytes();

                    out.write(CONTENT_DISPOSITION);
                    out.write(nameData);
                    out.write(QUOTE);
                    out.write(CONTENT_DISPOSITION_FILENAME);
                    out.write(nameData);
                    out.write(QUOTE);
                    out.write(CRLF);
                    out.write(CONTENT_TYPE);
                    out.write(CRLF);

                    out.write(CRLF);
                    out.write((byte[])value);
                } else if(value instanceof File) {
                    final byte[] nameData = name.getBytes();
                    final String fileNameEncoded = HttpEncoderAndDecoder.encode(((File)value).getName());
                    final byte[] fileNameData = TextUtils.isEmpty(fileNameEncoded) ?
                        nameData : fileNameEncoded.getBytes();

                    out.write(CONTENT_DISPOSITION);
                    out.write(nameData);
                    out.write(QUOTE);
                    out.write(CONTENT_DISPOSITION_FILENAME);
                    out.write(fileNameData);
                    out.write(QUOTE);
                    out.write(CRLF);
                    out.write(CONTENT_TYPE);
                    out.write(CRLF);

                    out.write(CRLF);

                    FileInputStream in = null;

                    try {
                        in = new FileInputStream((File)value);

                        IOUtil.copy(in, out);
                    } finally {
                        IOUtil.closeQuietly(in);
                    }
                }

                out.write(CRLF);
            }

            out.write(DASH_DASH);
            out.write(boundaryData);
            out.write(DASH_DASH);
            out.write(CRLF);
        }
    }
}
