package com.scipianus.pocketlibrary;

import android.app.ActionBar;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.sh1r0.caffe_android_lib.CaffeMobile;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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
        checkCaffeDataFiles(new File(mCaffeDataPath + "/caffe/bvlc_alexnet/"));
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

    private void checkCaffeDataFiles(File dir) {
        if (!dir.exists() && dir.mkdirs()) {
            copyCaffeDataFiles(MODEL_PATH, mCaffeDataPath + MODEL_PATH);
            copyCaffeDataFiles(WEIGHTS_PATH, mCaffeDataPath + WEIGHTS_PATH);
        }
        if (dir.exists()) {
            File modelFile = new File(mCaffeDataPath + MODEL_PATH);
            File weightsFile = new File(mCaffeDataPath + WEIGHTS_PATH);

            if (!modelFile.exists()) {
                copyCaffeDataFiles(MODEL_PATH, mCaffeDataPath + MODEL_PATH);
            }

            if (!weightsFile.exists()) {
                copyCaffeDataFiles(WEIGHTS_PATH, mCaffeDataPath + WEIGHTS_PATH);
            }
        }
    }

    private void copyCaffeDataFiles(String inputPath, String outputPath) {
        try {
            AssetManager assetManager = getAssets();

            InputStream inputStream = assetManager.open(inputPath);
            OutputStream outputStream = new FileOutputStream(outputPath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            File file = new File(outputPath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
