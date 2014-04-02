package com.android.fancyblurdemo.app;

import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;

import com.android.fancyblurdemo.app.imageblur.BlurManager;
import com.android.fancyblurdemo.volley.VolleyError;
import com.android.fancyblurdemo.volley.toolbox.ImageLoader;
import com.android.fancyblurdemo.volley.toolbox.NetworkImageView;

/**
 * The photo fragment. This class handles downloading and blurring of the photos,
 * as well as animating the title TextView.
 *
 * Created by kevin.marlow on 4/2/14.
 */
public class PhotoFragment extends Fragment implements View.OnClickListener {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String ARG_IMAGE_SHOWN = "image_shown";

    private FlickrPhoto mCurrentPhoto;
    private NetworkImageView mImageView;
    private BlurImageView mBlurImageView;
    private ProgressBar mProgressBar;
    private RobotoTextView mTitleText;
    private View mOverlay;

    // Animations and checks.
    private Animation mFadeIn;
    private Animation mFadeInWithDelay;
    private Animation mFadeOut;
    private TouchAnimListener mAnimListener;
    private boolean mIsAnimating = false;
    private boolean mIsTitleShown = false;
    private boolean mIsImageShown = false;
    private boolean mHasHadCallback = false;
//    private VelocityTracker mVelocityTracker;
//    private float maxYVelocity;
//    private int initialY;
//    private float initialTouchY;
//    private int mToValue = 1;
//    private Spring mSpring;
//    private int finalY;
//    private long mAnimStartTime;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static PhotoFragment newInstance(int sectionNumber) {
        PhotoFragment fragment = new PhotoFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putBoolean(ARG_IMAGE_SHOWN, false);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * No-op required by FragmentManager.
     * @see
     */
    public PhotoFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        rootView.setOnClickListener(this);

        mCurrentPhoto = MainActivity.photos.get(getArguments().getInt(ARG_SECTION_NUMBER));
        mIsImageShown = getArguments().getBoolean(ARG_IMAGE_SHOWN, false);

        mImageView = (NetworkImageView) rootView.findViewById(R.id.flickrView);
        mBlurImageView = (BlurImageView) rootView.findViewById(R.id.blurView);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        mOverlay = rootView.findViewById(R.id.overlay);

        mTitleText = (RobotoTextView) rootView.findViewById(R.id.titleText);
        mTitleText.setText(mCurrentPhoto.title);
//        mTitleText.setOnTouchListener(this);

//        SpringSystem springSystem = SpringSystem.create();
//        mSpring = springSystem.createSpring();
//        mSpring.addListener(new SimpleSpringListener() {
//
//            @Override
//            public void onSpringUpdate(Spring spring) {
//                int mappedY = (int) SpringUtil.mapValueFromRangeToRange(spring.getCurrentValue(), 1 - mToValue, mToValue, finalY, -mTitleText.getHeight());
//                Log.i("TAG", "Updating from " + finalY + " to " + mappedY);
//                RelativeLayout.LayoutParams layoutParams = ((RelativeLayout.LayoutParams) mTitleText.getLayoutParams());
//                layoutParams.topMargin = mappedY;
//                mTitleText.setLayoutParams(layoutParams);
//                if (spring.getCurrentValue() == mToValue) {
//                    mToValue = 1 - mToValue;
//                }
//            }
//        });

        TextPaint paint = mTitleText.getPaint();

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        int width = 0;
        int height = 0;
        int titleTextMargin = (int) getResources().getDimension(R.dimen.title_text_margin);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            display.getSize(size);
            width = size.x - 2 * titleTextMargin;
            height = size.y - 2 * titleTextMargin;
        } else {
            width = display.getWidth() - 2 * titleTextMargin;
            height = display.getHeight() - 2 * titleTextMargin;
        }
        new PreCompositeTextTask(paint, width, height).execute(mCurrentPhoto.title, mCurrentPhoto.preferredWidthStr);

        mTitleText.setVisibility(View.GONE);

        mFadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
        mFadeInWithDelay = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in_with_delay);
        mFadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);

        mAnimListener = new TouchAnimListener();

        mFadeIn.setAnimationListener(mAnimListener);
        mFadeInWithDelay.setAnimationListener(mAnimListener);
        mFadeOut.setAnimationListener(mAnimListener);

        mImageView.setImageListener(new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                if (response.getBitmap() != null && !TextUtils.isEmpty(response.getRequestUrl())) {
                    mOverlay.setVisibility(View.VISIBLE);
                    Animation fullFadeIn = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
                    fullFadeIn.setFillAfter(true);
                    mOverlay.startAnimation(fullFadeIn);
                    mBlurImageView.setImageToBlur(response.getBitmap(), response.getRequestUrl(), BlurManager.getImageBlurrer());
                    mBlurImageView.setImageAlpha(0);
                    mProgressBar.setVisibility(View.GONE);
                    mIsImageShown = true;
                    if (!mIsTitleShown && mHasHadCallback) {
                        mTitleText.setVisibility(View.VISIBLE);
                        mTitleText.startAnimation(mFadeInWithDelay);
                        mIsTitleShown = true;
                    }
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                // Do nothing.
            }
        });

        mImageView.setImageUrl(MainActivity.sUseHighRes ? mCurrentPhoto.highResUrl : mCurrentPhoto.photoUrl, VolleyManager.getImageLoader(), !mIsImageShown);

        return rootView;
    }

    /**
     * Set the blurred image alpha. This is called during
     * {@link android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrolled(int, float, int)}.
     *
     * @param alpha The alpha value.
     */
    public void setPageAlpha(float alpha) {
        mBlurImageView.setImageAlpha((int) (255 * alpha));
    }

    /**
     * Fade the title in if it is not shown.
     */
    public void showTitle() {
        mHasHadCallback = true;
        if (!mIsTitleShown && mIsImageShown) {
            mTitleText.setVisibility(View.VISIBLE);
            mTitleText.startAnimation(mFadeInWithDelay);
            mIsTitleShown = true;
        }
    }

    @Override
    public void onClick(View v) {
        if (!mIsAnimating) {
            if (mIsTitleShown) {
                mTitleText.startAnimation(mFadeOut);
                mIsTitleShown = false;
            } else if (mHasHadCallback && mIsImageShown) {
                mTitleText.startAnimation(mFadeIn);
                mIsTitleShown = true;
            }
        } else {
//            // Animation is in progress, reverse it!
//            if (mIsTitleShown) {
//                // Going to a shown state, let's hide it!
//                AlphaAnimation fadeOutPartial = new AlphaAnimation(mTitleText.getAlpha(), 0);
//                fadeOutPartial.setAnimationListener(new TouchAnimListener());
//                fadeOutPartial.setDuration(System.currentTimeMillis() - mAnimStartTime);
//                fadeOutPartial.setInterpolator(new DecelerateInterpolator());
//                fadeOutPartial.setFillAfter(true);
//                fadeOutPartial.setFillBefore(true);
////                mTitleText.clearAnimation();
//                mTitleText.startAnimation(fadeOutPartial);
//                mIsTitleShown = false;
//            } else {
//                // Going to a hidden state.
//                AlphaAnimation fadeInPartial = new AlphaAnimation(mTitleText.getAlpha(), (float) 0.3);
//                fadeInPartial.setAnimationListener(new TouchAnimListener());
//                fadeInPartial.setDuration(System.currentTimeMillis() - mAnimStartTime);
//                fadeInPartial.setInterpolator(new DecelerateInterpolator());
//                fadeInPartial.setFillAfter(true);
//                fadeInPartial.setFillBefore(true);
////                mTitleText.clearAnimation();
//                mTitleText.startAnimation(fadeInPartial);
//                mIsTitleShown = true;
//            }
        }
//        mAnimStartTime = System.currentTimeMillis();
    }

//    @Override
//    public boolean onTouch(View v, MotionEvent event) {
//
//        final int action = event.getAction() & MotionEventCompat.ACTION_MASK;
//
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                if (mVelocityTracker == null) {
//                    mVelocityTracker = VelocityTracker.obtain();
//                } else {
//                    mVelocityTracker.clear();
//                }
//                mVelocityTracker.addMovement(event);
//                maxYVelocity = 0;
//
//                Log.d("TAG", "Y-velocity (pixel/s): 0");
//                Log.d("TAG", "max. Y-velocity: 0");
//
//                RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
//                initialY = finalY = lParams.topMargin;
//                initialTouchY = event.getY();
//
//                return true;
//            case MotionEvent.ACTION_MOVE:
//
//                mVelocityTracker.addMovement(event);
//                mVelocityTracker.computeCurrentVelocity(1000);
//                //1000 provides pixels per second
//
//                float yVelocity = mVelocityTracker.getYVelocity();
//                maxYVelocity = (yVelocity + maxYVelocity) / 2;
//
//                Log.d("TAG", "Y-velocity (pixel/s): " + yVelocity);
//                Log.d("TAG", "max. Y-velocity: " + maxYVelocity);
//
//                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
//                int dY = (int) (event.getRawY() - initialTouchY);
//
//                layoutParams.topMargin = finalY = initialY + dY;
//                v.setLayoutParams(layoutParams);
//
//                return true;
//            case MotionEvent.ACTION_UP:
//            case MotionEvent.ACTION_CANCEL:
//                // TODO: Do velocity spring
////                mSpring.setVelocity(maxYVelocity);
////                mSpring.setEndValue(mToValue);
//                if (mVelocityTracker != null) {
//                    mVelocityTracker.recycle();
//                    mVelocityTracker = null;
//                }
//                return false;
//        }
//        return false;
//    }

    // Disable user interaction while animating, mostly for elegance.
    private class TouchAnimListener implements Animation.AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {
            mIsAnimating = true;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mIsAnimating = false;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {}
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_IMAGE_SHOWN, mIsImageShown);
    }

    private class PreCompositeTextTask extends AsyncTask<String, Void, Integer> {

        private static final int MIN_TEXT_SIZE = 28;
        private static final int MAX_TEXT_SIZE = 240;

        private final TextPaint mPaint;
        private final int mMaxWidth;
        private final int mMaxHeight;

        private PreCompositeTextTask(TextPaint paint, int maxWidth, int maxHeight) {
            // Make a copy
            mPaint = new TextPaint(paint);
            mMaxWidth = maxWidth;
            mMaxHeight = maxHeight;
        }

        @Override
        protected Integer doInBackground(String... params) {

            if (params.length != 0) {

                String text = params[0];
                String prefWidth = params.length == 1 ? "" : params[1];

                // Only perform the calculation if the text was passed in.
                if (!TextUtils.isEmpty(text)) {

                    // If preferred width string is empty, go ahead and calculate it.
                    if (TextUtils.isEmpty(prefWidth)) {
                        String[] split = text.split(" |\\r|\\n");
                        int len = 0;
                        for (String string : split) {
                            if (string.length() > len) {
                                len = string.length();
                            }
                        }
                        for (int i = len; i > 1; i--) {
                            prefWidth += "W";
                        }
                    }

                    return resizeText(text, prefWidth, mMaxWidth, mMaxHeight);


                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            if (integer != null) {
                // Some devices try to auto adjust line spacing, so force default line spacing
                // and invalidate the layout as a side effect
                mTitleText.setTextSize(TypedValue.COMPLEX_UNIT_PX, integer);
            }
        }

        /**
         * Resize the text size with specified width and height
         * @param text
         * @param prefWidth
         * @param width
         * @param height
         */
        public int resizeText(String text, String prefWidth, int width, int height) {
            // Do not resize if the view does not have dimensions or there is no text
            if (height <= 0 || width <= 0) {
                return width;
            }
            // Use a binary search to filter down to the appropriate size.
            return binarySearch(MIN_TEXT_SIZE, MAX_TEXT_SIZE, text, prefWidth, mPaint, width, height);
        }

        // Perform a binary search to determine appropriate text size.
        private int binarySearch(int minTextSize, int maxTextSize, CharSequence source, String longest, TextPaint paint, int widthLimit, int heightLimit) {

            int lastBest = minTextSize;
            int lo = minTextSize;
            int hi = maxTextSize;
            int mid = 0;
            while (lo < hi) {
                mid = (lo + hi) >>> 1;
                int midValCmp = 0;
                paint.setTextSize(mid);
                float currWidth = getTextWidth(longest, paint);
                float currHeight = getTextHeight(source, paint, widthLimit);
                if (currWidth > widthLimit || currHeight > heightLimit) {
                    hi = mid - 2;
                    lastBest = hi;
                } else {
                    lastBest = lo;
                    lo = mid + 2;
                }
            }
            // make sure to return last best
            // this is what should always be returned
            return lastBest;
        }

        // Use a static layout to render text off screen before measuring.
        private int getTextHeight(CharSequence source, TextPaint paint, int width) {
            // Have to make a copy because StaticLayout alters paint.
            TextPaint paintCopy = new TextPaint(paint);
            // Measure using a static layout
            StaticLayout layout = new StaticLayout(source, paintCopy, width, Layout.Alignment.ALIGN_CENTER, (float) 1.0, (float) 0.0, true);
            return layout.getHeight();
        }

        // Measure the text width based on the current text size.
        private float getTextWidth(CharSequence source, TextPaint paint) {
            return paint.measureText(source.toString());
        }

    }
}
