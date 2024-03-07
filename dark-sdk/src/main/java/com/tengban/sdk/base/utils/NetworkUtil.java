package com.tengban.sdk.base.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

/**
 * Created by tamrylei on 2016/12/16.
 *
 * 网络相关接口
 */
public final class NetworkUtil {

    public static final int NETWORK_TYPE_NONE = 0;
    public static final int NETWORK_TYPE_WIFI = 1;
    public static final int NETWORK_TYPE_2G = 2;
    public static final int NETWORK_TYPE_3G = 3;
    public static final int NETWORK_TYPE_4G = 4;

    public static boolean isNetworkAvailable(Context context) {
        final NetworkInfo info = getNetworkInfo(context);

        return info == null || info.isConnectedOrConnecting();
    }

    public static NetworkInfo getNetworkInfo(Context context) {
        NetworkInfo info = null;

        try {
            final ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            info = cm.getActiveNetworkInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return info;
    }

    public static int getNetworkType(Context context) {
        final NetworkInfo info = getNetworkInfo(context);

        if (info != null && info.isConnectedOrConnecting()) {
            switch (info.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    return NETWORK_TYPE_WIFI;
                case ConnectivityManager.TYPE_MOBILE: {
                    switch (info.getSubtype()) {
                        case TelephonyManager.NETWORK_TYPE_GPRS:
                        case TelephonyManager.NETWORK_TYPE_EDGE:
                        case TelephonyManager.NETWORK_TYPE_CDMA:
                            return NETWORK_TYPE_2G;
                        case TelephonyManager.NETWORK_TYPE_LTE:
                            return NETWORK_TYPE_4G;
                        default:
                            return NETWORK_TYPE_3G;
                    }
                }
            }
        }

        return NETWORK_TYPE_NONE;
    }
}
