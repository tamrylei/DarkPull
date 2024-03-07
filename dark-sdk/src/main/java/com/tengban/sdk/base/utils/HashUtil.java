package com.tengban.sdk.base.utils;

import com.tengban.sdk.base.hash.Murmur3Hash;

public class HashUtil {

    private static final Murmur3Hash sMurmurHash32 = Murmur3Hash.hash32();

    public static int hashInt(long value) {
        synchronized (sMurmurHash32) {
            sMurmurHash32.reset();
            sMurmurHash32.putLong(value);
            return sMurmurHash32.hash().toInt();
        }
    }

    public static int hashInt(String value, String value2) {
        synchronized (sMurmurHash32) {
            sMurmurHash32.reset();
            sMurmurHash32.putString(value);
            sMurmurHash32.putString(value2);
            return sMurmurHash32.hash().toInt();
        }
    }
}
