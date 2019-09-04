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
import org.akvo.flow.presentation.geoshape.create.entities.Shape;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ViewFeatures {

    public static final String POINT_ID = "point-id";
    public static final String FEATURE_ID = "shape-id";

    private final List<Feature> features;
    private final List<Feature> pointFeatures;
    private final List<LatLng> listOfCoordinates;

    private final List<Shape> shapes = new ArrayList<>();

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

    public void addSelectedFeature(Feature feature) {
        setSelectedFeature(feature);
        getFeatures().add(feature);
    }

    public void removeSelectedPoint(Feature feature) {
        Feature pointFeature = getSelectedPointFeature(
                feature.getStringProperty(ViewFeatures.FEATURE_ID));
        getPointFeatures().remove(pointFeature);
    }

    public void removeFeature(Feature feature) {
        getFeatures().remove(feature);
        getPointFeatures().removeAll(
                getSelectedFeaturePoints(feature.getStringProperty(FEATURE_ID)));
    }

    public void selectFeatureFromPoint(Feature pointFeature) {
        String selectedFeatureId = pointFeature.getStringProperty(FEATURE_ID);
        String selectedPointId = pointFeature.getStringProperty(POINT_ID);
        for (Feature f : pointFeatures) {
            if (f.getStringProperty(FEATURE_ID).equals(selectedFeatureId)) {
                f.addBooleanProperty(GeoShapeConstants.SHAPE_SELECTED_PROPERTY, true);
            } else {
                f.removeProperty(GeoShapeConstants.SHAPE_SELECTED_PROPERTY);
            }
            if (selectedPointId.equals(f.getStringProperty(POINT_ID))) {
                f.addBooleanProperty(GeoShapeConstants.POINT_SELECTED_PROPERTY, true);
                f.removeProperty(GeoShapeConstants.SHAPE_SELECTED_PROPERTY);
            } else {
                f.removeProperty(GeoShapeConstants.POINT_SELECTED_PROPERTY);
            }
        }
        Feature feature = getFeatureById(selectedFeatureId);
        setSelectedFeature(feature);
    }

    public void unSelectFeature() {
        setSelectedFeature(null);
        for (Feature f : pointFeatures) {
            f.removeProperty(GeoShapeConstants.SHAPE_SELECTED_PROPERTY);
            f.removeProperty(GeoShapeConstants.POINT_SELECTED_PROPERTY);
        }
    }

    private Feature getFeatureById(String featureId) {
        List<Feature> features = getFeatures();
        for (Feature feature : features) {
            if (featureId.equals(feature.getStringProperty(FEATURE_ID))) {
                return feature;
            }
        }
        return null;
    }

    private Feature getSelectedPointFeature(String featureId) {
        List<Feature> features = getPointFeatures();
        for (Feature feature : features) {
            if (featureId.equals(feature.getStringProperty(FEATURE_ID))
                    && feature.hasProperty(GeoShapeConstants.POINT_SELECTED_PROPERTY)
                    && feature.getBooleanProperty(GeoShapeConstants.POINT_SELECTED_PROPERTY)) {
                return feature;
            }
        }
        return null;
    }

    private List<Feature> getSelectedFeaturePoints(String featureId) {
        List<Feature> pointFeatures = getPointFeatures();
        List<Feature> selectedFeaturePoints = new ArrayList<>();
        for (Feature feature : pointFeatures) {
            if (featureId.equals(feature.getStringProperty(FEATURE_ID))) {
                selectedFeaturePoints.add(feature);
            }
        }
        return selectedFeaturePoints;
    }
}
