package com.android.fancyblurdemo.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.fancyblurdemo.app.imageblur.BlurError;
import com.android.fancyblurdemo.app.imageblur.ImageBlurrer;

/**
 * Created by kevin.marlow on 3/26/14.
 */
public class BlurImageView extends ImageView {

    /** Local copy of the ImageBlurrer. */
    private ImageBlurrer mImageBlurrer;

    /** Local copy of the request url. */
    private String mRequestUrl;

    /** Current BlurredImageContainer. */
    private ImageBlurrer.BlurredImageContainer mBlurredImageContainer;
    private Path mClipPath;

    public BlurImageView(Context context) {
        this(context, null);
    }

    public BlurImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BlurImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets URL of the image that should be loaded into this view. Note that calling this will
     * immediately either set the cached image (if available) or the default image specified by
     * {@link com.android.fancyblurdemo.volley.toolbox.NetworkImageView#setDefaultImageResId(int)} on the view.
     *
     * NOTE: If applicable, {@link com.android.fancyblurdemo.volley.toolbox.NetworkImageView#setDefaultImageResId(int)} and
     * {@link com.android.fancyblurdemo.volley.toolbox.NetworkImageView#setErrorImageResId(int)} should be called prior to calling
     * this function.
     *
     * @param bitmapToBlur The bitmap to be blurred.
     * @param requestUrl The request url used to cache the bitmap.
     * @param imageBlurrer ImageBlurrer that will be used the blur the image.
     */
    public void setImageToBlur(Bitmap bitmapToBlur, String requestUrl, ImageBlurrer imageBlurrer) {
        mImageBlurrer = imageBlurrer;
        mRequestUrl = requestUrl;
        // The URL has potentially changed. See if we need to load it.
        loadImageIfNecessary(bitmapToBlur, false);
    }

    public void setImageToBlur(Bitmap bitmapToBlur, String requestUrl, Path clipToPath, ImageBlurrer imageBlurrer) {
        mImageBlurrer = imageBlurrer;
        mRequestUrl = requestUrl;
        mClipPath = clipToPath;
        // The URL has potentially changed. See if we need to load it.
        loadImageIfNecessary(bitmapToBlur, false);
    }

    /**
     * Loads the image for the view if it isn't already loaded.
     * @param isInLayoutPass True if this was invoked from a layout pass, false otherwise.
     */
    void loadImageIfNecessary(final Bitmap bitmap, final boolean isInLayoutPass) {
        int width = getWidth();
        int height = getHeight();

        boolean wrapWidth = false, wrapHeight = false;
        if (getLayoutParams() != null) {
            wrapWidth = getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
            wrapHeight = getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        // if the view's bounds aren't known yet, and this is not a wrap-content/wrap-content
        // view, hold off on loading the image.
        boolean isFullyWrapContent = wrapWidth && wrapHeight;
        if (width == 0 && height == 0 && !isFullyWrapContent) {
            return;
        }


        // if there was an old request in this view, check if it needs to be canceled.
        if (mBlurredImageContainer != null && mBlurredImageContainer.getBitmap() != null) {
            // if the request is from the same URL, return.
            setImageBitmap(bitmap);
            return;
        }

        if (mImageBlurrer != null && !TextUtils.isEmpty(mRequestUrl)) {
            mBlurredImageContainer = mImageBlurrer.blur(bitmap, mRequestUrl, new ImageBlurrer.ImageBlurListener() {
                @Override
                public void onBlurResponse(final ImageBlurrer.BlurredImageContainer blurredResponse, boolean isImmediate) {

                    if (isImmediate && isInLayoutPass) {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                onBlurResponse(blurredResponse, false);
                            }
                        });
                        return;
                    }

                    if (blurredResponse.getBitmap() != null) {
                        if (mClipPath != null) {
                            Bitmap resultingImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                            Canvas canvas = new Canvas(resultingImage);

                            Paint paint = new Paint();
                            paint.setFilterBitmap(true);
                            paint.setAntiAlias(true);

                            canvas.drawPath(mClipPath, paint);

                            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                            Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                            Rect bounds = new Rect();
                            getDrawingRect(bounds);
                            canvas.drawBitmap(blurredResponse.getBitmap(), src, bounds, paint);

                            setImageBitmap(resultingImage);
                        } else {
                            setImageBitmap(blurredResponse.getBitmap());
                        }
                    }
                }

                @Override
                public void onErrorResponse(BlurError error) {
                    // Do nothing.
                }
            });
        } else {
            Log.i("NULL", mImageBlurrer == null ? "ImageBlurrer is null." : "RequestUrl is null.");
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mBlurredImageContainer != null) {
            // If the view was bound to a blur request, cancel it and clear
            // out the image from the view.
            mBlurredImageContainer.cancelRequest();
            setImageBitmap(null);
            // also clear out the container so we can reload the image if necessary.
            mBlurredImageContainer = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }
}
