package com.android.fancyblurdemo.app;

import android.content.Context;
import android.view.animation.Interpolator;
import android.widget.Scroller;

/**
 * Created by kevin.marlow on 4/4/14.
 */
public class FlickrScroller extends Scroller {

    private double mScrollFactor = 1;

    public FlickrScroller(Context context) {
        super(context);
    }

    public FlickrScroller(Context context, Interpolator interpolator) {
        super(context, interpolator);
    }

    public FlickrScroller(Context context, Interpolator interpolator, boolean flywheel) {
        super(context, interpolator, flywheel);
    }

    /**
     * Set the factor by which the duration will change
     */
    public void setScrollFactor(double scrollFactor) {
        mScrollFactor = scrollFactor;
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        super.startScroll(startX, startY, dx, dy, (int) (duration * mScrollFactor));
    }

    public double getScrollFactor() {
        return mScrollFactor;
    }
}
