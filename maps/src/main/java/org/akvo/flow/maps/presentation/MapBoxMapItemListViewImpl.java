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

package org.akvo.flow.maps.presentation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.AttributeSet;

import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.android.gestures.StandardScaleGestureDetector;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Projection;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.akvo.flow.maps.R;
import org.akvo.flow.maps.di.DaggerOfflineFeatureComponent;
import org.akvo.flow.maps.di.OfflineFeatureModule;
import org.akvo.flow.maps.domain.entity.MapInfo;
import org.akvo.flow.maps.presentation.infowindow.InfoWindowLayout;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.FragmentActivity;
import timber.log.Timber;

import static android.graphics.Color.rgb;
import static com.mapbox.mapboxsdk.style.expressions.Expression.all;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.gt;
import static com.mapbox.mapboxsdk.style.expressions.Expression.gte;
import static com.mapbox.mapboxsdk.style.expressions.Expression.has;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.lt;
import static com.mapbox.mapboxsdk.style.expressions.Expression.toNumber;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textSize;

/**
 * Map using mapbox which can display offline maps
 */
public class MapBoxMapItemListViewImpl extends MapView implements OnMapReadyCallback,
        MapboxMap.OnMapClickListener, MapBoxMapItemListView {

    private static final String MARKER_IMAGE = "custom-marker";
    private static final String SOURCE_ID = "points";
    private static final String POINT_COUNT = "point_count";
    private static final String UN_CLUSTERED_POINTS = "un-clustered-points";

    private SelectionManager selectionManager;

    @Nullable
    private MapboxMap mapboxMap;
    private GeoJsonSource source;
    private MapReadyCallback callback;

    @Inject
    MapBoxMapPresenter presenter;

    public MapBoxMapItemListViewImpl(@NonNull Context context) {
        super(context);
        init(context);
    }

    public MapBoxMapItemListViewImpl(@NonNull Context context,
            @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MapBoxMapItemListViewImpl(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public MapBoxMapItemListViewImpl(@NonNull Context context, @Nullable MapboxMapOptions options) {
        super(context, options);
        init(context);
    }

    private void init(Context context) {
        initialiseInjector(context);
        presenter.setView(this);
    }

    private void initialiseInjector(Context context) {
        DaggerOfflineFeatureComponent
                .builder()
                .offlineFeatureModule(
                        new OfflineFeatureModule(((AppCompatActivity) context).getApplication()))
                .build()
                .inject(this);
    }

    public void getMapAsyncWithCallback(MapReadyCallback callback) {
        this.callback = callback;
        getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        selectionManager = new SelectionManager(this, mapboxMap,
                getSelectionListener(getContext()));
        this.mapboxMap.addOnMapClickListener(this);
        addScaleAndMoveListeners();

        this.mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
            style.addImage(MARKER_IMAGE, BitmapFactory.decodeResource(
                    getResources(), R.drawable.marker), true);
            addClusteredGeoJsonSource(style, new ArrayList<>());
            if (callback != null) {
                callback.onMapReady();
                callback = null;
            }
            presenter.loadOfflineSettings();
        });
    }

    /**
     * When the map is zoomed or moved the displayed window popup may get displaced compared to the
     * marker itself or the marker may get clustered and the popup becomes orphaned
     * Better dismiss it and if user wants to select it, let him do it again.
     */
    private void addScaleAndMoveListeners() {
        if (mapboxMap != null) {
            mapboxMap.addOnScaleListener(new MapboxMap.OnScaleListener() {
                @Override
                public void onScaleBegin(@NonNull StandardScaleGestureDetector detector) {
                    if (selectionManager != null) {
                        selectionManager.unSelectFeature();
                    }
                }

                @Override
                public void onScale(@NonNull StandardScaleGestureDetector detector) {
                    //EMPTY
                }

                @Override
                public void onScaleEnd(@NonNull StandardScaleGestureDetector detector) {
                    //EMPTY
                }
            });

            mapboxMap.addOnMoveListener(new MapboxMap.OnMoveListener() {
                @Override
                public void onMoveBegin(@NonNull MoveGestureDetector detector) {
                    if (selectionManager != null) {
                        selectionManager.unSelectFeature();
                    }
                }

                @Override
                public void onMove(@NonNull MoveGestureDetector detector) {
                    //EMPTY
                }

                @Override
                public void onMoveEnd(@NonNull MoveGestureDetector detector) {
                    //EMPTY
                }
            });
        }
    }

    private InfoWindowLayout.InfoWindowSelectionListener getSelectionListener(Context context) {
        if (context instanceof InfoWindowLayout.InfoWindowSelectionListener) {
            return (InfoWindowLayout.InfoWindowSelectionListener) context;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implement InfoWindowSelectionListener");
        }
    }

    private void addClusteredGeoJsonSource(@NonNull Style loadedMapStyle, List<Feature> features) {
        addGeoJsonSource(loadedMapStyle, FeatureCollection.fromFeatures(features));
        addUnClusteredLayer(loadedMapStyle);
        addClusteredLayers(loadedMapStyle);
    }

    private void addClusteredLayers(@NonNull Style loadedMapStyle) {
        int[][] layers = new int[][] {
                new int[] { 50, Color.parseColor("#009954") },
                new int[] { 20, Color.parseColor("#007B99") },
                new int[] { 0, Color.parseColor("#005899") }
        };

        for (int i = 0; i < layers.length; i++) {
            addClusterLayer(loadedMapStyle, layers, i);
        }
        addCountLabels(loadedMapStyle);
    }

    private void addClusterLayer(@NonNull Style loadedMapStyle, int[][] layers, int layer) {
        int layerColor = layers[layer][1];
        CircleLayer circles = new CircleLayer("cluster-" + layer, SOURCE_ID);
        circles.setProperties(
                circleColor(layerColor),
                circleRadius(18f)
        );

        Expression pointCount = toNumber(get(POINT_COUNT));

        // Add a filter to the cluster layer that hides the circles based on "point_count"
        int minPointsNumber = layers[layer][0];
        circles.setFilter(layer == 0 ?
                all(has(POINT_COUNT), gte(pointCount, literal(minPointsNumber - 1))) :
                all(has(POINT_COUNT),
                        gt(pointCount, literal(minPointsNumber - 1)),
                        lt(pointCount, literal(layers[layer - 1][0]))
                )
        );
        loadedMapStyle.addLayer(circles);
    }

    private void addCountLabels(@NonNull Style loadedMapStyle) {
        SymbolLayer count = new SymbolLayer("count", SOURCE_ID);
        count.setProperties(
                textField(Expression.toString(get(POINT_COUNT))),
                textSize(12f),
                textColor(Color.WHITE),
                textIgnorePlacement(true),
                textAllowOverlap(true)
        );
        loadedMapStyle.addLayer(count);
    }

    private void addUnClusteredLayer(@NonNull Style loadedMapStyle) {
        SymbolLayer unClustered = new SymbolLayer(UN_CLUSTERED_POINTS, SOURCE_ID);
        unClustered.setProperties(iconImage(MARKER_IMAGE), iconColor(rgb(255, 119, 77)));
        loadedMapStyle.addLayer(unClustered);
    }

    private void addGeoJsonSource(@NonNull Style loadedMapStyle, FeatureCollection collection) {
        GeoJsonOptions options = new GeoJsonOptions().withCluster(true).withClusterRadius(50);
        source = new GeoJsonSource(SOURCE_ID, collection, options);
        loadedMapStyle.addSource(source);
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        if (mapboxMap != null) {
            Projection projection = mapboxMap.getProjection();
            List<Feature> features = mapboxMap
                    .queryRenderedFeatures(projection.toScreenLocation(point), UN_CLUSTERED_POINTS);
            Feature selected = features.isEmpty() ? null : features.get(0);
            return selectionManager.handleFeatureClick(selected);
        } else {
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void displayOfflineMap(@NonNull MapInfo mapInfo) {
        if (mapboxMap != null) {
            if (isLocationAllowed()) {
                LocationComponent locationComponent = mapboxMap.getLocationComponent();
                if (locationComponent.isLocationComponentActivated()) {
                    locationComponent.setLocationComponentEnabled(false);
                }
            }
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(mapInfo.getLatitude(), mapInfo.getLongitude()))
                    .zoom(mapInfo.getZoom())
                    .build();
            mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void displayUserLocation() {
        Context context = getContext();
        if (isLocationAllowed() && mapboxMap != null && context != null
                && mapboxMap.getStyle() != null) {
            try {
                LocationComponent locationComponent = mapboxMap.getLocationComponent();
                locationComponent.activateLocationComponent(
                        LocationComponentActivationOptions.builder(context, mapboxMap.getStyle())
                                .build());
                locationComponent.setLocationComponentEnabled(true);
                locationComponent.setCameraMode(CameraMode.TRACKING);
                locationComponent.setRenderMode(RenderMode.NORMAL);
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }

    private boolean isLocationAllowed() {
        FragmentActivity activity = (FragmentActivity) getContext();
        return activity != null && ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED;
    }

    public void refreshSelectedArea() {
        presenter.loadOfflineSettings();
    }

    public void displayDataPoints(FeatureCollection featureCollection) {
        if (source != null) {
            source.setGeoJson(featureCollection);
        }
        selectionManager.unSelectFeature();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.destroy();
        if (selectionManager != null) {
            selectionManager.destroy();
        }
    }
}
