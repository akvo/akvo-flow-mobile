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

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.List;

public class AreaShape extends Shape {

    public AreaShape(String featureId, List<ShapePoint> points) {
        super(featureId, points);
    }

    public AreaShape(Parcel in) {
        super(in);
    }

    public static final Creator<AreaShape> CREATOR = new Creator<AreaShape>() {
        @Override
        public AreaShape createFromParcel(Parcel in) {
            return new AreaShape(in);
        }

        @Override
        public AreaShape[] newArray(int size) {
            return new AreaShape[size];
        }
    };

    @Override
    public void removeSelectedPoint() {
        super.removeSelectedPoint();
        List<ShapePoint> points = getPoints();
        //Remove the extra point we added to create a valid shape since at this point the shape ceases to be valid
        if (points.size() == 3) {
            points.remove(points.size() - 1);
        }
    }

    /**
     * When adding a point to an area (polygon) we need to add an extra point to "close" the shape.
     * The closing point is the same as the first point added
     */
    @Override
    public void addPoint(LatLng latLng) {
        unSelectAllPoints();
        ShapePoint shapePoint = createSelectedShapePoint(latLng, getFeatureId());
        List<ShapePoint> points = getPoints();
        int size = points.size();
        if (size < 2) {
            points.add(shapePoint);
        } else if (size == 2) {
            points.add(shapePoint);
            points.add(points.get(0));
        } else {
            points.add(size - 1, shapePoint);
        }
    }
}
