package com.android.fancyblurdemo.app;

import android.app.ActivityManager;
import android.app.Application;

import com.android.fancyblurdemo.app.imageblur.BlurManager;

/**
 * Created by kevin.marlow on 3/20/14.
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    /**
     * Initializes the request manager, the image cache, and the blur manager
     */
    private void init() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int heapSize = am.getMemoryClass();
        VolleyManager.init(this, (heapSize * 1024 * 1024 / 8));
        BlurManager.init(this, this.getPackageCodePath());
    }

}
