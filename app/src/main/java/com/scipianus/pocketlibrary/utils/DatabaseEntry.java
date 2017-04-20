package com.scipianus.pocketlibrary.utils;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by scipianus on 10-Apr-17.
 */

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class DatabaseEntry implements Comparable<DatabaseEntry> {
    private String id;
    private Double score;
    private Bitmap coverImage;

    public DatabaseEntry(String id, Double score) {
        this.id = id;
        this.score = score;
    }

    @Override
    public int compareTo(@NonNull DatabaseEntry o) {
        if (this.equals(o)) {
            return 0;
        }
        return this.score.compareTo(o.score);
    }
}
