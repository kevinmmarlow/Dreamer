package com.android.fancyblurdemo.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.android.fancyblurdemo.volley.Response;
import com.android.fancyblurdemo.volley.VolleyError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends FragmentActivity {

    private static final String FLICKR_API_KEY = "70fcd966ffa1676f8726a7b3fee51188";
    private static final String FLICKR_API_SECRET = "5b38c304eda85d6a";
    private static final String FLICKR_INTERESTING_PHOTOS_URL = "https://api.flickr.com/services/rest/?method=flickr.interestingness.getList&api_key=%s&format=json&per_page=50";

    private static final String ARG_HAS_DOWNLOADED = "has_downloaded";
    private static final String ARG_USER_LEARNED_SCROLL = "user_scrolled";

    private static final long SCROLL_DELAY = 5000; // 5 second scroll delay.

    /**
     * Data store. The photomap maps the photo id to the photo for easy retrieval.
     */
    public static Map<String, FlickrPhoto> photoMap = new HashMap<String, FlickrPhoto>();
    public static List<FlickrPhoto> photos = new ArrayList<FlickrPhoto>();

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private EdgeEffectViewPager mViewPager;

    /**
     * The view to show when there is no connectivity.
     */
    private RobotoTextView mNoConnectionView;

    /**
     * The page change listener that is used for fading on scroll.
     */
    private FragmentPageChangeListener mPageChangeListener;

    /**
     * A flag to determine if we are on a tablet.
     */
    public static boolean sUseHighRes = false;
    private boolean mIsConnected = false;
    private ConnectionReceiver mConnectionReceiver;

    private Animation mFadeIn;
    private Animation mFadeOut;

    /** Handle autoscrolling. */
    private boolean mUserLearned;
    private boolean mForwardScroll = true;
    private Handler mScrollHandler;
    private Runnable mScroller = new Runnable() {
        @Override
        public void run() {
            if (mViewPager != null && mSectionsPagerAdapter != null && mSectionsPagerAdapter.getCount() > 1) {
                if (mViewPager.getCurrentItem() + 1 >= mSectionsPagerAdapter.getCount()) {
                    mForwardScroll = false;
                } else if (mForwardScroll == false && mViewPager.getCurrentItem() == 0) {
                    mForwardScroll = true;
                }
                if (mForwardScroll) {
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true);
                } else {
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1, true);
                }
                mScrollHandler.postDelayed(mScroller, SCROLL_DELAY);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final boolean hasDownloaded = savedInstanceState == null ? false : savedInstanceState.getBoolean(ARG_HAS_DOWNLOADED, false);

        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        mUserLearned = prefs.getBoolean(ARG_USER_LEARNED_SCROLL, false);

        sUseHighRes = getResources().getBoolean(R.bool.isTablet);

        if (!mUserLearned) {
            mScrollHandler = new Handler();
        }

        mNoConnectionView = (RobotoTextView) findViewById(R.id.noConnectionView);
        mFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        mFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mPageChangeListener = new FragmentPageChangeListener();
        mSectionsPagerAdapter.registerDataSetObserver(new PagerObserver());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (EdgeEffectViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(mPageChangeListener);
        mViewPager.setScrollDurationFactor(sUseHighRes ? 5 : 3);

        mViewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!mUserLearned) {
                    mUserLearned = true;
                    mScrollHandler.removeCallbacks(mScroller);
                    // Reset the scroll factor once the user has touched.
                    mViewPager.setScrollDurationFactor(1);
                }
                return false;
            }
        });

        if (!hasDownloaded) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            mIsConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            if (mIsConnected) {
                VolleyManager.getRequestQueue().add(new FlickrRequest(String.format(FLICKR_INTERESTING_PHOTOS_URL, FLICKR_API_KEY), sUseHighRes, new ResponseListener(), new ErrorListener()));
            } else {
                mConnectionReceiver = new ConnectionReceiver();
                mNoConnectionView.setVisibility(View.VISIBLE);
                mNoConnectionView.startAnimation(mFadeIn);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only register for changes if we didn't start with connectivity.
        if (mConnectionReceiver != null && !mIsConnected) {
            registerReceiver(mConnectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    @Override
    protected void onPause() {
        if (mConnectionReceiver != null) {
            final ConnectionReceiver receiver = mConnectionReceiver;
            unregisterReceiver(receiver);
            mConnectionReceiver = null;
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            sUseHighRes = !sUseHighRes;
//            Toast.makeText(this, "High-Res is now " + (sUseHighRes ? "enabled." : "disabled."), Toast.LENGTH_SHORT).show();
//            mSectionsPagerAdapter.notifyDataSetChanged();
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * <p>A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages. This class implements the registeredFragments
     * paradigm in order to get references to the fragments.</p>
     *
     * @see <a href="http://stackoverflow.com/questions/8785221/retrieve-a-fragment-from-a-viewpager">
     *     Retrieve a Fragment from a ViewPager</a>
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        SparseArray<PhotoFragment> registeredFragments = new SparseArray<PhotoFragment>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PhotoFragment (defined as a static inner class below).
            PhotoFragment fragment = PhotoFragment.newInstance(position);
            return fragment;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            PhotoFragment fragment = (PhotoFragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        @Override
        public int getCount() {
            return photos.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return photos.get(position).title;
        }

        public PhotoFragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }
    }

    /**
     * Callback interface for responding to page changes in the {@link EdgeEffectViewPager}.
     *
     * <p>This class is responsible for sending the alpha parameter to our Fragments.</p>
     */
    private class FragmentPageChangeListener implements ViewPager.OnPageChangeListener {

        // How fast blur happens on scroll. A number between 1 and 2 works best.
        private static final float BLUR_SENSITIVITY = (float) 1.16;

        private int mCurrentIndex = 0;
        private int mLastIndex = 0;
        private PhotoFragment mPreviousPage;
        private PhotoFragment mCurrentPage;
        private PhotoFragment mNextPage;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            if (mCurrentPage == null)
                return;

            // Check if we are looking at the current and next views.
            if (mCurrentIndex == position) {
                mCurrentPage.setPageAlpha(Math.min(BLUR_SENSITIVITY * positionOffset, 1));
                if (mNextPage != null) {
                    mNextPage.setPageAlpha(Math.min(1, BLUR_SENSITIVITY *  (1 - positionOffset)));
                }
            } else {
                // Here we are looking at the current and previous views.
                // The offset is based on the previous view's position,
                // so we invert the logic.
                mCurrentPage.setPageAlpha(Math.min(1, BLUR_SENSITIVITY * (1 - positionOffset)));
                if (mPreviousPage != null) {
                    mPreviousPage.setPageAlpha(Math.min(1, BLUR_SENSITIVITY * positionOffset));
                }
            }
        }

        @Override
        public void onPageSelected(int position) {
            // Do fade title callbacks.
            if (mSectionsPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem()) != null) {
                mSectionsPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem()).showTitle();
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                mCurrentIndex = mViewPager.getCurrentItem();
                if (mCurrentIndex == 0) {
                    mPreviousPage = null;
                    mCurrentPage = mSectionsPagerAdapter.getRegisteredFragment(0);
                    mNextPage = mSectionsPagerAdapter.getRegisteredFragment(1);
                } else if (mCurrentIndex == mLastIndex) {
                    mPreviousPage = mSectionsPagerAdapter.getRegisteredFragment(mCurrentIndex - 1);
                    mCurrentPage = mSectionsPagerAdapter.getRegisteredFragment(mCurrentIndex);
                    mNextPage = null;
                } else {
                    mPreviousPage = mSectionsPagerAdapter.getRegisteredFragment(mCurrentIndex - 1);
                    mCurrentPage = mSectionsPagerAdapter.getRegisteredFragment(mCurrentIndex);
                    mNextPage = mSectionsPagerAdapter.getRegisteredFragment(mCurrentIndex + 1);
                }
            } else if (state == ViewPager.SCROLL_STATE_IDLE) {
                // Reset references.
                mPreviousPage = null;
                mCurrentPage = null;
                mNextPage = null;
            }
        }

        private void dataSetChanged() {
            mLastIndex = mSectionsPagerAdapter.getCount() - 1;
        }
    }

    private class PagerObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            mPageChangeListener.dataSetChanged();
        }
        @Override
        public void onInvalidated() {
            mPageChangeListener.dataSetChanged();
        }
    }

    /**
     * The network response listener for successful calls. Upon response,
     * the data model is repopulated and the adapter is notified.
     */
    private class ResponseListener implements Response.Listener<List<FlickrPhoto>> {

        @Override
        public void onResponse(List<FlickrPhoto> response) {
            photos.clear();
            photoMap.clear();
            for (int i = 0; i < response.size(); i++) {
                final FlickrPhoto photo = response.get(i);
                photos.add(photo);
                photoMap.put(photo.id, photo);
            }
            mSectionsPagerAdapter.notifyDataSetChanged();

            /**
             * Our title fade algorithm works on page selection, which is not called for the first item.
             * Therefore, we call it manually.
             */
            if (mSectionsPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem()) != null) {
                mSectionsPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem()).showTitle();
            }

            /**
             * Auto-scroll if the user has not tried to scroll.
             */
            if (!mUserLearned) {
                mScrollHandler.postDelayed(mScroller, SCROLL_DELAY);
            }
        }
    }

    /**
     * The network response listener for failed calls.
     * Normally we would provide feedback to the user and
     * handle the error. For now, lets just print out the error.
     */
    private class ErrorListener implements Response.ErrorListener {

        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("FAILED", error.networkResponse == null ? error.getMessage() : "Status Code: " + error.networkResponse.statusCode);
        }
    }

    /**
     * The {@link BroadcastReceiver} responsible for monitoring network changes.
     * This is used when the app is launched without network connectivity.
     */
    private class ConnectionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final boolean wasConnected = mIsConnected;
            ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            mIsConnected = activeNetwork != null && activeNetwork.isConnected();
            if (!wasConnected && mIsConnected) {
                mNoConnectionView.startAnimation(mFadeOut);
                VolleyManager.getRequestQueue().add(new FlickrRequest(String.format(FLICKR_INTERESTING_PHOTOS_URL, FLICKR_API_KEY), sUseHighRes, new ResponseListener(), new ErrorListener()));
                if (mConnectionReceiver != null) {
                    final ConnectionReceiver receiver = mConnectionReceiver;
                    unregisterReceiver(receiver);
                    mConnectionReceiver = null;
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_HAS_DOWNLOADED, true);
    }
}
