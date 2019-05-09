/*
 *  G7ADAS
 *
 *  Created by lishengjun@g7.com.cn on 2018-08-25 10:31:54
 *  Last modified on 2018-08-15 11:52:26
 *
 *  Copyright (c) 2018. by G7, Inc. All rights reserved.
 */

package com.joseth.demo;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class HandlerThreadHandler extends Handler {
    private static final String TAG = "HandlerThreadHandler";

    private HandlerThreadHandler(final Looper looper) {
        super(looper);
    }

    private HandlerThreadHandler(final Looper looper, final Callback callback) {
        super(looper, callback);
    }

    public static final HandlerThreadHandler createHandler() {
        return createHandler(TAG);
    }

    public static final HandlerThreadHandler createHandler(final String name) {
        final HandlerThread thread = new HandlerThread(name);
        thread.start();

        return new HandlerThreadHandler(thread.getLooper());
    }

    public static final HandlerThreadHandler createHandler(final Callback callback) {
        return createHandler(TAG, callback);
    }

    public static final HandlerThreadHandler createHandler(final String name, final Callback callback) {
        final HandlerThread thread = new HandlerThread(name);
        thread.start();

        return new HandlerThreadHandler(thread.getLooper(), callback);
    }

    public boolean quitSafely() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quitSafely();
            return true;
        }
        return false;
    }
}
