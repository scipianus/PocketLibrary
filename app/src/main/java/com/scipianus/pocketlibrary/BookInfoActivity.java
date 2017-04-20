package com.scipianus.pocketlibrary;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.scipianus.pocketlibrary.utils.HTTPUtils;

import org.json.JSONObject;

/**
 * Created by scipianus on 01-Apr-17.
 */

public class BookInfoActivity extends AppCompatActivity {
    private static final String ID_EXTRA = "id";
    private static final String COVER_API_PATH_PREFIX = "https://openlibrary.org/api/books?bibkeys=OLID:";
    private static final String COVER_API_PATH_SUFFIX = "&jscmd=data&format=json";
    private String mBookId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_info);

        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        mBookId = extras.getString(ID_EXTRA);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String url = COVER_API_PATH_PREFIX + mBookId + COVER_API_PATH_SUFFIX;
                final JSONObject jsonObject = HTTPUtils.getJSONObject(url);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), jsonObject.toString(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        thread.start();
    }
}
