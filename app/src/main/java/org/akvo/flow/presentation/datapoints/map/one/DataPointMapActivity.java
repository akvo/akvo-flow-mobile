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

package org.akvo.flow.presentation.datapoints.map.one;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Toast;

import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Projection;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.offlinemaps.Constants;
import org.akvo.flow.offlinemaps.presentation.FeatureConstants;
import org.akvo.flow.offlinemaps.presentation.SelectionManager;
import org.akvo.flow.util.ConstantUtil;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;

public class DataPointMapActivity extends BackActivity implements DataPointMapView,
        MapboxMap.OnMapClickListener {

    private static final String MARKER_SOURCE = "markers-source";
    private static final String MARKER_STYLE_LAYER = "markers-style-layer";
    private static final String MARKER_IMAGE = "custom-marker";

    private MapView mapView;
    private MapboxMap mapboxMap;

    private String dataPointId;

    @Inject
    DataPointMapPresenter presenter;
    private SelectionManager selectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_point_map);
        setupToolBar();
        initializeInjector();
        presenter.setView(this);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        dataPointId = getIntent().getStringExtra(ConstantUtil.DATA_POINT_ID_EXTRA);
        mapView.getMapAsync(mapboxMap -> {
            this.mapboxMap = mapboxMap;
            this.mapboxMap.addOnMapClickListener(this);
            selectionManager = new SelectionManager(mapView, mapboxMap, null);
            mapboxMap.setStyle(Constants.MAPBOX_MAP_STYLE, style -> {
                style.addImage(MARKER_IMAGE, BitmapFactory.decodeResource(
                        DataPointMapActivity.this.getResources(), R.drawable.marker));
                addMarkers(style);
                presenter.loadDataPoint(dataPointId);
            });
        });
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent()).build();
        viewComponent.inject(this);
    }

    private void addMarkers(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addLayer(new SymbolLayer(MARKER_STYLE_LAYER, MARKER_SOURCE)
                .withProperties(
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.iconImage(MARKER_IMAGE)
                ));
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
        presenter.destroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void showDataPoint(String displayName, Feature feature) {
        setTitle(displayName);
        if (mapboxMap != null) {
            displayFeature(feature);
        }
    }

    private void displayFeature(@NonNull Feature feature) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(
                        feature.getNumberProperty(FeatureConstants.LATITUDE_PROPERTY).doubleValue(),
                        feature.getNumberProperty(FeatureConstants.LONGITUDE_PROPERTY)
                                .doubleValue()))
                .zoom(10)
                .build();
        mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        Style style = mapboxMap.getStyle();
        if (style != null) {
            style.addSource(new GeoJsonSource(MARKER_SOURCE, feature));
        }
    }

    @Override
    public void showDataPointError() {
        Toast.makeText(getApplicationContext(), R.string.error_displaying_datapoint,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void dismiss() {
        finish();
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        if (mapboxMap != null) {
            Projection projection = mapboxMap.getProjection();
            List<Feature> features = mapboxMap
                    .queryRenderedFeatures(projection.toScreenLocation(point), MARKER_STYLE_LAYER);
            Feature selected = features.isEmpty() ? null : features.get(0);
            return selectionManager.handleFeatureClick(selected);
        } else {
            return false;
        }
    }

}
