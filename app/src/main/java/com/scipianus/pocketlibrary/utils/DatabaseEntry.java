package com.scipianus.pocketlibrary.utils;

import android.support.annotation.NonNull;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by scipianus on 10-Apr-17.
 */

@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class DatabaseEntry implements Comparable<DatabaseEntry> {
    private Integer id;
    private Double score;

    @Override
    public int compareTo(@NonNull DatabaseEntry o) {
        if (this.equals(o)) {
            return 0;
        }
        return this.score.compareTo(o.score);
    }
}
