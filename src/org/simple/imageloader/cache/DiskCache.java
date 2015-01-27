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

package org.simple.imageloader.cache;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.jakewharton.disklrucache.DiskLruCache;
import com.jakewharton.disklrucache.DiskLruCache.Snapshot;
import com.jakewharton.disklrucache.Util;

import org.simple.imageloader.bean.RequestBean;
import org.simple.imageloader.utils.BitmapDecoder;
import org.simple.imageloader.utils.Md5Helper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author mrsimple
 */
public class DiskCache extends BitmapCache {

    /**
     * 1MB
     */
    private static final int MB = 1024 * 1024;

    /**
     * 
     */
    private static final String IMAGE_DISK_CACHE = "bitmap";

    private DiskLruCache mDiskLruCache;

    private static DiskCache mDiskCache;

    /**
     * @param context
     */
    private DiskCache(Context context) {
        initDiskCache(context);
    }

    public static DiskCache getDiskCache(Context context) {
        if (mDiskCache == null) {
            synchronized (DiskCache.class) {
                if (mDiskCache == null) {
                    mDiskCache = new DiskCache(context);
                }
            }

        }
        return mDiskCache;
    }

    /**
     * 初始化sdcard缓存
     */
    private void initDiskCache(Context context) {
        try {
            File cacheDir = getDiskCacheDir(context, IMAGE_DISK_CACHE);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            Toast.makeText(context, "init ", Toast.LENGTH_SHORT).show();

            DiskCache.cacheDir = cacheDir.getAbsolutePath();
            Log.d("", "### 缓存路径  :" + cacheDir);
            mDiskLruCache = DiskLruCache
                    .open(cacheDir, getAppVersion(context), 1, 50 * MB);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String cacheDir = "";

    /**
     * @param context
     * @param uniqueName
     * @return
     */
    public File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.d("", "### context : " + context + ", dir = " + context.getExternalCacheDir());
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * @param context
     * @return
     */
    private int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    0);
            return info.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    @Override
    public synchronized Bitmap get(RequestBean container) {

        final String md5 = Md5Helper.toMD5(container.imageUri);
        // 图片解析器
        BitmapDecoder decoder = new BitmapDecoder() {

            @Override
            public Bitmap decodeBitmapWithOption(Options options) {
                final InputStream inputStream = getInputStream(md5);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                Util.closeQuietly(inputStream);
                return bitmap;
            }
        };

        return decoder.decodeBitmap(container.getImageViewWidth(),
                container.getImageViewHeight());

    }

    private InputStream getInputStream(String md5) {
        Snapshot snapshot;
        try {
            snapshot = mDiskLruCache.get(md5);
            if (snapshot != null) {
                return snapshot.getInputStream(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void put(RequestBean key, Bitmap value) {
        DiskLruCache.Editor editor = null;
        try {
            // 如果没有找到对应的缓存，则准备从网络上请求数据，并写入缓存
            editor = mDiskLruCache.edit(Md5Helper.toMD5(key.imageUri));
            if (editor != null) {
                OutputStream outputStream = editor.newOutputStream(0);
                if (writeBitmapToDisk(value, outputStream)) {
                    // 写入disk缓存
                    editor.commit();
                    // mDiskLruCache.flush();
                } else {
                    editor.abort();
                }
                Util.closeQuietly(outputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean writeBitmapToDisk(Bitmap bitmap, OutputStream outputStream) {
        BufferedOutputStream bos = new BufferedOutputStream(outputStream, 8 * 1024);
        bitmap.compress(CompressFormat.JPEG, 100, bos);
        try {
            bos.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Util.closeQuietly(bos);
        }

        return false;
    }

    @Override
    public void remove(RequestBean key) {
        try {
            mDiskLruCache.remove(Md5Helper.toMD5(key.imageUri));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
