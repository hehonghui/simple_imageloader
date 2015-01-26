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

package org.simple.imageloader.request;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.simple.imageloader.core.SimpleImageLoader;
import org.simple.net.base.Request;
import org.simple.net.base.Response;

/**
 * @author mrsimple
 */
public class BitmapRequest extends Request<Bitmap> {

    public BitmapRequest(String url, RequestListener<Bitmap> listener) {
        super(HttpMethod.GET, url, listener);
    }

    @Override
    public Bitmap parseResponse(Response response) {
        if (response == null) {
            return null;
        }
        return BitmapFactory.decodeByteArray(response.getRawData(), 0,
                response.getRawData().length);
    }

    @Override
    public int compareTo(Request<Bitmap> another) {
        return SimpleImageLoader.getInstance().getConfig().loadPolicy.compare(this, another);
    }

}
