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

package org.akvo.flow.presentation.geoshape;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.mapbox.mapboxsdk.maps.Style;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.uicomponents.BackActivity;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.maps.presentation.geoshapes.GeoShapesMapViewImpl;
import org.akvo.flow.presentation.geoshape.entities.FeatureMapper;
import org.akvo.flow.presentation.geoshape.entities.Shape;
import org.akvo.flow.presentation.geoshape.entities.ViewFeatures;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class ViewGeoShapeActivity extends BackActivity {

    private GeoShapesMapViewImpl mapView;

    private ViewFeatures viewFeatures = new ViewFeatures(new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>());

    @Inject
    FeatureMapper featureMapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_geo_shape);
        initializeInjector();
        setupToolBar();
        setUpFeatures();
        setUpMapView(savedInstanceState);
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

    private void setUpMapView(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsyncWithCallback(() -> {
            mapView.initSources(viewFeatures.getFeatures(), viewFeatures.getPointFeatures());
            mapView.centerMap(viewFeatures.getListOfCoordinates());
        });
    }

    private void setUpFeatures() {
        String geoJSON = getIntent().getStringExtra(ConstantUtil.GEOSHAPE_RESULT);
        final List<Shape> shapes = featureMapper.toShapes(geoJSON);
        viewFeatures = featureMapper.toViewFeatures(shapes);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_geoshape_activity, menu);
        return super.onCreateOptionsMenu(menu);
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
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateMapStyle(String style) {
        mapView.updateMapStyle(style, callback -> {
            mapView.initSources(viewFeatures.getFeatures(), viewFeatures.getFeatures());
            mapView.centerMap(viewFeatures.getListOfCoordinates());
        });
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
}
