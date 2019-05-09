/*
 * G7ADAS
 *
 * Created by lishengjun@g7.com.cn on 2/22/19 7:53 PM
 * Last modified on 2/13/19 11:49 AM
 *
 * Copyright (c) 2019. by G7, Inc. All rights reserved.
 */

package com.joseth.demo;

import java.util.concurrent.ThreadFactory;

public class NameThreadFactory implements ThreadFactory {

    private final String name;
    private int count;


    public NameThreadFactory(String name) {
        super();
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName("T:" + name + "-" + count++);
        return thread;
    }
}
