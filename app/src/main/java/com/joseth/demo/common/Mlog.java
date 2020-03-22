/*
 *  G7ADAS
 *
 *  Created by lishengjun@g7.com.cn on 2018-08-25 10:31:54
 *  Last modified on 2018-08-15 11:52:26
 *
 *  Copyright (c) 2018. by G7, Inc. All rights reserved.
 */

package com.joseth.demo.common;

import android.util.Log;

import com.joseth.demo.BuildConfig;


public class Mlog {
    public static final String LOG_TAG = "AndroidDemo";

    public static final int DEBUG_LEVEL_VERBOSE = Log.VERBOSE;
    public static final int DEBUG_LEVEL_DEBUG = Log.DEBUG;
    public static final int DEBUG_LEVEL_INFO = Log.INFO;
    public static final int DEBUG_LEVEL_WARN = Log.WARN;
    public static final int DEBUG_LEVEL_ERROR = Log.ERROR;
    public static final int DEBUG_LEVEL_ASSERT = Log.ASSERT;
    private static final int DEBUG_LEVEL_MIN_DEFAULT = BuildConfig.DEBUG ? DEBUG_LEVEL_VERBOSE : DEBUG_LEVEL_DEBUG;
    private static int sDebugLevel = DEBUG_LEVEL_MIN_DEFAULT;
    private static final boolean USE_UNIFIED_TAG = false;
    private static boolean sToFile = false;
    private static boolean sEnable = BuildConfig.DEBUG;

    public static void v(String tag, String msg) {
        if (!isLoggable(DEBUG_LEVEL_VERBOSE))
            return;
         if (USE_UNIFIED_TAG)
            Log.v(LOG_TAG, tag + "---" + msg);
        else
            Log.v(tag, msg);
    }

    public static void v(String tag, String msg, Throwable tr) {
        if (!isLoggable(DEBUG_LEVEL_VERBOSE))
            return;

        if (USE_UNIFIED_TAG)
            Log.v(LOG_TAG, tag + "---" + msg, tr);
        else
            Log.v(tag, msg, tr);
    }

    public static void d(String tag, String msg) {
        if (!isLoggable(DEBUG_LEVEL_DEBUG))
            return;

        if (USE_UNIFIED_TAG)
            Log.d(LOG_TAG, tag + "---" + msg);
        else
            Log.d(tag, msg);

    }

    public static void d(String tag, String msg, Throwable tr) {
        if (!isLoggable(DEBUG_LEVEL_DEBUG))
            return;

        if (USE_UNIFIED_TAG)
            Log.d(LOG_TAG, tag + "---" + msg, tr);
        else
            Log.d(tag, msg, tr);

    }

    public static void i(String tag, String msg) {
        if (!isLoggable(DEBUG_LEVEL_INFO))
            return;

        if (USE_UNIFIED_TAG)
            Log.i(LOG_TAG, tag + "---" + msg);
        else
            Log.i(tag, msg);
    }

    public static void i(String tag, String msg, Throwable tr) {
        if (!isLoggable(DEBUG_LEVEL_INFO))
            return;

        if (USE_UNIFIED_TAG)
            Log.i(LOG_TAG, tag + "---" + msg, tr);
        else
            Log.i(tag, msg, tr);

    }

    public static void w(String tag, String msg) {
        if (!isLoggable(DEBUG_LEVEL_WARN))
            return;

        if (USE_UNIFIED_TAG)
            Log.w(LOG_TAG, tag + "---" + msg);
        else
            Log.w(tag, msg);

    }

    public static void w(String tag, String msg, Throwable tr) {
        if (!isLoggable(DEBUG_LEVEL_WARN))
            return;

        if (USE_UNIFIED_TAG)
            Log.w(LOG_TAG, tag + "---" + msg, tr);
        else
            Log.w(tag, msg, tr);

    }

    public static void e(String tag, String msg) {
        if (!isLoggable(DEBUG_LEVEL_ERROR))
            return;

        if (USE_UNIFIED_TAG)
            Log.e(LOG_TAG, tag + "---" + msg);
        else
            Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (!isLoggable(DEBUG_LEVEL_ERROR))
            return;

        if (USE_UNIFIED_TAG)
            Log.e(LOG_TAG, tag + "---" + msg, tr);
        else
            Log.e(tag, msg, tr);
    }

    public static boolean isLoggable(int level) {
        if (sDebugLevel > level)
            return false;

        if (sToFile)
            return true;

        if (BuildConfig.DEBUG)
            return true;

        return sEnable;
    }

    private static String toLevelString(final int level) {
        switch (level) {
            case DEBUG_LEVEL_VERBOSE:
                return "V";
            case DEBUG_LEVEL_DEBUG:
                return "D";
            case DEBUG_LEVEL_INFO:
                return "I";
            case DEBUG_LEVEL_WARN:
                return "W";
            case DEBUG_LEVEL_ERROR:
                return "E";
            case DEBUG_LEVEL_ASSERT:
                return "A";
        }
        return null;
    }
}
