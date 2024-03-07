package com.tengban.sdk.base.http;

import com.tengban.sdk.base.utils.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class HttpRequestDataBody implements HttpRequestBody {

    private final byte[] mData;
    private final File mFile;

    public HttpRequestDataBody(byte[] data) {
        mData = data;
        mFile = null;
    }

    public HttpRequestDataBody(File file) {
        mData = null;
        mFile = file;
    }

    @Override
    public String contentType() {
        return "application/octet-stream";
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        if(mData != null) {
            out.write(mData);
        } else if(mFile != null) {
            FileInputStream in = null;

            try {
                in = new FileInputStream(mFile);

                IOUtil.copy(in, out);
            } finally {
                IOUtil.closeQuietly(in);
            }
        }
    }
}
