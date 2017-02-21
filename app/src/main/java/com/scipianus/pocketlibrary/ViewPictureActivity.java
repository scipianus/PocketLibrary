package com.scipianus.pocketlibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

public class ViewPictureActivity extends AppCompatActivity {
    private static String IMAGE_EXTRA = "picture";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_picture);

        Bundle extras = getIntent().getExtras();
        byte[] byteArray = extras.getByteArray(IMAGE_EXTRA);

        if (byteArray != null) {
            Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            ImageView image = (ImageView) findViewById(R.id.imageView);
            image.setImageBitmap(bmp);
        }
    }
}
