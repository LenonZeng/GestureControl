/*****************************************
 * Package Name: com.example.lenon.module
 * Function:
 *
 * @author LENON
 * CREATED AT 2017/8/11 17:53
 *******************************************/
package com.example.lenon.module;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;

public class GesturePresenter implements IGestureContact.IPresenter {
    private float[] centerPointF = new float[2];
    private float[] dstCenter = new float[2];
    private Matrix origMatrix = new Matrix();
    private Matrix curMatrix = new Matrix();
    private Rect initRect = new Rect();
    private Rect currRect = new Rect();
    private Point offset = new Point();
    private PointF origValue = new PointF();
    private PointF lastMultiPoint0 = new PointF();
    private PointF lastMultiPoint1 = new PointF();
    private static final float MAX_ZOOM_SCALE = 5.0f;
    private static final float MIN_ZOOM_SCALE = 1.0f;
    private static final float FLOAT_TYPE = 1.0f;
    private float mCurrentScale = MIN_ZOOM_SCALE;
    IGestureContact.IView mView;
    private boolean outBoundary = false;
    private boolean moreScale = false;
    private boolean lessScale = false;
    private float exceedRatio = 1.0f;
    private Point transRebounce = new Point();

    public GesturePresenter(IGestureContact.IView view) {
        mView = view;
    }

    @Override
    public void actionDown(PointF point) {
        lastMultiPoint0 = point;
    }

    @Override
    public void multiActionDown(PointF point0, PointF point1) {
        lastMultiPoint0 = point0;
        lastMultiPoint1 = point1;
    }

    @Override
    public void moveLocalCurrent(PointF currPoint) {
        if (currPoint == null) {
            return;
        }

        PointF trans = new PointF();
        trans.x = currPoint.x - lastMultiPoint0.x;
        trans.y = currPoint.y - lastMultiPoint0.y;

        curMatrix.postTranslate(trans.x, trans.y);
        saveAndUpdate(curMatrix);

        lastMultiPoint0 = currPoint;
    }

    @Override
    public void checkBoundary() {
        Rect initRectTmp = new Rect(offset.x + initRect.left, offset.y + initRect.top,
                offset.x + initRect.right, offset.y + initRect.bottom);
        boolean conditionA = (currRect.top > initRectTmp.top);
        boolean conditionB = (currRect.bottom < initRectTmp.bottom);
        boolean conditionC = (currRect.left > initRectTmp.left);
        boolean conditionD = (currRect.right < initRectTmp.right);

        transRebounce.set(0, 0);
        if (conditionA) {
            transRebounce.y = -(currRect.top - initRectTmp.top);
            Log.i("conditionA", " top out");
            outBoundary = true;
        }

        if (conditionB) {
            transRebounce.y = -(currRect.bottom - initRectTmp.bottom);
            Log.i("conditionB", " bottom out");
            outBoundary = true;
        }

        if (conditionC) {
            transRebounce.x = -(currRect.left - initRectTmp.left);
            outBoundary = true;
        }

        if (conditionD) {
            transRebounce.x = -(currRect.right - initRectTmp.right);
            outBoundary = true;
        }
    }

    @Override
    public void moveRebounce() {
        if (outBoundary) {
            curMatrix.postTranslate(transRebounce.x, transRebounce.y);
            saveAndUpdate(curMatrix);
            outBoundary = false;
        }
    }

    @Override
    public void zoomAction(PointF currPoint0, PointF currPoint1) {
        if (currPoint0 == null || currPoint1 == null) {
            return;
        }

        PointF center = new PointF();

        /////////////////////////// 缩放.
        float currDistance = distance(currPoint0, currPoint1);
        float lastDistance = distance(lastMultiPoint0, lastMultiPoint1);
        float scale = currDistance / lastDistance;

        curMatrix.postScale(
                scale,
                scale,
                (currPoint0.x + currPoint1.x) / 2 - offset.x - mView.getContainer().getWidth() / 2,
                (currPoint0.y + currPoint1.y) / 2 - offset.y - mView.getContainer().getHeight() / 2); //依据图片中心距离放大
        mCurrentScale *= scale;
        saveAndUpdate(curMatrix);
        center.x = dstCenter[0];
        center.y = dstCenter[1];

        //缩放回弹设置
        if (mCurrentScale > MAX_ZOOM_SCALE) {
            exceedRatio = MAX_ZOOM_SCALE / mCurrentScale;
            moreScale = true;
        } else if (mCurrentScale < MIN_ZOOM_SCALE) {
            lessScale = true;
        }

        lastMultiPoint0 = currPoint0;
        lastMultiPoint1 = currPoint1;
    }

    @Override
    public void doubleTapsAction(PointF currPoint0) {
        if (mCurrentScale <= MIN_ZOOM_SCALE) {
            mCurrentScale = MAX_ZOOM_SCALE;
            curMatrix.reset();
            curMatrix.postScale(
                    MAX_ZOOM_SCALE,
                    MAX_ZOOM_SCALE,
                    currPoint0.x - offset.x - mView.getContainer().getWidth() / 2,
                    currPoint0.y - offset.y - mView.getContainer().getHeight() / 2);
            saveAndUpdate(curMatrix);
            PointF center = new PointF();
            center.x = dstCenter[0];
            center.y = dstCenter[1];
        } else {
            resetLocal();
        }
    }

    @Override
    public boolean saveAndUpdate(Matrix matrix) {
        setMatrix2View(mView.getContainer(), matrix);

        currRect.setEmpty();
        mView.getContainer().getHitRect(currRect);
        dstCenter[0] = (currRect.right + currRect.left) / 2;
        dstCenter[1] = (currRect.top + currRect.bottom) / 2;
        return true;
    }


    @Override
    public void initMatrix(Point point) {
        if (mView != null && mView.getContainer() != null)
            origValue = new PointF(mView.getImageView().getLayoutParams().width,
                    mView.getImageView().getLayoutParams().height);
        centerPointF[0] = offset.x + origValue.x / 2;
        centerPointF[1] = offset.y + origValue.y / 2;
        curMatrix.set(mView.getContainer().getMatrix());
        resetLocal();
        mView.setFirstZoom();
    }

    @Override
    public void initBoundary() {
        mView.getImageView().getHitRect(initRect);
        int w1 = initRect.right;
        int l1 = initRect.bottom;

        Matrix m = mView.getImageView().getImageMatrix();
        float[] values = new float[10];
        m.getValues(values);
        float sx = values[0];
        float sy = values[4];
        BitmapDrawable bm = (BitmapDrawable) mView.getImageView().getDrawable();
        int w2 = (int) (bm.getBitmap().getWidth() * sx);
        int l2 = (int) (bm.getBitmap().getHeight() * sy);
        initRect.set((w1 - w2) / 2, (l1 - l2) / 2, (w1 + w2) / 2, (l1 + l2) / 2);
    }

    @Override
    public void resetLocal() {
        mCurrentScale = MIN_ZOOM_SCALE;
        curMatrix.reset();
        curMatrix.set(origMatrix);
        centerPointF[0] = offset.x + origValue.x / 2;
        centerPointF[1] = offset.y + origValue.y / 2;
        saveAndUpdate(curMatrix);
    }

    @Override
    public float getCurrentScale() {
        return mCurrentScale;
    }

    @Override
    public void scaleRebounce() {
        if (moreScale) {
            mCurrentScale *= exceedRatio;
            PointF anchorTmp = new PointF((lastMultiPoint0.x + lastMultiPoint1.x) / 2,
                    (lastMultiPoint0.y + lastMultiPoint1.y) / 2);
            curMatrix.postScale(exceedRatio, exceedRatio, anchorTmp.x - offset.x - mView.getImageView().getWidth() / 2,
                    anchorTmp.y - offset.y - mView.getImageView().getHeight() / 2);
            saveAndUpdate(curMatrix);
            PointF center = new PointF();
            center.x = dstCenter[0];
            center.y = dstCenter[1];
            moreScale = false;
        }

        if (lessScale) {
            mCurrentScale = MIN_ZOOM_SCALE;
            curMatrix.reset();
            saveAndUpdate(origMatrix);
            mView.setFirstZoom();
            lessScale = false;
        }
    }

    private float distance(PointF point0, PointF point1) {
        return (float) Math.sqrt(
                (point0.x - point1.x) * (point0.x - point1.x)
                        + (point0.y - point1.y) * (point0.y - point1.y)
        );
    }

    public static PointF getTrans(Matrix matrix) {
        if (matrix == null) {
            return null;
        }

        float[] matrixValues = new float[9];
        matrix.getValues(matrixValues);

        float x = matrixValues[Matrix.MTRANS_X];
        float y = matrixValues[Matrix.MTRANS_Y];

        return new PointF(x, y);
    }

    public void setMatrix2View(View view, Matrix matrix) {
        float rotate = getDegree(matrix);
        PointF scale = getScale(matrix);
        final PointF trans = getTrans(matrix);
        ObjectAnimator animRotate = ObjectAnimator.ofFloat(view, "rotation", 0 - rotate);
        ObjectAnimator animTransX = ObjectAnimator.ofFloat(view, "translationX", trans.x);
        ObjectAnimator animTransY = ObjectAnimator.ofFloat(view, "translationY", trans.y);
        ObjectAnimator animScaleX = ObjectAnimator.ofFloat(view, "scaleX", scale.x);
        ObjectAnimator animScaleY = ObjectAnimator.ofFloat(view, "scaleY", scale.y);

        animRotate.setDuration(0);
        animTransX.setDuration(0);
        animTransY.setDuration(0);
        animScaleX.setDuration(0);
        animScaleY.setDuration(0);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animRotate, animScaleX, animScaleY, animTransX, animTransY);
        set.start();
    }

    /**
     * @return 旋转角度数
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public static float getDegree(Matrix matrix) {
        PointF skew = getMatrixSkew(matrix);
        PointF scale = getMatrixScale(matrix);
        return (float) Math.toDegrees(Math.atan2(skew.x, scale.x));
    }

    public static PointF getScale(Matrix matrix) {
        PointF skew = getMatrixSkew(matrix);
        PointF scale = getMatrixScale(matrix);
        float scaleX = (float) Math.sqrt(scale.x * scale.x + skew.y * skew.y);
        float scaleY = (float) Math.sqrt(scale.y * scale.y + skew.x * skew.x);
        return new PointF(scaleX, scaleY);
    }

    /**
     * 在变化矩阵中获取旋转弧度
     *
     * @param matrix 变换矩阵
     * @return x轴和y轴上的旋转弧度
     */
    public static PointF getMatrixSkew(Matrix matrix) {
        if (matrix == null) {
            return null;
        }

        float[] matrixValues = new float[9];
        matrix.getValues(matrixValues);

        float x = matrixValues[Matrix.MSKEW_X];
        float y = matrixValues[Matrix.MSKEW_Y];

        return new PointF(x, y);
    }

    /**
     * 在变化矩阵中获取缩放因子
     *
     * @param matrix 变换矩阵
     * @return x轴和y轴上的缩放因子
     */
    public static PointF getMatrixScale(Matrix matrix) {
        if (matrix == null) {
            return null;
        }

        float[] matrixValues = new float[9];
        matrix.getValues(matrixValues);

        float x = matrixValues[Matrix.MSCALE_X];
        float y = matrixValues[Matrix.MSCALE_Y];

        return new PointF(x, y);
    }

}
