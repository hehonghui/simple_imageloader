/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2015 bboyfeiyu@gmail.com, Inc
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
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import org.simple.imageloader.bean.RequestBean;
import org.simple.imageloader.cache.BitmapCache;
import org.simple.imageloader.cache.MemoryCache;
import org.simple.imageloader.config.DisplayConfig;
import org.simple.imageloader.config.ImageLoaderConfig;
import org.simple.imageloader.request.BitmapRequest;
import org.simple.imageloader.utils.BitmapDecoder;
import org.simple.imageloader.utils.Schema;
import org.simple.net.base.Request.RequestListener;
import org.simple.net.core.RequestQueue;
import org.simple.net.core.SimpleNet;

import java.io.File;

/**
 * 图片加载类
 * 
 * @author mrsimple
 */
public final class SimpleImageLoader {

    /**
     * HandlerThread内部封装了自己的Handler和Thead，有单独的Looper和消息队列
     */
    private static final HandlerThread sThread = new HandlerThread(
            SimpleImageLoader.class.getName(),
            android.os.Process.THREAD_PRIORITY_DEFAULT);
    /**
     * 获取mHt的Looper, 并且构造Handler, 注意的是Looper与ch的是不一样的.
     */
    private static Handler sThreadHandler;

    /**
     * 主线程消息队列的Handler
     */
    final static Handler mUIHandler = new Handler(Looper.getMainLooper());

    /**
     * 
     */
    private static SimpleImageLoader sInstance;

    /**
     * 
     */
    private RequestQueue mImageQueue = SimpleNet.newRequestQueue();
    /**
     * 
     */
    private volatile BitmapCache mCache = new MemoryCache();

    /**
     * 
     */
    private ImageLoaderConfig mConfig;

    /**
     * 
     */
    private SimpleImageLoader() {
    }

    /**
     * @return
     */
    public static SimpleImageLoader getInstance() {
        if (sInstance == null) {
            synchronized (SimpleImageLoader.class) {
                if (sInstance == null) {
                    sInstance = new SimpleImageLoader();
                    // 启动HandlerThread
                    sThread.start();
                    // 获取到HandlerThread的消息队列
                    sThreadHandler = new Handler(sThread.getLooper());
                }
            }
        }
        return sInstance;
    }

    /**
     * @param config
     */
    public void init(ImageLoaderConfig config) {
        mConfig = config;
        mCache = mConfig.bitmapCache;
    }

    public void displayImage(ImageView imageView, String uri) {
        displayImage(imageView, uri, null, null);
    }

    public void displayImage(ImageView imageView, String uri, DisplayConfig config) {
        displayImage(imageView, uri, config, null);
    }

    public void displayImage(ImageView imageView, String uri, ImageListener listener) {
        displayImage(imageView, uri, null, listener);
    }

    public void displayImage(final ImageView imageView, final String uri,
            final DisplayConfig config, final ImageListener listener) {
        // 将加载图片的操作放到队列中执行
        this.execute(new RequestBean(imageView, uri, config, listener));
    }

    /**
     * 执行数据库操作,可在此函数中添加整体的事物操作,避免性能问题. [ 目前还没有进行优化的必要 ]
     * 
     * @return
     */
    private final void execute(final RequestBean bean) {
        sThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                // 后台执行任务,并且将结果投递投UI线程中
                doInBackground(bean);
            }
        });
    }

    /**
     * 执行耗时操作
     * 
     * @param bean
     */
    protected void doInBackground(final RequestBean bean) {
        Log.e("", "#### 图片加载 ----->  : " + bean.imageUri);
        bean.displayConfig = bean.displayConfig != null ? bean.displayConfig
                : mConfig.displayConfig;
        // get from cache
        final Bitmap bitmap = mCache.get(bean);
        Log.e("", "### 缓存图片 = " + bitmap);

        Schema schema = bitmap != null ? Schema.CACHE : Schema.getSchema(bean.imageUri);
        switch (schema) {
            case URL:
                fetchBitmapFromUrl(bean);
                break;

            case FILE:
                getBitmapFromLocal(bean);
                break;

            case CACHE:
                deliveryToUIThread(bean, Schema.CACHE, bitmap);
                break;

            default:
                break;
        }
    }

    /**
     * 从网络上加载图片
     * 
     * @param bean
     */
    private void fetchBitmapFromUrl(final RequestBean bean) {
        final ImageView imageView = bean.getImageView();
        if (!isImageViewShowing(imageView)) {
            return;
        }

        if (hasLoadingPlaceholder(bean.displayConfig)) {
            mUIHandler.post(new Runnable() {

                @Override
                public void run() {
                    imageView.setImageResource(bean.displayConfig.loadingResId);
                }
            });
        }

        BitmapRequest bitmapRequest = new BitmapRequest(bean.imageUri, new
                RequestListener<Bitmap>() {
                    @Override
                    public void onComplete(int stCode, Bitmap response, String errMsg) {
                        updateImageViewIfNeed(bean, Schema.URL, response);
                    }
                });

        mImageQueue.addRequest(bitmapRequest);
    }

    /**
     * 从本地加载图片
     * 
     * @param bean
     */
    private void getBitmapFromLocal(final RequestBean bean) {
        final String imagePath = Uri.parse(bean.imageUri).getPath();
        final Bitmap bitmap = decodeBitmap(bean, imagePath);

        Log.e("", "### thread name = " + Thread.currentThread().getName());
        // 在UI线程更新ImageView
        deliveryToUIThread(bean, Schema.FILE, bitmap);
    }

    /**
     * 将结果投递到UI,更新ImageView
     * 
     * @param bean
     * @param bitmap
     */
    private void deliveryToUIThread(final RequestBean bean, final Schema schema, final Bitmap bitmap) {
        mUIHandler.post(new Runnable() {

            @Override
            public void run() {
                updateImageViewIfNeed(bean, schema, bitmap);
            }
        });
    }

    /**
     * 解析Bitmap
     * 
     * @param bean
     * @param imagePath
     * @return
     */
    private Bitmap decodeBitmap(final RequestBean bean, final String imagePath) {
        final File imgFile = new File(imagePath);
        if (!imgFile.exists()) {
            return null;
        }

        BitmapDecoder decoder = new BitmapDecoder() {

            @Override
            public Bitmap decodeBitmapWithOption(Options options) {
                return BitmapFactory.decodeFile(imagePath, options);
            }
        };

        return decoder.decodeBitmap(bean.getImageViewWidth(),
                bean.getImageViewHeight());
    }

    /**
     * 更新ImageView
     * 
     * @param bean
     * @param result
     */
    private void updateImageViewIfNeed(RequestBean bean, Schema schema, Bitmap result) {
        final ImageView imageView = bean.getImageView();
        if (!isImageViewShowing(imageView)) {
            return;
        }

        final String uri = bean.imageUri;
        if (result != null && imageView.getTag().equals(uri)) {
            imageView.setImageBitmap(result);
            if (schema != Schema.CACHE) {
                mCache.put(bean, result);
            }
        }

        // 加载失败
        if (result == null && hasFaildPlaceholder(bean.displayConfig)) {
            imageView.setImageResource(bean.displayConfig.failedResId);
        }

        // 回调接口
        if (bean.imageListener != null) {
            bean.imageListener.onComplete(imageView, result, uri);
        }
    }

    private boolean hasLoadingPlaceholder(DisplayConfig displayConfig) {
        return displayConfig != null && displayConfig.loadingResId > 0;
    }

    private boolean hasFaildPlaceholder(DisplayConfig displayConfig) {
        return displayConfig != null && displayConfig.failedResId > 0;
    }

    private boolean isImageViewShowing(ImageView imageView) {
        return imageView != null && imageView.isShown();
    }

    public ImageLoaderConfig getConfig() {
        return mConfig;
    }

    /**
     * 图片加载Listener
     * 
     * @author mrsimple
     */
    public static interface ImageListener {
        public void onComplete(ImageView imageView, Bitmap bitmap, String uri);
    }
}
