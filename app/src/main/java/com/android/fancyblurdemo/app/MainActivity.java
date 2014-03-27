package com.android.fancyblurdemo.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.android.fancyblurdemo.app.imageblur.BlurError;
import com.android.fancyblurdemo.app.imageblur.BlurManager;
import com.android.fancyblurdemo.app.imageblur.ImageBlurrer;
import com.android.fancyblurdemo.volley.Response;
import com.android.fancyblurdemo.volley.VolleyError;
import com.android.fancyblurdemo.volley.toolbox.ImageLoader;
import com.android.fancyblurdemo.volley.toolbox.NetworkImageView;


public class MainActivity extends ActionBarActivity {

    private static final String FLICKR_API_KEY = "70fcd966ffa1676f8726a7b3fee51188";
    private static final String FLICKR_API_SECRET = "5b38c304eda85d6a";
    private static final String FLICKR_RECENT_PHOTOS_URL = "https://api.flickr.com/services/rest/?method=flickr.interestingness.getList&api_key=%s&format=json&per_page=25";
    private static final String FLICKR_SIZES_URL = "https://api.flickr.com/services/rest/?method=flickr.photos.getSizes&api_key=%s&format=json&photo_id=%s";

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
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    private FragmentPageChangeListener mPageChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mPageChangeListener = new FragmentPageChangeListener();
        mSectionsPagerAdapter.registerDataSetObserver(new PagerObserver());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(mPageChangeListener);

        VolleyManager.getRequestQueue().add(new FlickrRequest(String.format(FLICKR_RECENT_PHOTOS_URL, FLICKR_API_KEY), new ResponseListener(), new ErrorListener()));
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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        SparseArray<PlaceholderFragment> registeredFragments = new SparseArray<PlaceholderFragment>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            PlaceholderFragment fragment = PlaceholderFragment.newInstance(position);
            return fragment;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            PlaceholderFragment fragment = (PlaceholderFragment) super.instantiateItem(container, position);
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

        public PlaceholderFragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }
    }

    private class FragmentPageChangeListener extends ViewPager.SimpleOnPageChangeListener {

        // How fast blur happens on scroll. A number between 1 and 2 works best.
        private static final float BLUR_SENSITIVITY = (float) 1.16;

        private int mCurrentIndex = 0;
        private int mLastIndex = 0;
        private PlaceholderFragment mPreviousPage;
        private PlaceholderFragment mCurrentPage;
        private PlaceholderFragment mNextPage;

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
                mCurrentIndex = mViewPager.getCurrentItem();
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
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        private FlickrPhoto mCurrentPhoto;
        private NetworkImageView mImageView;
        private BlurImageView mBlurImageView;
        private ProgressBar mProgressBar;
//        private View mDarkOverlay;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            mImageView = (NetworkImageView) rootView.findViewById(R.id.flickrView);
            mBlurImageView = (BlurImageView) rootView.findViewById(R.id.blurView);
//            mDarkOverlay = rootView.findViewById(R.id.darkOverlay);
            mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
            mCurrentPhoto = photos.get(getArguments().getInt(ARG_SECTION_NUMBER));

            mImageView.setImageListener(new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    if (response.getBitmap() != null && !TextUtils.isEmpty(response.getRequestUrl())) {
//                        mBlurImageView.setVisibility(View.VISIBLE);
//                        mBlurImageView.setImageAlpha(0);
                        mBlurImageView.setImageToBlur(response.getBitmap(), response.getRequestUrl(), BlurManager.getImageBlurrer());
                        mBlurImageView.setImageAlpha(0);
                    }
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    // Do nothing.
                }
            });

            if (TextUtils.isEmpty(mCurrentPhoto.source)) {
                mProgressBar.setVisibility(View.VISIBLE);
                String url = String.format(FLICKR_SIZES_URL, FLICKR_API_KEY, mCurrentPhoto.id);
                VolleyManager.getRequestQueue().add(new PhotoSizeRequest(url, new SizeResponseListener(), new SizeErrorListener()));
            } else {
                mImageView.setImageUrl(mCurrentPhoto.source, VolleyManager.getImageLoader());
                mProgressBar.setVisibility(View.GONE);
            }

            return rootView;
        }

        public void setPageAlpha(float alpha) {
//            mDarkOverlay.setAlpha(alpha);
            mBlurImageView.setImageAlpha((int) (255 * alpha));
        }

        public class SizeResponseListener implements Response.Listener<String> {

            @Override
            public void onResponse(String response) {
                mCurrentPhoto.source = response;
                mImageView.setImageUrl(mCurrentPhoto.source, VolleyManager.getImageLoader());
                mProgressBar.setVisibility(View.GONE);
            }
        }

        public class SizeErrorListener implements Response.ErrorListener {

            @Override
            public void onErrorResponse(VolleyError error) {
//                Log.i("FAILED", error.networkResponse == null ? error.getMessage() : "Status Code: " + error.networkResponse.statusCode);
            }
        }
    }

    private class ResponseListener implements Response.Listener<List<FlickrPhoto>> {

        @Override
        public void onResponse(List<FlickrPhoto> response) {
            for (int i = 0; i < response.size(); i++) {
                final FlickrPhoto photo = response.get(i);
                photos.add(photo);
                photoMap.put(photo.id, photo);
            }
            mSectionsPagerAdapter.notifyDataSetChanged();
        }
    }

    private class ErrorListener implements Response.ErrorListener {

        @Override
        public void onErrorResponse(VolleyError error) {
//            Log.e("FAILED", error.networkResponse == null ? error.getMessage() : "Status Code: " + error.networkResponse.statusCode);
        }
    }

}
