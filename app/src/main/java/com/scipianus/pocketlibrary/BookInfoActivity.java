package com.scipianus.pocketlibrary;

import android.app.ActionBar;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.scipianus.pocketlibrary.utils.HTTPUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by scipianus on 01-Apr-17.
 */

public class BookInfoActivity extends AppCompatActivity {
    private static final String ID_EXTRA = "id";
    private static final String COVER_API_PATH = "http://covers.openlibrary.org/b/olid/";
    private static final String BOOK_API_PATH_PREFIX = "https://openlibrary.org/api/books?bibkeys=OLID:";
    private static final String BOOK_API_PATH_SUFFIX = "&jscmd=data&format=json";
    private String mBookId;
    private JSONObject mBookInfo;
    private Bitmap mBookCover;
    private int[] views = new int[]{
            R.id.bookCoverImageView,
            R.id.bookTitleTextView,
            R.id.bookAuthorsTextView,
            R.id.bookISBNTextView,
            R.id.bookPublisherTextView,
            R.id.bookDateTextView,
            R.id.bookPagesTextView,
            R.id.bookURLTextView
    };

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
                String url = BOOK_API_PATH_PREFIX + mBookId + BOOK_API_PATH_SUFFIX;
                final JSONObject jsonObject = HTTPUtils.getJSONObject(url);

                String imageUrl = String.format("%s%s-L.jpg", COVER_API_PATH, mBookId);
                final Bitmap bookCover = HTTPUtils.getImage(imageUrl);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ProgressBar progressBar = (ProgressBar) findViewById(R.id.fetchingProgressBar);
                        progressBar.setVisibility(View.INVISIBLE);
                        try {
                            if (jsonObject.has("OLID:" + mBookId)) {
                                mBookInfo = jsonObject.getJSONObject("OLID:" + mBookId);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        mBookCover = bookCover;
                        displayInfo();
                        for (int resID : views) {
                            findViewById(resID).setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        });
        thread.start();
    }

    private void displayInfo() {
        if (mBookInfo == null) {
            TextView textView = (TextView) findViewById(R.id.bookTitleTextView);
            textView.setText("API Error\nSorry for the inconvenience");
        } else {
            displayCover();
            displayTitle();
            displayAuthors();
            displayISBN();
            displayPublisher();
            displayDate();
            displayPages();
            displayURL();
        }
    }

    private void displayCover() {
        if (mBookCover != null) {
            ImageView imageView = (ImageView) findViewById(R.id.bookCoverImageView);
            imageView.setImageBitmap(mBookCover);
        }
    }

    private void displayTitle() {
        try {
            if (mBookInfo.has("title")) {
                TextView textView = (TextView) findViewById(R.id.bookTitleTextView);
                textView.setText(mBookInfo.getString("title"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void displayAuthors() {
        try {
            if (mBookInfo.has("authors")) {
                TextView textView = (TextView) findViewById(R.id.bookAuthorsTextView);
                StringBuilder authors = new StringBuilder();
                JSONArray authorsArray = mBookInfo.getJSONArray("authors");
                for (int i = 0; i < authorsArray.length(); ++i) {
                    try {
                        if (authorsArray.getJSONObject(i).has("name")) {
                            authors.append(authorsArray.getJSONObject(i).getString("name"));
                            if (i + 1 < authorsArray.length()) {
                                authors.append(" ");
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                textView.setText(authors.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void displayISBN() {
        try {
            if (mBookInfo.has("identifiers")) {
                TextView textView = (TextView) findViewById(R.id.bookISBNTextView);
                JSONObject identifiers = mBookInfo.getJSONObject("identifiers");
                if (identifiers.has("isbn_13")) {
                    textView.setText("ISBN: " + identifiers.getJSONArray("isbn_13").getString(0));
                } else if (identifiers.has("isbn_10")) {
                    textView.setText("ISBN: " + identifiers.getJSONArray("isbn_10").getString(0));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void displayPublisher() {
        try {
            if (mBookInfo.has("publishers")) {
                TextView textView = (TextView) findViewById(R.id.bookPublisherTextView);
                textView.setText("Publisher: " + mBookInfo.getJSONArray("publishers").getJSONObject(0).getString("name"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void displayDate() {
        try {
            if (mBookInfo.has("publish_date")) {
                TextView textView = (TextView) findViewById(R.id.bookDateTextView);
                textView.setText("Publish date: " + mBookInfo.getString("publish_date"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void displayPages() {
        try {
            if (mBookInfo.has("number_of_pages")) {
                TextView textView = (TextView) findViewById(R.id.bookPagesTextView);
                textView.setText("Pages: " + mBookInfo.getString("number_of_pages"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void displayURL() {
        try {
            if (mBookInfo.has("url")) {
                TextView textView = (TextView) findViewById(R.id.bookURLTextView);
                textView.setText(mBookInfo.getString("url"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
