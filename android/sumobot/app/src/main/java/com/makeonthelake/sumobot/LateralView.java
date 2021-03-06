package com.makeonthelake.sumobot;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.support.v4.view.MotionEventCompat;


public class LateralView extends View {
    private static final String DEBUG_TAG = "LATERAL_VIEW";

    private static final int IDLE = 1001;
    private static final int ACTIVE = 1002;

    private int TRACK_WIDTH = 20;
    private int TRACK_HEIGHT = 20;
    private int SLIDER_HEIGHT = 175;
    private int SLIDER_WIDTH = 308;

    private OnLaterViewChangeListener changeListener;
    private Drawable trackDrawable;
    private Drawable controlDrawable;
    private int paddingLeft;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;
    private int contentWidth;
    private int contentHeight;
    private Rect trackLocation;
    private Rect sliderDockedLocation;
    private Rect sliderMoveLocation = new Rect();
    private int state = IDLE;
    private int lastTouchY = 0;
    private int trackStartX;
    private int trackStartY;

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

        if (a.hasValue(R.styleable.LateralView_trackDrawable)) {
            trackDrawable = a.getDrawable(
                    R.styleable.LateralView_trackDrawable);
            trackDrawable.setCallback(this);
            Bitmap trackBitmap = ((BitmapDrawable) trackDrawable).getBitmap();
            TRACK_HEIGHT = trackBitmap.getHeight();
            TRACK_WIDTH = trackBitmap.getWidth();
        }

        if (a.hasValue(R.styleable.LateralView_controlDrawable)) {
            controlDrawable = a.getDrawable(
                    R.styleable.LateralView_controlDrawable);
            controlDrawable.setCallback(this);
            Bitmap controlBitmap = ((BitmapDrawable) controlDrawable).getBitmap();
            SLIDER_HEIGHT = controlBitmap.getHeight();
            SLIDER_WIDTH = controlBitmap.getWidth();
        }

        a.recycle();

        Log.d(DEBUG_TAG, "initialized");
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

        int startX = (getWidth() / 2) - (SLIDER_WIDTH / 2);
        int startY = (getHeight() / 2) - (SLIDER_HEIGHT / 2);
        sliderDockedLocation = new Rect(startX, startY, startX + SLIDER_WIDTH, startY + SLIDER_HEIGHT);

        trackStartX = (getWidth() / 2) - (TRACK_WIDTH / 2);
        trackStartY = (getHeight() / 2) - (TRACK_HEIGHT / 2);
        trackLocation = new Rect(trackStartX - TRACK_WIDTH / 2, trackStartY, trackStartX + TRACK_WIDTH / 2, trackStartY + TRACK_HEIGHT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawTrack(canvas);
        drawSlider(canvas);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
        super.onSizeChanged(width, height, oldwidth, oldheight);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        int screenX = (int) event.getRawX();
        int screenY = (int) event.getRawY();
        int[] location = new int[2];
        getLocationOnScreen(location);
        int x = screenX - location[0];
        int y = screenY - location[1];


        switch (action) {
            case (MotionEvent.ACTION_DOWN):
                Log.d(DEBUG_TAG, "Action was DOWN");
                sliderMoveLocation = new Rect(x, y, x + 15, y + 20);
                lastTouchY = y;

                Log.d(DEBUG_TAG, "Hit:  " + Rect.intersects(sliderDockedLocation, sliderMoveLocation));
                if (Rect.intersects(sliderDockedLocation, sliderMoveLocation)) {
                    sliderMoveLocation = new Rect(sliderDockedLocation);
                    state = ACTIVE;
                }
                return true;
            case (MotionEvent.ACTION_MOVE):
                if (state != ACTIVE)
                    return false;

                int distanceMovedY = y - lastTouchY;
                sliderMoveLocation.offset(0, distanceMovedY);

                if (sliderMoveLocation.exactCenterY() <= trackStartY -1) {
                    sliderMoveLocation.offset(0, -distanceMovedY);
                } else if (sliderMoveLocation.exactCenterY() >= trackStartY + TRACK_HEIGHT + 1) {
                    sliderMoveLocation.offset(0, -distanceMovedY);
                }


                int change;
                int trackSectionHeight = TRACK_HEIGHT / 2;

                if (sliderMoveLocation.exactCenterY() < trackLocation.exactCenterY()) {
                    int normalizedCenter = sliderMoveLocation.centerY() - trackLocation.top;
                    change = (int) Math.min(100 - (((float) normalizedCenter / trackSectionHeight) * 100), 100);
                    changeListener.onMoveForward(change);
                } else if (sliderMoveLocation.exactCenterY() > trackLocation.exactCenterY()) {
                    int normalizedCenter = sliderMoveLocation.centerY() - trackLocation.centerY();
                    change = (int) Math.min((float) normalizedCenter / trackSectionHeight * 100, 100);
                    changeListener.onMoveBackward(change);

                }

                lastTouchY = y;
                invalidate();
                return true;

            case (MotionEvent.ACTION_UP):
                Log.d(DEBUG_TAG, "Action was UP");
                state = IDLE;
                changeListener.onMoveStop();
                invalidate();
                return true;
            case (MotionEvent.ACTION_CANCEL):
                state = IDLE;
                Log.d(DEBUG_TAG, "Action was CANCEL");
                return true;
            case (MotionEvent.ACTION_OUTSIDE):
                Log.d(DEBUG_TAG, "Movement occurred outside bounds " +
                        "of current screen element");
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    public void setOnLaterViewChangeListener(OnLaterViewChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    private void drawSlider(Canvas canvas) {
        if (controlDrawable == null)
            return;

        if (state == IDLE) {
            controlDrawable.setBounds(sliderDockedLocation);
        } else {
            controlDrawable.setBounds(sliderMoveLocation);
        }

        controlDrawable.draw(canvas);
    }

    private void drawTrack(Canvas canvas) {
        if (trackDrawable == null)
            return;

        trackDrawable.setBounds(trackStartX, trackStartY,
                trackStartX + TRACK_WIDTH, trackStartY + TRACK_HEIGHT);
        trackDrawable.draw(canvas);
    }

    public Drawable getTrackDrawable() {
        return trackDrawable;
    }

    public void setTrackDrawableDrawable(Drawable trackDrawable) {
        this.trackDrawable = trackDrawable;
    }
}
