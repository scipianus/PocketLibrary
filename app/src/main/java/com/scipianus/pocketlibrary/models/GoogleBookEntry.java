package com.scipianus.pocketlibrary.models;

import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by scipianus on 30-Apr-17.
 */

public class GoogleBookEntry extends BookEntry implements Serializable {

    public GoogleBookEntry(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
        this.jsonString = this.jsonObject.toString();
    }

    @Override
    protected String extractCoverUrl() {
        try {
            return jsonObject.getJSONObject("imageLinks").getString("thumbnail");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected String extractTitle() {
        try {
            return jsonObject.getString("title");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected String extractAuthors() {
        try {
            StringBuilder authors = new StringBuilder();
            for (int i = 0; i < jsonObject.getJSONArray("authors").length(); ++i) {
                authors.append(jsonObject.getJSONArray("authors").getString(i));
                if (i + 1 < jsonObject.getJSONArray("authors").length()) {
                    authors.append(" ");
                }
            }
            return authors.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected String extractIsbn() {
        try {
            for (int i = 0; i < jsonObject.getJSONArray("industryIdentifiers").length(); ++i) {
                if (jsonObject.getJSONArray("industryIdentifiers").getJSONObject(i).getString("type").equals("ISBN_13")) {
                    return jsonObject.getJSONArray("industryIdentifiers").getJSONObject(i).getString("identifier");
                }
            }
            for (int i = 0; i < jsonObject.getJSONArray("industryIdentifiers").length(); ++i) {
                if (jsonObject.getJSONArray("industryIdentifiers").getJSONObject(i).getString("type").equals("ISBN_10")) {
                    return jsonObject.getJSONArray("industryIdentifiers").getJSONObject(i).getString("identifier");
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected String extractPublisher() {
        try {
            return jsonObject.getString("publisher");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected String extractPublishedDate() {
        try {
            return jsonObject.getString("publishedDate");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected int extractPagesCount() {
        try {
            return jsonObject.getInt("pageCount");
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected String extractUrl() {
        try {
            return jsonObject.getString("infoLink");
        } catch (Exception e) {
            return "";
        }
    }
}
