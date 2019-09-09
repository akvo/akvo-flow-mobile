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

package org.akvo.flow.presentation.geoshape.properties;

import android.location.Location;

import org.akvo.flow.presentation.geoshape.entities.ShapePoint;

import java.util.List;

import javax.inject.Inject;

public class LengthCounter {

    @Inject
    public LengthCounter() {
    }

    public float computeLength(List<ShapePoint> points) {
        float length = 0f;
        ShapePoint previous = null;
        for (ShapePoint point : points) {
            if (previous != null) {
                float[] distance = new float[1];
                Location.distanceBetween(previous.getLatitude(), previous.getLongitude(),
                        point.getLatitude(), point.getLongitude(), distance);
                length += distance[0];
            }
            previous = point;
        }
        return length;
    }
}
