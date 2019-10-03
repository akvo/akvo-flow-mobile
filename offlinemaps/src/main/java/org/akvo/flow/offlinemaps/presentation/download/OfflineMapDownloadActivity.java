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

package org.akvo.flow.offlinemaps.presentation.download;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;

import org.akvo.flow.offlinemaps.Constants;
import org.akvo.flow.offlinemaps.R;
import org.akvo.flow.offlinemaps.di.DaggerOfflineFeatureComponent;
import org.akvo.flow.offlinemaps.di.OfflineFeatureModule;
import org.akvo.flow.offlinemaps.presentation.Navigator;
import org.akvo.flow.offlinemaps.presentation.ToolBarBackActivity;
import org.akvo.flow.offlinemaps.tracking.TrackingHelper;

import javax.inject.Inject;

import androidx.annotation.NonNull;

public class OfflineMapDownloadActivity extends ToolBarBackActivity
        implements OfflineMapDownloadView {

    private MapView mapView;
    private Button saveBt;
    private EditText mapNameEt;
    private ProgressBar downloadProgress;
    private MapboxMap mapboxMap;
    private int callingScreen;
    private TrackingHelper trackingHelper;

    @Inject
    OfflineMapDownloadPresenter presenter;

    @Inject
    Navigator navigator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
        setContentView(R.layout.activity_offline_map_download);
        initialiseInjector();
        setupToolBar();
        setUpViews();
        setupMap(savedInstanceState);
        callingScreen = getIntent()
                .getIntExtra(Constants.CALLING_SCREEN_EXTRA, Constants.CALLING_SCREEN_EXTRA_LIST);
        presenter.setView(this);
        trackingHelper = new TrackingHelper(this);
    }

    private void initialiseInjector() {
        DaggerOfflineFeatureComponent
                .builder()
                .offlineFeatureModule(new OfflineFeatureModule(getApplication()))
                .build()
                .inject(this);
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
                String styleUrl = mapboxMap.getStyle().getUri();
                LatLngBounds bounds = mapboxMap.getProjection().getVisibleRegion().latLngBounds;
                double zoom = mapboxMap.getCameraPosition().zoom;
                presenter.downloadArea(styleUrl, bounds, pixelRatio, zoom,
                        mapNameEt.getText().toString());
            }
            if (trackingHelper != null) {
                trackingHelper.logOfflineAreaDownloadPressed();
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
        mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
            //EMPTY
        });
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
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
        if (callingScreen == Constants.CALLING_SCREEN_EXTRA_DIALOG) {
            navigator.navigateToOfflineAreasList(this);
        }
        finish();
    }

    @Override
    public void showOfflineAreaError() {
        downloadProgress.setVisibility(View.GONE);
        saveBt.setEnabled(true);
        displaySnackBar(downloadProgress, R.string.offline_map_create_error);
    }
}
