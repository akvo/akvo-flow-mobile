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

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;

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

    public void removeSelectedPoint() {
        List<ShapePoint> points = getPoints();
        ShapePoint pointToDelete = null;
        for (ShapePoint point : points) {
            if (point.isSelected()) {
                pointToDelete = point;
                break;
            }
        }
        if (pointToDelete != null) {
            points.remove(pointToDelete);
        }
    }

    public void unSelect() {
        setSelected(false);
        unSelectAllPoints();
    }

    public void select(String selectedPointId) {
        setSelected(true);
        List<ShapePoint> points = getPoints();
        for (ShapePoint point : points) {
            if (point.getPointId().equals(selectedPointId)) {
                point.setSelected(true);
            } else {
                point.setSelected(false);
            }
        }
    }

    public void addPoint(LatLng latLng) {
        unSelectAllPoints();
        ShapePoint shapePoint = createSelectedShapePoint(latLng, getFeatureId());
        points.add(shapePoint);
    }

    @NonNull
    ShapePoint createSelectedShapePoint(LatLng latLng, String featureId) {
        ShapePoint shapePoint = new ShapePoint(UUID.randomUUID().toString(),
                featureId, latLng.getLatitude(), latLng.getLongitude());
        shapePoint.setSelected(true);
        return shapePoint;
    }

    void unSelectAllPoints() {
        List<ShapePoint> points = getPoints();
        for (ShapePoint point : points) {
            point.setSelected(false);
        }
    }
}
