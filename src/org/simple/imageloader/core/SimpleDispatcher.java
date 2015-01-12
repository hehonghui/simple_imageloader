/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2015 Umeng, Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.simple.imageloader.core;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import org.simple.imageloader.cache.BitmapCache;
import org.simple.net.core.RequestQueue;

/**
 * @author mrsimple
 */
final class SimpleDispatcher {

    RequestQueue mRequestQueue;
    BitmapCache mBitmapCache;

    public SimpleDispatcher(RequestQueue queue, BitmapCache cache) {

    }

    // HandlerThread内部封装了自己的Handler和Thead，有单独的Looper和消息队列
    private static final HandlerThread HT = new HandlerThread(
            SimpleDispatcher.class.getName(),
            android.os.Process.THREAD_PRIORITY_BACKGROUND);
    // 主线程消息队列的Handler
    final static Handler mUIHandler = new Handler(Looper.getMainLooper());
    static {
        HT.start();
    }

    /**
     * @功能描述 : onPreExecute任务执行之前的初始化操作等
     */
    protected void onPreExecute() {

    }

    /**
     * @功能描述 : doInBackground后台执行任务
     * @return
     */
    protected Bitmap doInBackground() {
        return null;
    }

    /**
     * @功能描述 : doInBackground返回结果，在onPostExecute更新UI线程
     * @param result
     */
    protected void onPostExecute(Bitmap result) {
    }

    /**
     * 执行数据库操作,可在此函数中添加整体的事物操作,避免性能问题. [ 目前还没有进行优化的必要 ]
     * 
     * @return
     */
    public final SimpleDispatcher execute() {

        // 获取mHt的Looper, 并且构造Handler, 注意的是Looper与ch的是不一样的.
        Handler htHandler = new Handler(HT.getLooper());

        onPreExecute();
        htHandler.post(new Runnable() {
            @Override
            public void run() {
                // 后台执行任务,并且将结果投递投UI线程中
                postResultToMainThread(doInBackground());
            }
        });

        return this;
    }

    private void postResultToMainThread(final Bitmap result) {
        // 向UI线程post数据，用以更新UI等操作
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                onPostExecute(result);
            }
        });
    }

}
