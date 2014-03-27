package com.android.fancyblurdemo.app.imageblur;

import android.graphics.Bitmap;

/**
 * Created by kevin.marlow on 3/26/14.
 */
public class BlurResponse {

    /** Callback interface for delivering blurred responses. */
    public interface Listener {
        /** Called when a response is received. */
        public void onResponse(Bitmap response);
    }

    /** Callback interface for delivering error responses. */
    public interface ErrorListener {
        /**
         * Callback method that an error has been occurred with the
         * provided error code and optional user-readable message.
         */
        public void onErrorResponse(BlurError error);
    }

    /** Returns a successful response containing the blurred result. */
    public static BlurResponse success(Bitmap result /*, Cache.Entry cacheEntry */) {
        return new BlurResponse(result); //, cacheEntry);
    }

    /**
     * Returns a failed response containing the given error code and an optional
     * localized message displayed to the user.
     */
    public static BlurResponse error(BlurError error) {
        return new BlurResponse(error);
    }

    /** Blurred response, or null in the case of error. */
    public final Bitmap result;

//    /** Cache metadata for this response, or null in the case of error. */
//    public final Cache.Entry cacheEntry;

    /** Detailed error information if <code>errorCode != OK</code>. */
    public final BlurError error;

    /** True if this response was a soft-expired one and a second one MAY be coming. */
    public boolean intermediate = false;

    /**
     * Returns whether this response is considered successful.
     */
    public boolean isSuccess() {
        return error == null;
    }


    private BlurResponse(Bitmap result) { //, Cache.Entry cacheEntry) {
        this.result = result;
        this.error = null;
    }

    private BlurResponse(BlurError error) {
        this.result = null;
        this.error = error;
    }
}
