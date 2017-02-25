package com.scipianus.pocketlibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.RETR_LIST;
import static org.opencv.imgproc.Imgproc.approxPolyDP;
import static org.opencv.imgproc.Imgproc.arcLength;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.getPerspectiveTransform;
import static org.opencv.imgproc.Imgproc.warpPerspective;

public class ViewPictureActivity extends AppCompatActivity {
    private static String IMAGE_EXTRA = "picture";
    private String mCurrentPhotoPath;
    private Bitmap mImageBitmap;
    private Mat mImage;
    private Mat mEdges;
    private Mat mCroppedImage;
    private MatOfPoint mContour;
    private int nrDisplays = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_picture);

        Bundle extras = getIntent().getExtras();
        mCurrentPhotoPath = extras.getString(IMAGE_EXTRA);

        if (mCurrentPhotoPath == null)
            return;

        mImage = new Mat();
        mImageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
        Bitmap bmp32 = mImageBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, mImage);

        displayImage(mImage);

        mEdges = edgeDetection(mImage);

        displayImage(mEdges);

        mContour = contoursFinding(mEdges);

        displayImage(addContourToImage(mImage, mContour));

        mCroppedImage = transformPerspective(mImage, mContour);

        displayImage(mCroppedImage);
    }

    private Mat edgeDetection(Mat image) {
        Mat grayImage = new Mat();
        Mat edges = new Mat();

        cvtColor(image, grayImage, COLOR_BGR2GRAY);
        GaussianBlur(grayImage, grayImage, new Size(5, 5), 0);
        Canny(grayImage, edges, 75, 200);

        return edges;
    }

    private MatOfPoint contoursFinding(Mat edges) {
        final List<MatOfPoint> contours = new ArrayList<>();
        MatOfPoint contour = new MatOfPoint();

        findContours(edges, contours, new Mat(), RETR_LIST, CHAIN_APPROX_SIMPLE);
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                return contourArea(o1) > contourArea(o2) ? -1 : 1;
            }
        });

        for (MatOfPoint c : contours) {
            MatOfPoint2f c2f = new MatOfPoint2f();
            MatOfPoint2f approx = new MatOfPoint2f();
            c.convertTo(c2f, CvType.CV_32FC2);

            double perimeter = arcLength(c2f, true);
            approxPolyDP(c2f, approx, 0.02 * perimeter, true);

            if (approx.toList().size() == 4) {
                approx.convertTo(contour, CvType.CV_32S);
                return contour;
            }
        }

        return null;
    }

    private Mat transformPerspective(Mat image, MatOfPoint contour) {
        Mat transformedImage = new Mat();
        List<Point> corners = sortRectCorners(contour.toList());
        Point topLeft = corners.get(0);
        Point topRight = corners.get(1);
        Point bottomRight = corners.get(2);
        Point bottomLeft = corners.get(3);

        double widthA = Math.sqrt(Math.pow(bottomRight.x - bottomLeft.x, 2) + Math.pow(bottomRight.y - bottomLeft.y, 2));
        double widthB = Math.sqrt(Math.pow(topRight.x - topLeft.x, 2) + Math.pow(topRight.y - topLeft.y, 2));
        final int maxWidth = Math.max((int) widthA, (int) widthB);

        double heightA = Math.sqrt(Math.pow(topRight.x - bottomRight.x, 2) + Math.pow(topRight.y - bottomRight.y, 2));
        double heightB = Math.sqrt(Math.pow(topLeft.x - bottomLeft.x, 2) + Math.pow(topLeft.y - bottomLeft.y, 2));
        final int maxHeight = Math.max((int) heightA, (int) heightB);

        List<Point> dst = new ArrayList<Point>() {{
            add(new Point(0, 0));
            add(new Point(maxWidth - 1, 0));
            add(new Point(maxWidth - 1, maxHeight - 1));
            add(new Point(0, maxHeight - 1));
        }};

        Mat m = getPerspectiveTransform(
                Converters.vector_Point2f_to_Mat(corners),
                Converters.vector_Point2f_to_Mat(dst));

        warpPerspective(image, transformedImage, m, new Size(maxWidth, maxHeight));

        return transformedImage;
    }

    private List<Point> sortRectCorners(final List<Point> corners) {
        List<Point> res = new ArrayList<>();
        List<Integer> idxSum = new ArrayList<Integer>() {{
            for (int i = 0; i < corners.size(); ++i)
                add(i);
        }};
        List<Integer> idxDiff = new ArrayList<>(idxSum);

        Collections.sort(idxSum, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                Point A, B;
                A = corners.get(o1);
                B = corners.get(o2);
                return (A.x + A.y) < (B.x + B.y) ? -1 : 1;
            }
        });

        Collections.sort(idxDiff, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                Point A, B;
                A = corners.get(o1);
                B = corners.get(o2);
                return (A.x - A.y) > (B.x - B.y) ? -1 : 1;
            }
        });

        res.add(corners.get(idxSum.get(0)));
        res.add(corners.get(idxDiff.get(0)));
        res.add(corners.get(idxSum.get(3)));
        res.add(corners.get(idxDiff.get(3)));

        return res;
    }

    private void displayImage(final Mat image) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(image, bmp);
                ImageView imageView = (ImageView) findViewById(R.id.imageView);
                imageView.setImageBitmap(bmp);
            }
        }, nrDisplays * 1000);
        nrDisplays++;
    }

    private Mat addContourToImage(Mat image, MatOfPoint contour) {
        Mat imageWithContour = image.clone();
        List<MatOfPoint> contours = new ArrayList<>();
        contours.add(contour);
        drawContours(imageWithContour, contours, 0, new Scalar(0, 255, 0), 20);
        return imageWithContour;
    }
}
