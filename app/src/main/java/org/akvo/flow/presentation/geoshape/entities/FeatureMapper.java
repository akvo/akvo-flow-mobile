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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.MultiPoint;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public class FeatureMapper {

    private final CoordinatesMapper coordinatesMapper;
    private final PointsLatLngMapper pointsLatLngMapper;
    private final LengthCounter lengthCounter;
    private final AreaCounter areaCounter;

    @Inject
    public FeatureMapper(CoordinatesMapper coordinatesMapper, PointsLatLngMapper pointsLatLngMapper,
            LengthCounter lengthCounter, AreaCounter areaCounter) {
        this.coordinatesMapper = coordinatesMapper;
        this.pointsLatLngMapper = pointsLatLngMapper;
        this.lengthCounter = lengthCounter;
        this.areaCounter = areaCounter;
    }

    public List<Shape> toShapes(@Nullable String gson) {
        List<Shape> shapes = new ArrayList<>();
        List<Feature> features = createFeatureList(gson);
        if (!features.isEmpty()) {
            for (Feature feature : features) {
                String featureId = UUID.randomUUID().toString();
                Geometry geometry = feature.geometry();
                if (geometry instanceof Polygon) {
                    shapes.add(createArea(featureId, (Polygon) geometry));
                } else if (geometry instanceof LineString) {
                    shapes.add(createLine(featureId, (LineString) geometry));
                } else if (geometry instanceof MultiPoint) {
                    shapes.add(createPoint(featureId, (MultiPoint) geometry));
                }
            }
        }
        return shapes;
    }

    public ViewFeatures toViewFeatures(@NonNull List<Shape> shapes) {
        final List<Feature> features = new ArrayList<>(shapes.size());
        final List<Feature> pointFeatures = new ArrayList<>();
        final List<LatLng> listOfCoordinates = new ArrayList<>();
        if (!shapes.isEmpty()) {
            for (Shape shape : shapes) {
                Feature feature;
                List<LatLng> shapeCoordinates = pointsLatLngMapper.transform(shape.getPoints());
                List<Point> points = coordinatesMapper.toPointList(shapeCoordinates);
                if (shape instanceof AreaShape) {
                    List<List<Point>> es = new ArrayList<>();
                    es.add(points);
                    feature = Feature.fromGeometry(Polygon.fromLngLats(es));
                    feature.addBooleanProperty(GeoShapeConstants.FEATURE_POLYGON, true);
                    features.add(feature);
                } else if (shape instanceof LineShape) {
                    feature = Feature.fromGeometry(LineString.fromLngLats(points));
                    feature.addBooleanProperty(GeoShapeConstants.FEATURE_LINE, true);
                    features.add(feature);
                } else if (shape instanceof PointShape) {
                    feature = Feature.fromGeometry(MultiPoint.fromLngLats(points));
                    feature.addBooleanProperty(GeoShapeConstants.FEATURE_POINT, true);
                    features.add(feature);
                }
                pointFeatures.addAll(createPointFeaturesForShape(shape));
                listOfCoordinates.addAll(shapeCoordinates);
            }
        }
        return new ViewFeatures(features, pointFeatures, listOfCoordinates);
    }

    @NonNull
    private Shape createArea(String featureId, Polygon geometry) {
        List<ShapePoint> shapePoints = new ArrayList<>();
        List<LatLng> latLngs = coordinatesMapper.toLatLng(geometry.coordinates().get(0));
        for (LatLng latLng : latLngs) {
            String pointId = UUID.randomUUID().toString();
            double latitude = latLng.getLatitude();
            double longitude = latLng.getLongitude();
            shapePoints.add(new ShapePoint(pointId, featureId, latitude, longitude));
        }
        return new AreaShape(featureId, shapePoints);
    }

    @NonNull
    private LineShape createLine(String featureId, LineString geometry) {
        List<ShapePoint> shapePoints = new ArrayList<>();
        List<LatLng> latLngs = coordinatesMapper.toLatLng(geometry.coordinates());
        for (LatLng latLng : latLngs) {
            String pointId = UUID.randomUUID().toString();
            double latitude = latLng.getLatitude();
            double longitude = latLng.getLongitude();
            shapePoints.add(new ShapePoint(pointId, featureId, latitude, longitude));
        }
        return new LineShape(featureId, shapePoints);
    }

    @NonNull
    private PointShape createPoint(String featureId, MultiPoint geometry) {
        List<ShapePoint> shapePoints = new ArrayList<>();
        List<LatLng> latLngs = coordinatesMapper.toLatLng(geometry.coordinates());
        for (LatLng latLng : latLngs) {
            String pointId = UUID.randomUUID().toString();
            double latitude = latLng.getLatitude();
            double longitude = latLng.getLongitude();
            shapePoints.add(new ShapePoint(pointId, featureId, latitude, longitude));
        }
        return new PointShape(featureId, shapePoints);
    }

    public List<Feature> createPointFeaturesForShape(Shape shape) {
        List<ShapePoint> points = shape.getPoints();
        List<Feature> features = new ArrayList<>(points.size());
        boolean shapeSelected = shape.isSelected();
        for (ShapePoint p : points) {
            features.add(createPointFeature(p, shapeSelected));
        }
        return features;
    }

    public Feature createPointFeature(ShapePoint point, boolean isShapeSelected) {
        Feature feature = Feature
                .fromGeometry(coordinatesMapper.toPoint(pointsLatLngMapper.transform(point)));
        feature.addStringProperty(GeoShapeConstants.LAT_LNG_PROPERTY,
                point.getLatitude() + ", " + point.getLongitude());
        feature.addStringProperty(GeoShapeConstants.FEATURE_ID, point.getFeatureId());
        feature.addStringProperty(GeoShapeConstants.POINT_ID, point.getPointId());
        if (point.isSelected()) {
            feature.addBooleanProperty(GeoShapeConstants.POINT_SELECTED_PROPERTY, true);
        } else if (isShapeSelected) {
            feature.addBooleanProperty(GeoShapeConstants.SHAPE_SELECTED_PROPERTY, true);
        }
        return feature;
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

    public String createFeaturesToSave(List<Shape> shapes) {
        final List<Feature> features = new ArrayList<>(shapes.size());
        for (Shape shape : shapes) {
            List<LatLng> shapeCoordinates = pointsLatLngMapper.transform(shape.getPoints());
            List<Point> points = coordinatesMapper.toPointList(shapeCoordinates);
            if (shape instanceof AreaShape) {
                List<List<Point>> es = new ArrayList<>();
                es.add(points);
                Feature feature = Feature.fromGeometry(Polygon.fromLngLats(es));
                int count = points.size();
                if (count > 3) {
                    //remove last point which does not count as point, is just there to
                    //close the shape
                    count = count - 1;
                }
                feature.addStringProperty(GeoShapeConstants.PROPERTY_POINT_COUNT,
                        count + "");
                feature.addStringProperty(GeoShapeConstants.PROPERTY_LENGTH,
                        lengthCounter.computeLength(shape.getPoints()) + "");
                feature.addStringProperty(GeoShapeConstants.PROPERTY_AREA,
                        areaCounter.computeArea(shape.getPoints()) + "");
                features.add(feature);
            } else if (shape instanceof LineShape) {
                Feature feature = Feature.fromGeometry(LineString.fromLngLats(points));
                feature.addStringProperty(GeoShapeConstants.PROPERTY_POINT_COUNT,
                        points.size() + "");
                feature.addStringProperty(GeoShapeConstants.PROPERTY_LENGTH,
                        lengthCounter.computeLength(shape.getPoints()) + "");
                features.add(feature);
            } else if (shape instanceof PointShape) {
                Feature feature = Feature.fromGeometry(MultiPoint.fromLngLats(points));
                feature.addStringProperty(GeoShapeConstants.PROPERTY_POINT_COUNT,
                        points.size() + "");
                features.add(feature);
            }
        }
        return FeatureCollection.fromFeatures(features).toJson();
    }
}
