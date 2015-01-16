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
import android.widget.ImageView;

import org.simple.imageloader.bean.RequestContainer;
import org.simple.imageloader.cache.BitmapCache;
import org.simple.imageloader.cache.MemoryCache;
import org.simple.imageloader.config.DisplayConfig;
import org.simple.imageloader.config.ImageLoaderConfig;
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
    /**
     * 
     */
    volatile BitmapCache mCache = new MemoryCache();

    /**
     * 
     */
    private ImageLoaderConfig mConfig = new ImageLoaderConfig();

    /**
     * 
     */
    ImageDispatcher mDispatcher;

    /**
     * 
     */
    private SimpleImageLoader() {
    }

    /**
     * @return
     */
    public static SimpleImageLoader getInstance() {
        return sInstance;
    }

    /**
     * @param config
     */
    public void init(ImageLoaderConfig config) {
        mConfig = config;
        mCache = mConfig.bitmapCache;
        mDispatcher = new ImageDispatcher(mImageQueue, mCache);
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

        // 将加载图片的操作放到Dispatcher中
        mDispatcher.execute(new RequestContainer(imageView, uri, config, listener));
    }

    /**
     * @author mrsimple
     */
    public static interface ImageListener {
        public void onComplete(ImageView imageView, Bitmap bitmap, String uri);
    }
}
