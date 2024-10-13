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

package org.akvo.flow.offlinemaps.presentation.view;

import android.os.Bundle;

//import com.mapbox.mapboxsdk.camera.CameraPosition;
//import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
//import com.mapbox.mapboxsdk.geometry.LatLng;
//import com.mapbox.mapboxsdk.maps.MapView;

//import org.akvo.flow.offlinemaps.Constants;
import org.akvo.flow.offlinemaps.R;
import org.akvo.flow.offlinemaps.domain.entity.MapInfo;
import org.akvo.flow.uicomponents.BackActivity;

import androidx.annotation.NonNull;

public class OfflineAreaViewActivity extends BackActivity {

    public static final String NAME_EXTRA = "name";
    public static final String MAP_INFO_EXTRA = "map-info";

//    private MapView mapView;
    private String mapName;
    private MapInfo mapInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_area_view);
        setupToolBar();
//        mapView = findViewById(R.id.mapView);
//        mapView.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mapInfo = extras.getParcelable(MAP_INFO_EXTRA);
            mapName = extras.getString(NAME_EXTRA);
        }
        setTitle(mapName);
//        mapView.getMapAsync(mapboxMap -> mapboxMap.setStyle(Constants.MAPBOX_MAP_STYLE, style -> {
//            double zoom = mapInfo.getZoom();
//            CameraPosition cameraPosition = new CameraPosition.Builder()
//                    .target(new LatLng(mapInfo.getLatitude(), mapInfo.getLongitude()))
//                    .zoom(zoom)
//                    .build();
//            mapboxMap.setMaxZoomPreference(zoom + Constants.MAP_BOX_ZOOM_MAX);
//            mapboxMap.setMinZoomPreference(zoom - Constants.MAP_BOX_ZOOM_MAX);
//            mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//        }));
    }

    @Override
    public void onResume() {
        super.onResume();
//        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
//        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
//        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
//        mapView.onSaveInstanceState(outState);
    }
}
