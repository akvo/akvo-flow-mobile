/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.presentation.geoshape.entities;

import android.os.Parcel;
import android.os.Parcelable;

public class ShapePoint implements Parcelable {

    private final String pointId;
    private final String featureId;
    private final double latitude;
    private final double longitude;

    private boolean isSelected = false;

    public ShapePoint(String pointId, String featureId, double latitude, double longitude) {
        this.pointId = pointId;
        this.featureId = featureId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    protected ShapePoint(Parcel in) {
        pointId = in.readString();
        featureId = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        isSelected = in.readByte() != 0;
    }

    public static final Creator<ShapePoint> CREATOR = new Creator<ShapePoint>() {
        @Override
        public ShapePoint createFromParcel(Parcel in) {
            return new ShapePoint(in);
        }

        @Override
        public ShapePoint[] newArray(int size) {
            return new ShapePoint[size];
        }
    };

    public String getPointId() {
        return pointId;
    }

    public String getFeatureId() {
        return featureId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(pointId);
        dest.writeString(featureId);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeByte((byte) (isSelected ? 1 : 0));
    }
}
