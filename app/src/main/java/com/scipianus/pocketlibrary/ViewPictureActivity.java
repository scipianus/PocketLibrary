package com.scipianus.pocketlibrary;

import android.app.ActionBar;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.RETR_EXTERNAL;
import static org.opencv.imgproc.Imgproc.approxPolyDP;
import static org.opencv.imgproc.Imgproc.arcLength;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.getPerspectiveTransform;
import static org.opencv.imgproc.Imgproc.isContourConvex;
import static org.opencv.imgproc.Imgproc.resize;
import static org.opencv.imgproc.Imgproc.warpPerspective;

public class ViewPictureActivity extends AppCompatActivity {
    private static final String IMAGE_EXTRA = "picture";
    private final String LANGUAGE = "eng";
    private final double EPS = 0.0000000001;
    private final double HEIGHT = 750;
    private double mRatio;
    private TessBaseAPI mTessBaseAPI;
    private String mTessDataPath;
    private String mCurrentPhotoPath;
    private Bitmap mImageBitmap;
    private Mat mInitialImage;
    private Mat mImage;
    private Mat mEdges;
    private Mat mCroppedImage;
    private MatOfPoint mContour;
    private Size mOriginalSize;
    private int mCurrentStep = 0;
    private Button mNextStepButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_picture);

        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        mCurrentPhotoPath = extras.getString(IMAGE_EXTRA);

        if (mCurrentPhotoPath == null)
            return;

        readImage(mCurrentPhotoPath);
        displayImage(mImage);

        mNextStepButton = (Button) findViewById(R.id.nextStepButton);
        mNextStepButton.setText(R.string.find_contour);
    }

    public void performNextStep(View view) {
        switch (mCurrentStep) {
            case 0:
                mEdges = edgeDetection(mImage);
                displayImage(mEdges);
                mCurrentStep++;
                mNextStepButton.setText(R.string.detect_rectangle);
                break;
            case 1:
                mContour = contoursFinding(mEdges);
                if (mContour == null) {
                    Toast.makeText(this, "Could not find a rectangle", Toast.LENGTH_LONG).show();
                    displayImage(mImage);
                    mNextStepButton.setVisibility(View.GONE);
                } else {
                    displayImage(addContourToImage(mImage, mContour));
                }
                mCurrentStep++;
                mNextStepButton.setText(R.string.adjust_perspective);
                break;
            case 2:
                mCroppedImage = transformPerspective(mInitialImage, mContour);
                displayImage(mCroppedImage);
                mCurrentStep++;
                mNextStepButton.setText(R.string.run_ocr);
                break;
            case 3:
                initTesseract();
                detectText(mCroppedImage);
                mNextStepButton.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }

    private void readImage(String path) {
        mImageBitmap = BitmapFactory.decodeFile(path);
        Bitmap bmp32 = mImageBitmap.copy(Bitmap.Config.ARGB_8888, true);

        mImage = new Mat();
        Utils.bitmapToMat(bmp32, mImage);
        mInitialImage = mImage.clone();
        mOriginalSize = mImage.size();
        mRatio = mImage.height() / HEIGHT;
        resize(mImage, mImage, new Size(mImage.width() / mRatio, HEIGHT));
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
        final MatOfPoint contour = new MatOfPoint();
        MatOfPoint bestContour = null;

        findContours(edges, contours, new Mat(), RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                double area1 = contourArea(o1);
                double area2 = contourArea(o2);
                if (Math.abs(area1 - area2) < EPS)
                    return 0;
                else
                    return area1 > area2 ? -1 : 1;
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
                if (isContourConvex(contour) &&
                        (bestContour == null || contourArea(bestContour) < contourArea(contour))) {
                    bestContour = new MatOfPoint(contour.clone());
                }
            }
        }

        return bestContour;
    }

    private Mat transformPerspective(Mat image, MatOfPoint contour) {
        Mat transformedImage = new Mat();
        List<Point> corners = sortRectCorners(contour.toList());
        for (Point p : corners) {
            p.x *= mRatio;
            p.y *= mRatio;
        }
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

    private void initTesseract() {
        mTessDataPath = getFilesDir() + "/tesseract";
        checkTessDataFile(new File(mTessDataPath + "/tessdata/"));
        mTessBaseAPI = new TessBaseAPI();
        mTessBaseAPI.init(mTessDataPath, LANGUAGE);
    }

    public void detectText(final Mat image) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(image, bmp);
                mTessBaseAPI.setImage(bmp);
                final String OCRresult = mTessBaseAPI.getUTF8Text();

                runOnUiThread(new Runnable() {
                    public void run() {
                        TextView textView = (TextView) findViewById(R.id.OCRTextView);
                        if (textView != null)
                            textView.setText(OCRresult);
                    }
                });
            }
        });
        thread.start();
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
                double sum1 = A.x + A.y;
                double sum2 = B.x + B.y;
                if (Math.abs(sum1 - sum2) < EPS)
                    return 0;
                else
                    return sum1 < sum2 ? -1 : 1;
            }
        });

        Collections.sort(idxDiff, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                Point A, B;
                A = corners.get(o1);
                B = corners.get(o2);
                double diff1 = A.x - A.y;
                double diff2 = B.x - B.y;
                if (Math.abs(diff1 - diff2) < EPS)
                    return 0;
                else
                    return diff1 > diff2 ? -1 : 1;
            }
        });

        res.add(corners.get(idxSum.get(0)));
        res.add(corners.get(idxDiff.get(0)));
        res.add(corners.get(idxSum.get(3)));
        res.add(corners.get(idxDiff.get(3)));

        return res;
    }

    private void displayImage(final Mat image) {
        Mat resizedImage = new Mat();
        Size size = new Size();
        if (image.width() > image.height()) {
            size.width = mOriginalSize.width;
            size.height = size.width * image.height() / image.width();
        } else {
            size.height = mOriginalSize.height;
            size.width = size.height * image.width() / image.height();
        }
        resize(image, resizedImage, size);
        Bitmap bmp = Bitmap.createBitmap(resizedImage.width(), resizedImage.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resizedImage, bmp);
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setImageBitmap(bmp);
    }

    private Mat addContourToImage(Mat image, MatOfPoint contour) {
        Mat imageWithContour = image.clone();
        Scalar color = new Scalar(0, 255, 0, 255);
        List<MatOfPoint> contours = new ArrayList<>();
        contours.add(contour);
        drawContours(imageWithContour, contours, 0, color, 3);
        return imageWithContour;
    }

    private void checkTessDataFile(File dir) {
        if (!dir.exists() && dir.mkdirs()) {
            copyTessDataFile();
        }
        if (dir.exists()) {
            String dataFilePath = mTessDataPath + "/tessdata/eng.traineddata";
            File dataFile = new File(dataFilePath);

            if (!dataFile.exists()) {
                copyTessDataFile();
            }
        }
    }

    private void copyTessDataFile() {
        try {
            String filePath = mTessDataPath + "/tessdata/eng.traineddata";
            AssetManager assetManager = getAssets();

            InputStream inputStream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outputStream = new FileOutputStream(filePath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            File file = new File(filePath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
