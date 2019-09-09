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
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.presentation.geoshape.entities.AreaShape;
import org.akvo.flow.presentation.geoshape.entities.FeatureMapper;
import org.akvo.flow.presentation.geoshape.entities.LineShape;
import org.akvo.flow.presentation.geoshape.entities.PointShape;
import org.akvo.flow.presentation.geoshape.entities.Shape;
import org.akvo.flow.presentation.geoshape.entities.ShapePoint;
import org.akvo.flow.presentation.geoshape.entities.ViewFeatures;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CreateGeoShapePresenter implements Presenter {

    private final FeatureMapper featureMapper;

    private ViewFeatures viewFeatures = new ViewFeatures(new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>());
    private final List<Shape> shapes = new ArrayList<>();
    private CreateGeoShapeView view;

    @Inject
    public CreateGeoShapePresenter(FeatureMapper featureMapper) {
        this.featureMapper = featureMapper;
    }

    @Override
    public void destroy() {
        //TODO:
    }

    public void setView(CreateGeoShapeView view) {
        this.view = view;
    }

    public void setUpFeatures(String geoJSON) {
        shapes.clear();
        shapes.addAll(featureMapper.toShapes(geoJSON));
        viewFeatures = featureMapper.toViewFeatures(shapes);
    }

    public void onShapeInfoPressed() {
        Shape shape = getSelectedShape();
        if (shape != null) {
            view.displaySelectedShapeInfo(shape);
        }
    }

    @Nullable
    private Shape getSelectedShape() {
        for (Shape shape : shapes) {
            if (shape.isSelected()) {
                return shape;
            }
        }
        return null;
    }

    public void onDeletePointPressed() {
        if (getSelectedShape() != null) {
            view.displayDeletePointDialog();
        }
    }

    public void onDeleteShapePressed() {
        if (getSelectedShape() != null) {
            view.displayDeleteShapeDialog();
        }
    }

    public void onMapReady() {
        view.displayMapItems(viewFeatures);
    }

    public boolean onMapClick(Feature feature) {
        Shape selected = selectFeatureFromPoint(feature);
        if (selected instanceof PointShape) {
            view.enablePointDrawMode();
        } else if (selected instanceof LineShape) {
            view.enableLineDrawMode();
        } else if (selected instanceof AreaShape) {
            view.enableAreaDrawMode();
        }
        updateSources();
        return true;
    }

    public void onAddPointRequested(LatLng latLng, DrawMode drawMode) {
        switch (drawMode) {
            case POINT:
                addPointToMultiPoint(latLng);
                break;
            case LINE:
                addPointToLineString(latLng);
                break;
            case AREA:
                addPointToPolygon(latLng);
                break;
            default:
                break;
        }
        view.updateMenu();
        updateSources();
    }

    public void addPointToMultiPoint(LatLng latLng) {
        Shape shape = getSelectedShape();
        if (shape instanceof PointShape) {
            List<ShapePoint> points = shape.getPoints();
            for (ShapePoint point : points) {
                point.setSelected(false);
            }
            ShapePoint shapePoint = createSelectedShapePoint(latLng, shape);
            points.add(shapePoint);
        } else {
            unSelectAllFeatures();
            Shape createdShape = new PointShape(UUID.randomUUID().toString(), new ArrayList<>());
            ShapePoint shapePoint = createSelectedShapePoint(latLng, createdShape);
            createdShape.getPoints().add(shapePoint);
            createdShape.setSelected(true);
            shapes.add(createdShape);
        }
    }

    public void addPointToLineString(LatLng latLng) {
        Shape shape = getSelectedShape();
        if (shape instanceof LineShape) {
            List<ShapePoint> points = shape.getPoints();
            for (ShapePoint point : points) {
                point.setSelected(false);
            }
            ShapePoint shapePoint = createSelectedShapePoint(latLng, shape);
            points.add(shapePoint);
        } else {
            unSelectAllFeatures();
            Shape createdShape = new LineShape(UUID.randomUUID().toString(), new ArrayList<>());
            ShapePoint shapePoint = createSelectedShapePoint(latLng, createdShape);
            createdShape.getPoints().add(shapePoint);
            createdShape.setSelected(true);
            shapes.add(createdShape);
        }
    }

    public void addPointToPolygon(LatLng latLng) {
        Shape shape = getSelectedShape();
        if (shape instanceof AreaShape) {
            List<ShapePoint> points = shape.getPoints();
            for (ShapePoint point : points) {
                point.setSelected(false);
            }
            ShapePoint shapePoint = createSelectedShapePoint(latLng, shape);
            int size = points.size();
            if (size < 2) {
                points.add(shapePoint);
            } else if (size == 2) {
                points.add(shapePoint);
                points.add(points.get(0));
            } else {
                points.add(size - 2, shapePoint);
            }
        } else {
            unSelectAllFeatures();
            Shape createdShape = new AreaShape(UUID.randomUUID().toString(), new ArrayList<>());
            ShapePoint shapePoint = createSelectedShapePoint(latLng, createdShape);
            createdShape.getPoints().add(shapePoint);
            createdShape.setSelected(true);
            shapes.add(createdShape);
        }
    }

    @NonNull
    private ShapePoint createSelectedShapePoint(LatLng latLng, Shape shape) {
        ShapePoint shapePoint = new ShapePoint(UUID.randomUUID().toString(),
                shape.getFeatureId(), latLng.getLatitude(), latLng.getLongitude());
        shapePoint.setSelected(true);
        return shapePoint;
    }

    private Shape selectFeatureFromPoint(Feature feature) {
        String selectedFeatureId = feature.getStringProperty(GeoShapeConstants.FEATURE_ID);
        String selectedPointId = feature.getStringProperty(GeoShapeConstants.POINT_ID);
        Shape selectedShape = null;
        for (Shape shape : shapes) {
            if (shape.getFeatureId().equals(selectedFeatureId)) {
                shape.setSelected(true);
                selectedShape = shape;
                List<ShapePoint> points = shape.getPoints();
                for (ShapePoint point : points) {
                    if (point.getPointId().equals(selectedPointId)) {
                        point.setSelected(true);
                    } else {
                        point.setSelected(false);
                    }
                }
            } else {
                shape.setSelected(false);
                List<ShapePoint> points = shape.getPoints();
                for (ShapePoint point : points) {
                    point.setSelected(false);
                }
            }
        }
        return selectedShape;
    }

    private void unSelectAllFeatures() {
        for (Shape shape : shapes) {
            shape.setSelected(false);
            List<ShapePoint> points = shape.getPoints();
            for (ShapePoint point : points) {
                point.setSelected(false);
            }
        }
    }

    public void onNewDrawModePresssed(DrawMode drawMode) {
        switch (drawMode) {
            case POINT:
                view.enablePointDrawMode();
                break;
            case LINE:
                view.enableLineDrawMode();
                break;
            case AREA:
                view.enableAreaDrawMode();
                break;
            default:
                break;
        }
        unSelectAllFeatures();
        updateSources();
    }

    public void onMapStyleUpdated() {
        view.displayNewMapStyle(FeatureCollection.fromFeatures(viewFeatures.getFeatures()),
                FeatureCollection.fromFeatures(viewFeatures.getPointFeatures()),
                viewFeatures.getListOfCoordinates());
    }

    public void onSavePressed(boolean changed) {
        if (isValidShape() && changed) {
            //TODO: shall we remove the id property?
            FeatureCollection features = FeatureCollection.fromFeatures(viewFeatures.getFeatures());
            view.setShapeResult(features.toJson());

        } else {
            view.setCanceledResult();
        }
    }

    private List<ShapePoint> removeLastPointFromFeature(Shape shape) {
        List<ShapePoint> points = shape.getPoints();
        if (shape instanceof AreaShape) {
            if (points.size() < 3) {
                points.remove(points.size() - 1);
            } else {
                points.remove(points.size() - 2);
            }
        } else if (shape instanceof LineShape || shape instanceof PointShape) {
            points.remove(points.size() - 1);
        }
        return points;
    }

    //TODO: validate shapes
    public boolean isValidShape() {
        List<Feature> features = viewFeatures.getFeatures();
        return features.size() > 0;
    }

    public void onDeletePointConfirmed() {
        Shape shape = getSelectedShape();
        if (shape != null) {
            List<ShapePoint> remainingPoints = removeLastPointFromFeature(shape);
            if (remainingPoints.size() == 0) {
                shapes.remove(shape);
                unSelectAllFeatures();
            }
            updateSources();
            view.updateMenu();
        }
    }

    public void onDeleteShapeConfirmed() {
        Shape shape = getSelectedShape();
        if (shape != null) {
            shapes.remove(shape);
            unSelectAllFeatures();
            updateSources();
            view.updateMenu();
        }
    }

    private void updateSources() {
        viewFeatures = featureMapper.toViewFeatures(shapes);
        view.updateSources(FeatureCollection.fromFeatures(viewFeatures.getFeatures()),
                FeatureCollection.fromFeatures(viewFeatures.getPointFeatures()));
    }
}
