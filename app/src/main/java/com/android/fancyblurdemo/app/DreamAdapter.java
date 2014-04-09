package com.android.fancyblurdemo.app;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;

import com.android.fancyblurdemo.volley.VolleyError;
import com.android.fancyblurdemo.volley.toolbox.ImageLoader;
import com.android.fancyblurdemo.volley.toolbox.NetworkImageView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>The adapter used by the {@link android.widget.AdapterViewFlipper}
 * in the {@link FlickrDream}.</p>
 *
 * <p>This adapter is responsible for downloading and displaying the Flickr
 * images asynchronously.</p>
 */
public class DreamAdapter extends BaseAdapter {

    private final Animation mFadeInWithDelay;
    private Context mContext;
    private List<FlickrPhoto> photos = new ArrayList<FlickrPhoto>();
    private Set<TextSizeTask> mCurrentTasks = new HashSet<TextSizeTask>();
    private int[] textSizes;
    private int screenWidth;
    private int screenHeight;
    private boolean mIsFirstRunThrough = false;

    @SuppressWarnings("deprecation")
    public DreamAdapter(Context context, List<FlickrPhoto> photos) {
        mContext = context;
        this.photos = photos;
        textSizes = new int[photos.size()];
        mFadeInWithDelay = AnimationUtils.loadAnimation(context, R.anim.fade_in_with_delay);

        Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int titleTextMargin = (int) context.getResources().getDimension(R.dimen.title_text_margin);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x - 2 * titleTextMargin;
            screenHeight = size.y - 2 * titleTextMargin;
        } else {
            screenWidth = display.getWidth() - 2 * titleTextMargin;
            screenHeight = display.getHeight() - 2 * titleTextMargin;
        }
    }

    @Override
    public int getCount() {
        return photos.size();
    }

    @Override
    public FlickrPhoto getItem(int position) {
        return photos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final DreamViewHolder holder;
        if (convertView == null) {
            convertView = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.service_inner_dream, null);

            holder = new DreamViewHolder();
            holder.mImageView = (NetworkImageView) convertView.findViewById(R.id.flickrDreamView);
            holder.mBlurImageView = (BlurImageView) convertView.findViewById(R.id.blurView);
            holder.mProgressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
            holder.mOverlay = convertView.findViewById(R.id.overlay);

            holder.mTitleText = (RobotoTextView) convertView.findViewById(R.id.titleText);

            convertView.setTag(holder);
        } else {
            holder = (DreamViewHolder) convertView.getTag();
        }

        if (textSizes[position] == 0) {
            TextSizeTask task = new TextSizeTask(holder.mTitleText, screenWidth, screenHeight, position);
            mCurrentTasks.add(task);
            task.execute(getItem(position).title, getItem(position).preferredWidthStr);
        } else {
            holder.mTitleText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizes[position]);
        }

        holder.mTitleText.setText(getItem(position).title);
        holder.mTitleText.setVisibility(View.GONE);
        holder.mImageView.setImageListener(new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                if (response.getBitmap() != null && !TextUtils.isEmpty(response.getRequestUrl())) {
                    holder.mOverlay.setVisibility(View.VISIBLE);
                    Animation fullFadeIn = AnimationUtils.loadAnimation(mContext, android.R.anim.fade_in);
                    fullFadeIn.setFillAfter(true);
                    holder.mOverlay.startAnimation(fullFadeIn);
//                    mBlurImageView.setImageToBlur(response.getBitmap(), response.getRequestUrl(), BlurManager.getImageBlurrer());
//                    mBlurImageView.setImageAlpha(0);
                    holder.mProgressBar.setVisibility(View.GONE);
                    holder.mTitleText.setVisibility(View.VISIBLE);
                    holder.mTitleText.startAnimation(mFadeInWithDelay);
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                // Do nothing.
            }
        });

        holder.mImageView.setImageUrl(FlickrDream.sUseHighRes ? getItem(position).highResUrl : getItem(position).photoUrl, VolleyManager.getImageLoader(), true);

        if (mIsFirstRunThrough) {
            final int width = holder.mImageView.getWidth();
            final int height = holder.mImageView.getHeight();
            VolleyManager.getImageLoader().get((FlickrDream.sUseHighRes ? getItem((position + 1) % getCount()).highResUrl : getItem((position + 1) % getCount()).photoUrl), new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    // Do Nothing. We are just caching early.
                }

                @Override
                public void onErrorResponse(VolleyError error) {
                    // Do Nothing. We are just caching early.
                }
            }, width, height);
        }

        if (position == getCount() - 1) {
            mIsFirstRunThrough = false;
        }

        return convertView;
    }

    public void cancelAllTasks() {
        for (TextSizeTask task : mCurrentTasks) {
            task.cancel(true);
        }
    }

    private class DreamViewHolder {
        public NetworkImageView mImageView;
        public BlurImageView mBlurImageView;
        public ProgressBar mProgressBar;
        public RobotoTextView mTitleText;
        public View mOverlay;
    }

    private class TextSizeTask extends PreCompositeTextTask {

        private RobotoTextView mTextView;
        private int mPosition;

        public TextSizeTask(RobotoTextView titleText, int maxWidth, int maxHeight, int position) {
            super(titleText.getPaint(), maxWidth, maxHeight);
            mTextView = titleText;
            mPosition = position;
        }

        @Override
        public void doOnComplete(int textSize) {
            mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            textSizes[mPosition] = textSize;
            Log.i("TAG", "Text size " + textSize);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + mPosition;
            return result;
        }
    }
}
