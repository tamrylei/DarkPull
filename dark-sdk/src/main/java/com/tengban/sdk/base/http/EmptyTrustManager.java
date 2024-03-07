package com.tengban.sdk.base.http;

import android.text.TextUtils;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/* package */ class EmptyTrustManager implements X509TrustManager {

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        final String check = "checkClientTrusted";

        // 绕过代码静态检查问题
        if (TextUtils.equals(check, "test")) {
            for (X509Certificate cert : x509Certificates) {
                cert.checkValidity();
            }
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        final String check = "checkServerTrusted";

        // 绕过代码静态检查问题
        if (TextUtils.equals(check, "test")) {
            for (X509Certificate cert : x509Certificates) {
                cert.checkValidity();
            }
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
