/*
 * G7ADAS
 *
 * Created by lishengjun@g7.com.cn on 1/10/19 10:28 AM
 * Last modified on 11/11/18 3:07 PM
 *
 * Copyright (c) 2019. by G7, Inc. All rights reserved.
 */

package com.joseth.demo.thread;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

public abstract class BaseTask implements Handler.Callback {
    protected Context mContext;
    private final String mThreadName;
    private HandlerThread mWorkerThread;
    private long mWorkerThreadID = -1;
    protected Handler mWorkerHandler;
    protected Handler mCurrentHander;
    private boolean mStarted = false;

    public BaseTask(Context context, final String name) {
        mContext = context;
        mCurrentHander = new Handler();
        mThreadName = name;
    }

    public int start() {
        mWorkerThread = new HandlerThread(mThreadName);
        mWorkerThread.start();

        mWorkerHandler = new Handler(mWorkerThread.getLooper(), this);
        mStarted = true;
        return 0;
    }

    public boolean stop() {
        if (mWorkerThread != null) {
            mStarted = false;
            mWorkerThread.quitSafely();
            mWorkerThread = null;
        }
        return true;
    }

    protected boolean isStarted() {
        return mStarted;
    }

    protected void removeWorkerMessage(final int what) {
        if (mWorkerThread == null)
            return;
        mWorkerHandler.removeMessages(what);
    }


    protected void removeCallbacksAndMessages() {
        if (mWorkerThread == null)
            return;
        mWorkerHandler.removeCallbacksAndMessages(null);
    }

    protected void sendWorkerEmptyMessage(int what) {
        if (mWorkerThread == null)
            return;
        mWorkerHandler.sendEmptyMessage(what);
    }

    protected void sendWorkerEmptyMessageDelayed(int what, long delayMillis) {
        if (mWorkerThread == null)
            return;
        mWorkerHandler.sendEmptyMessageDelayed(what, delayMillis);
    }

    protected void sendWorkerMessageDelayed(int what, int arg1, int arg2, long delayMillis) {
        if (mWorkerThread == null)
            return;
        sendWorkerMessageDelayed(mWorkerHandler.obtainMessage(what, arg1, arg2), delayMillis);
    }

    protected void sendWorkerMessageDelayed(int what, int arg1, int arg2, Object obj, long delayMillis) {
        if (mWorkerThread == null)
            return;
        sendWorkerMessageDelayed(mWorkerHandler.obtainMessage(what, arg1, arg2, obj), delayMillis);
    }

    protected void sendWorkerMessage(Message msg) {
        if (mWorkerThread == null)
            return;
        mWorkerHandler.sendMessage(msg);
    }

    protected void sendWorkerMessageDelayed(Message msg, long delayMillis) {
        if (mWorkerThread == null)
            return;
        mWorkerHandler.sendMessageDelayed(msg, delayMillis);
    }
}
