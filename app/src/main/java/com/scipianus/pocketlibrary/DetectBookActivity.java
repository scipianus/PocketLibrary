package com.scipianus.pocketlibrary;

import android.app.ActionBar;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.scipianus.pocketlibrary.utils.FileUtils;
import com.sh1r0.caffe_android_lib.CaffeMobile;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;

/**
 * Created by scipianus on 01-Apr-17.
 */

public class DetectBookActivity extends AppCompatActivity {
    private static final String IMAGE_EXTRA = "picture";
    private static final String MODEL_PATH = "caffe/bvlc_alexnet/deploy.prototxt";
    private static final String WEIGHTS_PATH = "caffe/bvlc_alexnet/bvlc_alexnet.caffemodel";
    private String mCaffeDataPath;
    private String mCurrentPhotoPath;
    private Bitmap mImageBitmap;
    private Mat mImage;
    private CaffeMobile mCaffeMobile;
    private TextView detectionStatus;
    private float[] mFeatures;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_book);

        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        mCurrentPhotoPath = extras.getString(IMAGE_EXTRA);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mCurrentPhotoPath == null)
            return;

        detectionStatus = (TextView) findViewById(R.id.detectionStatus);
        detectionStatus.setText(R.string.extracting_features);

        readImage(mCurrentPhotoPath);
        initializeCaffe();
        extractFeatures();
    }

    private void readImage(String path) {
        mImageBitmap = BitmapFactory.decodeFile(path);
        Bitmap bmp32 = mImageBitmap.copy(Bitmap.Config.ARGB_8888, true);

        mImage = new Mat();
        Utils.bitmapToMat(bmp32, mImage);
    }

    private void initializeCaffe() {
        mCaffeDataPath = getFilesDir() + "/caffe_mobile/";
        File dir = new File(mCaffeDataPath + "/caffe/bvlc_alexnet/");
        FileUtils.transferDataFile(getAssets(), dir, MODEL_PATH, mCaffeDataPath + MODEL_PATH);
        FileUtils.transferDataFile(getAssets(), dir, WEIGHTS_PATH, mCaffeDataPath + WEIGHTS_PATH);
        mCaffeMobile = new CaffeMobile();
        mCaffeMobile.setNumThreads(4);
        mCaffeMobile.loadModel(mCaffeDataPath + MODEL_PATH, mCaffeDataPath + WEIGHTS_PATH);
    }

    private void extractFeatures() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                mFeatures = mCaffeMobile.extractFeatures(mCurrentPhotoPath, "fc8")[0];

                runOnUiThread(new Runnable() {
                    public void run() {
                        detectionStatus.setText(R.string.database_searching);
                        searchBook();
                    }
                });
            }
        });
        thread.start();
    }

    private void searchBook() {
        // TODO: add the feature vectors database and search the current one
    }
}
