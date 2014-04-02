package com.android.fancyblurdemo.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * <p>This is the custom typeface {@link TextView}. It also will handle word
 * word wrapping more gracefully. Currently, it resizing the text
 * if it hits the view bounds. The resizing is based on Chase Colburn's
 * code from Stack Overflow, which can be found
 * <a href="http://stackoverflow.com/questions/5033012/auto-scale-textview-text-to-fit-within-bounds">here</a>.</p>
 *
 * <p>Later implementations will include proper word wrapping.</p>
 *
 * Created by kevin.marlow on 3/27/14.
 */
public class RobotoTextView extends TextView {

    public RobotoTextView(Context context) {
        this(context, null);
    }

    public RobotoTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RobotoTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if(!isInEditMode()) {
            if (attrs != null) {
                TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RobotoTextView, defStyle, 0);

                int fontFace = a.getInteger(R.styleable.RobotoTextView_fontFace, 0);
                setTypeface(fontFace);
                a.recycle();
            } else {
                setTypeface(0);
            }
        }
    }

    /**
     * Stylize the type.
     * 0 - Regular
     * 1 - Light
     * 2 - Medium
     * 3 - Bold
     * @param typeface
     */
    private void setTypeface(int typeface) {
        Typeface tf;
        switch (typeface) {
            case 0:
                tf = Typeface.createFromAsset(getContext().getAssets(), "Roboto-Regular.ttf");
                break;
            case 1:
                tf = Typeface.createFromAsset(getContext().getAssets(), "Roboto-Light.ttf");
                break;
            case 2:
                tf = Typeface.createFromAsset(getContext().getAssets(), "Roboto-Medium.ttf");
                break;
            case 3:
                tf = Typeface.createFromAsset(getContext().getAssets(), "Roboto-Bold.ttf");
                break;
            case 4:
                tf = Typeface.createFromAsset(getContext().getAssets(), "DIN-Condensed-Bold.ttf");
            default:
                tf = Typeface.createFromAsset(getContext().getAssets(), "Roboto-Regular.ttf");
                break;
        }
        setTypeface(tf);
    }
}
