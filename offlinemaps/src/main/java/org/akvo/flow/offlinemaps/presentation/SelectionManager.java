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

package org.akvo.flow.offlinemaps.presentation;

//import com.mapbox.geojson.Feature;
//import com.mapbox.mapboxsdk.geometry.LatLng;
//import com.mapbox.mapboxsdk.maps.MapView;
//import com.mapbox.mapboxsdk.maps.MapboxMap;
//import com.mapbox.mapboxsdk.plugins.markerview.MarkerView;
//import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager;

import org.akvo.flow.offlinemaps.presentation.infowindow.InfoWindowLayout;

import androidx.annotation.Nullable;

import static org.akvo.flow.offlinemaps.Constants.ID_PROPERTY;
import static org.akvo.flow.offlinemaps.Constants.LATITUDE_PROPERTY;
import static org.akvo.flow.offlinemaps.Constants.LONGITUDE_PROPERTY;
import static org.akvo.flow.offlinemaps.Constants.NAME_PROPERTY;

public class SelectionManager {
//    private final InfoWindowLayout markerLayout;
//    private final MarkerViewManager markerViewManager;
//    private final MarkerView markerView;

//    private Feature currentSelected;

//    public SelectionManager(MapView mapView, MapboxMap mapboxMap,
//            InfoWindowLayout.InfoWindowSelectionListener listener) {
//        markerViewManager = new MarkerViewManager(mapView, mapboxMap);
//        markerLayout = new InfoWindowLayout(mapView.getContext());
//        markerLayout.setSelectionListener(listener);
//        markerView = new MarkerView(new LatLng(), markerLayout);
//    }

//    public boolean handleFeatureClick(Feature feature) {
//        if (featureSelected(feature)) {
//            return true;
//        }
//        if (currentSelected != null) {
//            unSelectFeature();
//            return true;
//        }
//        return false;
//    }

//    private boolean featureSelected(@Nullable Feature feature) {
//        if (feature != null && feature.hasNonNullValueForProperty(ID_PROPERTY)) {
//            if (selectedFeatureClicked(feature)) {
//                unSelectFeature();
//            } else {
//                selectFeature(feature);
//            }
//            return true;
//        }
//        return false;
//    }

//    private void selectFeature(Feature feature) {
//        if (currentSelected != null) {
//            markerViewManager.removeMarker(markerView);
//        }
//        markerViewManager.addMarker(markerView);
//        updateSelectedFeature(feature);
//        currentSelected = feature;
//    }

//    private boolean selectedFeatureClicked(Feature feature) {
//        return currentSelected != null && currentSelected.getStringProperty(ID_PROPERTY)
//                .equals(feature.getStringProperty(ID_PROPERTY));
//    }

//    void unSelectFeature() {
//        if (markerView != null) {
//            markerViewManager.removeMarker(markerView);
//        }
//        currentSelected = null;
//    }

//    private void updateSelectedFeature(Feature feature) {
//        markerLayout.setUpMarkerInfo(feature.getStringProperty(ID_PROPERTY),
//                feature.getStringProperty(NAME_PROPERTY));
//        double latitude = feature.getNumberProperty(LATITUDE_PROPERTY).doubleValue();
//        double longitude = feature.getNumberProperty(LONGITUDE_PROPERTY).doubleValue();
//        LatLng latLng = new LatLng(latitude, longitude);
//        markerView.setLatLng(latLng);
//    }

    public void destroy() {
        /*markerViewManager.onDestroy();*/
    }
}
