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

package org.akvo.flow.presentation.geoshape;

import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class AreaCounter {

    private static final int EARTH_RADIUS = 6371000; // meters
    private static final double LATITUDE_SIZE = Math.PI * EARTH_RADIUS / 180;

    @Inject
    public AreaCounter() {
    }

    public double area(List<Point> originalPoints) {
        if (originalPoints.size() < 3) {
            return 0f;
        }

        // First we compute an equal-are projection of the polygon. We use a sinusoidal
        // projection for now: http://en.wikipedia.org/wiki/Sinusoidal_projection
        List<AreaCounterPoint> points = new ArrayList<>();
        for (Point location : originalPoints) {
            points.add(project(location));
        }

        // Now we calculate the area, using regular planar techniques.
        // http://mathworld.wolfram.com/PolygonArea.html
        double area = 0.0;
        AreaCounterPoint prev = points.get(points.size() - 1);// start form the last point
        int i = 0;
        while (i < points.size()) {
            AreaCounterPoint point = points.get(i);
            area += prev.x * point.y - point.x * prev.y;
            prev = point;
            i++;
        }
        return Math.abs(area) / 2;
    }

    private AreaCounterPoint project(Point location) {
        // Sinusoidal projection (equal-area)
        double y = location.latitude() * LATITUDE_SIZE;
        double x = location.longitude() * LATITUDE_SIZE * Math
                .cos(Math.toRadians(location.latitude()));
        return new AreaCounterPoint(x, y);
    }

    class AreaCounterPoint {
        final double x;
        final double y;

        AreaCounterPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}