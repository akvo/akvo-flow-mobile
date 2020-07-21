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

import androidx.annotation.NonNull;

import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class CoordinatesMapper {

    @Inject
    public CoordinatesMapper() {
    }

    @NonNull
    List<LatLng> toLatLng(List<Point> coordinates) {
        List<LatLng> latLngs = new ArrayList<>();
        for (Point p : coordinates) {
            latLngs.add(toLatLng(p));
        }
        return latLngs;
    }

    List<Point> toPointList(List<LatLng> latLngs) {
        List<Point> points = new ArrayList<>(latLngs.size());
        for (LatLng latLng : latLngs) {
            points.add(toPoint(latLng));
        }
        return points;
    }

    @NonNull
    public Point toPoint(LatLng latLng) {
        return Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude());
    }

    @NonNull
    private LatLng toLatLng(Point p) {
        return new LatLng(p.latitude(), p.longitude());
    }
}
