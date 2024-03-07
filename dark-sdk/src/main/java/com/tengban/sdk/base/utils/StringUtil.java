package com.tengban.sdk.base.utils;

import android.text.TextUtils;

import java.math.RoundingMode;
import java.security.MessageDigest;
import java.text.NumberFormat;

/**
 * Created by tamrylei on 2016/12/19.
 *
 * String相关接口
 */
public final class StringUtil {

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static String byte2HexString(byte b) {
        return byte2HexString(new byte[] { b });
    }

    public static String byte2HexString(byte[] bytes) {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }

        final StringBuilder sb = new StringBuilder(bytes.length * 2);

        for (int i = 0; i < bytes.length; i++) {
            if ((bytes[i] & 0xff) < 0x10) {
                sb.append("0");
            }
            sb.append(Long.toString(bytes[i] & 0xff, 16));
        }
        return sb.toString();
    }

    public static byte[] hexString2Byte(String hexStr) {
        if (TextUtils.isEmpty(hexStr)) {
            return null;
        }

        hexStr = hexStr.toUpperCase();

        int length = hexStr.length() / 2;
        char[] hexChars = hexStr.toCharArray();

        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            result[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return result;
    }

    public static String MD5Encode(String input) {
        if (TextUtils.isEmpty(input)) {
            return null;
        }

        String result = null;
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");

            result = byte2HexString(md.digest(input.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String SHA256Encode(String input) {
        if (TextUtils.isEmpty(input)) {
            return null;
        }

        String result = null;
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");

            result = byte2HexString(md.digest(input.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getNonNullString(String str) {
        return str == null ? "" : str;
    }

    public static boolean startsWithIgnoreCase(String str, String prefix) {
        if (str == null || prefix == null) {
            return (str == null && prefix == null);
        }

        if (prefix.length() > str.length()) {
            return false;
        }

        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static String toString(double val) {
        return toString(val, 2);
    }

    public static String toString(double val, int digit) {
        return toString(val, digit, false);
    }

    public static String toString(double val, int digit, boolean withSymbol) {
        final NumberFormat format = NumberFormat.getNumberInstance();
        format.setRoundingMode(RoundingMode.HALF_UP);
        format.setMinimumFractionDigits(digit);
        format.setMaximumFractionDigits(digit);
        format.setGroupingUsed(false);

        String str = format.format(val);
        if (withSymbol && val > 0) {
            str = "+".concat(str);
        }
        return str;
    }
}
