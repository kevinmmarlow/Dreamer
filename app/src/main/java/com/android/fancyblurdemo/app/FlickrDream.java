package com.android.fancyblurdemo.app;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterViewFlipper;

import com.android.fancyblurdemo.volley.Response;
import com.android.fancyblurdemo.volley.VolleyError;

import java.util.List;

/**
 * Created by kevin.marlow on 4/4/14.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class FlickrDream extends DreamService {

    private static final String FLICKR_API_KEY = "70fcd966ffa1676f8726a7b3fee51188";
    private static final String FLICKR_API_SECRET = "5b38c304eda85d6a";
    private static final String FLICKR_INTERESTING_PHOTOS_URL = "https://api.flickr.com/services/rest/?method=flickr.interestingness.getList&api_key=%s&format=json&per_page=50";

    private static final long SCROLL_DELAY = 4000; // 4 second scroll delay.

    /**
     * The {@link AdapterViewFlipper} that will host the section contents.
     */
    private AdapterViewFlipper mAdapterViewFlipper;
    private DreamAdapter mAdapter;

    /**
     * The view to show when there is no connectivity.
     */
    private RobotoTextView mNoConnectionView;

    /**
     * A flag to determine if we are on a tablet.
     */
    public static boolean sUseHighRes = false;
    private boolean mIsConnected = false;
    private ConnectionReceiver mConnectionReceiver;

    private Animation mFadeIn;
    private Animation mFadeOut;

    private Handler mScrollHandler = new Handler();
    private Runnable mScroller = new Runnable() {
        @Override
        public void run() {
            mAdapterViewFlipper.showNext();
            mScrollHandler.postDelayed(mScroller, SCROLL_DELAY);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setContentView(R.layout.service_dream);
//        setInteractive(true);
        setScreenBright(true);
        setFullscreen(true);

        mAdapterViewFlipper = (AdapterViewFlipper) findViewById(R.id.viewFlipper);
        mAdapterViewFlipper.setInAnimation(FlickrDream.this, R.anim.slide_in_left);
        mAdapterViewFlipper.setOutAnimation(FlickrDream.this, R.anim.slide_out_left);

        mNoConnectionView = (RobotoTextView) findViewById(R.id.noConnectionView);
        mFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        mFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        sUseHighRes = getResources().getBoolean(R.bool.isTablet);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        mIsConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (mIsConnected) {
            VolleyManager.getRequestQueue().add(new FlickrRequest(String.format(FLICKR_INTERESTING_PHOTOS_URL, FLICKR_API_KEY), sUseHighRes, new ResponseListener(), new ErrorListener()));
        } else {
            mConnectionReceiver = new ConnectionReceiver();
            mNoConnectionView.setVisibility(View.VISIBLE);
            mNoConnectionView.startAnimation(mFadeIn);
            // Only register for changes if we didn't start with connectivity.
            registerReceiver(mConnectionReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    @Override
    public void onDetachedFromWindow() {
        if (mConnectionReceiver != null) {
            final ConnectionReceiver receiver = mConnectionReceiver;
            unregisterReceiver(receiver);
            mConnectionReceiver = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
    }

    @Override
    public void onDreamingStopped() {
        mAdapter.cancelAllTasks();
        mScrollHandler.removeCallbacks(mScroller);
        super.onDreamingStopped();
    }

    /**
     * The network response listener for successful calls. Upon response,
     * the data model is repopulated and the adapter is notified.
     */
    private class ResponseListener implements Response.Listener<List<FlickrPhoto>> {

        @Override
        public void onResponse(List<FlickrPhoto> response) {
            mAdapter = new DreamAdapter(FlickrDream.this, response);
            mAdapterViewFlipper.setAdapter(mAdapter);
            mScrollHandler.postDelayed(mScroller, SCROLL_DELAY);
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
     * The {@link android.content.BroadcastReceiver} responsible for monitoring network changes.
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
}
