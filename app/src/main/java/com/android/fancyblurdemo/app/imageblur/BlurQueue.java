package com.android.fancyblurdemo.app.imageblur;

import android.media.RemoteControlClient;
import android.os.Handler;
import android.os.Looper;

import com.android.fancyblurdemo.volley.Cache;
import com.android.fancyblurdemo.volley.toolbox.ImageLoader;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by kevin.marlow on 3/26/14.
 */
public class BlurQueue {

    /** Used for generating monotonically-increasing sequence numbers for requests. */
    private AtomicInteger mSequenceGenerator = new AtomicInteger();

    /**
     * The set of all requests currently being processed by this RequestQueue. A Request
     * will be in this set if it is waiting in any queue or currently being processed by
     * any dispatcher.
     */
    private final Set<BlurRequest> mCurrentRequests = new HashSet<BlurRequest>();

    /** The cache triage queue. */
    private final PriorityBlockingQueue<BlurRequest> mBlurQueue =
            new PriorityBlockingQueue<BlurRequest>();

    /** Cache interface for retrieving blurred images. */
    private final ImageLoader.ImageCache mBlurCache;

    /** Response delivery mechanism. */
    private final BlurResponseDelivery mDelivery;

    /** The cache dispatcher. */
    private BlurDispatcher mBlurDispatcher;

    public BlurQueue(ImageLoader.ImageCache cache, BlurResponseDelivery delivery) {
        mBlurCache = cache;
        mDelivery = delivery;
    }

    public BlurQueue(ImageLoader.ImageCache cache) {
        this(cache, new BlurResponseDelivery(new Handler(Looper.getMainLooper())));
    }

    /**
     * Starts the dispatcher in this queue.
     */
    public void start() {
        stop(); // Make sure any currently running dispatchers are stopped.
        // Create the blur dispatcher and start it.
        mBlurDispatcher = new BlurDispatcher(mBlurQueue, mBlurCache, mDelivery);
        mBlurDispatcher.start();
    }

    /**
     * Stops the blur dispatcher.
     */
    public void stop() {
        if (mBlurDispatcher != null) {
            mBlurDispatcher.quit();
        }
    }

    /**
     * Gets a sequence number.
     */
    public int getSequenceNumber() {
        return mSequenceGenerator.incrementAndGet();
    }
    
    public BlurRequest add(BlurRequest request) {
        request.setBlurQueue(this);
        synchronized (mCurrentRequests) {
            mCurrentRequests.add(request);
        }

        // Process the requests in the order they are added.
        request.setSequence(getSequenceNumber());
        request.addMarker("add-to-queue");

        // Add the request to the queue.
        mBlurQueue.add(request);
        return request;
    }

    /**
     * Called from {@link BlurRequest#finish(String)},
     * indicating that the processing of the given request has finished.
     * @param request
     */
    void finish(BlurRequest request) {
        synchronized (mCurrentRequests) {
            mCurrentRequests.remove(request);
        }
    }

    public ImageLoader.ImageCache getCache() {
        return mBlurCache;
    }
}
