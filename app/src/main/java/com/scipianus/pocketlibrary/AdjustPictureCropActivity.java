package com.scipianus.pocketlibrary;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.View;

import com.scipianus.pocketlibrary.utils.Contour;
import com.scipianus.pocketlibrary.views.CroppableImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

/**
 * Created by scipianus on 19-Mar-17.
 */

public class AdjustPictureCropActivity extends AppCompatActivity {
    private static final String IMAGE_EXTRA = "picture";
    private static final String IMAGE_WIDTH = "width";
    private static final String IMAGE_HEIGHT = "height";
    private static final String POINTS_EXTRA = "corners";
    private String mCurrentPhotoPath;
    private Bitmap mImageBitmap;
    private Mat mImage;
    private Contour mContour;
    private Size mSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adjust_picture_crop);

        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        mCurrentPhotoPath = extras.getString(IMAGE_EXTRA);
        mContour = (Contour) extras.get(POINTS_EXTRA);
        mSize = new Size(extras.getInt(IMAGE_WIDTH), extras.getInt(IMAGE_HEIGHT));
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mCurrentPhotoPath == null)
            return;

        readImage(mCurrentPhotoPath);
        displayImage(mImage);
    }

    public void confirmCrop(View v) {
        CroppableImageView imageView = (CroppableImageView) findViewById(R.id.croppableImageView);
        android.graphics.Point[] points = imageView.getPoints();
        for (int i = 0; i < 4; ++i)
            mContour.getPoints().set(i, points[i]);

        Intent returnIntent = new Intent();
        returnIntent.putExtra(POINTS_EXTRA, mContour);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    private void readImage(String path) {
        mImageBitmap = BitmapFactory.decodeFile(path);
        Bitmap bmp32 = mImageBitmap.copy(Bitmap.Config.ARGB_8888, true);

        mImage = new Mat();
        Utils.bitmapToMat(bmp32, mImage);
    }

    private void displayImage(final Mat image) {
        Bitmap bmp = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bmp);
        CroppableImageView imageView = (CroppableImageView) findViewById(R.id.croppableImageView);
        imageView.setImageBitmap(bmp, mSize);
        imageView.setCorners(mContour.getPoints());
    }
}
