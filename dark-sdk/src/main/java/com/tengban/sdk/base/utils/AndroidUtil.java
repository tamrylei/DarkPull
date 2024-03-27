package com.tengban.sdk.base.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("MissingPermission")
public final class AndroidUtil {

    // 0: 未设置 1: NOT Debuggable 2: Debuggable
    private static volatile int sAppDebugFlag = 0;

    public static boolean isAppDebuggable(Context context) {
        if(sAppDebugFlag == 0 && context != null) {
            final ApplicationInfo ai = context.getApplicationInfo();

            if((ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                sAppDebugFlag = 2;
            } else {
                sAppDebugFlag = 1;
            }
        }

        return sAppDebugFlag != 1;
    }

    public static Application getAppContext(Context context) {
        if(context != null) {
            if(context instanceof Application) {
                return (Application)context;
            } else {
                return (Application)context.getApplicationContext();
            }
        }

        return null;
    }

    public static String getAppName(Context context) {
        final ApplicationInfo ai = context.getApplicationInfo();

        return context.getResources().getString(ai.labelRes);
    }

    public static int getAppIcon(Context context) {
        final ApplicationInfo ai = context.getApplicationInfo();

        return ai.icon;
    }

    public static int getVersionCode(Context context) {
        try {
            final PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            return pi.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String getVersionName(Context context) {
        try {
            final PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            return pi.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getDeviceId(Context context) {
        try {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(
                    Context.TELEPHONY_SERVICE);
            return tm.getDeviceId();
        } catch(Throwable t) {
            // Eat
        }
        return null;
    }

    public static String getMainClassName(Context context) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (intent != null) {
                return intent.getComponent().getClassName();
            }
        } catch (Throwable t) {
            // Eat
        }
        return null;
    }

    public static File getExternalStoragePath() {
        try {
            return Environment.getExternalStorageDirectory();
        } catch(Exception e) {
            return new File("/sdcard");
        }
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            // Eat
        }
        return false;
    }

    public static void sendBroadcast(Context context, String action) {
        final Intent intent = new Intent(action);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    public static void showKeyBoard(View view) {
        try {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        } catch (Exception e) {
            //Eat
        }
    }

    public static void hideKeyBoard(View view) {
        try {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (Exception e) {
            //Eat
        }
    }

    public static void copyToClipboard(Context context, String text) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(
                    Context.CLIPBOARD_SERVICE);

            ClipData clipData = ClipData.newPlainText(null, text);
            cm.setPrimaryClip(clipData);
        } catch (Exception e) {
            //Eat
        }
    }

    public static String getClipboardText(Context context) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(
                    Context.CLIPBOARD_SERVICE);

            ClipData clipData = cm.getPrimaryClip();
            if(clipData != null && clipData.getItemCount() > 0) {
                return clipData.getItemAt(0).getText().toString();
            }
        } catch (Exception e) {
            //Eat
        }
        return null;
    }

    public static void checkAndRequestPermission(Activity activity, String... permissions) {
        final List<String> ungrantedList = new ArrayList<>(permissions.length);

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                ungrantedList.add(permission);
            }
        }

        if (ungrantedList.size() > 0) {
            try {
                ActivityCompat.requestPermissions(activity, ungrantedList.toArray(new String[0]), 0);
            } catch (ActivityNotFoundException e) {
                // Fucked
            }
        }
    }

    public static void openByExternal(Context context, Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(uri);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void openByInternal(Context context, Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(uri);
            intent.setPackage(context.getPackageName());
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void shareText(Context context, String text) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
