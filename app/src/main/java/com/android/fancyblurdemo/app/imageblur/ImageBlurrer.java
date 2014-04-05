package com.android.fancyblurdemo.app.imageblur;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Created by kevin.marlow on 3/26/14.
 */
public class ImageBlurrer {

    /** BlurQueue for dispatching BlurRequests onto. */
    private final BlurQueue mBlurQueue;

    /** Amount of time to wait after first response arrives before delivering all responses. */
    private int mBatchResponseDelayMs = 100;

    /**
     * HashMap of Cache keys -> BatchedImageRequest used to track in-flight requests so
     * that we can coalesce multiple requests to the same URL into a single network request.
     */
    private final HashMap<String, BatchedImageRequest> mInFlightRequests =
            new HashMap<String, BatchedImageRequest>();

    /** HashMap of the currently pending responses (waiting to be delivered). */
    private final HashMap<String, BatchedImageRequest> mBatchedResponses =
            new HashMap<String, BatchedImageRequest>();

    /** Handler to the main thread. */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /** Runnable for in-flight response delivery. */
    private Runnable mRunnable;

    /**
     * Constructs a new ImageBlurrer
     * @param queue The {@link BlurQueue} instance.
     */
    public ImageBlurrer(BlurQueue queue) {
        mBlurQueue = queue;
    }

    /**
     * Interface for the response handlers on image requests.
     *
     * The call flow is this:
     * 1. Upon being  attached to a request, onResponse(response, true) will
     * be invoked to reflect any cached data that was already available. If the
     * data was available, response.getBitmap() will be non-null.
     *
     * 2. After a network response returns, only one of the following cases will happen:
     *   - onResponse(response, false) will be called if the image was loaded.
     *   or
     *   - onErrorResponse will be called if there was an error loading the image.
     */
    public interface ImageBlurListener extends BlurResponse.ErrorListener {
        /**
         * Listens for non-error changes to the blurring of the image.
         *
         * @param response Holds all information pertaining to the request, as well
         * as the bitmap (if it is loaded).
         * @param isImmediate True if this was called during ImageLoader.get() variants.
         * This can be used to differentiate between a cached image loading and a network
         * image loading in order to, for example, run an animation to fade in network loaded
         * images.
         */
        public void onBlurResponse(BlurredImageContainer response, boolean isImmediate);
    }

    /**
     * Issues a bitmap blur request with the given bitmap,
     * and returns a bitmap container that contains all of the data
     * relating to the request (as well as the default image if the requested
     * image is not available).
     * @param bitmapToBlur The bitmap to blur.
     * @param cacheKey The cache key that is associated with the blur request.
     * @param blurListener The listener to call when the remote image is loaded
     * @return A container object that contains all of the properties of the request, as well as
     *     the currently available image (default if remote is not loaded).
     */
    public BlurredImageContainer blur(final Bitmap bitmapToBlur, final String cacheKey, ImageBlurListener blurListener) {
        return this.blur(bitmapToBlur, cacheKey, 0, 0, 0, 0, blurListener);
    }

    /**
     * Issues a bitmap blur request with the given bitmap,
     * and crops to the given view's bounds.
     * @param bitmapToBlur The bitmap to blur.
     * @param cacheKey The cache key that is associated with the blur request.
     * @param cropToView The view whose bounds with be used for cropping.
     * @param blurListener The listener to call when the remote image is loaded
     * @return A container object that contains all of the properties of the request, as well as
     *     the currently available image (default if remote is not loaded).
     */
    public BlurredImageContainer blur(final Bitmap bitmapToBlur, final String cacheKey, final View cropToView, ImageBlurListener blurListener) {
        return this.blur(bitmapToBlur, cacheKey, cropToView.getMeasuredWidth(), cropToView.getMeasuredHeight(), cropToView.getLeft(), cropToView.getTop(), blurListener);
    }

    /**
     * Issues a bitmap blur request with the given bitmap,
     * and crops to the given bounds.
     * @param bitmapToBlur The bitmap to blur.
     * @param cacheKey The cache key that is associated with the blur request.
     * @param cropWidth The crop width.
     * @param cropHeight The crop height.
     * @param leftPos The left position of the crop.
     * @param cropWidth The top position of the crop.
     * @param blurListener The listener to call when the remote image is loaded
     * @return A container object that contains all of the properties of the request, as well as
     *     the currently available image (default if remote is not loaded).
     */
    public BlurredImageContainer blur(final Bitmap bitmapToBlur, final String cacheKey, int cropWidth, int cropHeight, int leftPos, int topPos, ImageBlurListener blurListener) {
        // only fulfill requests that were initiated from the main thread.
        throwIfNotOnMainThread();

        final String blurredKey = createCacheKey(cacheKey);

        // Create an empty container.
        BlurredImageContainer BlurredImageContainer = new BlurredImageContainer(null, blurredKey, blurListener);

        // Check to see if a request is already in-flight.
        BatchedImageRequest request = mInFlightRequests.get(blurredKey);
        if (request != null) {
            // If it is, add this request to the list of listeners.
            request.addContainer(BlurredImageContainer);
            return BlurredImageContainer;
        }

        // The request is not already in flight. Send the new request
        // to the dispatcher and track it.
        BlurRequest newRequest = new BlurRequest(bitmapToBlur, blurredKey, new BlurResponse.Listener() {

            @Override
            public void onResponse(Bitmap response) {
                onGetImageSuccess(blurredKey, response);
            }
        }, new BlurResponse.ErrorListener() {

            @Override
            public void onErrorResponse(BlurError error) {
                onGetImageError(blurredKey, error);
            }
        }, cropWidth, cropHeight, leftPos, topPos);

        mBlurQueue.add(newRequest);
        mInFlightRequests.put(blurredKey, new BatchedImageRequest(newRequest, BlurredImageContainer));
        return BlurredImageContainer;
    }

    /**
     * Sets the amount of time to wait after the first response arrives before delivering all
     * responses. Batching can be disabled entirely by passing in 0.
     * @param newBatchedResponseDelayMs The time in milliseconds to wait.
     */
    public void setBatchedResponseDelay(int newBatchedResponseDelayMs) {
        mBatchResponseDelayMs = newBatchedResponseDelayMs;
    }

    /**
     * Handler for when an image was successfully loaded.
     * @param blurredKey The cache key that is associated with the image request.
     * @param response The bitmap that was returned from the network.
     */
    private void onGetImageSuccess(String blurredKey, Bitmap response) {

        // remove the request from the list of in-flight requests.
        BatchedImageRequest request = mInFlightRequests.remove(blurredKey);

        if (request != null) {
            // Update the response bitmap.
            request.mResponseBitmap = response;

            // Send the batched response
            batchResponse(blurredKey, request);
        }
    }

    /**
     * Handler for when an image failed to load.
     * @param blurredKey The cache key that is associated with the image request.
     */
    private void onGetImageError(String blurredKey, BlurError error) {
        // Notify the requesters that something failed via a null result.
        // Remove this request from the list of in-flight requests.
        BatchedImageRequest request = mInFlightRequests.remove(blurredKey);

        // Set the error for this request
        request.setError(error);

        if (request != null) {
            // Send the batched response
            batchResponse(blurredKey, request);
        }
    }

    /**
     * Container object for all of the data surrounding an image request.
     */
    public class BlurredImageContainer {
        /**
         * The most relevant bitmap for the container. If the image was in cache, the
         * Holder to use for the final bitmap (the one that pairs to the requested URL).
         */
        private Bitmap mBitmap;

        private final ImageBlurListener mListener;

        /** The cache key that was associated with the request */
        private final String mBlurCacheKey;

        /**
         * Constructs a BitmapContainer object.
         * @param bitmap The final bitmap (if it exists).
         * @param cacheKey The cache key that identifies the requested URL for this container.
         * @param listener The ImageBlurListener associated with the bitmap.
         */
        public BlurredImageContainer(Bitmap bitmap, String cacheKey, ImageBlurListener listener) {
            mBitmap = bitmap;
            mBlurCacheKey = cacheKey;
            mListener = listener;
        }

        /**
         * Releases interest in the in-flight request (and cancels it if no one else is listening).
         */
        public void cancelRequest() {
            if (mListener == null) {
                return;
            }

            BatchedImageRequest request = mInFlightRequests.get(mBlurCacheKey);
            if (request != null) {
                boolean canceled = request.removeContainerAndCancelIfNecessary(this);
                if (canceled) {
                    mInFlightRequests.remove(mBlurCacheKey);
                }
            } else {
                // check to see if it is already batched for delivery.
                request = mBatchedResponses.get(mBlurCacheKey);
                if (request != null) {
                    request.removeContainerAndCancelIfNecessary(this);
                    if (request.mContainers.size() == 0) {
                        mBatchedResponses.remove(mBlurCacheKey);
                    }
                }
            }
        }

        /**
         * Returns the bitmap associated with the cacheKey if it has been loaded, null otherwise.
         */
        public Bitmap getBitmap() {
            return mBitmap;
        }
    }

    /**
     * Wrapper class used to map a Request to the set of active BlurredImageContainer objects that are
     * interested in its results.
     */
    private class BatchedImageRequest {
        /** The request being tracked */
        private final BlurRequest mRequest;

        /** The result of the request being tracked by this item */
        private Bitmap mResponseBitmap;

        /** Error if one occurred for this response */
        private BlurError mError;

        /** List of all of the active BlurredImageContainers that are interested in the request */
        private final LinkedList<BlurredImageContainer> mContainers = new LinkedList<BlurredImageContainer>();

        /**
         * Constructs a new BatchedImageRequest object
         * @param request The request being tracked
         * @param container The BlurredImageContainer of the person who initiated the request.
         */
        public BatchedImageRequest(BlurRequest request, BlurredImageContainer container) {
            mRequest = request;
            mContainers.add(container);
        }

        /**
         * Set the error for this response
         */
        public void setError(BlurError error) {
            mError = error;
        }

        /**
         * Get the error for this response
         */
        public BlurError getError() {
            return mError;
        }

        /**
         * Adds another BlurredImageContainer to the list of those interested in the results of
         * the request.
         */
        public void addContainer(BlurredImageContainer container) {
            mContainers.add(container);
        }

        /**
         * Detatches the bitmap container from the request and cancels the request if no one is
         * left listening.
         * @param container The container to remove from the list
         * @return True if the request was canceled, false otherwise.
         */
        public boolean removeContainerAndCancelIfNecessary(BlurredImageContainer container) {
            mContainers.remove(container);
            if (mContainers.size() == 0) {
                mRequest.cancel();
                return true;
            }
            return false;
        }
    }

    /**
     * Starts the runnable for batched delivery of responses if it is not already started.
     * @param blurredKey The cacheKey of the response being delivered.
     * @param request The BatchedImageRequest to be delivered.
     */
    private void batchResponse(String blurredKey, BatchedImageRequest request) {
        mBatchedResponses.put(blurredKey, request);
        // If we don't already have a batch delivery runnable in flight, make a new one.
        // Note that this will be used to deliver responses to all callers in mBatchedResponses.
        if (mRunnable == null) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    for (BatchedImageRequest bir : mBatchedResponses.values()) {
                        for (BlurredImageContainer container : bir.mContainers) {
                            // If one of the callers in the batched request canceled the request
                            // after the response was received but before it was delivered,
                            // skip them.
                            if (container.mListener == null) {
                                continue;
                            }
                            if (bir.getError() == null) {
                                container.mBitmap = bir.mResponseBitmap;
                                container.mListener.onBlurResponse(container, false);
                            } else {
                                container.mListener.onErrorResponse(bir.getError());
                            }
                        }
                    }
                    mBatchedResponses.clear();
                    mRunnable = null;
                }

            };
            // Post the runnable.
            mHandler.postDelayed(mRunnable, mBatchResponseDelayMs);
        }
    }

    private void throwIfNotOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("ImageBlurrer must be invoked from the main thread.");
        }
    }

    /**
     * Creates a cache key.
     * @param originalCacheKey The non-blurred image's cacheKey.
     * @return The cache key string for the blurred image.
     */
    private static String createCacheKey(String originalCacheKey) {
        return String.valueOf(originalCacheKey.hashCode());
    }

    /**
     * Generates a cache key.
     * @return
     */
    private static String generateCacheKey() {
        return UUID.randomUUID().toString();
    }
}
