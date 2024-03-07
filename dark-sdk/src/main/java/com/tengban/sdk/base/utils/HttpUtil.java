package com.tengban.sdk.base.utils;

import com.tengban.sdk.base.http.HttpClient;
import com.tengban.sdk.base.http.HttpRequest;
import com.tengban.sdk.base.http.HttpResponse;

public final class HttpUtil {

    public interface Callback {
        void onResponse(HttpResponse response);
    }

    private static final HttpClient sHttpClient =
            HttpClient.defaultHttpClient();

    public static void enqueue(final HttpRequest request, final Callback callback) {
        ThreadUtil.execute(new Runnable() {
            @Override
            public void run() {
                final HttpResponse response = execute(request);

                ThreadUtil.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onResponse(response);
                        }
                    }
                });
            }
        });
    }

    public static HttpResponse execute(HttpRequest request) {
        final HttpResponse response = sHttpClient.sendRequest(request);

        if("application/json".equals(response.contentType)) {
            response.json(); // 子线程读取数据流，否则可能出现NetworkOnMainThreadException的异常
        }

        return response;
    }
}
