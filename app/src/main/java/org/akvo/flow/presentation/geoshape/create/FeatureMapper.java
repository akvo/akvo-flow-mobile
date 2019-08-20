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
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.MultiPoint;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FeatureMapper {

    public ViewFeatures toViewFeatures(@Nullable String gson) {
        FeatureCollection featureCollection = gson == null ?
                FeatureCollection.fromFeatures(new ArrayList<>()) :
                FeatureCollection.fromJson(gson);

        List<Feature> features = featureCollection.features();
        if (features == null) {
            features = new ArrayList<>();
        }
        final List<Feature> pointFeatures = new ArrayList<>();
        final List<LatLng> listOfCoordinates = new ArrayList<>();
        if (!features.isEmpty()) {
            for (Feature feature : features) {
                String featureId = UUID.randomUUID().toString();
                feature.addStringProperty(ViewFeatures.FEATURE_ID, featureId);
                Geometry geometry = feature.geometry();
                if (geometry instanceof Polygon) {
                    feature.addBooleanProperty(ViewFeatures.FEATURE_POLYGON, true);
                    List<List<Point>> coordinatesList = ((Polygon) geometry).coordinates();
                    for (List<Point> coordinates : coordinatesList) {
                        List<LatLng> listOfPointsCoordinates = getListOfCoordinates(coordinates);
                        listOfCoordinates.addAll(listOfPointsCoordinates);
                        for (LatLng latLng : listOfPointsCoordinates) {
                            pointFeatures.add(getPointFeature(latLng, featureId));
                        }
                    }
                } else if (geometry instanceof LineString) {
                    feature.addBooleanProperty(ViewFeatures.FEATURE_LINE, true);
                    List<Point> coordinates = ((LineString) geometry).coordinates();
                    List<LatLng> listOfLinesCoordinates = getListOfCoordinates(coordinates);
                    listOfCoordinates.addAll(listOfLinesCoordinates);
                    for (LatLng latLng : listOfLinesCoordinates) {
                        pointFeatures
                                .add(getPointFeature(latLng, featureId));
                    }
                } else if (geometry instanceof MultiPoint) {
                    feature.addBooleanProperty(ViewFeatures.FEATURE_POINT, true);
                    List<Point> coordinates = ((MultiPoint) geometry).coordinates();
                    List<LatLng> listOfPointsCoordinates = getListOfCoordinates(coordinates);
                    listOfCoordinates.addAll(listOfPointsCoordinates);
                    for (LatLng latLng : listOfCoordinates) {
                        pointFeatures.add(getPointFeature(latLng, featureId));
                    }
                }
            }
        }
        return new ViewFeatures(features, pointFeatures, listOfCoordinates);
    }

    @NonNull
    private Feature getPointFeature(LatLng latLng, String featureId) {
        Feature feature = Feature.fromGeometry(
                Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude()));
        feature.addStringProperty(ViewFeatures.FEATURE_ID, featureId);
        feature.addStringProperty(ViewFeatures.POINT_ID, UUID.randomUUID().toString());
        return feature;
    }

    private List<LatLng> getListOfCoordinates(List<Point> coordinates) {
        List<LatLng> latLngs = new ArrayList<>();
        for (Point p : coordinates) {
            LatLng latLng = new LatLng(p.latitude(), p.longitude());
            latLngs.add(latLng);
        }
        return latLngs;
    }
}
