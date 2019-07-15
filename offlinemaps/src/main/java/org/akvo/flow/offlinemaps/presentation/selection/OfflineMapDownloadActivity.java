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

package org.akvo.flow.offlinemaps.presentation.selection;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.offline.OfflineManager;

import org.akvo.flow.mapbox.offline.reactive.RegionNameMapper;
import org.akvo.flow.offlinemaps.R;
import org.akvo.flow.offlinemaps.presentation.ToolBarBackActivity;
import org.akvo.flow.offlinemaps.presentation.list.OfflineAreasListActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class OfflineMapDownloadActivity extends ToolBarBackActivity
        implements OfflineMapDownloadView {

    private MapView mapView;
    private Button saveBt;
    private EditText mapNameEt;
    private ProgressBar downloadProgress;

    private OfflineMapDownloadPresenter presenter;
    private MapboxMap mapboxMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_map_download);
        setupToolBar();
        setUpViews();
        setupMap(savedInstanceState);
        presenter = new OfflineMapDownloadPresenter(OfflineManager.getInstance(this),
                new RegionNameMapper());
        presenter.setView(this);
    }

    private void setupMap(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this::setUpMapBox);
    }

    private void setUpViews() {
        saveBt = findViewById(R.id.offline_map_save_button);
        saveBt.setOnClickListener(v -> {
            if (mapboxMap != null && mapboxMap.getStyle() != null) {
                float pixelRatio = getResources().getDisplayMetrics().density;
                String styleUrl = mapboxMap.getStyle().getUrl();
                LatLngBounds bounds = mapboxMap.getProjection().getVisibleRegion().latLngBounds;
                double zoom = mapboxMap.getCameraPosition().zoom;
                presenter.downloadArea(styleUrl, bounds, pixelRatio, zoom,
                        mapNameEt.getText().toString());
            }
        });
        mapNameEt = findViewById(R.id.offline_map_name);
        mapNameEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                //EMPTY
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //EMPTY
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(mapNameEt.getText().toString())) {
                    saveBt.setEnabled(false);
                } else {
                    saveBt.setEnabled(true);
                }
            }
        });
        downloadProgress = findViewById(R.id.offline_map_download_progress);
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
        presenter.destroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void showProgress() {
        saveBt.setEnabled(false);
        downloadProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public void navigateToMapsList() {
        navigateToOfflineAreasList(this);
        finish();
    }

    @Override
    public void showOfflineAreaError() {
        downloadProgress.setVisibility(View.GONE);
        saveBt.setEnabled(true);
        displaySnackBar(downloadProgress, R.string.offline_map_create_error);
    }

    public void navigateToOfflineAreasList(@Nullable Context context) {
        if (context != null) {
            Intent intent = new Intent(context, OfflineAreasListActivity.class);
            context.startActivity(intent);
        }
    }
}
