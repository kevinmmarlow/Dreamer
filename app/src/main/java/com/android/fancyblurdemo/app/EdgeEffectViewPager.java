/*
 * Copyright (c) 2013 Android Alliance, LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.fancyblurdemo.app;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.animation.Interpolator;

import java.lang.reflect.Field;

/**
 * This code was created by the AndroidAlliance and
 * has been borrrowed from https://github.com/AndroidAlliance/EdgeEffectOverride.
 */
public class EdgeEffectViewPager extends ViewPager {

    private Interpolator mInterpolator;

    public EdgeEffectViewPager(Context context) {
        this(context, null);
    }

    public EdgeEffectViewPager(Context context, AttributeSet attrs) {
        super(new ContextWrapperEdgeEffect(context), attrs);
        init(context, attrs, 0);
        postInitViewPager();
    }

    private void init(Context context, AttributeSet attrs, int defStyle){
        int color = context.getResources().getColor(R.color.edgeeffect_color);
        setEdgeEffectColor(color);
    }

    public void setEdgeEffectColor(int edgeEffectColor){
        ((ContextWrapperEdgeEffect)  getContext()).setEdgeEffectColor(edgeEffectColor);
    }

    private FlickrScroller mScroller = null;

    /**
     * Override the Scroller instance with our own class so we can change the
     * duration
     */
    private void postInitViewPager() {
        try {
            Field scroller = ViewPager.class.getDeclaredField("mScroller");
            scroller.setAccessible(true);
            Field interpolator = ViewPager.class.getDeclaredField("sInterpolator");
            interpolator.setAccessible(true);

            mScroller = new FlickrScroller(getContext(), mInterpolator == null ? (Interpolator) interpolator.get(null) : mInterpolator);
            scroller.set(this, mScroller);
        } catch (Exception e) {
        }
    }

    /**
     * Set the factor by which the duration will change
     */
    public void setScrollDurationFactor(double scrollFactor) {
        mScroller.setScrollFactor(scrollFactor);
    }

    public void setInterpolator(Interpolator interpolator) {
        final double scrollFactor = mScroller.getScrollFactor();
        mInterpolator = interpolator;
        postInitViewPager();
        mScroller.setScrollFactor(scrollFactor);
    }
}
