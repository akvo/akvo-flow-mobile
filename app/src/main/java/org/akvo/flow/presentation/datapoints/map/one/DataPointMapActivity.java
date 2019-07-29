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

import android.os.Bundle;
import android.widget.Toast;

import com.mapbox.geojson.Feature;

import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.offlinemaps.presentation.MapBoxMapItemView;
import org.akvo.flow.offlinemaps.presentation.MapReadyCallback;
import org.akvo.flow.util.ConstantUtil;

import javax.inject.Inject;

public class DataPointMapActivity extends BackActivity implements DataPointMapView,
        MapReadyCallback {

    @Inject
    DataPointMapPresenter presenter;

    private MapBoxMapItemView mapView;
    private String dataPointId;

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
        mapView.getMapAsyncWithCallback(this);
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent()).build();
        viewComponent.inject(this);
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
        mapView.displayFeature(feature);
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
    public void onMapReady() {
        presenter.loadDataPoint(dataPointId);
    }
}
