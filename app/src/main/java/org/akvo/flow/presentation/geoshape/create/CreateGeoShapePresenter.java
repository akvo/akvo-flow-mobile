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

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
//import com.mapbox.mapboxsdk.geometry.LatLng;

import org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.presentation.geoshape.entities.AreaShape;
import org.akvo.flow.presentation.geoshape.entities.FeatureMapper;
import org.akvo.flow.presentation.geoshape.entities.LineShape;
import org.akvo.flow.presentation.geoshape.entities.PointShape;
import org.akvo.flow.presentation.geoshape.entities.PointsLatLngMapper;
import org.akvo.flow.presentation.geoshape.entities.Shape;
import org.akvo.flow.presentation.geoshape.entities.ShapePoint;
import org.akvo.flow.presentation.geoshape.entities.ViewFeatures;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public class CreateGeoShapePresenter implements Presenter {

    private final FeatureMapper featureMapper;
    private final PointsLatLngMapper pointsLatLngMapper;

    private ViewFeatures viewFeatures = new ViewFeatures(new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>());
    private final List<Shape> shapes = new ArrayList<>();
    private CreateGeoShapeView view;

    @Inject
    public CreateGeoShapePresenter(FeatureMapper featureMapper, PointsLatLngMapper pointsLatLngMapper) {
        this.featureMapper = featureMapper;
        this.pointsLatLngMapper = pointsLatLngMapper;
    }

    @Override
    public void destroy() {
        //EMPTY
    }

    public void setView(CreateGeoShapeView view) {
        this.view = view;
    }

    public void setUpFeatures(String geoJSON) {
        shapes.clear();
        shapes.addAll(featureMapper.toEditableShapes(geoJSON));
        viewFeatures = featureMapper.toViewFeatures(shapes);
    }

    public void onShapeInfoPressed() {
        Shape shape = getSelectedShape();
        if (shape != null) {
            view.displaySelectedShapeInfo(shape);
        }
    }

    public void onDeletePointPressed() {
        if (getSelectedPoint() != null) {
            view.displayDeletePointDialog();
        } else {
            view.displayNoPointSelectedError();
        }
    }

    public void onDeleteShapePressed() {
        if (getSelectedShape() != null) {
            view.displayDeleteShapeDialog();
        } else {
            view.displayNoShapeSelectedError();
        }
    }

    public void onMapReady() {
        view.displayMapItems(viewFeatures);
        Shape selectedShape = getSelectedShape();
        if (selectedShape != null) {
            ShapePoint selectedPoint = selectedShape.getSelectedPoint();
            if (selectedPoint != null) {
                view.updateSelected(pointsLatLngMapper.transform(selectedPoint));
            }
            if (selectedShape instanceof PointShape) {
                view.enablePointDrawMode();
                view.hideShapeSelection();
            } else if (selectedShape instanceof LineShape) {
                view.enableLineDrawMode();
                view.hideShapeSelection();
            } else if (selectedShape instanceof AreaShape) {
                view.enableAreaDrawMode();
                view.hideShapeSelection();
            }
        }
    }

    public void onGeoshapeSelected(Feature feature) {
        Shape selected = selectFeature(feature);
        if (selected instanceof PointShape) {
            view.enablePointDrawMode();
        } else if (selected instanceof LineShape) {
            view.enableLineDrawMode();
        } else if (selected instanceof AreaShape) {
            view.enableAreaDrawMode();
        }
        updateSources();
    }

//    public void onAddPointRequested(LatLng latLng, DrawMode drawMode) {
//        Shape shape = getSelectedShape();
//        if (shape != null) {
//            shape.addPoint(latLng);
//        } else {
//            unSelectAllFeatures();
//            Shape createdShape = createShape(drawMode);
//            if (createdShape != null) {
//                createdShape.setSelected(true);
//                createdShape.addPoint(latLng);
//                shapes.add(createdShape);
//            }
//        }
//        view.updateMenu();
//        updateSources();
//    }

    @Nullable
    private Shape createShape(DrawMode drawMode) {
        String featureId = UUID.randomUUID().toString();
        ArrayList<ShapePoint> points = new ArrayList<>();
        Shape createdShape = null;
        switch (drawMode) {
            case POINT:
                createdShape = new PointShape(featureId, points);
                break;
            case LINE:
                createdShape = new LineShape(featureId, points);
                break;
            case AREA:
                createdShape = new AreaShape(featureId, points);
                break;
            default:
                break;
        }
        return createdShape;
    }

    public void onNewDrawModePressed(DrawMode drawMode) {
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
        view.displayNewMapStyle(viewFeatures);
        Shape selectedShape = getSelectedShape();
        if (selectedShape != null) {
            ShapePoint selectedPoint = selectedShape.getSelectedPoint();
            if (selectedPoint != null) {
                view.updateSelected(pointsLatLngMapper.transform(selectedPoint));
            }
        }
    }

    public void onBackPressed(boolean changed) {
        saveShape(changed);
    }

    public void onSavePressed(boolean changed) {
        saveShape(changed);
    }

    private void saveShape(boolean changed) {
        if (isValidShape() && changed) {
            String featureString = featureMapper.createFeaturesToSave(shapes);
            view.setShapeResult(featureString);
        } else {
            view.setCanceledResult();
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

    @Nullable
    private ShapePoint getSelectedPoint() {
        for (Shape shape : shapes) {
            if (shape.isSelected()) {
                return shape.getSelectedPoint();
            }
        }
        return null;
    }

    //TODO: validate shapes
    public boolean isValidShape() {
        List<Feature> features = viewFeatures.getFeatures();
        return features.size() > 0;
    }

    public void onDeletePointConfirmed() {
        Shape shape = getSelectedShape();
        if (shape != null) {
            shape.removeSelectedPoint();
            if (shape.getPoints().size() == 0) {
                shapes.remove(shape);
            }
            updateSources();
            view.updateMenu();
        }
    }

    public void onDeleteShapeConfirmed() {
        Shape shape = getSelectedShape();
        if (shape != null) {
            shapes.remove(shape);
            updateSources();
            view.updateMenu();
        }
    }

    public void onGeoshapeMoved(Point point) {
        Shape shape = getSelectedShape();
        if (shape != null) {
            ShapePoint shapePoint = shape.getSelectedPoint();
            if (shapePoint != null) {
                shapePoint.setLatitude(point.latitude());
                shapePoint.setLongitude(point.longitude());
                updateSources();
                view.updateMenu();
            }
        }
    }

    private void updateSources() {
        viewFeatures = featureMapper.toViewFeatures(shapes);
        view.updateSources(viewFeatures);

        Shape selectedShape = getSelectedShape();
        if (selectedShape != null) {
            ShapePoint selectedPoint = selectedShape.getSelectedPoint();
            if (selectedPoint != null) {
                view.updateSelected(pointsLatLngMapper.transform(selectedPoint));
            } else {
                view.clearSelected();
            }
        } else {
            view.clearSelected();
        }
    }

    private Shape selectFeature(Feature feature) {
        String selectedFeatureId = feature.getStringProperty(GeoShapeConstants.FEATURE_ID);
        Shape selectedShape = null;
        String selectedPointId = feature.getStringProperty(GeoShapeConstants.POINT_ID);
        for (Shape shape : shapes) {
            if (shape.getFeatureId().equals(selectedFeatureId)) {
                if (!TextUtils.isEmpty(selectedPointId)) {
                    shape.select(selectedPointId);
                } else {
                    shape.setSelected(true);
                }
                selectedShape = shape;
            } else {
                shape.unSelect();
            }
        }
        return selectedShape;
    }

    private void unSelectAllFeatures() {
        for (Shape shape : shapes) {
            shape.unSelect();
        }
    }
}
