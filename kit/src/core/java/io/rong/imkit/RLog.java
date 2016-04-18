package io.rong.imkit;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by DragonJ on 14-10-17.
 */
public class RLog {

    static final String TAG = "##KIT_";
    static final boolean DEBUG = true;
    static PrintWriter mPrint;

    public static void init(Context context) {
        File dir = context.getExternalFilesDir("log");
        if (!dir.exists() && dir.canWrite()) {
            dir.mkdirs();
        }

        Calendar calendar = Calendar.getInstance();

        File file = new File(dir, "kit_" + calendar.get(Calendar.MONTH) + "_" + calendar.get(Calendar.DAY_OF_MONTH) + "-" + calendar.get(Calendar.HOUR_OF_DAY) + "_" + calendar.get(Calendar.MINUTE) + ".log");

        try {
            mPrint = new PrintWriter(new FileOutputStream(file), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a {@link #VERBOSE} log message.
     *
     * @param obj object of call.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int v(Object obj, String tag, String msg) {
        if (!DEBUG) return 0;

        if (mPrint != null){
                
            mPrint.print(new Date(SystemClock.currentThreadTimeMillis()).toString());
                mPrint.print('\t');
                mPrint.print(TAG + obj.getClass().getCanonicalName() + ":" + tag + "########" + msg);
                mPrint.print('\n');
                mPrint.flush();
        }


        return Log.v(TAG + obj.getClass().getCanonicalName() + ":" + tag, msg);
    }

    /**
     * Send a {@link #VERBOSE} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int v(Object obj, String tag, String msg, Throwable tr) {
        if (!DEBUG) return 0;
        if (mPrint != null){
            mPrint.print(new Date(SystemClock.currentThreadTimeMillis()).toString());
            mPrint.print('\t');
            mPrint.print(TAG + obj.getClass().getCanonicalName() + ":" + tag + "########" + msg);
            mPrint.print('\n');
            mPrint.flush();
        }
        return Log.v(TAG + obj.getClass().getCanonicalName() + ":" + tag, msg, tr);
    }

    /**
     * Send a {@link #DEBUG} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int d(Object obj, String tag, String msg) {
        if (!DEBUG) return 0;
        if (mPrint != null){
            mPrint.print(new Date(SystemClock.currentThreadTimeMillis()).toString());
            mPrint.print('\t');
            mPrint.print(TAG + obj.getClass().getCanonicalName() + ":" + tag + "########" + msg);
            mPrint.print('\n');
            mPrint.flush();
        }
        return Log.d(TAG + obj.getClass().getCanonicalName() + ":" + tag, msg);
    }

    /**
     * Send a {@link #DEBUG} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int d(Object obj, String tag, String msg, Throwable tr) {
        if (!DEBUG) return 0;
        if (mPrint != null){
            mPrint.print(new Date(SystemClock.currentThreadTimeMillis()).toString());
            mPrint.print('\t');
            mPrint.print(TAG + obj.getClass().getCanonicalName() + ":" + tag + "########" + msg);
            mPrint.print('\n');
            mPrint.flush();
        }
        return Log.d(TAG + obj.getClass().getCanonicalName() + ":" + tag, msg, tr);
    }

    /**
     * Send an {@link #INFO} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int i(Object obj, String tag, String msg) {
        if (!DEBUG) return 0;
        if (mPrint != null){
            mPrint.print(new Date(SystemClock.currentThreadTimeMillis()).toString());
            mPrint.print('\t');
            mPrint.print(TAG + obj.getClass().getCanonicalName() + ":" + tag + "########" + msg);
            mPrint.print('\n');
            mPrint.flush();
        }
        return Log.i(TAG + obj.getClass().getCanonicalName() + ":" + tag, msg);
    }

    /**
     * Send a {@link #INFO} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int i(Object obj, String tag, String msg, Throwable tr) {
        if (!DEBUG) return 0;
        if (mPrint != null){
            mPrint.print(new Date(SystemClock.currentThreadTimeMillis()).toString());
            mPrint.print('\t');
            mPrint.print(TAG + obj.getClass().getCanonicalName() + ":" + tag + "########" + msg);
            mPrint.print('\n');
            mPrint.flush();
        }
        return Log.i(TAG + obj.getClass().getCanonicalName() + ":" + tag, msg, tr);
    }

    /**
     * Send a {@link #WARN} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int w(Object obj, String tag, String msg) {
        if (!DEBUG) return 0;
        if (mPrint != null){
            mPrint.print(new Date(SystemClock.currentThreadTimeMillis()).toString());
            mPrint.print('\t');
            mPrint.print(TAG + obj.getClass().getCanonicalName() + ":" + tag + "########" + msg);
            mPrint.print('\n');
            mPrint.flush();
        }
        return Log.w(TAG + obj.getClass().getCanonicalName() + ":" + tag, msg);
    }

    /**
     * Send a {@link #WARN} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int w(Object obj, String tag, String msg, Throwable tr) {
        if (!DEBUG) return 0;
        if (mPrint != null){
            mPrint.print(new Date(SystemClock.currentThreadTimeMillis()).toString());
            mPrint.print('\t');
            mPrint.print(TAG + obj.getClass().getCanonicalName() + ":" + tag + "########" + msg);
            mPrint.print('\n');
            mPrint.flush();
        }
        return Log.w(TAG + obj.getClass().getCanonicalName() + ":" + tag, msg, tr);
    }


    /*
     * Send a {@link #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    public static int w(Object obj, String tag, Throwable tr) {
        if (!DEBUG) return 0;
        if (mPrint != null){
            mPrint.print(new Date(SystemClock.currentThreadTimeMillis()).toString());
            mPrint.print('\t');
            mPrint.print(TAG + obj.getClass().getCanonicalName() + ":" + tag + "########" + tr.getMessage());
            mPrint.print('\n');
            mPrint.flush();
        }
        return Log.w(TAG + obj.getClass().getCanonicalName() + ":" + tag, tr);
    }

    /**
     * Send an {@link #ERROR} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int e(Object obj, String tag, String msg) {
        if (!DEBUG) return 0;
        if (mPrint != null){
            mPrint.print(new Date(SystemClock.currentThreadTimeMillis()).toString());
            mPrint.print('\t');
            mPrint.print(TAG + obj.getClass().getCanonicalName() + ":" + tag + "########" + msg);
            mPrint.print('\n');
            mPrint.flush();
        }
        return Log.e(TAG + obj.getClass().getCanonicalName() + ":" + tag, msg);
    }

    /**
     * Send a {@link #ERROR} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int e(Object obj, String tag, String msg, Throwable tr) {
        if (!DEBUG) return 0;
        if (mPrint != null){
            mPrint.print(new Date(SystemClock.currentThreadTimeMillis()).toString());
            mPrint.print('\t');
            mPrint.print(TAG + obj.getClass().getCanonicalName() + ":" + tag + "########" + msg);
            mPrint.print('\n');
            mPrint.flush();
        }
        return Log.e(TAG + obj.getClass().getCanonicalName() + ":" + tag, msg, tr);
    }

    /**
     * What a Terrible Failure: Report a condition that should never happen.
     * The error will always be logged at level ASSERT with the call stack.
     * Depending on system configuration, a report may be added to the
     * {@link android.os.DropBoxManager} and/or the process may be terminated
     * immediately with an error dialog.
     *
     * @param tag Used to identify the source of a log message.
     * @param msg The message you would like logged.
     */
    public static int wtf(Object obj, String tag, String msg) {
        if (!DEBUG) return 0;
        if (mPrint != null){
            mPrint.print(new Date(SystemClock.currentThreadTimeMillis()).toString());
            mPrint.print('\t');
            mPrint.print(TAG + obj.getClass().getCanonicalName() + ":" + tag + "########" + msg);
            mPrint.print('\n');
            mPrint.flush();
        }
        return Log.wtf(TAG + obj.getClass().getCanonicalName() + ":" + tag, msg);
    }


    /**
     * What a Terrible Failure: Report an exception that should never happen.
     * Similar to {@link #wtf(String, String)}, with an exception to log.
     *
     * @param tag Used to identify the source of a log message.
     * @param tr  An exception to log.
     */
    public static int wtf(Object obj, String tag, Throwable tr) {
        if (!DEBUG) return 0;
        if (mPrint != null){
            mPrint.print(new Date(SystemClock.currentThreadTimeMillis()).toString());
            mPrint.print('\t');
            mPrint.print(TAG + obj.getClass().getCanonicalName() + ":" + tag + "########" + tr.getMessage());
            mPrint.print('\n');
            mPrint.flush();
        }
        return Log.wtf(TAG + obj.getClass().getCanonicalName() + ":" + tag, tr);
    }

    /**
     * What a Terrible Failure: Report an exception that should never happen.
     * Similar to {@link #wtf(String, Throwable)}, with a message as well.
     *
     * @param tag Used to identify the source of a log message.
     * @param msg The message you would like logged.
     * @param tr  An exception to log.  May be null.
     */
    public static int wtf(Object obj, String tag, String msg, Throwable tr) {
        if (!DEBUG) return 0;
        if (mPrint != null){
            mPrint.print(new Date(SystemClock.currentThreadTimeMillis()).toString());
            mPrint.print('\t');
            mPrint.print(TAG + obj.getClass().getCanonicalName() + ":" + tag + "########" + msg);
            mPrint.print('\n');
            mPrint.flush();
        }
        return Log.wtf(TAG + obj.getClass().getCanonicalName() + ":" + tag, msg, tr);
    }


}
