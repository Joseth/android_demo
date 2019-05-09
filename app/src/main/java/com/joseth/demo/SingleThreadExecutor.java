/*
 * G7ADAS
 *
 * Created by lishengjun@g7.com.cn on 2/22/19 7:52 PM
 * Last modified on 2/13/19 11:49 AM
 *
 * Copyright (c) 2019. by G7, Inc. All rights reserved.
 */

package com.joseth.demo;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SingleThreadExecutor extends ThreadPoolExecutor {

    public SingleThreadExecutor() {
        super(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1), new NameThreadFactory("Single"),
                new DiscardPolicy());
    }

    public SingleThreadExecutor(String name) {
        super(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1), new NameThreadFactory(name),
                new DiscardPolicy());
    }

    public SingleThreadExecutor(RejectedExecutionHandler handler) {
        super(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1), new NameThreadFactory("Single"),
                handler);
    }
}
