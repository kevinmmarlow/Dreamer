package com.android.fancyblurdemo.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
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

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RobotoTextView, defStyle, 0);

            int fontFace = a.getInteger(R.styleable.RobotoTextView_fontFace, 0);
            setFontFace(fontFace);
            a.recycle();
        } else {
            setFontFace(0);
        }
    }

    /**
     * Stylize the type.
     * 0 - Regular
     * 1 - Light
     * 2 - Medium
     * 3 - Bold
     * @param fontFace
     */
    public void setFontFace(int fontFace) {
        Typeface tf;
        switch (fontFace) {
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
            default:
                tf = Typeface.createFromAsset(getContext().getAssets(), "Roboto-Regular.ttf");
                break;
        }
        setTypeface(tf);
    }
}
