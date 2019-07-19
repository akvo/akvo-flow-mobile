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

import android.os.Bundle;

import com.mapbox.mapboxsdk.maps.MapView;

import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.offlinemaps.Constants;
import org.akvo.flow.util.ConstantUtil;

public class DataPointMapActivity extends BackActivity {

    private MapView mapView;
    private String dataPointId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_point_map);
        setupToolBar();
        mapView = findViewById(org.akvo.flow.offlinemaps.R.id.mapView);
        mapView.onCreate(savedInstanceState);
        dataPointId = getIntent().getStringExtra(ConstantUtil.DATA_POINT_ID_EXTRA);
        //setTitle(mapName);
        mapView.getMapAsync(mapboxMap -> mapboxMap.setStyle(Constants.MAPBOX_MAP_STYLE, style -> {
           /* double zoom = mapInfo.getZoom();
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(mapInfo.getLatitude(), mapInfo.getLongitude()))
                    .zoom(zoom)
                    .build();*/
           /* mapboxMap.setMaxZoomPreference(zoom + Constants.MAP_BOX_ZOOM_MAX);
            mapboxMap.setMinZoomPreference(zoom - Constants.MAP_BOX_ZOOM_MAX);
            mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));*/
        }));
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        mapboxMap.setStyle(new Style.Builder().fromUrl(mapbox://styles/mapbox/streets-v11), new Style.OnStyleLoaded() {
        @Override
        public void onStyleLoaded(@NonNull Style style) {
            /* Image: An image is loaded and added to the map. */
            style.addImage(MARKER_IMAGE, BitmapFactory.decodeResource(
                    MainActivity.this.getResources(), R.drawable.custom_marker));
            addMarkers(style);
        }
    });
}

    private void addMarkers(@NonNull Style loadedMapStyle) {
        List<Feature> features = new ArrayList<>();
        features.add(Feature.fromGeometry(Point.fromLngLat(-77.9406, 38.8119)));

        /* Source: A data source specifies the geographic coordinate where the image marker gets placed. */

        loadedMapStyle.addSource(new GeoJsonSource(MARKER_SOURCE, FeatureCollection.fromFeatures(features)));

        /* Style layer: A style layer ties together the source and image and specifies how they are displayed on the map. */
        loadedMapStyle.addLayer(new SymbolLayer(MARKER_STYLE_LAYER, MARKER_SOURCE)
                .withProperties(
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.iconImage(MARKER_IMAGE),
                        // Adjust the second number of the Float array based on the height of your marker image.
                        // This is because the bottom of the marker should be anchored to the coordinate point, rather
                        // than the middle of the marker being the anchor point on the map.
                        PropertyFactory.iconOffset(new Float[] {0f, -52f})
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
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
