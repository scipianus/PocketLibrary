package com.scipianus.pocketlibrary;

import android.app.ActionBar;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.scipianus.pocketlibrary.models.BookEntry;
import com.scipianus.pocketlibrary.models.GoogleBookEntry;
import com.scipianus.pocketlibrary.utils.FileUtils;
import com.scipianus.pocketlibrary.utils.HTTPUtils;
import com.scipianus.pocketlibrary.views.BookEntryAdapter;

import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by scipianus on 30-Apr-17.
 */

public class DetectBookByTextActivity extends AppCompatActivity {
    private static final String IMAGE_EXTRA = "picture";
    private static final String LANGUAGE = "eng";
    private static final Integer TOP_COUNT = 25;
    private TessBaseAPI mTessBaseAPI;
    private String mTessDataPath;
    private String mCurrentPhotoPath;
    private Bitmap mImageBitmap;
    private Mat mImage;
    private TextView mDetectionStatus;
    private ProgressBar mSearchProgressBar;
    private RecyclerView mRecyclerView;
    private BookEntryAdapter mBookEntryAdapter;
    private List<BookEntry> mGoogleBookEntriesList;
    private JSONObject mJSONResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_book_by_text);

        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        mCurrentPhotoPath = extras.getString(IMAGE_EXTRA);

        if (mCurrentPhotoPath == null)
            return;

        mGoogleBookEntriesList = new ArrayList<>();
        mBookEntryAdapter = new BookEntryAdapter(mGoogleBookEntriesList);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mBookEntryAdapter);

        mDetectionStatus = (TextView) findViewById(R.id.detectionStatus);
        mSearchProgressBar = (ProgressBar) findViewById(R.id.searchProgressBar);

        readImage(mCurrentPhotoPath);
        initTesseract();
        detectText(mImage);
    }

    private void readImage(String path) {
        mImageBitmap = BitmapFactory.decodeFile(path);
        Bitmap bmp32 = mImageBitmap.copy(Bitmap.Config.ARGB_8888, true);

        mImage = new Mat();
        Utils.bitmapToMat(bmp32, mImage);
    }

    private void initTesseract() {
        mTessDataPath = getFilesDir() + "/tesseract";
        File dir = new File(mTessDataPath + "/tessdata/");
        FileUtils.transferDataFile(getAssets(), dir, "tessdata/eng.traineddata", mTessDataPath + "/tessdata/eng.traineddata");
        mTessBaseAPI = new TessBaseAPI();
        mTessBaseAPI.init(mTessDataPath, LANGUAGE);
    }

    private void detectText(final Mat image) {
        mDetectionStatus.setText(R.string.running_ocr);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(image, bmp);
                mTessBaseAPI.setImage(bmp);
                mTessBaseAPI.getUTF8Text();
                final StringBuilder OCRresult = new StringBuilder();
                ResultIterator iterator = mTessBaseAPI.getResultIterator();
                do {
                    if (iterator.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD) >= 30) {
                        OCRresult.append(iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD).replaceAll("[^A-Za-z]", ""));
                        OCRresult.append(" ");
                    }
                } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD));

                runOnUiThread(new Runnable() {
                    public void run() {
                        queryGoogleBooksAPI(OCRresult.toString());
                    }
                });
            }
        });
        thread.start();
    }

    private void queryGoogleBooksAPI(final String text) {
        mDetectionStatus.setText(R.string.google_searching);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String url = new Uri.Builder()
                        .scheme("https")
                        .authority("www.googleapis.com")
                        .appendPath("books")
                        .appendPath("v1")
                        .appendPath("volumes")
                        .appendQueryParameter("q", text.replaceAll("(\\s|\\n|\\t)", " ").replaceAll("[ ]{1,}", "+"))
                        .appendQueryParameter("maxResults", TOP_COUNT.toString())
                        .build()
                        .toString();
                mJSONResult = HTTPUtils.getJSONObject(url);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        parseJSONIntoEntries();
                    }
                });
            }
        });
        thread.start();
    }

    private void parseJSONIntoEntries() {
        if (mJSONResult == null || !mJSONResult.has("totalItems")) {
            return;
        }
        try {
            int nrItems = Math.min(TOP_COUNT, mJSONResult.getInt("totalItems"));
            for (int i = 0; i < nrItems; ++i) {
                mGoogleBookEntriesList.add(new GoogleBookEntry(mJSONResult
                        .getJSONArray("items")
                        .getJSONObject(i)
                        .getJSONObject("volumeInfo")));
            }
            mDetectionStatus.setVisibility(View.INVISIBLE);
            mSearchProgressBar.setVisibility(View.INVISIBLE);
            mRecyclerView.setVisibility(View.VISIBLE);
            mBookEntryAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
