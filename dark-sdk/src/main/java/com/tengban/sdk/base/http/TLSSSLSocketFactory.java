package com.tengban.sdk.base.http;

import android.os.Build;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/* package */ class TLSSSLSocketFactory extends SSLSocketFactory {

    private static final String[] TLS_SUPPORT_VERSION = { "TLSv1.1", "TLSv1.2" };

    private final SSLSocketFactory mSocketFactory;

    public TLSSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[] { new EmptyTrustManager() }, null);
        mSocketFactory = context.getSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return mSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return mSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
        return patch(mSocketFactory.createSocket(socket, s, i, b));
    }

    @Override
    public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
        return patch(mSocketFactory.createSocket(s, i));
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
        return patch(mSocketFactory.createSocket(s, i, inetAddress, i1));
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return patch(mSocketFactory.createSocket(inetAddress, i));
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        return patch(mSocketFactory.createSocket(inetAddress, i, inetAddress1, i1));
    }

    private Socket patch(Socket s) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (s != null && (s instanceof SSLSocket)) {
                ((SSLSocket) s).setEnabledProtocols(TLS_SUPPORT_VERSION);
            }
        }

        return s;
    }
}
