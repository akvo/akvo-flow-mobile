/*
 *  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.ui.map;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import org.akvo.flow.R;

public class PointsFeature extends Feature {
    public static final String GEOMETRY_TYPE = "MultiPoint";

    public PointsFeature(GoogleMap map) {
        super(map);
    }

    @Override
    public void addPoint(LatLng point) {
        super.addPoint(point);
    }

    @Override
    public int getTitle() {
        return R.string.geoshape_points;
    }

    @Override
    public String geoGeometryType() {
        return GEOMETRY_TYPE;
    }

    @Override
    public boolean highlightNext(int position) {
        return false;
    }

}
