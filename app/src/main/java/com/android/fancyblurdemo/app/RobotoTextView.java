package com.android.fancyblurdemo.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
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

    private final Point mCenter;

    public RobotoTextView(Context context) {
        this(context, null);
    }

    public RobotoTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RobotoTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mCenter = new Point();
        
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int heightSize = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
        mCenter.x = widthSize / 2;
        mCenter.y = heightSize / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int mTextWidth, mTextHeight; // Our calculated text bounds

        final String text = (String) getText();
        TextPaint paint = getPaint();
        // Now lets calculate the size of the text
        Rect textBounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), textBounds);
        mTextWidth = (int) paint.measureText(text); // Use measureText to calculate width
        mTextHeight = textBounds.height(); // Use height from getTextBounds()
//
//        // Later when you draw...
//        canvas.drawText(mText, // Text to display
//                mCenter.x - (mTextWidth / 2f),
//                mCenter.y + (mTextHeight / 2f),
//                mTextPaint);

        Path path = new Path();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeMiter(2);
        paint.setStrokeWidth(1);

        paint.getTextPath(text, 0, text.length(), mCenter.x - (mTextWidth / 2f), mCenter.y + (mTextHeight / 2f), path);

        PathMeasure measure = new PathMeasure(path, false);
        float length = measure.getLength();

//        PathEffect effect = new DashPathEffect(new float[] { length, length }, 1.0f);
//        paint.setPathEffect(effect);

//        canvas.drawPath(path, mTextPaint);
        super.onDraw(canvas);

//        Log.i("TAG", mCenter.toString() + ". Path: " + path.toString());
    }

}
