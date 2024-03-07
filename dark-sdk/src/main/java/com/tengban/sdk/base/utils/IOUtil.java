package com.tengban.sdk.base.utils;

import com.tengban.sdk.base.toolbox.ByteArrayPool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class IOUtil {

    private static final int BUFFER_SIZE = 8 * 1024;

    private static final ByteArrayPool BYTE_POOL = new ByteArrayPool(BUFFER_SIZE);

    public static byte[] zip(byte[] data) {
        final InputStream in = new ByteArrayInputStream(data);
        return zip(in);
    }

    public static byte[] unzip(byte[] data) {
        final InputStream in = new ByteArrayInputStream(data);
        return unzip(in);
    }

    public static byte[] zip(File file) {
        InputStream in = null;

        try {
            in = new FileInputStream(file);
            return zip(in);
        } catch (FileNotFoundException e) {
            // Eat
        } finally {
            closeQuietly(in);
        }

        return null;
    }

    public static byte[] unzip(File file) {
        InputStream in = null;

        try {
            in = new FileInputStream(file);
            return unzip(in);
        } catch (FileNotFoundException e) {
            // Eat
        } finally {
            closeQuietly(in);
        }

        return null;
    }

    public static byte[] zip(InputStream in) {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE / 2);
            final ZipOutputStream zip = new ZipOutputStream(out);

            final ZipEntry entry = new ZipEntry("data");
            zip.putNextEntry(entry);

            copy(in, zip);

            zip.closeEntry();

            zip.flush();
            zip.close();

            return out.toByteArray();
        } catch (IOException e) {
            // Eat
        }

        return null;
    }

    public static byte[] unzip(InputStream in) {
        try {
            final ZipInputStream zip = new ZipInputStream(in);

            if(zip.getNextEntry() != null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE / 2);

                copy(zip, out);

                return out.toByteArray();
            }
        } catch (IOException e) {
            // Eat
        }

        return null;
    }

    public static byte[] gzip(byte[] data) {
        if(data != null && data.length > 0) {
            try {
                final ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE / 2);
                final GZIPOutputStream gzip = new GZIPOutputStream(out);

                final InputStream in = new ByteArrayInputStream(data);

                copy(in, gzip);

                gzip.flush();
                gzip.close();

                return out.toByteArray();
            } catch (IOException e) {
                // Eat
            }
        }

        return null;
    }

    public static byte[] ungzip(byte[] data) {
        if(data != null && data.length > 0) {
            try {
                final ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE / 2);

                final InputStream in = new ByteArrayInputStream(data);
                final GZIPInputStream gzip = new GZIPInputStream(in);

                copy(gzip, out);

                return out.toByteArray();
            } catch (IOException e) {
                // Eat
            }
        }

        return null;
    }

    public static boolean copy(File from, File to) {
        FileInputStream in = null;
        FileOutputStream out = null;

        try {
            in = new FileInputStream(from);
            out = new FileOutputStream(to);

            copy(in, out);

            return true;
        } catch (IOException e) {
            // Eat
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }

        return false;
    }

    public static long copy(InputStream from, OutputStream to) throws IOException {
        final byte[] buffer = BYTE_POOL.getBuf(BUFFER_SIZE);

        long total = 0;
        int read = 0;

        try {
            while((read = from.read(buffer)) != -1) {
                to.write(buffer, 0, read);
                total += read;
            }
        } finally {
            BYTE_POOL.returnBuf(buffer);
        }

        return total;
    }

    public static boolean toFile(byte[] data, File file) {
        if(data != null && data.length > 0 && file != null) {
            FileOutputStream out = null;

            try {
                out = new FileOutputStream(file);

                copy(new ByteArrayInputStream(data), out);

                return true;
            } catch (IOException e) {
                // Eat
            } finally {
                closeQuietly(out);
            }
        }

        return false;
    }

    public static byte[] toBytes(File file) {
        return toBytes(file, -1);
    }

    public static byte[] toBytes(File file, long limitSize) {
        if(file != null && file.exists()) {
            FileInputStream in = null;

            try {
                in = new FileInputStream(file);

                return toBytes(in, limitSize);
            } catch (IOException e) {
                // Eat
            } finally {
                closeQuietly(in);
            }
        }


        return null;
    }

    public static byte[] toBytes(InputStream in) throws IOException {
        return toBytes(in, -1);
    }

    public static byte[] toBytes(InputStream in, long limitSize) throws IOException {
        limitSize = (limitSize > 0 ? limitSize : Long.MAX_VALUE);

        final int bufferSize = limitSize > BUFFER_SIZE ? BUFFER_SIZE : (int)limitSize;

        final ByteArrayOutputStream out = new ByteArrayOutputStream(bufferSize);
        final byte[] buffer = BYTE_POOL.getBuf(bufferSize);

        int read = 0;
        long remaining = limitSize;

        try {
            while (remaining > 0) {
                read = in.read(buffer, 0,
                        remaining > buffer.length ? buffer.length : (int)remaining);

                if(read == -1) {
                    break;
                }

                out.write(buffer, 0, read);

                remaining -= read;
            }
        } finally {
            BYTE_POOL.returnBuf(buffer);
        }

        return out.toByteArray();
    }

    public static void closeQuietly(Closeable closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                // Eat
            }
        }
    }
}
