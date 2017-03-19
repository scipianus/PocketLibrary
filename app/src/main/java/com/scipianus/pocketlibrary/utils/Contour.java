package com.scipianus.pocketlibrary.utils;

import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by scipianus on 19-Mar-17.
 */

public class Contour implements Parcelable {
    private List<Point> points;

    public Contour(List<Point> points) {
        this.points = points;
    }

    public Contour(Parcel in) {
        points = new ArrayList<>();
        in.readList(points, Point.class.getClassLoader());
    }

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(points);
    }

    public static final Parcelable.Creator<Contour> CREATOR = new Parcelable.Creator<Contour>() {
        public Contour createFromParcel(Parcel in) {
            return new Contour(in);
        }

        public Contour[] newArray(int size) {
            return new Contour[size];
        }
    };
}
