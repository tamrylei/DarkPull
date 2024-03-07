package com.tengban.sdk.base.http;

import java.io.IOException;
import java.io.OutputStream;

public interface HttpRequestBody {

    String contentType();

    void writeTo(OutputStream out) throws IOException;
}
