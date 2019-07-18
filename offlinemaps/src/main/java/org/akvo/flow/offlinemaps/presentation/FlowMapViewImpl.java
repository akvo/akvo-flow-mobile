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

import android.Manifest;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Projection;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.markerview.MarkerView;
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.akvo.flow.offlinemaps.R;
import org.akvo.flow.offlinemaps.di.DaggerOfflineFeatureComponent;
import org.akvo.flow.offlinemaps.di.OfflineFeatureModule;
import org.akvo.flow.offlinemaps.domain.entity.MapInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.FragmentActivity;

import static android.graphics.Color.rgb;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
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

public class FlowMapViewImpl extends MapView implements OnMapReadyCallback,
        MapboxMap.OnMapClickListener, FlowMapView {

    public static final String LATITUDE_PROPERTY = "latitude";
    public static final String LONGITUDE_PROPERTY = "longitude";
    private static final String MARKER_IMAGE = "custom-marker";
    private static final String SOURCE_ID = "datapoints";
    private static final String UNCLUSTERED_POINTS = "unclustered-points";
    private static final String POINT_COUNT = "point_count";
    public static final String ID_PROPERTY = "id";
    public static final String NAME_PROPERTY = "name";

    @Nullable
    private MapboxMap mapboxMap;

    private MarkerView markerView;
    private MarkerViewManager markerViewManager;
    private View customView;
    private TextView titleTextView;
    private TextView snippetTextView;
    private Feature currentSelected;
    private GeoJsonSource source;

    @Inject
    FlowMapPresenter presenter;

    public FlowMapViewImpl(@NonNull Context context) {
        super(context);
        init(context);
    }

    public FlowMapViewImpl(@NonNull Context context,
            @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FlowMapViewImpl(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public FlowMapViewImpl(@NonNull Context context, @Nullable MapboxMapOptions options) {
        super(context, options);
        init(context);
    }

    private void init(Context context) {
        customView = LayoutInflater.from(context).inflate(
                R.layout.symbol_layer_info_window_layout_callout, this, false);
        customView.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        titleTextView = customView.findViewById(R.id.info_window_title);
        snippetTextView = customView.findViewById(R.id.info_window_description);
        customView.setOnClickListener(v -> onInfoWindowClick((String) customView.getTag()));
        initialiseInjector(context);
        presenter.setView(this);
    }

    private void initialiseInjector(Context context) {
        DaggerOfflineFeatureComponent
                .builder()
                .offlineFeatureModule(new OfflineFeatureModule(((AppCompatActivity)context).getApplication()))
                .build()
                .inject(this);
    }


    private void onInfoWindowClick(String featureId) {
        //TODO: add listener to activity
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        this.mapboxMap.addOnMapClickListener(this);
        markerViewManager = new MarkerViewManager(this, mapboxMap);
        this.mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
            style.addImage(MARKER_IMAGE, BitmapFactory.decodeResource(
                    getResources(), R.drawable.marker), true);
            addClusteredGeoJsonSource(style, new ArrayList<>());
            presenter.loadOfflineSettings();
        });
    }

    private void addClusteredGeoJsonSource(@NonNull Style loadedMapStyle, List<Feature> features) {
        addGeoJsonSource(loadedMapStyle, FeatureCollection.fromFeatures(features));
        addUnClusteredLayer(loadedMapStyle);

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

    private void addClusterLayer(@NonNull Style loadedMapStyle, int[][] layers, int position) {
        int layerColor = layers[position][1];
        CircleLayer circles = new CircleLayer("cluster-" + position, SOURCE_ID);
        circles.setProperties(
                circleColor(layerColor),
                circleRadius(18f)
        );

        Expression pointCount = toNumber(get(POINT_COUNT));

        // Add a filter to the cluster layer that hides the circles based on "point_count"
        int minPointsNumber = layers[position][0];
        circles.setFilter(
                position == 0
                        ? all(has(POINT_COUNT),
                        gte(pointCount, literal(minPointsNumber))
                ) : all(has(POINT_COUNT),
                        gt(pointCount, literal(minPointsNumber)),
                        lt(pointCount, literal(layers[position - 1][0]))
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
        SymbolLayer unClustered = new SymbolLayer(UNCLUSTERED_POINTS, SOURCE_ID);

        unClustered.setProperties(
                iconImage(MARKER_IMAGE),
                iconColor(
                        rgb(255, 119, 77)
                )
        );
        loadedMapStyle.addLayer(unClustered);
    }

    private void addGeoJsonSource(@NonNull Style loadedMapStyle,
            FeatureCollection featureCollection) {
        source = new GeoJsonSource(SOURCE_ID,
                featureCollection,
                new GeoJsonOptions()
                        .withCluster(true)
                        .withClusterRadius(50)
        );
        loadedMapStyle.addSource(source);
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        if (mapboxMap != null) {
            Projection projection = mapboxMap.getProjection();
            return handlePointClick(projection.toScreenLocation(point));
        } else {
            return false;
        }
    }

    private boolean handlePointClick(PointF screenPoint) {
        Feature feature = getSelectedFeature(screenPoint);
        if (featureSelected(feature)) {
            return true;
        }
        if (currentSelected != null) {
            unSelectDataPoint();
            return true;
        }
        return false;
    }

    private boolean featureSelected(@Nullable Feature feature) {
        if (feature != null && feature.hasNonNullValueForProperty(ID_PROPERTY)) {
            onDataPointSelected(feature);
            return true;
        }
        return false;
    }

    @Nullable
    private Feature getSelectedFeature(PointF screenPoint) {
        List<Feature> features = mapboxMap == null ?
                Collections.emptyList() :
                mapboxMap.queryRenderedFeatures(screenPoint, UNCLUSTERED_POINTS);
        return features.isEmpty() ? null : features.get(0);
    }

    private void onDataPointSelected(Feature feature) {
        String id = feature.getStringProperty(ID_PROPERTY);
        if (currentSelected != null && currentSelected.getStringProperty(ID_PROPERTY)
                .equals(id)) {
            unSelectDataPoint();
        } else {
            if (currentSelected != null) {
                markerViewManager.removeMarker(markerView);
            }
            customView.setTag(id);
            titleTextView.setText(feature.getStringProperty(NAME_PROPERTY));
            snippetTextView.setText(id);

            //TODO: from geometry or point to latLng???
            LatLng latLng = new LatLng(feature.getNumberProperty(LATITUDE_PROPERTY).doubleValue(),
                    feature.getNumberProperty(LONGITUDE_PROPERTY).doubleValue());
            if (markerView == null) {
                markerView = new MarkerView(latLng, customView);
            } else {
                markerView.setLatLng(latLng);
            }
            markerViewManager.addMarker(markerView);
            currentSelected = feature;
        }
    }

    private void unSelectDataPoint() {
        if (markerView != null) {
            markerViewManager.removeMarker(markerView);
        }
        currentSelected = null;
    }

    @Override
    public void displayOfflineMap(@NonNull MapInfo mapInfo) {
        if (mapboxMap != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(mapInfo.getLatitude(), mapInfo.getLongitude()))
                    .zoom(mapInfo.getZoom())
                    .build();
            mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    @Override
    public void displayUserLocation() {
        enableLocationComponent();
    }

    @SuppressWarnings({ "MissingPermission" })
    private void enableLocationComponent() {
        Context context = getContext();
        if (isLocationAllowed() && mapboxMap != null && context != null
                && mapboxMap.getStyle() != null) {
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            //TODO: replace deprecated method
            locationComponent.activateLocationComponent(context, mapboxMap.getStyle());
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.NORMAL);
        }
    }

    private boolean isLocationAllowed() {
        FragmentActivity activity = (FragmentActivity) getContext();
        return activity != null && ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED;
    }

    //TODO: weird, see if better solution
    public void refreshSelectedArea() {
        presenter.loadOfflineSettings();
    }

    public void displayDataPoints(FeatureCollection featureCollection) {
        source.setGeoJson(featureCollection);
        unSelectDataPoint();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.destroy();
    }
}
