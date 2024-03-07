package com.tengban.sdk.base.log;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.util.Pools;

import com.tengban.sdk.base.utils.AndroidUtil;
import com.tengban.sdk.base.utils.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Logger implements Handler.Callback {

    static final int MSG_APPEND_LOG = 1000;
    static final int MSG_WRITE_LOG = 1001;

    private static Logger sInstance;

    Context context;

    private HandlerThread mThread;
    private Handler mHandler;

    private Pools.SynchronizedPool<LoggerObject> mPools;

    private SimpleDateFormat mSDF;
    private StringBuilder mSB;

    private Map<String, List<LoggerObject>> mLogCache;
    private int mTotalLogCount;

    private Logger() {
        mThread = new HandlerThread("LoggerHandlerThread", HandlerThread.MIN_PRIORITY);
        mThread.start();

        mHandler = new Handler(mThread.getLooper(), this);

        mPools = new Pools.SynchronizedPool<>(64);

        mSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        mSB = new StringBuilder(256);

        mLogCache = new HashMap<>(8);
        mTotalLogCount = 0;
    }

    public static Logger get() {
        if(sInstance == null) {
            synchronized (Logger.class) {
                if(sInstance == null) {
                    sInstance = new Logger();
                }
            }
        }

        return sInstance;
    }

    public static void setContext(Context context) {
        get().context = AndroidUtil.getAppContext(context);
    }

    public static void log(String tag, String format, Object... args) {
        get().log(tag, format, false, args);
    }

    public static void logFile(String tag, String format, Object... args) {
        get().log(tag, format, true, args);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_APPEND_LOG: {
                final LoggerObject o = (LoggerObject)msg.obj;

                List<LoggerObject> list = mLogCache.get(o.tag);

                if(list == null) {
                    list = new ArrayList<>(32);
                    mLogCache.put(o.tag, list);
                }

                list.add(o);

                mTotalLogCount++;

                if(list.size() >= 32 || mTotalLogCount >= 256) {
                    writeLog();
                }

                mHandler.removeMessages(MSG_WRITE_LOG);
                mHandler.sendEmptyMessageDelayed(MSG_WRITE_LOG, 10 * 1000);

                break;
            }

            case MSG_WRITE_LOG: {
                writeLog();

                break;
            }
        }

        return true;
    }

    private void log(String tag, String format, boolean toFile, Object... args) {
        if(TextUtils.isEmpty(format)) {
            return;
        }

        if(TextUtils.isEmpty(tag)) {
            tag = "Logger";
        }

        String message = null;

        // 需要打日志的时候才去组装
        if(toFile || AndroidUtil.isAppDebuggable(context)) {
            try {
                message = String.format(format, args);
            } catch (Exception e) {
                message = "Log format error: " + e.getMessage();
            }

            // 只有Debug包才输出到Logcat
            if(AndroidUtil.isAppDebuggable(context)) {
                Log.d(tag, message);
            }
        }

        if(toFile) {
            LoggerObject o = mPools.acquire();

            if (o == null) {
                o = new LoggerObject();
            }

            o.update(tag, message, new Date(), Thread.currentThread());

            mHandler.obtainMessage(MSG_APPEND_LOG, o).sendToTarget();
        }
    }

    private String logFilePath(String tag) throws IOException {
        final Context ctx = context;

        if(ctx != null) {
            final File fileDir = ctx.getExternalFilesDir(null);
            if (fileDir == null) {
                return null;
            }

            final File logDir = new File(fileDir, "Logger");

            if(!logDir.exists()) {
                logDir.mkdirs();
            }

            if(logDir.exists()) {
                return new File(logDir, tag + ".log").getCanonicalPath();
            }
        }

        return null;
    }

    private void writeLog() {
        if(!mLogCache.isEmpty()) {
            List<LoggerObject> list;

            for (Map.Entry<String, List<LoggerObject>> entry : mLogCache.entrySet()) {
                list = entry.getValue();

                if(!list.isEmpty()) {
                    writeLog(entry.getKey(), list);
                }

                mTotalLogCount -= list.size();

                list.clear();
            }

            if(mLogCache.size() > 8) {
                mLogCache.clear();
            }
        }
    }

    private void writeLog(String tag, List<LoggerObject> logs) {
        RandomAccessFile file = null;

        try {
            final String path = logFilePath(tag);
            if(TextUtils.isEmpty(path)) {
                return;
            }

            file = new RandomAccessFile(path, "rw");

            // 加锁, 防止写缓存时相互覆盖, close文件的时候会自动释放
            file.getChannel().lock();

            // Trim日志
            if(file.length() >= 2 * 1024 * 1024) {
                file.seek(0);

                // 取最后的4000行, 再取最后的3/4
                // 这么做是因为万一日志不足4000行, 也能保证trim
                List<String> keepLines = readLinesReverse(file, 4000);
                final int offset = keepLines.size() / 4;
                keepLines = keepLines.subList(offset, keepLines.size());

                // 清空文件
                file.setLength(0);

                final String newContent = TextUtils.join("\n", keepLines);

                file.write(newContent.getBytes("UTF-8"));
                file.write("\n".getBytes("UTF-8"));
            } else {
                file.seek(file.length());
            }

            mSB.setLength(0);

            for(LoggerObject log : logs) {
                mSB.append(mSDF.format(log.date));
                mSB.append(" [");
                mSB.append(log.tag);
                mSB.append("]");
                mSB.append("[");
                mSB.append(log.thread.getId());
                mSB.append(":");
                mSB.append(log.thread.getName());
                mSB.append("] ");
                mSB.append(log.message);
                mSB.append("\n");

                mPools.release(log);
            }

            file.write(mSB.toString().getBytes("UTF-8"));
        } catch (Exception e) {
            // Eat
        } finally {
            IOUtil.closeQuietly(file);
        }
    }

    private List<String> readLinesReverse(RandomAccessFile file, int count) throws IOException {
        final LinkedList<String> lines = new LinkedList<>();

        final ReversedLinesReader linesReader = new ReversedLinesReader(file, 4096);

        String line = null;

        for(int i = 0; i < count; ++i) {
            line = linesReader.readLine();

            if(line == null) {
                break;
            }

            if(!line.isEmpty()) {
                lines.addFirst(line);
            }
        }

        return lines;
    }

    static class LoggerObject {

        String tag;
        String message;
        Date date;
        Thread thread;

        public void update(String tag, String message, Date date, Thread thread) {
            this.tag = tag;
            this.message = message;
            this.date = date;
            this.thread = thread;
        }
    }
}
