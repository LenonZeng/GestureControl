/*****************************************
 * Package Name: com.example.lenon.module
 * Function:
 *
 * @author LENON
 * CREATED AT 2017/8/11 17:55
 *******************************************/
package com.example.lenon.module;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class GestureControlLayout extends FrameLayout implements IGestureContact.IView {
    private GestureDetectorCompat mDetector;
    private IGestureContact.IPresenter mPresenter;
    private PointF lastMultiPoint0 = new PointF();
    private PointF lastMultiPoint1 = new PointF();
    private static final int ZOOM = 2;
    private static final int DRAG = 1;
    private static final int NONE = 0;
    private boolean firstZoom = false;
    private int mStatus = NONE;
    private View contentView;
    private Drawable mDrawable;
    private ImageView ivImage;
    private FrameLayout mContainer;
    private LinearLayout llContainer;

    public GestureControlLayout(@NonNull Context context) {
        super(context);
    }

    public GestureControlLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public GestureControlLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        contentView = inflater.inflate(R.layout.gesture_control_layout, this);
        ivImage = (ImageView) contentView.findViewById(R.id.iv_source);
        mContainer = (FrameLayout) contentView.findViewById(R.id.fl_image_container);
        llContainer = (LinearLayout)contentView.findViewById(R.id.ll_image_container2);

        mDetector = new GestureDetectorCompat(getContext(), new GestureListener());
        mDetector.setOnDoubleTapListener(new DoubleTapListener());

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.GestureControlLayout, 0, 0);
        try {
            for (int i = 0; i < array.getIndexCount(); i++) {
                int i1 = array.getIndex(i);
                if (i1 == R.styleable.GestureControlLayout_imagesrc) {
                    mDrawable = array.getDrawable(i1);
                    ivImage.setDrawingCacheEnabled(true);
                    ivImage.setImageDrawable(mDrawable);
                }
            }
        } finally {
            array.recycle();
        }


        mPresenter = new GesturePresenter(this);

        llContainer.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleGesture(event);
            }
        });
        ivImage.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mPresenter.initBoundary();
            }
        });
    }

    public ImageView getImageView() {
        return ivImage;
    }

    private boolean handleGesture(MotionEvent event) {
        mDetector.onTouchEvent(event);
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                lastMultiPoint0.set(event.getX(), event.getY());
                mPresenter.actionDown(lastMultiPoint0);
                mStatus = DRAG;
                break;
            case MotionEvent.ACTION_UP:
                mPresenter.moveRebounce();
                if (mStatus == ZOOM) {
                    mPresenter.scaleRebounce();
                }
                mStatus = NONE;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mStatus = ZOOM;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                lastMultiPoint0.set(event.getX(0), event.getY(0));
                lastMultiPoint1.set(event.getX(1), event.getY(1));
                mPresenter.multiActionDown(lastMultiPoint0, lastMultiPoint1);
                mStatus = ZOOM;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mStatus == DRAG && firstZoom) {
                    PointF currPoint = new PointF(event.getX(0), event.getY(0));
                    mPresenter.moveLocalCurrent(currPoint);
                    mPresenter.checkBoundary();
                } else if (mStatus == ZOOM) {
                    if (event.getPointerCount() == 1)
                        return true;
                    firstZoom = true;
                    PointF currPoint0 = new PointF(event.getX(0), event.getY(0));
                    PointF currPoint1 = new PointF(event.getX(1), event.getY(1));
                    mPresenter.zoomAction(currPoint0, currPoint1);
                    mPresenter.checkBoundary();
                } else {
                    return true;
                }
                break;
            default:
                break;
        }
        return true;
    }

    private class GestureListener implements GestureDetector.OnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    private class DoubleTapListener implements GestureDetector.OnDoubleTapListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            PointF currPoint0 = new PointF(e.getX(0), e.getY(0));
            mPresenter.doubleTapsAction(currPoint0);
            firstZoom = (mPresenter.getCurrentScale()) <= 1.0f ? false : true;
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }
    }

    @Override
    public FrameLayout getContainer() {
        return mContainer;
    }

    @Override
    public Matrix getContainerMatrix() {
        return getMatrix();
    }

    @Override
    public void setFirstZoom() {
        firstZoom = false;
    }
}
