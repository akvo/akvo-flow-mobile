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

package org.akvo.flow.presentation.geoshape.create;

import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ViewFeatures {

    public static final String POINT_ID = "point-id";
    public static final String FEATURE_ID = "shape-id";

    private final List<Feature> features;
    private final List<Feature> pointFeatures;
    private final List<LatLng> listOfCoordinates;

    @Nullable
    private Feature selectedFeature;

    public ViewFeatures(@NonNull List<Feature> features, @NonNull  List<Feature> pointFeatures,
            @NonNull List<LatLng> listOfCoordinates) {
        this.features = features;
        this.pointFeatures = pointFeatures;
        this.listOfCoordinates = listOfCoordinates;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public List<Feature> getPointFeatures() {
        return pointFeatures;
    }

    public List<LatLng> getListOfCoordinates() {
        return listOfCoordinates;
    }

    @Nullable
    public Feature getSelectedFeature() {
        return selectedFeature;
    }

    public void setSelectedFeature(@Nullable Feature selectedFeature) {
        this.selectedFeature = selectedFeature;
    }

    public void updatePointsList(Feature newPointFeature) {
        String selectedFeatureId = newPointFeature.getStringProperty(ViewFeatures.FEATURE_ID);
        List<Feature> pointFeatureList = getPointFeatures();
        for (Feature f : pointFeatureList) {
            if (f.getStringProperty(ViewFeatures.FEATURE_ID).equals(selectedFeatureId)) {
                f.addBooleanProperty(GeoShapeConstants.SHAPE_SELECTED_PROPERTY, true);
            } else {
                f.removeProperty(GeoShapeConstants.SHAPE_SELECTED_PROPERTY);
            }
            f.removeProperty(GeoShapeConstants.POINT_SELECTED_PROPERTY);
        }
        pointFeatureList.add(newPointFeature);
    }

    public void addSelectedFeature(Feature feature) {
        setSelectedFeature(feature);
        getFeatures().add(feature);
    }
}
