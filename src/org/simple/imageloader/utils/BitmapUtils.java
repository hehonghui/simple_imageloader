/**
 *
 *	created by Mr.Simple, Sep 5, 201410:51:40 AM.
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
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;

public final class BitmapUtils {

    /**
     * 图片大于800 kb 则进行压缩
     */
    private static final float BITMAP_BYTES_LIMIT = 800f;

    /**
     * KB
     */
    private static final int KB = 1024;

    /**
     * <p>
     * 该方法来自google的图片缓存Demo
     * <p>
     * 请查看：http://developer.android.com/training/displaying-bitmaps/index.html
     * <p>
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options}
     * object when decoding bitmaps using the decode* methods from
     * {@link BitmapFactory}. This implementation calculates the closest
     * inSampleSize that will result in the final decoded bitmap having a width
     * and height equal to or larger than the requested width and height. This
     * implementation does not ensure a power of 2 is returned for inSampleSize
     * which can be faster when decoding but results in a larger bitmap which
     * isn't as useful for caching purposes.
     * 
     * @param options An options object with out* params already populated (run
     *            through a decode* method with inJustDecodeBounds==true
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    public static int computeInSmallSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            // Calculate ratios of height and width to requested height and
            // width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value,
            // this will guarantee a final image
            // with both dimensions larger than or equal to the requested
            // height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger
            // inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down
            // further
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    /**
     * @param bmp
     * @return
     */
    public static byte[] bitmapToBytes(Bitmap bmp) {
        return bitmapToBytes(bmp, 800);
    }

    /**
     * 压缩图片, 最小压缩到40 %
     * 
     * @param bmp
     * @param scaleSlop
     * @return
     */
    public static byte[] bitmapToBytes(Bitmap bmp, float scaleSlop) {
        byte[] bmpBytes = new byte[0];
        if (bmp != null && !bmp.isRecycled()) {
            final int bitmapKB = (bmp.getRowBytes() * bmp.getHeight() / KB);
            ByteArrayOutputStream bmpBytesOut = new ByteArrayOutputStream();
            int scale = 100;
            // 缩放
            if (bitmapKB > BITMAP_BYTES_LIMIT) {
                scale = (int) ((BITMAP_BYTES_LIMIT / bitmapKB) * 100);
            }
            if (scale < 40) {
                scale = 40;
            }

            bmp.compress(CompressFormat.JPEG, scale, bmpBytesOut);
            bmpBytes = bmpBytesOut.toByteArray();
            Log.d("", "### 压缩比例 : " +
                    scale + ", 图片大小 : "
                    + (bmpBytes.length / KB) + " kb");
        }

        return bmpBytes;
    }

}
