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

package org.akvo.flow.offlinemaps.presentation;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;

//import com.mapbox.geojson.Feature;
//import com.mapbox.mapboxsdk.camera.CameraPosition;
//import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
//import com.mapbox.mapboxsdk.geometry.LatLng;
//import com.mapbox.mapboxsdk.maps.MapView;
//import com.mapbox.mapboxsdk.maps.MapboxMap;
//import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
//import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
//import com.mapbox.mapboxsdk.maps.Projection;
//import com.mapbox.mapboxsdk.maps.Style;
//import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
//import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
//import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.akvo.flow.offlinemaps.Constants;
import org.akvo.flow.offlinemaps.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MapBoxMapItemView /*extends MapView*/ /*implements OnMapReadyCallback,*/
        /*MapboxMap.OnMapClickListener*/ {

    private static final String MARKER_SOURCE = "markers-source";
    private static final String MARKER_STYLE_LAYER = "markers-style-layer";
    private static final String MARKER_IMAGE = "custom-marker";

//    private MapboxMap mapboxMap;
    private SelectionManager selectionManager;
    private MapReadyCallback callback;

    public MapBoxMapItemView(@NonNull Context context) {
//        super(context);
    }

    public MapBoxMapItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
//        super(context, attrs);
    }

    public MapBoxMapItemView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
    }

//    public MapBoxMapItemView(@NonNull Context context, @Nullable MapboxMapOptions options) {
//        super(context, options);
//    }

//    public void getMapAsyncWithCallback(MapReadyCallback callback) {
//        this.callback = callback;
//        getMapAsync(this);
//    }

//    @Override
//    public void onMapReady(@NonNull MapboxMap mapboxMap) {
//        this.mapboxMap = mapboxMap;
//        this.mapboxMap.addOnMapClickListener(this);
//        selectionManager = new SelectionManager(this, mapboxMap, null);
//        mapboxMap.setStyle(Constants.MAPBOX_MAP_STYLE, style -> {
//            style.addImage(MARKER_IMAGE, BitmapFactory.decodeResource(
//                    getContext().getResources(), R.drawable.marker));
//            addMarkers(style);
//            if (callback != null) {
//                callback.onMapReady();
//                callback = null;
//            }
//        });
//    }

//    public void displayFeature(Feature feature) {
//        if (mapboxMap != null) {
//            CameraPosition cameraPosition = new CameraPosition.Builder()
//                    .target(new LatLng(
//                            feature.getNumberProperty(Constants.LATITUDE_PROPERTY)
//                                    .doubleValue(),
//                            feature.getNumberProperty(Constants.LONGITUDE_PROPERTY)
//                                    .doubleValue()))
//                    .zoom(10)
//                    .build();
//            mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//
//            Style style = mapboxMap.getStyle();
//            if (style != null) {
//                style.addSource(new GeoJsonSource(MARKER_SOURCE, feature));
//            }
//        }
//    }

//    @Override
//    public boolean onMapClick(@NonNull LatLng point) {
//        if (mapboxMap != null) {
//            Projection projection = mapboxMap.getProjection();
//            List<Feature> features = mapboxMap
//                    .queryRenderedFeatures(projection.toScreenLocation(point), MARKER_STYLE_LAYER);
//            Feature selected = features.isEmpty() ? null : features.get(0);
//            return selectionManager.handleFeatureClick(selected);
//        } else {
//            return false;
//        }
//    }

//    private void addMarkers(@NonNull Style loadedMapStyle) {
//        loadedMapStyle.addLayer(new SymbolLayer(MARKER_STYLE_LAYER, MARKER_SOURCE)
//                .withProperties(
//                        PropertyFactory.iconAllowOverlap(true),
//                        PropertyFactory.iconIgnorePlacement(true),
//                        PropertyFactory.iconImage(MARKER_IMAGE)
//                ));
//    }

}
