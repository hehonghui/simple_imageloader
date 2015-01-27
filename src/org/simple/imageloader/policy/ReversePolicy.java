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

package org.simple.imageloader.policy;

import android.graphics.Bitmap;

import org.simple.net.base.Request;
import org.simple.net.base.Request.Priority;

/**
 * 逆序加载策略,即从最后加入队列的请求进行加载
 * 
 * @author mrsimple
 */
public class ReversePolicy implements LoadPolicy {

    @Override
    public int compare(Request<Bitmap> request1, Request<Bitmap> request2) {
        Priority myPriority = request1.getPriority();
        Priority anotherPriority = request2.getPriority();
        // 注意Bitmap请求要先执行最晚加入队列的请求,ImageLoader的策略
        return myPriority.equals(anotherPriority)
                ? request2.getSerialNumber() - request1.getSerialNumber()
                : myPriority.ordinal() - anotherPriority.ordinal();
    }

}
