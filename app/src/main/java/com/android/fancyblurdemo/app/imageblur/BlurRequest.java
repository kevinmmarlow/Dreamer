package com.android.fancyblurdemo.app.imageblur;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

import com.android.fancyblurdemo.volley.VolleyLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kevin.marlow on 3/25/14.
 */
public class BlurRequest implements Comparable<BlurRequest> {

    /** Threshold at which we should log the request (even when debug logging is not enabled). */
    private static final long SLOW_REQUEST_THRESHOLD_MS = 3000;

    /** An event log tracing the lifetime of this request; for debugging. */
    private final MarkerLog mEventLog = MarkerLog.ENABLED ? new MarkerLog() : null;

    /** Sequence number of this request, used to enforce FIFO ordering. */
    private Integer mSequence;

    /** The blur queue this request is associated with. */
    private BlurQueue mBlurQueue;

    /** The listener interface for errors */
    private BlurResponse.ErrorListener mErrorListener;

    /** The listener interface for successful blurs. */
    private BlurResponse.Listener mListener;

    /** The bitmap to blur. We must be careful with memory here. */
    private Bitmap mBitmap;

    /** The cache key associated with the bitmap. */
    private String mCacheKey;

    /** Whether or not this request has been canceled. */
    private boolean mCanceled = false;

    /** Whether or not a response has been delivered for this request yet. */
    private boolean mResponseDelivered = false;

    // A cheap variant of request tracing used to dump slow requests.
    private long mRequestBirthTime = 0;

    /**
     * Pixel metrics associated with the view.
     * Used if we are only blurring part of a bitmap.
     */
    private int mCropWidth = 0;
    private int mCropHeight = 0;
    private int mLeftPosition = 0;
    private int mTopPosition = 0;

    public BlurRequest(Bitmap bitmap, String cacheKey, BlurResponse.Listener listener, BlurResponse.ErrorListener errorListener) {
        this(bitmap, cacheKey, listener, errorListener, 0, 0, 0, 0);
    }

    public BlurRequest(Bitmap bitmap, String cacheKey, BlurResponse.Listener listener, BlurResponse.ErrorListener errorListener, View cropToView) {
        this(bitmap, cacheKey, listener, errorListener, cropToView.getMeasuredWidth(), cropToView.getMeasuredHeight(), cropToView.getLeft(), cropToView.getTop());
    }

    public BlurRequest(Bitmap bitmap, String cacheKey, BlurResponse.Listener listener, BlurResponse.ErrorListener errorListener, int cropWidth, int cropHeight, int leftPosition, int topPosition) {
        mBitmap = bitmap;
        mCacheKey = cacheKey;
        mListener = listener;
        mErrorListener = errorListener;
        mCropWidth = cropWidth;
        mCropHeight = cropHeight;
        mLeftPosition = Math.max(leftPosition, 0);
        mTopPosition = Math.max(topPosition, 0);
    }

    /**
     * Associates this request with a given queue.
     * The queue will be notified when this request has finished.
     *
     * @param blurQueue The BlurQueue.
     * @return This request object.
     */
    public BlurRequest setBlurQueue(BlurQueue blurQueue) {
        mBlurQueue = blurQueue;
        return this;
    }

    /**
     * Returns the cache key associated with the bitmap.
     * @return The cache key.
     */
    public String getCacheKey() {
        return mCacheKey;
    }

    /**
     * Returns the bitmap to blur.
     * @return The bitmap.
     */
    public Bitmap getBitmap() {
        return  mBitmap;
    }

    /**
     * Returns true if the request has been canceled.
     * @return True if canceled.
     */
    public boolean isCanceled() {
        return mCanceled;
    }

    /**
     * Marks the request as canceled. No callback will be delivered.
     */
    public void cancel() {
        mCanceled = true;
    }

    public int getCropWidth() {
        return mCropWidth;
    }

    public int getCropHeight() {
        return mCropHeight;
    }

    public int getLeftPosition() {
        return mLeftPosition;
    }

    public int getTopPosition() {
        return mTopPosition;
    }

    /**
     * Adds an event to this request's event log; for debugging.
     */
    public void addMarker(String tag) {
        if (MarkerLog.ENABLED) {
            mEventLog.add(tag, Thread.currentThread().getId());
        } else if (mRequestBirthTime == 0) {
            mRequestBirthTime = SystemClock.elapsedRealtime();
        }
    }

    /**
     * Sets the sequence number of this request.  Used by {@link BlurQueue}.
     *
     * @return This Request object to allow for chaining.
     */
    public final BlurRequest setSequence(int sequence) {
        mSequence = sequence;
        return this;
    }

    /**
     * Returns the sequence number of this request.
     */
    public final int getSequence() {
        if (mSequence == null) {
            throw new IllegalStateException("getSequence called before setSequence");
        }
        return mSequence;
    }

    /**
     * Notifies the request queue that this request has finished (successfully or with error).
     *
     * <p>Also dumps all events from this request's event log; for debugging.</p>
     */
    void finish(final String tag) {
        if (mBlurQueue != null) {
            mBlurQueue.finish(this);
        }
        if (MarkerLog.ENABLED) {
            final long threadId = Thread.currentThread().getId();
            if (Looper.myLooper() != Looper.getMainLooper()) {
                // If we finish marking off of the main thread, we need to
                // actually do it on the main thread to ensure correct ordering.
                Handler mainThread = new Handler(Looper.getMainLooper());
                mainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        mEventLog.add(tag, threadId);
                        mEventLog.finish(BlurRequest.this.toString());
                    }
                });
                return;
            }

            mEventLog.add(tag, threadId);
            mEventLog.finish(this.toString());
        } else {
            long requestTime = SystemClock.elapsedRealtime() - mRequestBirthTime;
            if (requestTime >= SLOW_REQUEST_THRESHOLD_MS) {
                VolleyLog.d("%d ms: %s", requestTime, this.toString());
            }
        }
    }

    /**
     * Mark this request as having a response delivered on it.  This can be used
     * later in the request's lifetime for suppressing identical responses.
     */
    public void markDelivered() {
        mResponseDelivered = true;
    }

    /**
     * Returns true if this request has had a response delivered for it.
     */
    public boolean hasHadResponseDelivered() {
        return mResponseDelivered;
    }

    /**
     * The given response is guaranteed to be non-null;
     * Responses that fail to blur are not delivered.
     * @param response The blurred response returned by the dispatcher
     */
    protected void deliverResponse(Bitmap response) {
        if (mListener != null) {
            mListener.onResponse(response);
        }
    }

    /**
     * Delivers error message to the ErrorListener that the Request was
     * initialized with.
     *
     * @param error Error details
     */
    public void deliverError(BlurError error) {
        if (mErrorListener != null) {
            mErrorListener.onErrorResponse(error);
        }
    }

    /**
     * Our comparator sorts by
     * sequence number to provide FIFO ordering.
     */
    @Override
    public int compareTo(BlurRequest another) {
        return this.mSequence - another.mSequence;
    }

    @Override
    public String toString() {
        return (mCanceled ? "[X] " : "[ ] ") + getCacheKey() + " " + mSequence;
    }

    /**
     * A simple event log with records containing a name, thread ID, and timestamp.
     */
    static class MarkerLog {
        public static final boolean ENABLED = VolleyLog.DEBUG;

        /** Minimum duration from first marker to last in an marker log to warrant logging. */
        private static final long MIN_DURATION_FOR_LOGGING_MS = 0;

        private static class Marker {
            public final String name;
            public final long thread;
            public final long time;

            public Marker(String name, long thread, long time) {
                this.name = name;
                this.thread = thread;
                this.time = time;
            }
        }

        private final List<Marker> mMarkers = new ArrayList<Marker>();
        private boolean mFinished = false;

        /** Adds a marker to this log with the specified name. */
        public synchronized void add(String name, long threadId) {
            if (mFinished) {
                throw new IllegalStateException("Marker added to finished log");
            }

            mMarkers.add(new Marker(name, threadId, SystemClock.elapsedRealtime()));
        }

        /**
         * Closes the log, dumping it to logcat if the time difference between
         * the first and last markers is greater than {@link #MIN_DURATION_FOR_LOGGING_MS}.
         * @param header Header string to print above the marker log.
         */
        public synchronized void finish(String header) {
            mFinished = true;

            long duration = getTotalDuration();
            if (duration <= MIN_DURATION_FOR_LOGGING_MS) {
                return;
            }

            long prevTime = mMarkers.get(0).time;
            VolleyLog.d("(%-4d ms) %s", duration, header);
            for (Marker marker : mMarkers) {
                long thisTime = marker.time;
                VolleyLog.d("(+%-4d) [%2d] %s", (thisTime - prevTime), marker.thread, marker.name);
                prevTime = thisTime;
            }
        }

        @Override
        protected void finalize() throws Throwable {
            // Catch requests that have been collected (and hence end-of-lifed)
            // but had no debugging output printed for them.
            if (!mFinished) {
                finish("Request on the loose");
                VolleyLog.e("Marker log finalized without finish() - uncaught exit point for request");
            }
        }

        /** Returns the time difference between the first and last events in this log. */
        private long getTotalDuration() {
            if (mMarkers.size() == 0) {
                return 0;
            }

            long first = mMarkers.get(0).time;
            long last = mMarkers.get(mMarkers.size() - 1).time;
            return last - first;
        }
    }
}
