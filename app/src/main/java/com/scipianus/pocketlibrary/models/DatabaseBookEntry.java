package com.scipianus.pocketlibrary.models;

import android.support.annotation.NonNull;

import com.scipianus.pocketlibrary.utils.HTTPUtils;

import org.json.JSONObject;

import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Created by scipianus on 10-Apr-17.
 */

@EqualsAndHashCode(callSuper = true)
public class DatabaseBookEntry extends BookEntry implements Comparable<DatabaseBookEntry>, Serializable {
    private static final String COVER_API_PATH = "http://covers.openlibrary.org/b/olid/";
    private static final String BOOK_API_PATH_PREFIX = "https://openlibrary.org/api/books?bibkeys=OLID:";
    private static final String BOOK_API_PATH_SUFFIX = "&jscmd=data&format=json";
    @Getter
    private String id;
    @Getter
    private transient Double score;

    public DatabaseBookEntry(String id, Double score) {
        this.id = id;
        this.score = score;
    }

    public void fetchJSONData() {
        String url = BOOK_API_PATH_PREFIX + id + BOOK_API_PATH_SUFFIX;
        JSONObject jsonObject = HTTPUtils.getJSONObject(url);
        try {
            this.jsonObject = jsonObject.getJSONObject("OLID:" + id);
        } catch (Exception e) {
            this.jsonObject = new JSONObject();
        }
        this.jsonString = this.jsonObject.toString();
    }

    @Override
    public int compareTo(@NonNull DatabaseBookEntry o) {
        if (this.equals(o)) {
            return 0;
        }
        return this.score.compareTo(o.score);
    }

    @Override
    protected String extractCoverUrl() {
        return String.format("%s%s-L.jpg", COVER_API_PATH, id);
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
                authors.append(jsonObject.getJSONArray("authors").getJSONObject(i).getString("name"));
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
            if (jsonObject.getJSONObject("identifiers").has("isbn_13")) {
                return jsonObject.getJSONObject("identifiers").getJSONArray("isbn_13").getString(0);
            }
            if (jsonObject.getJSONObject("identifiers").has("isbn_10")) {
                return jsonObject.getJSONObject("identifiers").getJSONArray("isbn_10").getString(0);
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected String extractPublisher() {
        try {
            return jsonObject.getJSONArray("publishers").getJSONObject(0).getString("name");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected String extractPublishedDate() {
        try {
            return jsonObject.getString("publish_date");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected int extractPagesCount() {
        try {
            return jsonObject.getInt("number_of_pages");
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected String extractUrl() {
        try {
            return jsonObject.getString("url");
        } catch (Exception e) {
            return "";
        }
    }
}
