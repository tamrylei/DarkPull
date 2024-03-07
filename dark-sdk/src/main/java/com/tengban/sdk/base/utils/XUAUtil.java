package com.tengban.sdk.base.utils;

import android.content.Context;
import android.text.TextUtils;

public final class XUAUtil {

    private static String sChannel = null;

    public static void setChannel(String channel) {
        if(!TextUtils.isEmpty(channel)) {
            sChannel = channel;
            sXUA = null;
        }
    }

    public static String getChannel() {
        return sChannel;
    }

    private static volatile String sXUA;

    public static String getXUA(Context context) {
        if (sXUA == null) {
            synchronized (XUAUtil.class) {
                if (sXUA == null) {
                    sXUA = createXUA(context);
                }
            }
        }
        return sXUA;
    }

    private static String createXUA(Context context) {
        final StringBuilder sb = new StringBuilder(128);
        final StringBuilder tempSb = new StringBuilder(64);

        sb.append("SN=")
                .append("ADR_")
                .append(context.getPackageName());

        sb.append("&");
        sb.append("VN=")
                .append(AndroidUtil.getVersionName(context))
                .append("_")
                .append(AndroidUtil.getVersionCode(context));

        sb.append("&");
        sb.append("MO=")
                .append(replaceString(tempSb, android.os.Build.MODEL));

        sb.append("&");
        sb.append("MAN=")
                .append(replaceString(tempSb, android.os.Build.MANUFACTURER));

        sb.append("&");
        sb.append("RV=")
                .append(replaceString(tempSb, android.os.Build.BRAND))
                .append("_")
                .append(replaceString(tempSb, android.os.Build.VERSION.INCREMENTAL));

        sb.append("&");
        sb.append("OS=")
                .append(android.os.Build.VERSION.SDK_INT);

        String imei = AndroidUtil.getDeviceId(context);
        if(!TextUtils.isEmpty(imei)) {
            sb.append("&");
            sb.append("IMEI=")
                    .append(imei);
        }

        if(!TextUtils.isEmpty(sChannel)) {
            sb.append("&");
            sb.append("CHAN=")
                    .append(sChannel);
        }

        return sb.toString();
    }

    private static String replaceString(StringBuilder tempSb, String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }

        str = str.trim();

        tempSb.setLength(0);

        char c;
        for (int i = 0; i < str.length(); ++i) {
            c = str.charAt(i);

            switch (c) {
                case '/':
                    tempSb.append("#10");
                    break;
                case '&':
                    tempSb.append("#20");
                    break;
                case '|':
                    tempSb.append("#30");
                    break;
                case '=':
                    tempSb.append("#40");
                    break;
                case '_':
                    tempSb.append("-");
                    break;
                default:
                    tempSb.append(c);
            }
        }

        return tempSb.toString();
    }
}
