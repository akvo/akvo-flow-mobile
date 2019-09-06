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

package org.akvo.flow.presentation.geoshape.create.entities;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public abstract class Shape implements Parcelable {

    private final String featureId;
    private boolean isSelected;
    private final List<ShapePoint> points;

    public Shape(String featureId, List<ShapePoint> points) {
        this.featureId = featureId;
        this.isSelected = false;
        this.points = points;
    }

    public Shape(Parcel in) {
        featureId = in.readString();
        isSelected = in.readByte() != 0;
        points = in.createTypedArrayList(ShapePoint.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(featureId);
        dest.writeByte((byte) (isSelected ? 1 : 0));
        dest.writeTypedList(points);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getFeatureId() {
        return featureId;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public List<ShapePoint> getPoints() {
        return points;
    }
}
