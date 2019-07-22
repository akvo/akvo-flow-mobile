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

package org.akvo.flow.presentation.datapoints.one;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Toast;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.offlinemaps.Constants;
import org.akvo.flow.presentation.datapoints.map.entity.MapDataPoint;
import org.akvo.flow.util.ConstantUtil;

import javax.inject.Inject;

import androidx.annotation.NonNull;

public class DataPointMapActivity extends BackActivity implements DataPointMapView {

    private static final String MARKER_SOURCE = "markers-source";
    private static final String MARKER_STYLE_LAYER = "markers-style-layer";
    private static final String MARKER_IMAGE = "custom-marker";

    private MapView mapView;
    private MapboxMap mapBoxMap;

    private String dataPointId;

    @Inject
    DataPointMapPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_point_map);
        setupToolBar();
        initializeInjector();
        presenter.setView(this);
        mapView = findViewById(org.akvo.flow.offlinemaps.R.id.mapView);
        mapView.onCreate(savedInstanceState);
        dataPointId = getIntent().getStringExtra(ConstantUtil.DATA_POINT_ID_EXTRA);
        mapView.getMapAsync(mapboxMap -> mapboxMap.setStyle(Constants.MAPBOX_MAP_STYLE, style -> {
            style.addImage(MARKER_IMAGE, BitmapFactory.decodeResource(
                    DataPointMapActivity.this.getResources(), R.drawable.marker));
            addMarkers(style);
            this.mapBoxMap = mapboxMap;
            presenter.loadDataPoint(dataPointId);
        }));
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
    public void showDataPoint(@NonNull MapDataPoint dataPoint) {
        setTitle(dataPoint.getName());
        if (mapBoxMap != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(dataPoint.getLatitude(), dataPoint.getLongitude()))
                    .zoom(10)
                    .build();
            mapBoxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            Style style = mapBoxMap.getStyle();
            if (style != null) {
                Feature e = Feature.fromGeometry(
                        Point.fromLngLat(dataPoint.getLongitude(), dataPoint.getLatitude()));
                style.addSource(new GeoJsonSource(MARKER_SOURCE, e));
            }
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
}
