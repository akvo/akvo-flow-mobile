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
}
