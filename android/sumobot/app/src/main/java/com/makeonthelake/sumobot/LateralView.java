package com.makeonthelake.sumobot;

import android.content.Context;

import com.makeonthelake.sumobot.R;

import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


public class LateralView extends View {
    private static final int TRACK_WIDTH = 20;
    private static final int SLIDER_WIDTH = 308;
    private static final int SLIDER_HEIGHT = 175;
    private int mExampleColor = Color.RED;
    private float mExampleDimension = 0;
    private Drawable mExampleDrawable;
    private Drawable trackDrawable;
    private Drawable controlDrawable;
    private int paddingLeft;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;
    private int contentWidth;
    private int contentHeight;


    public LateralView(Context context) {
        super(context);
        init(null, 0);
    }

    public LateralView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public LateralView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.LateralView, defStyle, 0);

        mExampleColor = a.getColor(
                R.styleable.LateralView_exampleColor,
                mExampleColor);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mExampleDimension = a.getDimension(
                R.styleable.LateralView_exampleDimension,
                mExampleDimension);

        if (a.hasValue(R.styleable.LateralView_exampleDrawable)) {
            mExampleDrawable = a.getDrawable(
                    R.styleable.LateralView_exampleDrawable);
            mExampleDrawable.setCallback(this);
        }

        trackDrawable = getContext().getResources().getDrawable(R.mipmap.track);
        controlDrawable = getContext().getResources().getDrawable(R.mipmap.slider);

        a.recycle();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        paddingLeft = getPaddingLeft();
        paddingTop = getPaddingTop();
        paddingRight = getPaddingRight();
        paddingBottom = getPaddingBottom();

        contentWidth = getWidth() - paddingLeft - paddingRight;
        contentHeight = getHeight() - paddingTop - paddingBottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawTrack(canvas);
        drawSlider(canvas);
    }

    private void drawSlider(Canvas canvas) {
        int startX = (getWidth() / 2) - (SLIDER_WIDTH / 2);
        int startY = (getHeight() / 2) - (SLIDER_HEIGHT / 2);
        controlDrawable.setBounds(startX, startY,
                startX + SLIDER_WIDTH, startY + SLIDER_HEIGHT);
        controlDrawable.draw(canvas);


    }

    private void drawTrack(Canvas canvas) {
        int startX = (getWidth() / 2) - (TRACK_WIDTH / 2);
        trackDrawable.setBounds(startX, paddingTop,
                startX + TRACK_WIDTH, paddingTop + contentHeight);
        trackDrawable.draw(canvas);
    }


    /**
     * Gets the example color attribute value.
     *
     * @return The example color attribute value.
     */
    public int getExampleColor() {
        return mExampleColor;
    }

    /**
     * Sets the view's example color attribute value. In the example view, this color
     * is the font color.
     *
     * @param exampleColor The example color attribute value to use.
     */
    public void setExampleColor(int exampleColor) {
        mExampleColor = exampleColor;
    }

    /**
     * Gets the example dimension attribute value.
     *
     * @return The example dimension attribute value.
     */
    public float getExampleDimension() {
        return mExampleDimension;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setExampleDimension(float exampleDimension) {
        mExampleDimension = exampleDimension;
    }

    /**
     * Gets the example drawable attribute value.
     *
     * @return The example drawable attribute value.
     */
    public Drawable getExampleDrawable() {
        return mExampleDrawable;
    }

    /**
     * Sets the view's example drawable attribute value. In the example view, this drawable is
     * drawn above the text.
     *
     * @param exampleDrawable The example drawable attribute value to use.
     */
    public void setExampleDrawable(Drawable exampleDrawable) {
        mExampleDrawable = exampleDrawable;
    }
}
