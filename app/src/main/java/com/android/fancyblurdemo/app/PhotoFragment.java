package com.android.fancyblurdemo.app;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

    /** The fragment argument representing the page number for this fragment. */
    private static final String ARG_PAGE_NUMBER = "page_number";
    /** The fragment argument tracking if the image has been shown for this fragment. */
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
        args.putInt(ARG_PAGE_NUMBER, sectionNumber);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mIsImageShown = savedInstanceState.getBoolean(ARG_IMAGE_SHOWN, false);
            mCurrentPhoto = MainActivity.photos.get(savedInstanceState.getInt(ARG_PAGE_NUMBER));
        } else {
            mCurrentPhoto = MainActivity.photos.get(getArguments().getInt(ARG_PAGE_NUMBER));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        rootView.setOnClickListener(this);

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

        new TextSizeTask(paint, width, height).execute(mCurrentPhoto.title, mCurrentPhoto.preferredWidthStr);

        mTitleText.setVisibility(View.GONE);
//        mIsTitleShown = true;

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

                    int mTextWidth, mTextHeight; // Our calculated text bounds
                    final String text = mCurrentPhoto.title;
                    final TextPaint paint = mTitleText.getPaint();

                    Rect textBounds = new Rect();
                    paint.getTextBounds(text, 0, text.length(), textBounds);
                    mTextWidth = (int) paint.measureText(text); // Use measureText to calculate width
                    mTextHeight = textBounds.height(); // Use height from getTextBounds()

                    Path path = new Path();
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeJoin(Paint.Join.MITER);
                    paint.setStrokeMiter(2);
                    paint.setStrokeWidth(1);

                    paint.getTextPath(text, 0, text.length(), mTitleText.getWidth() / 2 - (mTextWidth / 2f), mTitleText.getHeight() / 2 + (mTextHeight / 2f), path);

                    mBlurImageView.setImageToBlur(response.getBitmap(), response.getRequestUrl(), path, BlurManager.getImageBlurrer());
                    mBlurImageView.setImageAlpha(255);
                    mProgressBar.setVisibility(View.GONE);
                    if (!mIsTitleShown && (mHasHadCallback || mIsImageShown)) {
                        mTitleText.setVisibility(View.VISIBLE);
                        mTitleText.startAnimation(mFadeInWithDelay);
                        mIsTitleShown = true;
                    }
                    mIsImageShown = true;
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                // Do nothing.
            }
        });

//        Log.i("TAG", "Is downloading image - " + mCurrentPhoto.title);
        mImageView.setImageUrl(MainActivity.sUseHighRes ? mCurrentPhoto.highResUrl : mCurrentPhoto.photoUrl, VolleyManager.getImageLoader(), true);

        return rootView;
    }

    /**
     * Set the blurred image alpha. This is called during
     * {@link android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrolled(int, float, int)}.
     *
     * @param alpha The alpha value.
     */
    public void setPageAlpha(float alpha) {
//        mBlurImageView.setImageAlpha((int) (255 * alpha));
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
        outState.putInt(ARG_PAGE_NUMBER, getArguments().getInt(ARG_PAGE_NUMBER));
    }

    private class TextSizeTask extends PreCompositeTextTask {

        private TextSizeTask(TextPaint paint, int maxWidth, int maxHeight) {
            super(paint, maxWidth, maxHeight);
        }
        @Override
        public void doOnComplete(int textSize) {
            // Some devices try to auto adjust line spacing, so force default line spacing
            // and invalidate the layout as a side effect
            mTitleText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        }
    }
}
