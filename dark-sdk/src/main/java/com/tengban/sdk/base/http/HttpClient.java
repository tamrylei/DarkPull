package com.tengban.sdk.base.http;

import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.tengban.sdk.base.utils.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public final class HttpClient {

    public interface HttpRewriter {
        String handleUrl(String url);
        void addHeader(URLConnection connection);
    }

    private static final String TOKEN = "([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)";
    private static final String QUOTED = "\"([^\"]*)\"";
    private static final Pattern TYPE_SUBTYPE = Pattern.compile(TOKEN + "/" + TOKEN);
    private static final Pattern PARAMETER = Pattern.compile(
        ";\\s*(?:" + TOKEN + "=(?:" + TOKEN + "|" + QUOTED + "))?");

    private static HttpRewriter sRewriter;

    private final SSLSocketFactory mSSLSocketFactory;
    private final HostnameVerifier mHostnameVerifier;

    private int mMaxRedirectCount = 2;
    private boolean mEnableCache = false;
    private boolean mEnableRedirect = true;

    public static HttpClient defaultHttpClient() {
        final EmptyHostnameVerifier hostnameVerifier = new EmptyHostnameVerifier();

        try {
            return new HttpClient(new TLSSSLSocketFactory(), hostnameVerifier);
        } catch (Exception e) {
            return new HttpClient(null, hostnameVerifier);
        }
    }

    public static void setRewriter(HttpRewriter urlRewriter) {
        sRewriter = urlRewriter;
    }

    public HttpClient(SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier) {
        mSSLSocketFactory = sslSocketFactory;
        mHostnameVerifier = hostnameVerifier;
    }

    public void setEnableCache(boolean enable) {
        mEnableCache = enable;
    }

    public void setEnableRedirect(boolean enable) {
        mEnableRedirect = enable;
    }

    public HttpResponse sendRequest(HttpRequest request) {
        HttpResponse response = null;
        int redirectCount = 0;

        try {
            response = sendRequestInternal(null, request);

            // 重定向
            while(mEnableRedirect && needRedirect(response.code) && redirectCount < mMaxRedirectCount) {
                final String redirectUrl = resolveRedirectUrl(request.getUrl(), response.header.get("Location"));

                if (!TextUtils.isEmpty(redirectUrl)) {
                    response = sendRequestInternal(redirectUrl, request);

                    ++redirectCount;
                } else {
                    break;
                }
            }
        } catch (Throwable t) {
            response = new HttpResponse(
                -1, null, null,
                -1, null, new Exception(t));
        }

        return response;
    }

    private HttpResponse sendRequestInternal(String realUrl, HttpRequest request) throws Exception {
        String url = realUrl;

        if(TextUtils.isEmpty(url)) {
            url = request.getUrl();
        }

        final HttpRewriter rewriter = sRewriter;

        if(rewriter != null) {
            final String rewriteUrl = rewriter.handleUrl(url);

            if(!TextUtils.isEmpty(rewriteUrl)) {
                url = rewriteUrl;
            }
        }

        final HttpHeader header = request.header;
        final HttpRequestBody body = request.body;
        final String method = request.method;

        final URL parsedUrl = new URL(url);

        final HttpURLConnection connection = openConnection(parsedUrl, request.timeout);
        connection.setRequestMethod(method);

        header.writeTo(connection);

        if(rewriter != null) {
            rewriter.addHeader(connection);
        }

        if(body != null) {
            connection.addRequestProperty("Content-Type", body.contentType());
            connection.setDoOutput(true);

            final OutputStream out = connection.getOutputStream();

            body.writeTo(out);

            out.flush();
            out.close();
        }

        final int code = connection.getResponseCode();

        if (code == -1) {
            // -1 is returned by getResponseCode() if the response code could not be retrieved.
            // Signal to the caller that something was wrong with the connection.
            throw new IOException("Could not retrieve response code from connection");
        }

        InputStream in = null;

        if(hasResponseBody(code)) {
            try {
                in = connection.getInputStream();
            } catch (IOException e) {
                in = connection.getErrorStream();
            }

            if (in == null) {
                throw new IOException("Could not retrieve data from connection");
            }
        }

        long contentLength = connection.getContentLength();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            contentLength = connection.getContentLengthLong();
        }

        final String[] contentTypeData = parseContentType(connection.getContentType());

        String contentType = null;
        String charset = null;

        if(contentTypeData != null && contentTypeData.length > 1) {
            contentType = contentTypeData[0];
            charset = contentTypeData[1];
        }

        final HttpResponse response = new HttpResponse(
            code, contentType, charset, contentLength, in, null);

        for (Map.Entry<String, List<String>> headerFields : connection.getHeaderFields().entrySet()) {
            if (headerFields.getKey() != null) {
                for(String value : headerFields.getValue()) {
                    response.header.addEncoded(headerFields.getKey(), value);
                }
            }
        }

        return response;
    }

    private HttpURLConnection createConnection(URL url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // 禁用内部的重定向处理, sendRequest中会自己处理
        connection.setInstanceFollowRedirects(false);

        return connection;
    }

    private HttpURLConnection openConnection(URL url, int timeout) throws IOException {
        HttpURLConnection connection = createConnection(url);

        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setUseCaches(mEnableCache);
        connection.setDoInput(true);
        // workaround请求超时自动重发的问题
        // https://code.google.com/p/android/issues/detail?id=163595
        connection.setChunkedStreamingMode(0);
        // fix the bug with recycled url connections
        connection.setRequestProperty("Connection", "close");

        // for HTTPS
        if ("https".equals(url.getProtocol())) {
            if(mSSLSocketFactory != null) {
                ((HttpsURLConnection)connection).setSSLSocketFactory(mSSLSocketFactory);
            }

            if(mHostnameVerifier != null) {
                ((HttpsURLConnection)connection).setHostnameVerifier(mHostnameVerifier);
            }
        }

        return connection;
    }

    private boolean hasResponseBody(int responseCode) {
        return !(responseCode >= 100 /* CONTINUE */ && responseCode < 200 /* OK */)
            && responseCode != 204 /* NO_CONTENT */
            && responseCode != 304 /* NOT_MODIFIED */;
    }

    private boolean needRedirect(int responseCode) {
        return responseCode == 300 || // Multiple Choices
            responseCode == 301 || // Moved Permanently
            responseCode == 302 || // Found
            responseCode == 303 || // See Other
            responseCode == 307 || // Temporary Redirect
            responseCode == 308; // Permanent Redirect
    }

    private String resolveRedirectUrl(String originUrl, String redirectLink) {
        if(TextUtils.isEmpty(originUrl) || TextUtils.isEmpty(redirectLink)) {
            return null;
        }

        if(StringUtil.startsWithIgnoreCase(redirectLink, "http://") ||
                StringUtil.startsWithIgnoreCase(redirectLink, "https://")) {
            return redirectLink;
        } else if(StringUtil.startsWithIgnoreCase(redirectLink, "//")) {
            if(StringUtil.startsWithIgnoreCase(originUrl, "https:")) {
                return "https:" + redirectLink;
            } else {
                return "http:" + redirectLink;
            }
        } else {
            final Uri originUri = Uri.parse(originUrl);
            final Uri redirectLinkUri = Uri.parse(redirectLink);

            if(originUri != null && redirectLinkUri != null) {
                final Uri.Builder finalRedirectUri = originUri.buildUpon();

                finalRedirectUri.encodedPath(redirectLinkUri.getEncodedPath());
                finalRedirectUri.encodedQuery(redirectLinkUri.getEncodedQuery());
                finalRedirectUri.encodedFragment(redirectLinkUri.getEncodedFragment());

                return finalRedirectUri.toString();
            }
        }

        return null;
    }

    private String[] parseContentType(String contentType) {
        if(TextUtils.isEmpty(contentType)) {
            return null;
        }

        final Matcher typeSubtype = TYPE_SUBTYPE.matcher(contentType);
        if (!typeSubtype.lookingAt()) return null;
        final String type = typeSubtype.group(1).toLowerCase();
        final String subtype = typeSubtype.group(2).toLowerCase();

        String charset = null;
        final Matcher parameter = PARAMETER.matcher(contentType);

        for (int s = typeSubtype.end(); s < contentType.length(); s = parameter.end()) {
            parameter.region(s, contentType.length());

            if (!parameter.lookingAt()) return null; // This is not a well-formed media type.

            String name = parameter.group(1);

            if (name == null || !name.equalsIgnoreCase("charset")) continue;

            String charsetParameter;
            String token = parameter.group(2);

            if (token != null) {
                // If the token is 'single-quoted' it's invalid! But we're lenient and strip the quotes.
                charsetParameter = (token.startsWith("'") && token.endsWith("'") && token.length() > 2) ?
                    token.substring(1, token.length() - 1) : token;
            } else {
                // Value is "double-quoted". That's valid and our regex group already strips the quotes.
                charsetParameter = parameter.group(3);
            }

            if (charset != null && !charsetParameter.equalsIgnoreCase(charset)) {
                return null; // Multiple different charsets!
            }

            charset = charsetParameter;
        }

        return new String[] { type + "/" + subtype, charset };
    }
}
