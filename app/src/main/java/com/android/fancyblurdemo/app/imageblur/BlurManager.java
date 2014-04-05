package com.android.fancyblurdemo.app.imageblur;

import android.content.Context;
import android.graphics.Bitmap;

import com.android.fancyblurdemo.volley.toolbox.ImageLoader;

/**
 * Created by kevin.marlow on 2/21/14.
 */
public class BlurManager {

    private static int DISK_IMAGECACHE_SIZE = 1024*1024*10;
    private static Bitmap.CompressFormat DISK_IMAGECACHE_COMPRESS_FORMAT = Bitmap.CompressFormat.PNG;
    private static int DISK_IMAGECACHE_QUALITY = 100;  //PNG is lossless so quality is ignored but must be provided

    /** Internal instance variable. */
    private static BlurManager sInstance;

    /** The blur queue. */
    private BlurQueue mBlurQueue;

    /**
     * Image blurrer.
     */
    private ImageBlurrer mImageBlurer;

    /**
     * Image cache implementation
     */
    private ImageLoader.ImageCache mImageCache;

    private BlurManager() {
        // no instances
    }

    /**
     * This is the initializer.
     * @param context The application context.
     * @param uniqueName The unique name of the image cache.
     */
    public static void init(Context context, String uniqueName) {
        if (sInstance == null) {
            sInstance = new BlurManager();
            sInstance.mImageCache = new DiskLruImageCache(context, uniqueName, DISK_IMAGECACHE_SIZE, DISK_IMAGECACHE_COMPRESS_FORMAT, DISK_IMAGECACHE_QUALITY);
            sInstance.mBlurQueue = new BlurQueue(sInstance.mImageCache);
            sInstance.mBlurQueue.start();
            sInstance.mImageBlurer = new ImageBlurrer(sInstance.mBlurQueue);
        }
    }

    /**
     * Gets the blur queue from the singleton.
     * @return The BlurQueue.
     * @throws IllegalStateException This is thrown if init has not been called.
     */
    public static BlurQueue getBlurQueue() {
        if (sInstance == null) {
            throw new IllegalStateException("The BlurManager must be initialized.");
        }
        return sInstance.mBlurQueue;
    }

    /**
     * Gets the image blurrer from the singleton.
     * @return The ImageBlurrer.
     * @throws IllegalStateException This is thrown if init has not been called.
     */
    public static ImageBlurrer getImageBlurrer() {
        if (sInstance == null) {
            throw new IllegalStateException("The BlurManager must be initialized.");
        }
        return sInstance.mImageBlurer;
    }

    /**
     * Gets a bitmap from the cache.
     * @param cacheKey The key used for cache lookup.
     * @return The bitmap.
     */
    public static Bitmap getBitmap(String cacheKey) {
        if (sInstance == null) {
            throw new IllegalStateException("You must call init() first.");
        }
        return sInstance.mImageCache.getBitmap(cacheKey);
    }

    /**
     * Puts a bitmap in the cache.
     * @param cacheKey The key used for cache lookup.
     * @param bitmap The bitmap to cache.
     */
    public static void putBitmap(String cacheKey, Bitmap bitmap) {
        if (sInstance == null) {
            throw new IllegalStateException("You must call init() first.");
        }
        sInstance.mImageCache.putBitmap(cacheKey, bitmap);
    }
}
