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

package org.akvo.flow.presentation.datapoints.map.offline.selection;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;

import org.akvo.flow.R;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.BaseActivity;
import org.akvo.flow.presentation.SnackBarManager;
import org.akvo.flow.ui.Navigator;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

public class OfflineMapDownloadActivity extends BaseActivity implements OfflineMapDownloadView {

    @BindView(R.id.mapView)
    MapView mapView;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.offline_map_save_button)
    Button saveBt;

    @BindView(R.id.offline_map_name)
    EditText mapNameEt;

    @BindView(R.id.offline_map_download_progress)
    ProgressBar downloadProgress;

    @Inject
    OfflineMapDownloadPresenter presenter;

    @Inject
    Navigator navigator;

    @Inject
    SnackBarManager snackBarManager;

    private MapboxMap mapboxMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_map_download);
        initializeInjector();
        ButterKnife.bind(this);
        setupToolBar();
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this::setUpMapBox);
        presenter.setView(this);
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent()).build();
        viewComponent.inject(this);
    }

    private void setupToolBar() {
        toolbar.setNavigationIcon(R.drawable.ic_close);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setUpMapBox(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(defaultMapStyle(), style -> {
            //EMPTY
        });
    }

    @NonNull
    private Style.Builder defaultMapStyle() {
        return new Style.Builder().fromUrl("mapbox://styles/mapbox/light-v10");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @OnTextChanged(value = R.id.offline_map_name,
            callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    protected void afterEditTextChanged() {
        if (TextUtils.isEmpty(mapNameEt.getText().toString())) {
            saveBt.setEnabled(false);
        } else {
            saveBt.setEnabled(true);
        }
    }

    @OnClick(R.id.offline_map_save_button)
    protected void onSavePressed() {
        if (mapboxMap != null && mapboxMap.getStyle() != null) {
            String styleUrl = mapboxMap.getStyle().getUrl();
            LatLngBounds bounds = mapboxMap.getProjection().getVisibleRegion().latLngBounds;
            double minZoom = mapboxMap.getCameraPosition().zoom;
            double maxZoom = mapboxMap.getMaxZoomLevel();
            float pixelRatio = this.getResources().getDisplayMetrics().density;
            OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                    styleUrl, bounds, minZoom, maxZoom, pixelRatio);
            presenter.downloadArea(definition, mapNameEt.getText().toString());
        }
    }

    @Override
    public void showProgress() {
        saveBt.setEnabled(false);
        downloadProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public void navigateToMapsList() {
        navigator.navigateToOfflineAreasList(this);
        finish();
    }

    @Override
    public void showOfflineAreaError() {
        downloadProgress.setVisibility(View.GONE);
        saveBt.setEnabled(true);
        snackBarManager.displaySnackBar(downloadProgress, R.string.offline_map_create_error, this);
    }
}
