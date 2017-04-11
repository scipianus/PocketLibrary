package com.scipianus.pocketlibrary;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.scipianus.pocketlibrary.utils.Contour;
import com.scipianus.pocketlibrary.utils.FileUtils;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.COLOR_RGBA2RGB;
import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.GC_INIT_WITH_RECT;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.RETR_TREE;
import static org.opencv.imgproc.Imgproc.approxPolyDP;
import static org.opencv.imgproc.Imgproc.arcLength;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.convexHull;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.getPerspectiveTransform;
import static org.opencv.imgproc.Imgproc.grabCut;
import static org.opencv.imgproc.Imgproc.isContourConvex;
import static org.opencv.imgproc.Imgproc.resize;
import static org.opencv.imgproc.Imgproc.warpPerspective;

public class ViewPictureActivity extends AppCompatActivity {
    private static final String IMAGE_EXTRA = "picture";
    private static final String IMAGE_WIDTH = "width";
    private static final String IMAGE_HEIGHT = "height";
    private static final String POINTS_EXTRA = "corners";
    private static final int ADJUST_CROP = 1;
    private static final double GRABCUT_EDGE = 0.05;
    private final String LANGUAGE = "eng";
    private final double EPS = 0.0000000001;
    private final double HEIGHT = 750;
    private double mRatio, mAdjustRatio;
    private TessBaseAPI mTessBaseAPI;
    private String mTessDataPath;
    private String mCurrentPhotoPath;
    private String mCroppedPhotoPath;
    private Bitmap mImageBitmap;
    private Mat mInitialImage;
    private Mat mImage;
    private Mat mForeground;
    private Mat mEdges;
    private Mat mCroppedImage;
    private MatOfPoint mContour;
    private Size mOriginalSize;
    private int mCurrentStep;
    private Button mNextStepButton;
    private Button mAdjustCropButton;
    private Button mConfirmCropButton;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_picture);

        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
            mCurrentPhotoPath = extras.getString(IMAGE_EXTRA);

        mCurrentStep = 0;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mCurrentPhotoPath == null || mCurrentStep != 0)
            return;

        readImage(mCurrentPhotoPath);
        displayImage(mImage);

        mNextStepButton = (Button) findViewById(R.id.nextStepButton);
        mNextStepButton.setText(R.string.remove_background);

        mAdjustCropButton = (Button) findViewById(R.id.adjustCropButton);
        mConfirmCropButton = (Button) findViewById(R.id.confirmCropButton);
        mProgressBar = (ProgressBar) findViewById(R.id.extractProgressBar);
    }

    public void performNextStep(View view) {
        switch (mCurrentStep) {
            case 0:
                applyGrabCut(mImage);
                mCurrentStep++;
                mNextStepButton.setText(R.string.find_contour);
                break;
            case 1:
                mEdges = edgeDetection(mForeground);
                displayImage(mEdges);
                mCurrentStep++;
                mNextStepButton.setText(R.string.detect_rectangle);
                break;
            case 2:
                mContour = contoursFinding(mEdges);
                if (mContour == null) {
                    Toast.makeText(this, "Could not find a rectangle", Toast.LENGTH_LONG).show();
                    List<Point> list = new ArrayList<>();
                    list.add(new Point(0, 0));
                    list.add(new Point(0, mImage.height()));
                    list.add(new Point(mImage.width(), mImage.height()));
                    list.add(new Point(mImage.width(), 0));
                    mContour = new MatOfPoint();
                    mContour.fromList(list);
                    mContour.fromList(new ArrayList<Point>());
                }
                displayImage(addContourToImage(mImage, mContour));
                mCurrentStep++;
                mNextStepButton.setVisibility(View.INVISIBLE);
                mAdjustCropButton.setVisibility(View.VISIBLE);
                mConfirmCropButton.setVisibility(View.VISIBLE);
                break;
            case 3:
                mCroppedImage = transformPerspective(mInitialImage, mContour);
                displayImage(mCroppedImage);
                mCurrentStep++;
                mNextStepButton.setVisibility(View.VISIBLE);
                mAdjustCropButton.setVisibility(View.INVISIBLE);
                mConfirmCropButton.setVisibility(View.INVISIBLE);
                mNextStepButton.setText(R.string.detect_book);
                break;
            case 4:
                saveCroppedImage(mCroppedImage);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADJUST_CROP) {
            if (resultCode == RESULT_OK) {
                Contour contour = data.getExtras().getParcelable(POINTS_EXTRA);
                if (contour != null) {
                    List<Point> points = toListOfOpenCVPoints(contour.getPoints());
                    mContour.fromList(points);
                    displayImage(addContourToImage(mImage, mContour));
                }
            }
        }
    }

    public void openDetectBookActivity() {
        Intent intent = new Intent(this, DetectBookActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(IMAGE_EXTRA, mCroppedPhotoPath);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void openAdjustCropActivity(View v) {
        Intent intent = new Intent(this, AdjustPictureCropActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(IMAGE_EXTRA, mCurrentPhotoPath);
        bundle.putParcelable(POINTS_EXTRA, new Contour(toListOfGraphicPoints(sortRectCorners(mContour.toList()))));
        bundle.putInt(IMAGE_WIDTH, getImageSize().getWidth());
        bundle.putInt(IMAGE_HEIGHT, getImageSize().getHeight());
        intent.putExtras(bundle);
        startActivityForResult(intent, ADJUST_CROP);
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
        cvtColor(mImage, mImage, COLOR_RGBA2RGB);
    }

    private void applyGrabCut(final Mat image) {
        mNextStepButton.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        Thread thread = new Thread(new Runnable() {
            private Mat result;

            @Override
            public void run() {
                Mat mask = new Mat(image.size(), CvType.CV_8UC1);
                Rect rect = new Rect(
                        new Point(GRABCUT_EDGE * image.width(), GRABCUT_EDGE * image.height()),
                        new Point((1 - GRABCUT_EDGE) * image.width(), (1 - GRABCUT_EDGE) * image.height()));
                Mat backgroundModel = new Mat();
                Mat foregroundModel = new Mat();
                grabCut(image, mask, rect, backgroundModel, foregroundModel, 5, GC_INIT_WITH_RECT);
                for (int i = 0; i < mask.height(); ++i) {
                    for (int j = 0; j < mask.width(); ++j) {
                        if (mask.get(i, j)[0] == 0 || mask.get(i, j)[0] == 2) {
                            mask.put(i, j, 0);
                        } else {
                            mask.put(i, j, 1);
                        }
                    }
                }
                result = new Mat(image.size(), image.type());
                image.copyTo(result, mask);


                runOnUiThread(new Runnable() {
                    public void run() {
                        displayImage(result);
                        mForeground = result.clone();
                        mProgressBar.setVisibility(View.INVISIBLE);
                        mNextStepButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
        thread.start();

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

        findContours(edges, contours, new Mat(), RETR_TREE, CHAIN_APPROX_SIMPLE);
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
            MatOfInt convexHullIndices = new MatOfInt();
            convexHull(c, convexHullIndices);
            List<Point> convexHullPoints = new ArrayList<>();
            for (int idx : convexHullIndices.toList()) {
                convexHullPoints.add(c.toList().get(idx));
            }

            MatOfPoint2f c2f = new MatOfPoint2f();
            MatOfPoint2f approx = new MatOfPoint2f();
            c2f.fromList(convexHullPoints);

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

        MatOfPoint boundingBoxContour = getBoundingBoxContour(edges);
        if (contourArea(bestContour) / contourArea(boundingBoxContour) < 0.5) {
            return boundingBoxContour;
        }
        return bestContour;
    }

    private MatOfPoint getBoundingBoxContour(Mat image) {
        int minX, maxX, minY, maxY;
        minX = image.width();
        minY = image.height();
        maxX = 0;
        maxY = 0;
        for (int i = 0; i < image.height(); ++i) {
            for (int j = 0; j < image.width(); ++j) {
                if (image.get(i, j)[0] >= 127) {
                    minX = Math.min(minX, j);
                    minY = Math.min(minY, i);
                    maxX = Math.max(maxX, j);
                    maxY = Math.max(maxY, i);
                }
            }
        }

        List<Point> boundingRectPoints = new ArrayList<Point>();
        boundingRectPoints.add(new Point(minX, minY));
        boundingRectPoints.add(new Point(minX, maxY));
        boundingRectPoints.add(new Point(maxX, maxY));
        boundingRectPoints.add(new Point(maxX, minY));

        MatOfPoint boundingRectContour = new MatOfPoint();
        boundingRectContour.fromList(boundingRectPoints);
        return boundingRectContour;
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
        File dir = new File(mTessDataPath + "/tessdata/");
        FileUtils.transferDataFile(getAssets(), dir, mTessDataPath + "/tessdata/eng.traineddata", "tessdata/eng.traineddata");
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
                        //TextView textView = (TextView) findViewById(R.id.OCRTextView);
                        //if (textView != null)
                        //    textView.setText(OCRresult);
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
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setImageBitmap(toBitmap(image));
    }

    private Bitmap toBitmap(final Mat image) {
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
        return bmp;
    }

    private Mat addContourToImage(Mat image, MatOfPoint contour) {
        Mat imageWithContour = image.clone();
        Scalar color = new Scalar(0, 255, 0, 255);
        List<MatOfPoint> contours = new ArrayList<>();
        contours.add(contour);
        drawContours(imageWithContour, contours, 0, color, 3);
        return imageWithContour;
    }

    private List<android.graphics.Point> toListOfGraphicPoints(List<Point> points) {
        mAdjustRatio = getImageSize().getHeight() / HEIGHT;
        List<android.graphics.Point> list = new ArrayList<>();
        for (Point point : points) {
            list.add(new android.graphics.Point(
                    (int) (mAdjustRatio * point.x),
                    (int) (mAdjustRatio * point.y)));
        }
        return list;
    }


    private List<Point> toListOfOpenCVPoints(List<android.graphics.Point> points) {
        List<Point> list = new ArrayList<>();
        for (android.graphics.Point point : points) {
            list.add(new Point(point.x / mAdjustRatio, point.y / mAdjustRatio));
        }
        return list;
    }

    private android.util.Size getImageSize() {
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        final int actualHeight, actualWidth;
        final int imageViewHeight = imageView.getHeight(), imageViewWidth = imageView.getWidth();
        final int bitmapHeight = mImageBitmap.getHeight(), bitmapWidth = mImageBitmap.getWidth();
        if (imageViewHeight * bitmapWidth <= imageViewWidth * bitmapHeight) {
            actualWidth = bitmapWidth * imageViewHeight / bitmapHeight;
            actualHeight = imageViewHeight;
        } else {
            actualHeight = bitmapHeight * imageViewWidth / bitmapWidth;
            actualWidth = imageViewWidth;
        }

        return new android.util.Size(actualWidth, actualHeight);
    }

    private void saveCroppedImage(final Mat image) {
        mNextStepButton.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "_crop";
                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                try {
                    File imageFile = File.createTempFile(
                            imageFileName,
                            ".jpg",
                            storageDir
                    );
                    mCroppedPhotoPath = imageFile.getAbsolutePath();
                    FileOutputStream out = null;
                    Bitmap bitmap = toBitmap(image);
                    try {
                        out = new FileOutputStream(mCroppedPhotoPath);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        openDetectBookActivity();
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
        thread.start();

    }
}
