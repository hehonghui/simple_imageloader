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
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import org.simple.imageloader.bean.RequestContainer;
import org.simple.imageloader.cache.BitmapCache;
import org.simple.imageloader.request.BitmapRequest;
import org.simple.imageloader.utils.BitmapDecoder;
import org.simple.imageloader.utils.Schema;
import org.simple.net.base.Request.RequestListener;
import org.simple.net.core.RequestQueue;

/**
 * 图片请求分发器
 * 
 * @author mrsimple
 */
final class ImageDispatcher {

    // HandlerThread内部封装了自己的Handler和Thead，有单独的Looper和消息队列
    private static final HandlerThread HT = new HandlerThread(
            ImageDispatcher.class.getName(),
            android.os.Process.THREAD_PRIORITY_BACKGROUND);
    /**
     * 主线程消息队列的Handler
     */
    final static Handler mUIHandler = new Handler(Looper.getMainLooper());
    /**
     * 启动HandlerThread
     */
    static {
        HT.start();
    }

    // 获取mHt的Looper, 并且构造Handler, 注意的是Looper与ch的是不一样的.
    Handler mThreadHandler = new Handler(HT.getLooper());
    /**
     * 网络请求队列,执行图片请求
     */
    RequestQueue mRequestQueue;
    /**
     * 图片缓存
     */
    BitmapCache mBitmapCache;

    /**
     * @param queue 网络请求队列
     * @param cache 缓存
     */
    public ImageDispatcher(RequestQueue queue, BitmapCache cache) {
        mRequestQueue = queue;
        mBitmapCache = cache;
    }

    /**
     * 执行耗时操作
     * 
     * @param container
     */
    protected void doInBackground(final RequestContainer container) {
        
        Log.e("", "#### 图片加载 ----->  : " + container.imageUri) ;
        // get from cache
        final Bitmap bitmap = mBitmapCache.get(container.imageUri);
        Schema schema = bitmap != null ? Schema.CACHE : Schema.getSchema(container.imageUri);
        switch (schema) {
            case URL:
                fetchBitmapFromUrl(container);
                break;

            case FILE:
                getBitmapFromLocal(container);
                break;

            case CACHE:
                deliveryToUIThread(container, bitmap);
                break;

            default:
                break;
        }
    }

    /**
     * 从网络上加载图片
     * 
     * @param container
     */
    private void fetchBitmapFromUrl(final RequestContainer container) {
        final ImageView imageView = container.getImageView();
        if (!isImageViewShowing(imageView)) {
            return;
        }

        if (container.displayConfig != null) {
            imageView.setImageResource(container.displayConfig.loadingResId);
        }

        BitmapRequest bitmapRequest = new BitmapRequest(container.imageUri, new
                RequestListener<Bitmap>() {
                    @Override
                    public void onComplete(int stCode, Bitmap response, String errMsg) {
                        Log.e("", "### 执行网络请求 : stCode : " + stCode + ", response : " +
                                response);
                        updateImageViewIfNeed(container, response);
                    }
                });

        mRequestQueue.addRequest(bitmapRequest);

    }

    /**
     * 从本地加载图片
     * 
     * @param container
     */
    private void getBitmapFromLocal(final RequestContainer container) {
        final String imagePath = Uri.parse(container.imageUri).getPath();
        // 在UI线程更新ImageView
        deliveryToUIThread(container, decodeBitmap(container, imagePath));

    }

    /**
     * 将结果投递到UI,更新ImageView
     * 
     * @param container
     * @param bitmap
     */
    private void deliveryToUIThread(final RequestContainer container, final Bitmap bitmap) {
        mUIHandler.post(new Runnable() {

            @Override
            public void run() {
                updateImageViewIfNeed(container, bitmap);
            }
        });
    }

    /**
     * 解析Bitmap
     * 
     * @param container
     * @param imagePath
     * @return
     */
    private Bitmap decodeBitmap(final RequestContainer container, final String imagePath) {
        BitmapDecoder decoder = new BitmapDecoder() {

            @Override
            public Bitmap decodeBitmapWithOption(Options options) {
                return BitmapFactory.decodeFile(imagePath, options);
            }
        };

        return decoder.decodeBitmap(container.getImageViewWidth(),
                container.getImageViewHeight());
    }

    /**
     * 更新ImageView
     * 
     * @param container
     * @param result
     */
    private void updateImageViewIfNeed(RequestContainer container, Bitmap result) {
        final ImageView imageView = container.getImageView();
        if (!isImageViewShowing(imageView)) {
            return;
        }

        final String uri = container.imageUri;
        if (result != null && imageView.getTag().equals(uri)) {
            imageView.setImageBitmap(result);
            mBitmapCache.put(uri, result);
        } else {
            if (container.displayConfig != null) {
                imageView.setImageResource(container.displayConfig.failedResId);
            } else {
                imageView.setImageDrawable(new ColorDrawable(Color.LTGRAY));
            }

        }

        // 回调接口
        if (container.imageListener != null) {
            container.imageListener.onComplete(imageView, result, uri);
        }
    }

    private boolean isImageViewShowing(ImageView imageView) {
        return imageView != null && imageView.isShown();
    }

    /**
     * 执行数据库操作,可在此函数中添加整体的事物操作,避免性能问题. [ 目前还没有进行优化的必要 ]
     * 
     * @return
     */
    public final ImageDispatcher execute(final RequestContainer requestContainer) {
        mThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                // 后台执行任务,并且将结果投递投UI线程中
                doInBackground(requestContainer);
            }
        });

        return this;
    }

}
