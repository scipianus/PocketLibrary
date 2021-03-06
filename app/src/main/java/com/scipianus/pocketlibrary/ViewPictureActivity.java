package com.scipianus.pocketlibrary;

import android.Manifest;
import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.BootstrapText;
import com.scipianus.pocketlibrary.utils.Contour;

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
    private static final int INTERNET_PERMISSION_REQUEST = 1;
    private static final int ADJUST_CROP = 1;
    private static final double GRABCUT_EDGE = 0.05;
    private static final double RECTANGLE_AREA_RATIO = 0.8;
    private final double EPS = 0.0000000001;
    private final double HEIGHT = 750;
    private double mRatio, mAdjustRatio;
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
    private BootstrapButton mNextStepButton;
    private BootstrapButton mAdjustCropButton;
    private BootstrapButton mConfirmCropButton;
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

        mNextStepButton = (BootstrapButton) findViewById(R.id.nextStepButton);
        mNextStepButton.setText(R.string.remove_background);

        mAdjustCropButton = (BootstrapButton) findViewById(R.id.adjustCropButton);
        mConfirmCropButton = (BootstrapButton) findViewById(R.id.confirmCropButton);
        mProgressBar = (ProgressBar) findViewById(R.id.extractProgressBar);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case INTERNET_PERMISSION_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openDetectBookActivity();
                } else {
                    Toast.makeText(this, "Required permission not provided", Toast.LENGTH_LONG).show();
                }
                break;
            }
            default:
                break;
        }
    }

    public void performNextStep(View view) {
        switch (mCurrentStep) {
            case 0:
                applyGrabCut(mImage);
                mCurrentStep++;
                mNextStepButton.setText(R.string.find_contour);
                break;
            case 1:
                edgeDetection(mForeground);
                mCurrentStep++;
                mNextStepButton.setText(R.string.detect_rectangle);
                break;
            case 2:
                contoursFinding(mEdges);
                mCurrentStep++;
                break;
            case 3:
                transformPerspective(mInitialImage, mContour);
                mCurrentStep++;
                mNextStepButton.setBootstrapText(new BootstrapText
                        .Builder(this)
                        .addFontAwesomeIcon("fa_search")
                        .addText(" " + getResources().getString(R.string.detect_book))
                        .build());
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

    public void tryOpenDetectBookActivity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET},
                    INTERNET_PERMISSION_REQUEST);
        } else {
            openDetectBookActivity();
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

    private void edgeDetection(final Mat image) {
        mNextStepButton.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Mat grayImage = new Mat();
                mEdges = new Mat();

                cvtColor(image, grayImage, COLOR_BGR2GRAY);
                GaussianBlur(grayImage, grayImage, new Size(5, 5), 0);
                Canny(grayImage, mEdges, 75, 200);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayImage(mEdges);
                        mProgressBar.setVisibility(View.INVISIBLE);
                        mNextStepButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
        thread.start();
    }

    private void contoursFinding(final Mat edges) {
        mNextStepButton.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
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
                if (bestContour == null || contourArea(bestContour) / contourArea(boundingBoxContour) < RECTANGLE_AREA_RATIO) {
                    mContour = boundingBoxContour;
                } else {
                    mContour = bestContour;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayImage(addContourToImage(mImage, mContour));
                        mProgressBar.setVisibility(View.INVISIBLE);
                        mAdjustCropButton.setVisibility(View.VISIBLE);
                        mConfirmCropButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
        thread.start();
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

        List<Point> boundingRectPoints = new ArrayList<>();
        boundingRectPoints.add(new Point(minX, minY));
        boundingRectPoints.add(new Point(minX, maxY));
        boundingRectPoints.add(new Point(maxX, maxY));
        boundingRectPoints.add(new Point(maxX, minY));

        MatOfPoint boundingRectContour = new MatOfPoint();
        boundingRectContour.fromList(boundingRectPoints);
        return boundingRectContour;
    }

    private void transformPerspective(final Mat image, final MatOfPoint contour) {
        mAdjustCropButton.setVisibility(View.INVISIBLE);
        mConfirmCropButton.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                mCroppedImage = new Mat();
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

                warpPerspective(image, mCroppedImage, m, new Size(maxWidth, maxHeight));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayImage(mCroppedImage);
                        mProgressBar.setVisibility(View.INVISIBLE);
                        mNextStepButton.setVisibility(View.VISIBLE);
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
                        tryOpenDetectBookActivity();
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
        thread.start();
    }
}
