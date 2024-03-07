package com.tengban.sdk.base.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public final class ImageUtil {

    public static Bitmap decodeBitmapFromResource(Context context, int resId) {
        return decodeBitmapFromResource(context, resId,
                context.getResources().getDisplayMetrics().widthPixels,
                context.getResources().getDisplayMetrics().heightPixels);
    }

    public static Bitmap decodeBitmapFromResource(Context context, int resId, int width, int height) {
        if(context == null || resId <= 0) {
            return null;
        }

        return decodeBitmap(context, resId, width, height);
    }

    public static Bitmap decodeBitmapFromFile(Context context, File file) {
        return decodeBitmapFromFile(context, file,
                context.getResources().getDisplayMetrics().widthPixels,
                context.getResources().getDisplayMetrics().heightPixels);
    }

    public static Bitmap decodeBitmapFromFile(Context context, File file, int width, int height) {
        if(context == null || file == null) {
            return null;
        }

        return decodeBitmap(context, file, width, height);
    }

    public static Bitmap decodeBitmapFromUri(Context context, Uri uri) {
        return decodeBitmapFromUri(context, uri,
                context.getResources().getDisplayMetrics().widthPixels,
                context.getResources().getDisplayMetrics().heightPixels);
    }

    public static Bitmap decodeBitmapFromUri(Context context, Uri uri, int width, int height) {
        if(context == null || uri == null) {
            return null;
        }

        return decodeBitmap(context, uri, width, height);
    }

    public static byte[] loadImageFromFile(Context context, File file) {
        return loadImageFromFile(context, file,
                context.getResources().getDisplayMetrics().widthPixels,
                context.getResources().getDisplayMetrics().heightPixels);
    }

    public static byte[] loadImageFromFile(Context context, File file, int width, int height) {
        if(context == null || file == null) {
            return null;
        }

        byte[] data = null;

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        decodeBitmap(context, file, options);

        if(options.outWidth <= 0 || options.outHeight <= 0) {
            return null;
        }

        if(options.outWidth <= width && options.outHeight <= height) {
            FileInputStream in = null;

            try {
                in = new FileInputStream(file);
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();

                IOUtil.copy(in, bos);

                data = bos.toByteArray();
            } catch (Throwable t) {
                // Eat
            } finally {
                IOUtil.closeQuietly(in);
            }
        } else {
            Bitmap.CompressFormat originFormat = Bitmap.CompressFormat.JPEG;

            if(TextUtils.equals(options.outMimeType, "image/jpeg")) {
                originFormat = Bitmap.CompressFormat.JPEG;
            } else if(TextUtils.equals(options.outMimeType, "image/png")) {
                originFormat = Bitmap.CompressFormat.PNG;
            } else if(TextUtils.equals(options.outMimeType, "image/webp")) {
                originFormat = Bitmap.CompressFormat.WEBP;
            }

            final Bitmap bitmap = decodeBitmap(context, file, width, height);

            if(bitmap != null) {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(originFormat, 100, bos);
                bitmap.recycle();

                data = bos.toByteArray();
            }
        }

        return data;
    }

    public static byte[] compressImageToSizeFromFile(Context context, File file, long maxSize) {
        if(context == null || file == null) {
            return null;
        }

        // 最大8M
        if(maxSize <= 0 || maxSize > 8 * 1024 * 1024) {
            maxSize = 8 * 1024 * 1024;
        }

        if(file.length() <= maxSize) {
            FileInputStream in = null;

            try {
                in = new FileInputStream(file);
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();

                IOUtil.copy(in, bos);

                return bos.toByteArray();
            } catch (Exception e) {
                //Eat
            } finally {
                IOUtil.closeQuietly(in);
            }
        } else {
            Bitmap bitmap = null;
            int width, height;

            // 高宽差距在2倍以上，还是先缩小吧
            if(file.length() / maxSize >= 4) {
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                decodeBitmap(context, file, options);

                // 放大一点大小，因为后续还可以通过降低质量来控制大小，所以这里不一定要按照比例来缩小
                final double ratio = Math.sqrt((double)maxSize / file.length()) * 1.5f;

                width = (int)(options.outWidth * ratio);
                height = (int)(options.outHeight * ratio);
            } else {
                width = context.getResources().getDisplayMetrics().widthPixels;
                height = context.getResources().getDisplayMetrics().heightPixels;
            }

            bitmap = decodeBitmap(context, file, width, height);

            if(bitmap != null) {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int quality = 95;

                try {
                    do {
                        bos.reset();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
                        quality -= 10;
                    } while (quality >= 30 && bos.size() > maxSize); // 低于30基本没法看

                    bitmap.recycle();

                    if(bos.size() <= maxSize) {
                        return bos.toByteArray();
                    }
                } catch (Exception e) {
                    // Eat
                }
            }
        }

        return null;
    }

    public static byte[] compressImageToSizeFromData(Context context, byte[] imageData, long maxSize) {
        if(context == null || imageData == null) {
            return null;
        }

        // 最大8M
        if(maxSize <= 0 || maxSize > 8 * 1024 * 1024) {
            maxSize = 8 * 1024 * 1024;
        }

        if(imageData.length <= maxSize) {
            return imageData;
        } else {
            Bitmap bitmap = null;
            int width, height;

            // 高宽差距在2倍以上，还是先缩小吧
            if(imageData.length / maxSize >= 4) {
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                decodeBitmap(context, imageData, options);

                // 放大一点大小，因为后续还可以通过降低质量来控制大小，所以这里不一定要按照比例来缩小
                final double ratio = Math.sqrt((double)maxSize / imageData.length) * 1.5f;

                width = (int)(options.outWidth * ratio);
                height = (int)(options.outHeight * ratio);
            } else {
                width = context.getResources().getDisplayMetrics().widthPixels;
                height = context.getResources().getDisplayMetrics().heightPixels;
            }

            bitmap = decodeBitmap(context, imageData, width, height);

            if(bitmap != null) {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int quality = 95;

                do {
                    bos.reset();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
                    quality -= 10;
                } while (quality >= 20 && bos.size() > maxSize); // 低于20基本没法看

                bitmap.recycle();

                if(bos.size() <= maxSize) {
                    return bos.toByteArray();
                }
            }
        }

        return null;
    }

    private static Bitmap decodeBitmap(Context context, Object param, int width, int height) {
        Bitmap bitmap = null;

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        decodeBitmap(context, param, options);

        int bitmapWidth = options.outWidth;
        int bitmapHeight = options.outHeight;

        if(bitmapWidth > 0 && bitmapHeight > 0) {
            options.inJustDecodeBounds = false;

            if(width <= 0) width = bitmapWidth;
            if(height <= 0) height = bitmapHeight;

            if(bitmapWidth <= width && bitmapHeight <= height) {
                bitmap = decodeBitmap(context, param, options);
            } else {
                /*
                 * 缩小逻辑：
                 * 1、计算宽、高需要缩小的比例，以大的比例为主，得到ratio
                 * 2、计算跟ratio最接近的min和max sample size，sample size必须是2的倍数
                 * 3、看看min和max哪个跟ratio更接近一些，得到nearest sample size
                 * 4、使用得到nearest去decode，如果decode失败了，再尝试用max去decode
                 * 5、判断是否需要再次缩小
                 */
                float ratio = Math.max((float)bitmapWidth / width, (float)bitmapHeight / height);
                int minSampleSize = Math.max((int)ratio / 2 * 2, 1);
                int maxSampleSize = ((int)ratio / 2 + 1) * 2;

                int nearestSampleSize;

                if(Math.abs(ratio - minSampleSize) <= Math.abs(ratio - maxSampleSize)) {
                    nearestSampleSize = minSampleSize;
                } else {
                    nearestSampleSize = maxSampleSize;
                }

                options.inSampleSize = nearestSampleSize;
                bitmap = decodeBitmap(context, param, options);

                // 可能是OOM了
                if(bitmap == null && nearestSampleSize != maxSampleSize) {
                    options.inSampleSize = maxSampleSize;
                    bitmap = decodeBitmap(context, param, options);
                }

                if(bitmap != null) {
                    try {
                        bitmapWidth = bitmap.getWidth();
                        bitmapHeight = bitmap.getHeight();

                        if(bitmapWidth > width || bitmapHeight > height) {
                            ratio = Math.min((float)width / bitmapWidth, (float)height / bitmapHeight);

                            final Matrix matrix = new Matrix();
                            matrix.postScale(ratio, ratio);

                            final Bitmap resized = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
                            bitmap.recycle();
                            bitmap = resized;
                        }
                    } catch (Throwable t) {
                        // Eat
                    }
                }
            }
        }

        return bitmap;
    }

    private static Bitmap decodeBitmap(Context context, Object param, BitmapFactory.Options options) {
        Bitmap bitmap = null;

        InputStream in = null;

        try {
            if(param instanceof Integer) {
                bitmap = BitmapFactory.decodeResource(context.getResources(), (int)param, options);
            } else if(param instanceof File) {
                bitmap = BitmapFactory.decodeFile(((File)param).getCanonicalPath(), options);
            } else if(param instanceof String) {
                bitmap = BitmapFactory.decodeFile((String)param, options);
            } else if(param instanceof Uri) {
                in = context.getContentResolver().openInputStream((Uri)param);
                bitmap = BitmapFactory.decodeStream(in, null, options);
            } else if(param instanceof byte[]) {
                bitmap = BitmapFactory.decodeByteArray((byte[])param, 0, ((byte[]) param).length, options);
            }
        } catch (Throwable t) {
            // Eat
        } finally {
            IOUtil.closeQuietly(in);
        }

        return bitmap;
    }
}
