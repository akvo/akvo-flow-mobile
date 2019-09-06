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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.Style;

import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants;
import org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapesMapViewImpl;
import org.akvo.flow.presentation.SnackBarManager;
import org.akvo.flow.presentation.geoshape.DeletePointDialog;
import org.akvo.flow.presentation.geoshape.DeleteShapeDialog;
import org.akvo.flow.presentation.geoshape.create.entities.AreaShape;
import org.akvo.flow.presentation.geoshape.create.entities.LineShape;
import org.akvo.flow.presentation.geoshape.create.entities.PointShape;
import org.akvo.flow.presentation.geoshape.create.entities.Shape;
import org.akvo.flow.presentation.geoshape.create.entities.ShapePoint;
import org.akvo.flow.presentation.geoshape.properties.PropertiesDialog;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.ACCURACY_THRESHOLD;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.CIRCLE_SOURCE_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.FILL_SOURCE_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.LINE_SOURCE_ID;
import static org.akvo.flow.presentation.geoshape.create.DrawMode.AREA;
import static org.akvo.flow.presentation.geoshape.create.DrawMode.LINE;
import static org.akvo.flow.presentation.geoshape.create.DrawMode.POINT;

public class CreateGeoShapeActivity extends BackActivity implements
        DeletePointDialog.PointDeleteListener, DeleteShapeDialog.ShapeDeleteListener {

    private GeoShapesMapViewImpl mapView;
    private boolean changed = false;
    private boolean allowPoints;
    private boolean allowLine;
    private boolean allowPolygon;

    private DrawMode drawMode = DrawMode.NONE;

    private boolean manualInputEnabled;
    private TextView bottomBarTitle;
    private BottomAppBar bottomAppBar;

    private ViewFeatures viewFeatures = new ViewFeatures(new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>());
    private final List<Shape> shapes = new ArrayList<>();

    @Inject
    FeatureMapper featureMapper;

    @Inject
    SnackBarManager snackBarManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_geo_shape);
        initializeInjector();
        setupToolBar();
        setUpBottomBar();
        setUpFeatures();
        setUpMapView(savedInstanceState);

        allowPoints = getIntent().getBooleanExtra(ConstantUtil.EXTRA_ALLOW_POINTS, true);
        allowLine = getIntent().getBooleanExtra(ConstantUtil.EXTRA_ALLOW_LINE, true);
        allowPolygon = getIntent().getBooleanExtra(ConstantUtil.EXTRA_ALLOW_POLYGON, true);
        manualInputEnabled = getIntent().getBooleanExtra(ConstantUtil.EXTRA_MANUAL_INPUT, true);
    }

    private void initializeInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder()
                        .applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    private void setUpBottomBar() {
        bottomAppBar = findViewById(R.id.bottomBar);
        bottomBarTitle = findViewById(R.id.bottomBarTitle);

        bottomAppBar.replaceMenu(R.menu.create_geoshape_activity_bottom);
        bottomAppBar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.shape_info:
                    displaySelectedShapeInfo();
                    break;
                case R.id.add_point:
                    if (drawMode != DrawMode.NONE) {
                        addLocationPoint();
                    } else {
                        showMessage(R.string.geoshapes_error_select_shape);
                    }
                    break;
                case R.id.delete_point:
                    if (getSelectedShape() != null) {
                        DeletePointDialog pointDelete = DeletePointDialog.newInstance();
                        pointDelete.show(getSupportFragmentManager(), DeletePointDialog.TAG);
                    }
                    break;
                case R.id.delete_feature:
                    if (getSelectedShape() != null) {
                        DeleteShapeDialog shapeDelete = DeleteShapeDialog.newInstance();
                        shapeDelete.show(getSupportFragmentManager(), DeleteShapeDialog.TAG);
                    }
                    break;
                default:
                    break;
            }
            return true;
        });
    }

    private void displaySelectedShapeInfo() {
        Shape shape = getSelectedShape();
        if (shape != null) {
            PropertiesDialog dialog = PropertiesDialog.newInstance(shape);
            dialog.show(getSupportFragmentManager(), PropertiesDialog.TAG);
        }
    }

    @Nullable
    private Shape getSelectedShape() {
        for (Shape shape: shapes) {
            if (shape.isSelected()) {
                return shape;
            }
        }
        return null;
    }

    private void addLocationPoint() {
        //TODO: if location permission lacking we should prompt the used to give them
        Location location = isLocationAllowed() ? mapView.getLocation() : null;
        if (location != null && location.getAccuracy() <= ACCURACY_THRESHOLD) {
            addPoint(new LatLng(location.getLatitude(), location.getLongitude()));
            updateChanged();
            updateSources();
        } else {
            int messageId = location != null ?
                    R.string.location_inaccurate :
                    R.string.location_unknown;
            showMessage(messageId);
        }
    }

    private void setUpMapView(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsyncWithCallback(() -> {
            //TODO: maybe only when a shape type is selected
            updateAttributionMargin();
            mapView.initSources(FeatureCollection.fromFeatures(viewFeatures.getFeatures()),
                    FeatureCollection.fromFeatures(viewFeatures.getPointFeatures()));
            mapView.initCircleSelectionSources();
            displayUserLocation();
            mapView.centerMap(viewFeatures.getListOfCoordinates());
            setMapClicks();
        });
    }

    private void updateAttributionMargin() {
        View view = findViewById(R.id.toolbar);
        int height = view.getHeight();
        if (height > 0) {
            mapView.setBottomMargin(height);
        } else {
            view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                        int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    int toolBarHeight = bottom - top;
                    if (toolBarHeight > 0) {
                        view.removeOnLayoutChangeListener(this);
                        mapView.setBottomMargin(toolBarHeight);
                    }
                }
            });
        }
    }

    private void setUpFeatures() {
        String geoJSON = getIntent().getStringExtra(ConstantUtil.GEOSHAPE_RESULT);
        shapes.clear();
        shapes.addAll(featureMapper.toShapes(geoJSON));
        viewFeatures = featureMapper.toViewFeatures(shapes);
    }

    @SuppressLint("MissingPermission")
    public void displayUserLocation() {
        if (isLocationAllowed()) {
            mapView.displayUserLocation();
        }
    }

    private void setMapClicks() {
        mapView.setMapClicks(this::onMapLongClick, this::onMapClick);
    }

    private boolean onMapClick(Feature feature) {
        Shape selected = selectFeatureFromPoint(feature);
        if (selected instanceof PointShape) {
            enableShapeDrawMode(R.string.geoshape_points, POINT);
        } else  if (selected instanceof LineShape) {
            enableShapeDrawMode(R.string.geoshape_line, LINE);
        } else  if (selected instanceof AreaShape) {
            enableShapeDrawMode(R.string.geoshape_area, AREA);
        }
        updateSources();
        return true;
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

    private boolean onMapLongClick(LatLng point) {
        if (!manualInputEnabled) {
            showMessage(R.string.geoshapes_error_manual_disabled);
            return false;
        }
        if (drawMode != DrawMode.NONE) {
            addPoint(point);
            updateChanged();
            updateSources();
        } else {
            showMessage(R.string.geoshapes_error_select_shape);
        }
        return true;
    }

    private void showMessage(@StringRes int messageResId) {
        snackBarManager.displaySnackBar(bottomAppBar, messageResId, this);
    }

    private void updateChanged() {
        changed = true;
        invalidateOptionsMenu();
    }

    private void addPoint(LatLng latLng) {
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
    }

    private void addPointToMultiPoint(LatLng latLng) {
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

    private void addPointToLineString(LatLng latLng) {
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

    private void addPointToPolygon(LatLng latLng) {
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

    private void updateSources() {
        viewFeatures = featureMapper.toViewFeatures(shapes);
        FeatureCollection features = FeatureCollection.fromFeatures(viewFeatures.getFeatures());
        FeatureCollection pointList = FeatureCollection
                .fromFeatures(viewFeatures.getPointFeatures());
        mapView.setSource(features, FILL_SOURCE_ID);
        mapView.setSource(features, LINE_SOURCE_ID);
        mapView.setSource(pointList, CIRCLE_SOURCE_ID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_geoshape_activity, menu);
        if (!allowPoints) {
            hideMenuItem(menu, R.id.add_points);
        }
        if (!allowLine) {
            hideMenuItem(menu, R.id.add_line);
        }
        if (!allowPolygon) {
            hideMenuItem(menu, R.id.add_polygon);
        }
        MenuItem item = menu.findItem(R.id.save);
        if (item != null) {
            item.setVisible(isValidShape() && changed);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void hideMenuItem(Menu menu, int itemId) {
        menu.findItem(itemId).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.map_normal:
                updateMapStyle(Style.MAPBOX_STREETS);
                break;
            case R.id.map_satellite:
                updateMapStyle(Style.SATELLITE_STREETS);
                break;
            case R.id.map_terrain:
                updateMapStyle(Style.OUTDOORS);
                break;
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.add_points:
                enableNewShapeType(POINT, R.string.geoshape_points);
                break;
            case R.id.add_line:
                enableNewShapeType(LINE, R.string.geoshape_line);
                break;
            case R.id.add_polygon:
                enableNewShapeType(AREA, R.string.geoshape_area);
                break;
            case R.id.save:
                setShapeResult();
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableNewShapeType(DrawMode point, @StringRes int stringRes) {
        if (drawMode != point) {
            enableShapeDrawMode(stringRes, point);
            unSelectAllFeatures();
            updateSources();
        }
    }

    private void enableShapeDrawMode(@StringRes int textStringId, DrawMode point) {
        bottomAppBar.setVisibility(View.VISIBLE);
        bottomBarTitle.setText(textStringId);
        drawMode = point;
    }

    private void updateMapStyle(String style) {
        mapView.updateMapStyle(style, callback -> {
            mapView.initSources(FeatureCollection.fromFeatures(viewFeatures.getFeatures()),
                    FeatureCollection.fromFeatures(viewFeatures.getPointFeatures()));
            mapView.initCircleSelectionSources();
            mapView.centerMap(viewFeatures.getListOfCoordinates());
        });
    }

    private void setShapeResult() {
        Intent intent = new Intent();
        if (isValidShape() && changed) {
            FeatureCollection features = FeatureCollection.fromFeatures(viewFeatures.getFeatures());
            //TODO: shall we remove the id property?
            intent.putExtra(ConstantUtil.GEOSHAPE_RESULT, features.toJson());
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED, intent);
        }
    }

    //TODO: validate shapes
    private boolean isValidShape() {
        List<Feature> features = viewFeatures.getFeatures();
        return features.size() > 0;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void deletePoint() {
        Shape shape = getSelectedShape();
        if (shape != null) {
            List<ShapePoint> remainingPoints = removeLastPointFromFeature(shape);
            if (remainingPoints.size() == 0) {
                shapes.remove(shape);
                unSelectAllFeatures();
            }
            updateSources();
            updateChanged();
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

    @Override
    public void deleteShape() {
        Shape shape = getSelectedShape();
        if (shape != null) {
            shapes.remove(shape);
            unSelectAllFeatures();
            updateSources();
            updateChanged();
        }
    }
}
