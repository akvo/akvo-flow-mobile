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
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.Style;

import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapesMapView;
import org.akvo.flow.presentation.SnackBarManager;
import org.akvo.flow.presentation.geoshape.DeletePointDialog;
import org.akvo.flow.presentation.geoshape.DeleteShapeDialog;
import org.akvo.flow.presentation.geoshape.PropertiesDialog;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import timber.log.Timber;

import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.ACCURACY_THRESHOLD;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.CIRCLE_SOURCE_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.FILL_SOURCE_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.LINE_SOURCE_ID;
import static org.akvo.flow.presentation.geoshape.create.DrawMode.AREA;

public class CreateGeoShapeActivity extends BackActivity implements
        DeletePointDialog.PointDeleteListener, DeleteShapeDialog.ShapeDeleteListener {

    private GeoShapesMapView mapView;
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

    @Inject
    FeatureMapper featureMapper;

    @Inject
    CoordinatesMapper coordinatesMapper;

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
                        snackBarManager
                                .displaySnackBar(bottomAppBar,
                                        R.string.geoshapes_error_select_shape,
                                        this);
                    }
                    break;
                case R.id.delete_point:
                    if (viewFeatures.getSelectedFeature() != null) {
                        DeletePointDialog pointDelete = DeletePointDialog.newInstance();
                        pointDelete.show(getSupportFragmentManager(), DeletePointDialog.TAG);
                    }
                    break;
                case R.id.delete_feature:
                    if (viewFeatures.getSelectedFeature() != null) {
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
        Feature selectedFeature = viewFeatures.getSelectedFeature();
        if (selectedFeature != null) {
            String pointCount = selectedFeature
                    .getStringProperty(FeatureMapper.POINT_COUNT_PROPERTY_NAME);
            String length = selectedFeature.hasProperty(FeatureMapper.LENGTH_PROPERTY_NAME) ?
                    selectedFeature.getStringProperty(FeatureMapper.LENGTH_PROPERTY_NAME) : "";
            String area = selectedFeature.hasProperty(FeatureMapper.AREA_PROPERTY_NAME) ?
                    selectedFeature.getStringProperty(FeatureMapper.AREA_PROPERTY_NAME) : "";
            PropertiesDialog dialog = PropertiesDialog.newInstance(pointCount, length, area);
            dialog.show(getSupportFragmentManager(), PropertiesDialog.TAG);
        }
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
            snackBarManager.displaySnackBar(bottomAppBar, messageId, this);
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
            mapView.centerMap(viewFeatures.getListOfCoordinates());
            setMapClicks();
            displayUserLocation();
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
        viewFeatures = featureMapper.toViewFeatures(geoJSON);
    }

    @SuppressLint("MissingPermission")
    public void displayUserLocation() {
        if (isLocationAllowed()) {
            mapView.displayUserLocation();
        }
    }

    private void setMapClicks() {
        mapView.setMapClicks(point -> {
            if (!manualInputEnabled) {
                snackBarManager
                        .displaySnackBar(bottomAppBar, R.string.geoshapes_error_manual_disabled,
                                this);
                return false;
            }
            if (drawMode != DrawMode.NONE) {
                addPoint(point);
                updateChanged();
                updateSources();
            } else {
                snackBarManager
                        .displaySnackBar(bottomAppBar, R.string.geoshapes_error_select_shape,
                                this);
            }
            return true;
        }, feature -> {
            selectFeature(feature);
            return true;
        });
    }

    private void selectFeature(Feature feature) {
        viewFeatures.selectFeatureFromPoint(feature);
        updateSources();
    }

    private void updateChanged() {
        changed = true;
        invalidateOptionsMenu();
    }

    private void addPoint(LatLng latLng) {
        Point mapTargetPoint = Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude());
        switch (drawMode) {
            case POINT:
                addPointToMultiPoint(mapTargetPoint);
                break;
            case LINE:
                addPointToLineString(mapTargetPoint);
                break;
            case AREA:
                addPointToPolygon(mapTargetPoint);
                break;
            default:
                break;
        }
    }

    private void addPointToMultiPoint(Point mapTargetPoint) {
        Feature selectedFeature = viewFeatures.getSelectedFeature();
        if (featureMapper.isMultiPointFeature(selectedFeature)) {
            featureMapper.updateExistingMultiPoint(mapTargetPoint, selectedFeature);
        } else {
            selectedFeature = createNewMultiPointFeature(mapTargetPoint);
        }
        if (selectedFeature != null) {
            Feature feature = featureMapper.createPointFeature(mapTargetPoint, selectedFeature);
            featureMapper.updatePointsList(feature, viewFeatures.getPointFeatures());
        }
    }

    private void addPointToLineString(Point mapTargetPoint) {
        Feature selectedFeature = viewFeatures.getSelectedFeature();
        if (featureMapper.isLineStringFeature(selectedFeature)) {
            featureMapper.updateExistingLineString(mapTargetPoint, selectedFeature);
        } else {
            selectedFeature = createNewLineStringFeature(mapTargetPoint);
        }
        if (selectedFeature != null) {
            Feature feature = featureMapper.createPointFeature(mapTargetPoint, selectedFeature);
            featureMapper.updatePointsList(feature, viewFeatures.getPointFeatures());
        }
    }

    private void addPointToPolygon(Point mapTargetPoint) {
        Feature selectedFeature = viewFeatures.getSelectedFeature();
        if (featureMapper.isPolygonFeature(selectedFeature)) {
            featureMapper.updateExistingPolygon(mapTargetPoint, selectedFeature);
        } else {
            selectedFeature = createNewPolygonFeature(mapTargetPoint);
        }
        if (selectedFeature != null) {
            Feature feature = featureMapper.createPointFeature(mapTargetPoint, selectedFeature);
            featureMapper.updatePointsList(feature, viewFeatures.getPointFeatures());
        }
    }

    @NonNull
    private Feature createNewMultiPointFeature(Point mapTargetPoint) {
        final Feature feature = featureMapper.createNewMultiPointFeature(mapTargetPoint);
        viewFeatures.addSelectedFeature(feature);
        return feature;
    }

    private Feature createNewLineStringFeature(Point mapTargetPoint) {
        final Feature feature = featureMapper.createNewLineStringFeature(mapTargetPoint);
        viewFeatures.addSelectedFeature(feature);
        return feature;
    }

    private Feature createNewPolygonFeature(Point mapTargetPoint) {
        final Feature feature = featureMapper.createNewPolygonFeature(mapTargetPoint);
        viewFeatures.addSelectedFeature(feature);
        return feature;
    }

    private void updateSources() {
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
                updateMapStyle(Style.LIGHT);
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
                bottomAppBar.setVisibility(View.VISIBLE);
                bottomBarTitle.setText(R.string.geoshape_points);
                drawMode = DrawMode.POINT;
                break;
            case R.id.add_line:
                bottomAppBar.setVisibility(View.VISIBLE);
                bottomBarTitle.setText(R.string.geoshape_line);
                drawMode = DrawMode.LINE;
                break;
            case R.id.add_polygon:
                bottomAppBar.setVisibility(View.VISIBLE);
                bottomBarTitle.setText(R.string.geoshape_area);
                drawMode = AREA;
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
            Timber.d(features.toJson());
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
        Feature feature = viewFeatures.getSelectedFeature();
        if (feature != null) {
            List<Point> remainingPoints = featureMapper.removeLastPointFromFeature(feature);
            if (remainingPoints.size() == 0) {
                viewFeatures.setSelectedFeature(null);
                viewFeatures.removeFeature(feature);
            } else {
                viewFeatures
                        .removeSelectedPoint(feature.getStringProperty(ViewFeatures.FEATURE_ID));
            }
            updateSources();
            updateChanged();
        }
    }

    @Override
    public void deleteShape() {
        Feature feature = viewFeatures.getSelectedFeature();
        if (feature != null) {
            viewFeatures.setSelectedFeature(null);
            viewFeatures.removeFeature(feature);
            updateSources();
            updateChanged();
        }
    }
}
