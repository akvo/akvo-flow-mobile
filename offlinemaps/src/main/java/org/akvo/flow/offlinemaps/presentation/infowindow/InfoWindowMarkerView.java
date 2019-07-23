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

package org.akvo.flow.offlinemaps.presentation.infowindow;

import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.plugins.markerview.MarkerView;

import static org.akvo.flow.offlinemaps.presentation.MapBoxMapViewImpl.ID_PROPERTY;
import static org.akvo.flow.offlinemaps.presentation.MapBoxMapViewImpl.LATITUDE_PROPERTY;
import static org.akvo.flow.offlinemaps.presentation.MapBoxMapViewImpl.LONGITUDE_PROPERTY;
import static org.akvo.flow.offlinemaps.presentation.MapBoxMapViewImpl.NAME_PROPERTY;

public class InfoWindowMarkerView extends MarkerView {

    private final InfoWindowLayout infoWindowLayout;

    public InfoWindowMarkerView(InfoWindowLayout view) {
        super(new LatLng(0,0), view);
        this.infoWindowLayout = view;
    }

    public void updateSelectedFeature(Feature feature) {
        infoWindowLayout.setUpMarkerInfo(feature.getStringProperty(ID_PROPERTY),
                feature.getStringProperty(NAME_PROPERTY));
        LatLng latLng = new LatLng(feature.getNumberProperty(LATITUDE_PROPERTY).doubleValue(),
                feature.getNumberProperty(LONGITUDE_PROPERTY).doubleValue());
        setLatLng(latLng);
    }
}
