package com.scipianus.pocketlibrary.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;

import com.scipianus.pocketlibrary.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by scipianus on 19-Mar-17.
 */

public class CroppableImageView extends View {

    private Point[] points = new Point[4];
    private ArrayList<ColorBall> colorballs = new ArrayList<>();
    private int currentBall = 0;
    private Paint mPaint;
    private Canvas mCanvas;
    private Path mPath;
    private Bitmap mImageBitmap;
    private Size mSize;

    public CroppableImageView(Context context) {
        super(context);
        mPaint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        mCanvas = new Canvas();
        mPath = new Path();
    }

    public CroppableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPaint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        mCanvas = new Canvas();
        mPath = new Path();
    }

    public CroppableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        mCanvas = new Canvas();
        mPath = new Path();
    }

    public void setImageBitmap(Bitmap bmp, Size size) {
        mImageBitmap = bmp;
        mSize = size;
        invalidate();
    }

    public void setCorners(List<Point> pointList) {
        colorballs.clear();
        for (int i = 0; i < 4; ++i) {
            points[i] = pointList.get(i);
            colorballs.add(new ColorBall(getContext(), R.drawable.corner_circle, points[i]));
        }
        invalidate();
    }

    public Point[] getPoints() {
        return points;
    }

    // the method that draws the balls
    @Override
    protected void onDraw(Canvas canvas) {
        if (colorballs.size() == 0)
            return;
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeJoin(Paint.Join.ROUND);

        //draw image
        if (mImageBitmap != null) {
            Rect srcRect = new Rect(0, 0, mImageBitmap.getWidth(), mImageBitmap.getHeight());
            Rect destRect = new Rect(0, 0, mSize.getWidth(), mSize.getHeight());
            canvas.drawBitmap(mImageBitmap, srcRect, destRect, mPaint);
        }

        //draw polygon
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(getResources().getColor(R.color.colorCropRect));
        mPaint.setStrokeWidth(10);

        mPath.reset();
        mPath.moveTo(colorballs.get(0).getX(), colorballs.get(0).getY());
        mPath.lineTo(colorballs.get(1).getX(), colorballs.get(1).getY());
        mPath.lineTo(colorballs.get(2).getX(), colorballs.get(2).getY());
        mPath.lineTo(colorballs.get(3).getX(), colorballs.get(3).getY());
        mPath.lineTo(colorballs.get(0).getX(), colorballs.get(0).getY());
        canvas.drawPath(mPath, mPaint);

        // draw the balls on the mCanvas
        mPaint.setColor(Color.BLUE);
        mPaint.setStrokeWidth(0);
        for (int i = 0; i < colorballs.size(); i++) {
            ColorBall ball = colorballs.get(i);
            canvas.drawBitmap(
                    ball.getBitmap(),
                    ball.getX() - ball.getWidthOfBall() / 2,
                    ball.getY() - ball.getHeightOfBall() / 2,
                    mPaint);
        }
    }

    // events when touching the screen
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        int X = (int) event.getX();
        int Y = (int) event.getY();

        switch (action) {

            case MotionEvent.ACTION_DOWN: // touch down so check if the finger is on a ball
                currentBall = -1;
                for (int i = 0; i < 4; ++i) {
                    ColorBall ball = colorballs.get(i);
                    // check if inside the bounds of the ball (circle)
                    // get the center for the ball
                    int centerX = ball.getX();
                    int centerY = ball.getY();
                    // calculate the radius from the touch to the center of the ball
                    double radCircle = Math
                            .sqrt((double) (((centerX - X) * (centerX - X)) + (centerY - Y)
                                    * (centerY - Y)));

                    if (radCircle < ball.getWidthOfBall()) {
                        currentBall = i;
                        invalidate();
                        break;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE: // touch drag with the ball
                if (currentBall > -1) {
                    // move the ball the same as the finger
                    colorballs.get(currentBall).setX(X);
                    colorballs.get(currentBall).setY(Y);
                    points[currentBall].x = X;
                    points[currentBall].y = Y;
                    invalidate();
                }
                break;

            default:
                break;
        }
        // redraw the mCanvas
        invalidate();
        return true;
    }

    private static class ColorBall {

        private Bitmap bitmap;
        private Point point;

        public ColorBall(Context context, int resourceId, Point point) {
            this.bitmap = drawableToBitmap(context.getDrawable(resourceId));
            this.point = point;
        }

        public int getWidthOfBall() {
            return (bitmap != null ? bitmap.getWidth() : 0);
        }

        public int getHeightOfBall() {
            return (bitmap != null ? bitmap.getHeight() : 0);
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public int getX() {
            return point.x;
        }

        public int getY() {
            return point.y;
        }

        public void setX(int x) {
            point.x = x;
        }

        public void setY(int y) {
            point.y = y;
        }

        private Bitmap drawableToBitmap(Drawable drawable) {

            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            }

            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            return bitmap;
        }
    }
}