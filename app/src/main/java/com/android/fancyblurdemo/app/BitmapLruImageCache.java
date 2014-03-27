package com.android.fancyblurdemo.app;

import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.fancyblurdemo.volley.toolbox.ImageLoader;


/**
 * Created by kevin.marlow on 2/21/14.
 */
public class BitmapLruImageCache extends LruCache<String, Bitmap> implements ImageLoader.ImageCache {

    public BitmapLruImageCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getByteCount();
    }

    @Override
    public Bitmap getBitmap(String url) {
        return get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap) {
        put(url, bitmap);
    }
}
