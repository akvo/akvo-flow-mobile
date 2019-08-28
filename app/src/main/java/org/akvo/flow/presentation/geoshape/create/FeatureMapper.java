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

import org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants;
import org.akvo.flow.presentation.geoshape.AreaCounter;
import org.akvo.flow.presentation.geoshape.LengthCounter;
import org.akvo.flow.util.GeoUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FeatureMapper {

    private final CoordinatesMapper coordinatesMapper;
    private final LengthCounter lengthCounter;
    private final AreaCounter areaCounter;
    private final GeoUtil geoUtil;

    @Inject
    public FeatureMapper(CoordinatesMapper coordinatesMapper,
            LengthCounter lengthCounter,
            AreaCounter areaCounter, GeoUtil geoUtil) {
        this.coordinatesMapper = coordinatesMapper;
        this.lengthCounter = lengthCounter;
        this.areaCounter = areaCounter;
        this.geoUtil = geoUtil;
    }

    public ViewFeatures toViewFeatures(@Nullable String gson) {
        List<Feature> features = createFeatureList(gson);
        final List<Feature> pointFeatures = new ArrayList<>();
        final List<LatLng> listOfCoordinates = new ArrayList<>();
        if (!features.isEmpty()) {
            for (Feature feature : features) {
                String featureId = UUID.randomUUID().toString();
                feature.addStringProperty(ViewFeatures.FEATURE_ID, featureId);
                Geometry geometry = feature.geometry();
                if (geometry instanceof Polygon) {
                    feature.addBooleanProperty(GeoShapeConstants.FEATURE_POLYGON, true);
                    List<List<Point>> coordinatesList = ((Polygon) geometry).coordinates();
                    for (List<Point> coordinates : coordinatesList) {
                        List<LatLng> listOfPointsCoordinates = coordinatesMapper
                                .toLatLng(coordinates);
                        listOfCoordinates.addAll(listOfPointsCoordinates);
                        for (LatLng latLng : listOfPointsCoordinates) {
                            pointFeatures.add(createPointFeature(latLng, featureId));
                        }
                    }
                } else if (geometry instanceof LineString) {
                    feature.addBooleanProperty(GeoShapeConstants.FEATURE_LINE, true);
                    List<Point> coordinates = ((LineString) geometry).coordinates();
                    List<LatLng> listOfLinesCoordinates = coordinatesMapper.toLatLng(coordinates);
                    listOfCoordinates.addAll(listOfLinesCoordinates);
                    for (LatLng latLng : listOfLinesCoordinates) {
                        pointFeatures.add(createPointFeature(latLng, featureId));
                    }
                } else if (geometry instanceof MultiPoint) {
                    feature.addBooleanProperty(GeoShapeConstants.FEATURE_POINT, true);
                    List<Point> coordinates = ((MultiPoint) geometry).coordinates();
                    List<LatLng> listOfPointsCoordinates = coordinatesMapper.toLatLng(coordinates);
                    listOfCoordinates.addAll(listOfPointsCoordinates);
                    for (LatLng latLng : listOfCoordinates) {
                        pointFeatures.add(createPointFeature(latLng, featureId));
                    }
                }
            }
        }
        return new ViewFeatures(features, pointFeatures, listOfCoordinates);
    }

    public Feature createPointFeature(Point mapTargetPoint, Feature containingFeature) {
        String containingFeatureId = containingFeature.getStringProperty(ViewFeatures.FEATURE_ID);
        Feature feature = Feature.fromGeometry(mapTargetPoint);
        feature.addBooleanProperty(GeoShapeConstants.POINT_SELECTED_PROPERTY, true);
        feature.addStringProperty(ViewFeatures.FEATURE_ID, containingFeatureId);
        feature.addStringProperty(ViewFeatures.POINT_ID, UUID.randomUUID().toString());
        return feature;
    }

    public Feature createNewMultiPointFeature(Point mapTargetPoint) {
        List<Point> points = getPointInList(mapTargetPoint);
        Feature feature = Feature.fromGeometry(MultiPoint.fromLngLats(points));
        feature.addStringProperty("pointCount", "1");
        feature.addBooleanProperty(GeoShapeConstants.FEATURE_POINT, true);
        feature.addStringProperty(ViewFeatures.FEATURE_ID, UUID.randomUUID().toString());
        return feature;
    }

    public Feature createNewLineStringFeature(Point mapTargetPoint) {
        List<Point> points = getPointInList(mapTargetPoint);
        Feature feature = Feature.fromGeometry(LineString.fromLngLats(points));
        feature.addStringProperty("pointCount", "1");
        feature.addStringProperty("length", "0.00");
        feature.addBooleanProperty(GeoShapeConstants.FEATURE_LINE, true);
        feature.addStringProperty(ViewFeatures.FEATURE_ID, UUID.randomUUID().toString());
        return feature;
    }

    public Feature createNewPolygonFeature(Point mapTargetPoint) {
        List<Point> points = new ArrayList<>();
        points.add(mapTargetPoint);
        List<List<Point>> es = new ArrayList<>();
        es.add(points);
        Feature feature = Feature.fromGeometry(Polygon.fromLngLats(es));
        feature.addStringProperty("pointCount", "1");
        feature.addStringProperty("length", "0.00");
        feature.addStringProperty("area", "0.00");
        feature.addBooleanProperty(GeoShapeConstants.FEATURE_POLYGON, true);
        feature.addStringProperty(ViewFeatures.FEATURE_ID, UUID.randomUUID().toString());
        return feature;
    }

    public boolean isValidMultiPointFeature(Feature selectedFeature) {
        return selectedFeature != null && selectedFeature.geometry() instanceof MultiPoint;
    }

    public boolean isValidLineStringFeature(Feature selectedFeature) {
        return selectedFeature != null && selectedFeature.geometry() instanceof LineString;
    }

    public boolean isValidPolygonFeature(Feature selectedFeature) {
        return selectedFeature != null && selectedFeature.geometry() instanceof Polygon;
    }

    public void updateExistingMultiPoint(Point mapTargetPoint, Feature selectedFeature) {
        List<Point> points = getMultiPointCoordinates(selectedFeature);
        points.add(mapTargetPoint);
        selectedFeature.addStringProperty("pointCount", points.size() + "");
    }

    public void updateExistingLineString(Point mapTargetPoint, Feature selectedFeature) {
        List<Point> points = getLineStringCoordinates(selectedFeature);
        points.add(mapTargetPoint);
        selectedFeature.addStringProperty("pointCount", points.size() + "");
        selectedFeature.addStringProperty("length",
                geoUtil.getDisplayLength(lengthCounter.computeLength(points)));
    }

    public void updateExistingPolygon(Point mapTargetPoint, Feature selectedFeature) {
        List<Point> points = getPolygonCoordinates(selectedFeature);
        int size = points.size();
        int count;
        if (size < 2) {
            points.add(mapTargetPoint);
            count = points.size();
        } else if (size == 2) {
            points.add(mapTargetPoint);
            points.add(points.get(0));
            count = points.size() - 1;
        } else {
            points.add(size - 2, mapTargetPoint);
            count = points.size() - 1;
        }
        selectedFeature.addStringProperty("pointCount", count + "");
        selectedFeature.addStringProperty("length",
                geoUtil.getDisplayLength(lengthCounter.computeLength(points)));
        selectedFeature.addStringProperty("area",
                geoUtil.getDisplayArea(areaCounter.area(points)));
    }

    private List<Point> getLineStringCoordinates(Feature selectedFeature) {
        Geometry geometry = selectedFeature == null ? null : selectedFeature.geometry();
        return geometry == null ? Collections.emptyList() : ((LineString) geometry).coordinates();
    }

    private List<Point> getPolygonCoordinates(Feature selectedFeature) {
        Geometry geometry = selectedFeature == null ? null : selectedFeature.geometry();
        return geometry == null ?
                Collections.emptyList() :
                ((Polygon) geometry).coordinates().get(0);
    }

    private List<Point> getMultiPointCoordinates(Feature selectedFeature) {
        Geometry geometry = selectedFeature == null ? null : selectedFeature.geometry();
        return geometry == null ? Collections.emptyList() : ((MultiPoint) geometry).coordinates();
    }

    @NonNull
    private List<Feature> createFeatureList(@Nullable String gson) {
        FeatureCollection featureCollection = gson == null ?
                FeatureCollection.fromFeatures(new ArrayList<>()) :
                FeatureCollection.fromJson(gson);

        List<Feature> features = featureCollection.features();
        if (features == null) {
            features = new ArrayList<>();
        }
        return features;
    }

    @NonNull
    private Feature createPointFeature(LatLng latLng, String featureId) {
        Feature feature = Feature.fromGeometry(
                Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude()));
        feature.addStringProperty(ViewFeatures.FEATURE_ID, featureId);
        feature.addStringProperty(ViewFeatures.POINT_ID, UUID.randomUUID().toString());
        return feature;
    }

    @NonNull
    private List<Point> getPointInList(Point mapTargetPoint) {
        List<Point> points = new ArrayList<>(1);
        points.add(mapTargetPoint);
        return points;
    }
}
