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
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import org.simple.imageloader.bean.RequestBean;
import org.simple.imageloader.cache.BitmapCache;
import org.simple.imageloader.cache.MemoryCache;
import org.simple.imageloader.cache.NoCache;
import org.simple.imageloader.config.DisplayConfig;
import org.simple.imageloader.config.ImageLoaderConfig;
import org.simple.imageloader.policy.SerialPolicy;
import org.simple.imageloader.request.BitmapRequest;
import org.simple.imageloader.utils.BitmapDecoder;
import org.simple.imageloader.utils.Schema;
import org.simple.net.base.Request.RequestListener;
import org.simple.net.core.RequestQueue;
import org.simple.net.core.SimpleNet;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 图片加载类,支持url和本地图片的uri形式加载.根据图片路径格式来判断是网络图片还是本地图片,如果是网络图片则交给SimpleNet框架来加载，
 * 如果是本地图片那么则交给mExecutorService从sd卡中加载
 * .加载之后直接更新UI，无需用户干预.如果用户设置了缓存策略,那么会将加载到的图片缓存起来.用户也可以设置加载策略，例如顺序加载{@see
 * SerialPolicy}和逆向加载{@see ReversePolicy}.
 * 
 * @author mrsimple
 */
public final class SimpleImageLoader {
    /**
     * 用于加载本地图片的线程池
     */
    ExecutorService mExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime()
            .availableProcessors());

    /**
     * 主线程消息队列的Handler
     */
    final static Handler mUIHandler = new Handler(Looper.getMainLooper());

    /**
     * SimpleImageLoader实例
     */
    private static SimpleImageLoader sInstance;

    /**
     * 网络请求队列
     */
    private RequestQueue mImageQueue = SimpleNet.newRequestQueue();
    /**
     * 缓存
     */
    private volatile BitmapCache mCache = new MemoryCache();

    /**
     * 图片加载配置对象
     */
    private ImageLoaderConfig mConfig;

    /**
     * 
     */
    private SimpleImageLoader() {
    }

    /**
     * 获取ImageLoader单例
     * 
     * @return
     */
    public static SimpleImageLoader getInstance() {
        if (sInstance == null) {
            synchronized (SimpleImageLoader.class) {
                if (sInstance == null) {
                    sInstance = new SimpleImageLoader();
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
        checkConfig();
    }

    private void checkConfig() {
        if (mConfig == null) {
            throw new RuntimeException(
                    "The config of SimpleImageLoader is Null, please call the init(ImageLoaderConfig config) method to initialize");
        }

        if (mConfig.loadPolicy == null) {
            mConfig.loadPolicy = new SerialPolicy();
        }
        if (mCache == null) {
            mCache = new NoCache();
        }

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
        mExecutorService.execute(new Runnable() {
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
                        updateImageView(bean, Schema.URL, response);
                        // 缓存新的图片
                        if (response != null) {
                            saveInCache(bean, response);
                        }
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
        final Bitmap bitmap = decodeBitmapFromPath(bean, imagePath);
        // 缓存新的图片
        if (bitmap != null) {
            saveInCache(bean, bitmap);
        }
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
                updateImageView(bean, schema, bitmap);
            }
        });
    }

    /**
     * 从本地路径中解析Bitmap
     * 
     * @param bean
     * @param imagePath
     * @return
     */
    private Bitmap decodeBitmapFromPath(final RequestBean bean, final String imagePath) {
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
    private void updateImageView(RequestBean bean, Schema schema, Bitmap result) {
        final ImageView imageView = bean.getImageView();
        if (!isImageViewShowing(imageView)) {
            return;
        }

        final String uri = bean.imageUri;
        if (result != null && imageView.getTag().equals(uri)) {
            imageView.setImageBitmap(result);
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

    private void saveInCache(final RequestBean bean, final Bitmap result) {
        mExecutorService.execute(new Runnable() {

            @Override
            public void run() {
                synchronized (mCache) {
                    mCache.put(bean, result);
                }
            }
        });
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

    public void stop() {
        mImageQueue.stop();
        mExecutorService.shutdown();
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
