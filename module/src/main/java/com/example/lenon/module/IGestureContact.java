/*****************************************
 * Package Name: com.example.lenon.module
 * Function:
 *
 * @author LENON
 * CREATED AT 2017/8/11 17:55
 *******************************************/
package com.example.lenon.module;

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.widget.FrameLayout;
import android.widget.ImageView;

public interface IGestureContact {
    interface IView {

        FrameLayout getContainer();

        ImageView getImageView();

        Matrix getContainerMatrix();

        void setFirstZoom();

    }

    interface IPresenter {

        void actionDown(PointF point);

        void multiActionDown(PointF point0, PointF point1);

        void moveLocalCurrent(PointF point);

        void checkBoundary();

        void moveRebounce();

        void zoomAction(PointF currPoint0, PointF currPoint1);

        void doubleTapsAction(PointF currPoint0);

        boolean saveAndUpdate(Matrix matrix);

        void initMatrix(Point point);

        void initBoundary();

        void resetLocal();

        float getCurrentScale();

        void scaleRebounce();
    }
}
