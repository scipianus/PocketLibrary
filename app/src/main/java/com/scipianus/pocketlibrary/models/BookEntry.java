package com.scipianus.pocketlibrary.models;

import android.graphics.Bitmap;

import com.scipianus.pocketlibrary.utils.HTTPUtils;

import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by scipianus on 30-Apr-17.
 */

public abstract class BookEntry implements Serializable {
    private String coverUrl;
    private transient Bitmap coverImage;
    private String title;
    private String authors;
    private String isbn;
    private String publisher;
    private String publishedDate;
    private int pagesCount;
    private String url;
    protected transient JSONObject jsonObject;
    protected String jsonString;

    protected abstract String extractCoverUrl();

    protected abstract String extractTitle();

    protected abstract String extractAuthors();

    protected abstract String extractIsbn();

    protected abstract String extractPublisher();

    protected abstract String extractPublishedDate();

    protected abstract int extractPagesCount();

    protected abstract String extractUrl();

    public void deserializeJSONObject() {
        try {
            jsonObject = new JSONObject(jsonString);
        } catch (Exception e) {
            jsonObject = new JSONObject();
        }
    }

    public Bitmap fetchCoverImage() {
        coverImage = HTTPUtils.getImage(getCoverUrl());
        return coverImage;
    }

    public Bitmap getCoverImage() {
        return coverImage;
    }

    public String getCoverUrl() {
        if (coverUrl == null) {
            coverUrl = extractCoverUrl();
        }
        return coverUrl;
    }

    public String getTitle() {
        if (title == null) {
            title = extractTitle();
        }
        return title;
    }

    public String getAuthors() {
        if (authors == null) {
            authors = extractAuthors();
        }
        return authors;
    }

    public String getIsbn() {
        if (isbn == null) {
            isbn = extractIsbn();
        }
        return isbn;
    }

    public String getPublisher() {
        if (publisher == null) {
            publisher = extractPublisher();
        }
        return publisher;
    }

    public String getPublishedDate() {
        if (publishedDate == null) {
            publishedDate = extractPublishedDate();
        }
        return publishedDate;
    }

    public int getPagesCount() {
        if (pagesCount == 0) {
            pagesCount = extractPagesCount();
        }
        return pagesCount;
    }

    public String getUrl() {
        if (url == null) {
            url = extractUrl();
        }
        return url;
    }
}
