/**
 *
 *	created by Mr.Simple, Sep 5, 201411:09:38 AM.
 *	Copyright (c) 2014, hehonghui@umeng.com All Rights Reserved.
 *
 *                #####################################################
 *                #                                                   #
 *                #                       _oo0oo_                     #   
 *                #                      o8888888o                    #
 *                #                      88" . "88                    #
 *                #                      (| -_- |)                    #
 *                #                      0\  =  /0                    #   
 *                #                    ___/`---'\___                  #
 *                #                  .' \\|     |# '.                 #
 *                #                 / \\|||  :  |||# \                #
 *                #                / _||||| -:- |||||- \              #
 *                #               |   | \\\  -  #/ |   |              #
 *                #               | \_|  ''\---/''  |_/ |             #
 *                #               \  .-\__  '-'  ___/-. /             #
 *                #             ___'. .'  /--.--\  `. .'___           #
 *                #          ."" '<  `.___\_<|>_/___.' >' "".         #
 *                #         | | :  `- \`.;`\ _ /`;.`/ - ` : | |       #
 *                #         \  \ `_.   \_ __\ /__ _/   .-` /  /       #
 *                #     =====`-.____`.___ \_____/___.-`___.-'=====    #
 *                #                       `=---='                     #
 *                #     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   #
 *                #                                                   #
 *                #               佛祖保佑         永无BUG              #
 *                #                                                   #
 *                #####################################################
 */

package org.simple.imageloader.utils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.Log;

/**
 * @author mrsimple
 */
public abstract class BitmapDecoder {

    /**
     * @param options
     * @return
     */
    public abstract Bitmap decodeBitmapWithOption(Options options);

    /**
     * @param width 图片的目标宽度
     * @param height 图片的目标高度
     * @return
     */
    public Bitmap decodeBitmap(int width, int height) {
        return decodeBitmap(width, height, false, false);
    }

    /**
     * @param width 图片的目标宽度
     * @param height 图片的目标高度
     * @param origiBitmap 是否直接解析原图
     * @return
     */
    public Bitmap decodeBitmap(int width, int height, boolean origiBitmap) {
        return decodeBitmap(width, height, origiBitmap, false);
    }

    /**
     * 
     */
    public Bitmap decodeBitmap(int width, int height, boolean origiBitmap, boolean lowerQuality) {

        // 如果请求原图,则直接加载原图
        if (origiBitmap || width <= 0 || height <= 0) {
            return decodeBitmapWithOption(null);
        }

        // 获取只加载Bitmap宽高等数据的Option, 即设置options.inJustDecodeBounds = true;
        BitmapFactory.Options options = getJustDecodeBoundsOptions();
        // 通过options加载bitmap，此时返回的bitmap为空,数据将存储在options中
        decodeBitmapWithOption(options);
        // 计算缩放比例, 并且将options.inJustDecodeBounds设置为false;
        calculateInSmall(options, width, height, lowerQuality);
        // 通过options设置的缩放比例加载图片
        return decodeBitmapWithOption(options);
    }

    /**
     * @return
     */
    protected Options getJustDecodeBoundsOptions() {
        //
        BitmapFactory.Options options = new BitmapFactory.Options();
        // 设置为true,表示解析Bitmap对象，该对象不占内存
        options.inJustDecodeBounds = true;
        return options;
    }

    /**
     * @param options
     * @param width
     * @param height
     * @param lowQuality
     */
    protected void calculateInSmall(Options options, int width, int height, boolean lowQuality) {

        // 设置缩放比例
        options.inSampleSize = BitmapUtils.computeInSmallSize(options, width, height);

        Log.d("", "$## inSampleSize = " + options.inSampleSize + ", width = " + width
                + ", height= "
                + height);
        if (lowQuality) {
            Log.d("", "@@@ origin, options.inSampleSize = " + options.inSampleSize);
            // PS:为了减少内存，可以将缩放比例调的更大一些，这样就不会导致系统频繁GC的情况了
            options.inSampleSize += options.inSampleSize / 3;
            Log.d("", "@@@ lowQuality, options.inSampleSize = " + options.inSampleSize);
        }
        
        // 图片质量
        options.inPreferredConfig = Config.RGB_565;

        // 设置为false,解析Bitmap对象加入到内存中
        options.inJustDecodeBounds = false;

        options.inPurgeable = true;
        options.inInputShareable = true;
    }

}
