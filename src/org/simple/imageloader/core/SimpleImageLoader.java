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
import android.util.Log;
import android.widget.ImageView;

import org.simple.imageloader.cache.BitmapCache;
import org.simple.imageloader.cache.MemoryCache;
import org.simple.imageloader.config.DisplayConfig;
import org.simple.imageloader.config.ImageLoaderConfig;
import org.simple.imageloader.request.BitmapRequest;
import org.simple.net.base.Request.RequestListener;
import org.simple.net.core.RequestQueue;

/**
 * @author mrsimple
 */
public class SimpleImageLoader {

    /**
     * 
     */
    private static SimpleImageLoader sInstance = new SimpleImageLoader();

    /**
     * 
     */
    private RequestQueue mImageQueue = RequestQueue.newRequestQueue();

    volatile BitmapCache mCache = new MemoryCache();

    private ImageLoaderConfig mConfig = new ImageLoaderConfig();

    private SimpleImageLoader() {
    }

    public static SimpleImageLoader getInstance() {
        return sInstance;
    }

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
            final DisplayConfig config,
            final ImageListener listener) {

        if (mCache.contains(uri)) {
            Bitmap value = mCache.get(uri);
            if (listener != null) {
                listener.onComplete(imageView, value, uri);
            }

            if (imageView != null && value != null) {
                imageView.setImageBitmap(value);
            }

            return;
        }

        Log.e("", "### image loader 没有缓存  ");

        // 设置tag
        imageView.setTag(uri);
        imageView.setImageResource(mConfig.displayConfig.loadingResId);
        BitmapRequest bitmapRequest = new BitmapRequest(uri, new RequestListener<Bitmap>() {
            @Override
            public void onComplete(int stCode, Bitmap response, String errMsg) {
                Log.e("", "### 执行网络请求 : stCode : " + stCode + ", response : " + response);
                if (stCode == 200 && response != null && imageView.getTag().equals(uri)) {
                    imageView.setImageBitmap(response);
                    mCache.put(uri, response);
                } else if (stCode != 200) {
                    imageView.setImageResource(mConfig.displayConfig.failedResId);
                }

                if (listener != null) {
                    listener.onComplete(imageView, response, uri);
                }
            }

            @Override
            public void onStart() {

            }
        });

        mImageQueue.addRequest(bitmapRequest);
    }

    /**
     * @author mrsimple
     */
    public static interface ImageListener {
        public void onComplete(ImageView imageView, Bitmap bitmap, String uri);
    }
}
