package com.scipianus.pocketlibrary;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.scipianus.pocketlibrary.models.BookEntry;

/**
 * Created by scipianus on 01-Apr-17.
 */

public class BookInfoActivity extends AppCompatActivity {
    private static final String BOOK_EXTRA = "book";
    private BookEntry mBookInfo;
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
        mBookInfo = (BookEntry) extras.getSerializable(BOOK_EXTRA);

        if (mBookInfo == null) {
            TextView textView = (TextView) findViewById(R.id.bookTitleTextView);
            textView.setVisibility(View.VISIBLE);
            textView.setText("API Error\nSorry for the inconvenience");
            return;
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                mBookInfo.deserializeJSONObject();
                mBookInfo.fetchCoverImage();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ProgressBar progressBar = (ProgressBar) findViewById(R.id.fetchingProgressBar);
                        progressBar.setVisibility(View.INVISIBLE);

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
        displayCover();
        displayTitle();
        displayAuthors();
        displayISBN();
        displayPublisher();
        displayDate();
        displayPages();
        displayURL();
    }

    private void displayCover() {
        if (mBookInfo.getCoverImage() != null) {
            ImageView imageView = (ImageView) findViewById(R.id.bookCoverImageView);
            imageView.setImageBitmap(mBookInfo.getCoverImage());
        }
    }

    private void displayTitle() {
        if (mBookInfo.getTitle().length() > 0) {
            TextView textView = (TextView) findViewById(R.id.bookTitleTextView);
            textView.setText(mBookInfo.getTitle());
        }
    }

    private void displayAuthors() {
        if (mBookInfo.getAuthors().length() > 0) {
            TextView textView = (TextView) findViewById(R.id.bookAuthorsTextView);
            textView.setText(mBookInfo.getAuthors());
        }
    }

    private void displayISBN() {
        if (mBookInfo.getIsbn().length() > 0) {
            TextView textView = (TextView) findViewById(R.id.bookISBNTextView);
            textView.setText("ISBN: " + mBookInfo.getIsbn());
        }
    }

    private void displayPublisher() {
        if (mBookInfo.getPublisher().length() > 0) {
            TextView textView = (TextView) findViewById(R.id.bookPublisherTextView);
            textView.setText("Publisher: " + mBookInfo.getPublisher());
        }
    }

    private void displayDate() {
        if (mBookInfo.getPublishedDate().length() > 0) {
            TextView textView = (TextView) findViewById(R.id.bookDateTextView);
            textView.setText("Publish date: " + mBookInfo.getPublishedDate());
        }
    }

    private void displayPages() {
        if (mBookInfo.getPagesCount() > 0) {
            TextView textView = (TextView) findViewById(R.id.bookPagesTextView);
            textView.setText("Pages: " + mBookInfo.getPagesCount());
        }
    }

    private void displayURL() {
        if (mBookInfo.getUrl().length() > 0) {
            TextView textView = (TextView) findViewById(R.id.bookURLTextView);
            textView.setText(mBookInfo.getUrl());
        }
    }
}
