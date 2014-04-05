package com.android.fancyblurdemo.app;

import android.os.AsyncTask;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

/**
 * Created by kevin.marlow on 4/4/14.
 */
public abstract class PreCompositeTextTask extends AsyncTask<String, Void, Integer> {

    private static final int MIN_TEXT_SIZE = 28;
    private static final int MAX_TEXT_SIZE = 240;

    private final TextPaint mPaint;
    private final int mMaxWidth;
    private final int mMaxHeight;

    public PreCompositeTextTask(TextPaint paint, int maxWidth, int maxHeight) {
        // Make a copy
        mPaint = new TextPaint(paint);
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
    }

    public abstract void doOnComplete(int textSize);

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
                    prefWidth += "w";
                }

                return resizeText(text, prefWidth, mMaxWidth, mMaxHeight);


            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(Integer integer) {
        if (integer != null) {
            doOnComplete(integer);
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
        StaticLayout layout = new StaticLayout(source, paintCopy, width, Layout.Alignment.ALIGN_CENTER, (float) 1.2, (float) 0.0, true);
        return layout.getHeight();
    }

    // Measure the text width based on the current text size.
    private float getTextWidth(CharSequence source, TextPaint paint) {
        return paint.measureText(source.toString());
    }
}
