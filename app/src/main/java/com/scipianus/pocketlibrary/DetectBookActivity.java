package com.scipianus.pocketlibrary;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * Created by scipianus on 30-Apr-17.
 */

public class DetectBookActivity extends AppCompatActivity {

    private Bundle bundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_book);

        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        bundle = getIntent().getExtras();
    }

    public void detectByCover(View v) {
        Intent intent = new Intent(this, DetectBookByCoverActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void detectByText(View v) {
        Intent intent = new Intent(this, DetectBookByTextActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}
