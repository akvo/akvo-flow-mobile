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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.Style;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapesClickListener;
import org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapesMapViewImpl;
import org.akvo.flow.presentation.geoshape.DeletePointDialog;
import org.akvo.flow.presentation.geoshape.DeleteShapeDialog;
import org.akvo.flow.presentation.geoshape.entities.Shape;
import org.akvo.flow.presentation.geoshape.entities.ViewFeatures;
import org.akvo.flow.presentation.geoshape.properties.PropertiesDialog;
import org.akvo.flow.uicomponents.BackActivity;
import org.akvo.flow.uicomponents.SnackBarManager;
import org.akvo.flow.util.ConstantUtil;

import javax.inject.Inject;

import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.ACCURACY_THRESHOLD;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.CIRCLE_SOURCE_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.CIRCLE_SOURCE_ID_LABEL;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.FILL_SOURCE_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.LINE_SOURCE_ID;
import static org.akvo.flow.presentation.geoshape.create.DrawMode.AREA;
import static org.akvo.flow.presentation.geoshape.create.DrawMode.LINE;
import static org.akvo.flow.presentation.geoshape.create.DrawMode.POINT;

public class CreateGeoShapeActivity extends BackActivity implements
        DeletePointDialog.PointDeleteListener, DeleteShapeDialog.ShapeDeleteListener,
        CreateGeoShapeView {

    private GeoShapesMapViewImpl mapView;
    private boolean changed = false;
    private boolean allowPoints;
    private boolean allowLine;
    private boolean allowPolygon;

    private DrawMode drawMode = DrawMode.NONE;

    private boolean manualInputEnabled;
    private TextView bottomBarTitle;
    private BottomAppBar bottomAppBar;

    @Inject
    CreateGeoShapePresenter presenter;

    @Inject
    SnackBarManager snackBarManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_geo_shape);
        initializeInjector();
        setupToolBar();
        setUpBottomBar();
        presenter.setView(this);
        presenter.setUpFeatures(getIntent().getStringExtra(ConstantUtil.GEOSHAPE_RESULT));
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

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return {@link ApplicationComponent}
     */
    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getApplication()).getApplicationComponent();
    }

    private void setUpBottomBar() {
        bottomAppBar = findViewById(R.id.bottomBar);
        bottomBarTitle = findViewById(R.id.bottomBarTitle);

        bottomAppBar.replaceMenu(R.menu.create_geoshape_activity_bottom);
        bottomAppBar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.shape_info:
                    presenter.onShapeInfoPressed();
                    break;
                case R.id.add_point:
                    if (drawMode != DrawMode.NONE) {
                        addLocationPoint();
                    } else {
                        showMessage(R.string.geoshapes_error_select_shape);
                    }
                    break;
                case R.id.delete_point:
                    presenter.onDeletePointPressed();
                    break;
                case R.id.delete_feature:
                    presenter.onDeleteShapePressed();
                    break;
                default:
                    break;
            }
            return true;
        });
    }

    @Override
    public void displayDeleteShapeDialog() {
        DeleteShapeDialog shapeDelete = DeleteShapeDialog.newInstance();
        shapeDelete.show(getSupportFragmentManager(), DeleteShapeDialog.TAG);
    }

    @Override
    public void displayDeletePointDialog() {
        DeletePointDialog pointDelete = DeletePointDialog.newInstance();
        pointDelete.show(getSupportFragmentManager(), DeletePointDialog.TAG);
    }

    @Override
    public void displaySelectedShapeInfo(Shape shape) {
        PropertiesDialog dialog = PropertiesDialog.newInstance(shape);
        dialog.show(getSupportFragmentManager(), PropertiesDialog.TAG);
    }

    private void addLocationPoint() {
        //TODO: if location permission lacking we should prompt the used to give them
        Location location = isLocationAllowed() ? mapView.getLocation() : null;
        if (location != null && location.getAccuracy() <= ACCURACY_THRESHOLD) {
            presenter.onAddPointRequested(
                    new LatLng(location.getLatitude(), location.getLongitude()), drawMode);
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
        mapView.getMapAsyncWithCallback(presenter::onMapReady);
    }

    @Override
    public void displayMapItems(ViewFeatures viewFeatures) {
        updateAttributionMargin();
        mapView.centerMap(viewFeatures.getListOfCoordinates());
        mapView.initSources(viewFeatures.getFeatures(),
                viewFeatures.getPointFeatures());
        mapView.initCircleSelectionSources();
        displayUserLocation();
        setMapClicks();
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

    @SuppressLint("MissingPermission")
    public void displayUserLocation() {
        if (isLocationAllowed()) {
            mapView.displayUserLocation();
        }
    }

    protected boolean isLocationAllowed() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED;
    }

    private void setMapClicks() {
        mapView.setMapClicks(this::onMapLongClick, new GeoShapesClickListener() {
            @Override
            public boolean onGeoShapeSelected(Feature feature) {
                return presenter.onGeoshapeSelected(feature);
            }

            @Override
            public boolean onGeoShapeMoved(Point point) {
                return presenter.onGeoshapeMoved(point);
            }
        });
    }

    @Override
    public void enableAreaDrawMode() {
        enableShapeDrawMode(R.string.geoshape_area, AREA);
    }

    @Override
    public void enableLineDrawMode() {
        enableShapeDrawMode(R.string.geoshape_line, LINE);
    }

    @Override
    public void enablePointDrawMode() {
        enableShapeDrawMode(R.string.geoshape_points, POINT);
    }

    private boolean onMapLongClick(LatLng point) {
        if (mapView.clicksAllowed()) {
            if (!manualInputEnabled) {
                showMessage(R.string.geoshapes_error_manual_disabled);
                return false;
            }
            if (drawMode != DrawMode.NONE) {
                presenter.onAddPointRequested(point, drawMode);
            } else {
                showMessage(R.string.geoshapes_error_select_shape);
            }
            return true;
        }
        return false;
    }

    private void showMessage(@StringRes int messageResId) {
        snackBarManager.displaySnackBar(bottomAppBar, messageResId, this);
    }

    @Override
    public void updateMenu() {
        changed = true;
        invalidateOptionsMenu();
    }

    @Override
    public void updateSources(ViewFeatures viewFeatures) {
        mapView.setSource(viewFeatures.getPointFeatures(), CIRCLE_SOURCE_ID);
        mapView.setSource(viewFeatures.getPointFeatures(), CIRCLE_SOURCE_ID_LABEL);
        mapView.setSource(viewFeatures.getFeatures(), LINE_SOURCE_ID);
        mapView.setSource(viewFeatures.getFeatures(), FILL_SOURCE_ID);
    }

    @Override
    public void updateSelected(LatLng coordinates) {
        mapView.displaySelectedPoint(coordinates);
    }

    @Override
    public void clearSelected() {
        mapView.clearSelected();
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
            item.setVisible(presenter.isValidShape() && changed);
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
                enableNewShapeType(POINT);
                break;
            case R.id.add_line:
                enableNewShapeType(LINE);
                break;
            case R.id.add_polygon:
                enableNewShapeType(AREA);
                break;
            case R.id.save:
                presenter.onSavePressed(changed);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableNewShapeType(DrawMode drawMode) {
        if (this.drawMode != drawMode) {
            presenter.onNewDrawModePressed(drawMode);
        }
    }

    private void enableShapeDrawMode(@StringRes int textStringId, DrawMode point) {
        bottomAppBar.setVisibility(View.VISIBLE);
        bottomBarTitle.setText(textStringId);
        drawMode = point;
    }

    private void updateMapStyle(String style) {
        mapView.updateMapStyle(style, callback -> {
            presenter.onMapStyleUpdated();
        });
    }

    @Override
    public void displayNewMapStyle(ViewFeatures viewFeatures) {
        mapView.initSources(viewFeatures.getFeatures(), viewFeatures.getPointFeatures());
        mapView.initCircleSelectionSources();
        mapView.centerMap(viewFeatures.getListOfCoordinates());
    }

    @Override
    public void setShapeResult(String shapesAsString) {
        Intent intent = new Intent();
        intent.putExtra(ConstantUtil.GEOSHAPE_RESULT, shapesAsString);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void setCanceledResult() {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
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
        presenter.onDeletePointConfirmed();
    }

    @Override
    public void deleteShape() {
        presenter.onDeleteShapeConfirmed();
    }
}
