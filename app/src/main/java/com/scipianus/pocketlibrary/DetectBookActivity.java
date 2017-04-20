package com.scipianus.pocketlibrary;

import android.app.ActionBar;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.scipianus.pocketlibrary.utils.DatabaseEntry;
import com.scipianus.pocketlibrary.utils.FileUtils;
import com.scipianus.pocketlibrary.views.DatabaseEntryAdapter;
import com.sh1r0.caffe_android_lib.CaffeMobile;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by scipianus on 01-Apr-17.
 */

public class DetectBookActivity extends AppCompatActivity {
    private static final String IMAGE_EXTRA = "picture";
    private static final String MODEL_PATH = "caffe/bvlc_alexnet/deploy.prototxt";
    private static final String WEIGHTS_PATH = "caffe/bvlc_alexnet/bvlc_alexnet.caffemodel";
    private static final String DATABASE_PATH = "database/scores_m.txt";
    private static final String IDS_PATH = "database/ids.txt";
    private static final Integer FEATURES = 4096;
    private static final Integer DATABASE_ENTRIES = 10000;
    private static final Integer TOP_COUNT = 25;
    private String mCaffeDataPath;
    private String mDatabasePath;
    private String mCurrentPhotoPath;
    private Bitmap mImageBitmap;
    private Mat mImage;
    private CaffeMobile mCaffeMobile;
    private TextView mDetectionStatus;
    private ProgressBar mExtractProgressBar;
    private ProgressBar mSearchProgressBar;
    private RecyclerView mRecyclerView;
    private DatabaseEntryAdapter mDatabaseEntryAdapter;
    private float[] mFeatures;
    private SortedSet<DatabaseEntry> mDatabaseEntries;
    private List<DatabaseEntry> mDatabaseEntriesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_book);

        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        mCurrentPhotoPath = extras.getString(IMAGE_EXTRA);

        if (mCurrentPhotoPath == null)
            return;

        mDatabaseEntriesList = new ArrayList<>();
        mDatabaseEntryAdapter = new DatabaseEntryAdapter(mDatabaseEntriesList);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mDatabaseEntryAdapter);

        mDetectionStatus = (TextView) findViewById(R.id.detectionStatus);
        mDetectionStatus.setText(R.string.extracting_features);

        mExtractProgressBar = (ProgressBar) findViewById(R.id.extractProgressBar);
        mSearchProgressBar = (ProgressBar) findViewById(R.id.searchProgressBar);

        readImage(mCurrentPhotoPath);
        initializeCaffe();
        initializeDatabase();
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
        File dir = new File(mCaffeDataPath + "caffe/bvlc_alexnet/");
        FileUtils.transferDataFile(getAssets(), dir, MODEL_PATH, mCaffeDataPath + MODEL_PATH);
        FileUtils.transferDataFile(getAssets(), dir, WEIGHTS_PATH, mCaffeDataPath + WEIGHTS_PATH);
        mCaffeMobile = new CaffeMobile();
        mCaffeMobile.setNumThreads(4);
        mCaffeMobile.loadModel(mCaffeDataPath + MODEL_PATH, mCaffeDataPath + WEIGHTS_PATH);
    }

    private void initializeDatabase() {
        mDatabasePath = getFilesDir().toString() + "/";
        File dir = new File(mDatabasePath + "database/");
        FileUtils.transferDataFile(getAssets(), dir, DATABASE_PATH, mDatabasePath + DATABASE_PATH);
        FileUtils.transferDataFile(getAssets(), dir, IDS_PATH, mDatabasePath + IDS_PATH);
    }

    private void extractFeatures() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                mFeatures = mCaffeMobile.extractFeatures(mCurrentPhotoPath, "fc7")[0];

                runOnUiThread(new Runnable() {
                    public void run() {
                        mExtractProgressBar.setVisibility(View.INVISIBLE);
                        searchBook();
                    }
                });
            }
        });
        thread.start();
    }

    private void searchBook() {
        mSearchProgressBar.setVisibility(View.VISIBLE);
        mSearchProgressBar.setMax(DATABASE_ENTRIES);
        mSearchProgressBar.setProgress(0);
        mDetectionStatus.setText(R.string.database_searching);

        Thread thread = new Thread(new Runnable() {
            FileReader fileReader = null;
            FileReader idFileReader = null;
            BufferedReader bufferedReader = null;
            BufferedReader idBufferedReader = null;
            int lineCounter = 0;

            @Override
            public void run() {
                try {
                    fileReader = new FileReader(mDatabasePath + DATABASE_PATH);
                    bufferedReader = new BufferedReader(fileReader);
                    idFileReader = new FileReader(mDatabasePath + IDS_PATH);
                    idBufferedReader = new BufferedReader(idFileReader);

                    float[] features = new float[FEATURES];
                    if (mDatabaseEntries != null) {
                        mDatabaseEntries.clear();
                    } else {
                        mDatabaseEntries = new TreeSet<>();
                    }

                    for (lineCounter = 1; lineCounter <= DATABASE_ENTRIES; ++lineCounter) {
                        String[] tokens = bufferedReader.readLine().split(" ");
                        String[] idTokens = idBufferedReader.readLine().split(" ");
                        for (int i = 0; i < FEATURES; ++i) {
                            features[i] = Float.parseFloat(tokens[i]);
                        }
                        mDatabaseEntries.add(new DatabaseEntry(idTokens[1], getDistance(features, mFeatures)));
                        if (mDatabaseEntries.size() > TOP_COUNT) {
                            mDatabaseEntries.remove(mDatabaseEntries.last());
                        }
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mSearchProgressBar.setProgress(lineCounter);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (bufferedReader != null) {
                            bufferedReader.close();
                        }
                        if (fileReader != null) {
                            fileReader.close();
                        }
                        if (idBufferedReader != null) {
                            idBufferedReader.close();
                        }
                        if (idFileReader != null) {
                            idFileReader.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mSearchProgressBar.setVisibility(View.INVISIBLE);
                                mDetectionStatus.setVisibility(View.INVISIBLE);
                                mRecyclerView.setVisibility(View.VISIBLE);
                                mDatabaseEntriesList.clear();
                                mDatabaseEntriesList.addAll(mDatabaseEntries);
                                mDatabaseEntryAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            }
        });
        thread.start();
    }

    private double getDistance(float[] A, float[] B) {
        if (A.length != B.length)
            return 0.0;
        double distance = 0.0;
        for (int i = 0; i < A.length; ++i) {
            distance += (A[i] - B[i]) * (A[i] - B[i]);
        }
        return distance;
    }
}
