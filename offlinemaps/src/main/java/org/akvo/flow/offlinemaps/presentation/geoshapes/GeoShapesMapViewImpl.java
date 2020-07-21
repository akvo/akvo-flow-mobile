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

package org.akvo.flow.offlinemaps.presentation.geoshapes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.FragmentActivity;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Projection;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.mapboxsdk.plugins.annotation.Circle;
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager;
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions;
import com.mapbox.mapboxsdk.plugins.annotation.OnCircleDragListener;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.akvo.flow.offlinemaps.di.DaggerOfflineFeatureComponent;
import org.akvo.flow.offlinemaps.di.OfflineFeatureModule;
import org.akvo.flow.offlinemaps.domain.entity.MapInfo;
import org.akvo.flow.offlinemaps.presentation.MapReadyCallback;

import java.util.List;

import javax.inject.Inject;

import static com.mapbox.mapboxsdk.style.expressions.Expression.all;
import static com.mapbox.mapboxsdk.style.expressions.Expression.any;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.has;
import static com.mapbox.mapboxsdk.style.expressions.Expression.not;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeWidth;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textSize;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.ANIMATION_DURATION_MS;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.CIRCLE_COLOR;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.CIRCLE_LAYER_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.CIRCLE_LINE_COLOR;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.CIRCLE_SOURCE_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.CIRCLE_SOURCE_ID_LABEL;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.FEATURE_LINE;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.FEATURE_POLYGON;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.FILL_COLOR;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.FILL_LAYER_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.FILL_SOURCE_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.LINE_COLOR;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.LINE_LAYER_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.LINE_SOURCE_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.ONE_POINT_ZOOM;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.SELECTED_FEATURE_POINT_LAYER_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.SELECTED_POINT_BORDER_COLOR;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.SELECTED_POINT_COLOR;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.SELECTED_POINT_FILL_COLOR;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.SELECTED_POINT_TEXT_LAYER_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.SELECTED_SHAPE_BORDER_COLOR;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.SELECTED_SHAPE_COLOR;

public class GeoShapesMapViewImpl extends MapView implements OnMapReadyCallback, GeoShapesMapView {

    private MapboxMap mapboxMap;
    private MapReadyCallback mapReadyCallback;
    private CircleManager circleManager;

    @Inject
    GeoShapesMapPresenter presenter;
    private boolean clicksAllowed = true;

    public GeoShapesMapViewImpl(@NonNull Context context) {
        super(context);
        init(context);
    }

    public GeoShapesMapViewImpl(@NonNull Context context,
                                @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GeoShapesMapViewImpl(@NonNull Context context, @Nullable AttributeSet attrs,
                                int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public GeoShapesMapViewImpl(@NonNull Context context,
                                @Nullable MapboxMapOptions options) {
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
        this.mapReadyCallback = callback;
        getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        updateMapStyle(Style.MAPBOX_STREETS, style -> {
            if (mapReadyCallback != null) {
                mapReadyCallback.onMapReady();
                mapReadyCallback = null;
            }
        });
    }

    public void updateMapStyle(String style, Style.OnStyleLoaded callback) {
        if (mapboxMap != null) {
            if (circleManager != null) {
                circleManager.deleteAll();
            }
            mapboxMap.setStyle(style, callback);
        }
    }

    public void initSources(List<Feature> features, List<Feature> pointFeatures) {
        Style style = mapboxMap.getStyle();
        if (style != null) {
            initFillLayer(style);
            initLineLayer(style);
            initCircleLayer(style);

            initCircleSource(style, pointFeatures);
            initCircleTextSource(style, pointFeatures);
            initLineSource(style, features);
            initFillSource(style, features);
        }
    }

    public void initCircleSelectionSources() {
        Style style = mapboxMap.getStyle();
        if (style != null) {
            initShapeSelectedCircleLayer(style);
            initPointSelectedTextLayer(style);
            if (circleManager == null) {
                circleManager = new CircleManager(this, mapboxMap, style);
            }
        }
    }

    public void centerMap(List<LatLng> listOfCoordinates) {
        presenter.loadOfflineSettings(listOfCoordinates);
    }

    @Override
    public void centerOnCoordinates(List<LatLng> listOfCoordinates) {
        if (mapboxMap != null) {
            if (listOfCoordinates.size() == 1) {
                CameraUpdate cameraUpdate = CameraUpdateFactory
                        .newLatLngZoom(listOfCoordinates.get(0), ONE_POINT_ZOOM);
                mapboxMap.animateCamera(cameraUpdate, ANIMATION_DURATION_MS);
            } else if (listOfCoordinates.size() > 1) {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.includes(listOfCoordinates);
                LatLngBounds latLngBounds = builder.build();
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds, 100);
                mapboxMap.animateCamera(cameraUpdate, ANIMATION_DURATION_MS);
            }
        }
    }

    public void setSource(List<Feature> features, String sourceId) {
        if (mapboxMap != null && mapboxMap.getStyle() != null) {
            GeoJsonSource source = (GeoJsonSource) mapboxMap.getStyle().getSource(sourceId);
            if (source != null) {
                FeatureCollection featureCollection = FeatureCollection.fromFeatures(features);
                source.setGeoJson(featureCollection);
            }
        }
    }

    public void setBottomMargin(int height) {
        if (mapboxMap != null) {
            UiSettings uiSettings = mapboxMap.getUiSettings();
            uiSettings.setAttributionMargins(uiSettings.getAttributionMarginLeft(),
                    uiSettings.getAttributionMarginTop(), uiSettings.getAttributionMarginRight(),
                    uiSettings.getAttributionMarginBottom() + height);
            uiSettings.setLogoMargins(uiSettings.getLogoMarginLeft(), uiSettings.getLogoMarginTop(),
                    uiSettings.getLogoMarginRight(), uiSettings.getLogoMarginBottom() + height);
        }
    }

    @SuppressLint("MissingPermission")
    public void displayUserLocation() {
        if (mapboxMap != null && mapboxMap.getStyle() != null && isLocationAllowed()) {
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            LocationComponentOptions componentOptions = LocationComponentOptions
                    .builder(getContext())
                    .foregroundTintColor(Color.parseColor("#904A90E2"))
                    .backgroundTintColor(Color.parseColor("#404A90E2"))
                    .enableStaleState(false)
                    .accuracyAnimationEnabled(false)
                    .accuracyAlpha(0f)
                    .build();
            LocationComponentActivationOptions activationOptions = LocationComponentActivationOptions
                    .builder(getContext(), mapboxMap.getStyle())
                    .locationComponentOptions(componentOptions)
                    .build();
            locationComponent.activateLocationComponent(activationOptions);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.NORMAL);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void centerOnOfflineArea(@NonNull MapInfo mapInfo) {
        if (mapboxMap != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(mapInfo.getLatitude(), mapInfo.getLongitude()))
                    .zoom(mapInfo.getZoom())
                    .build();
            mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    @Nullable
    public Location getLocation() {
        boolean locationUnavailable = mapboxMap == null || !mapboxMap.getLocationComponent()
                .isLocationComponentActivated();
        return locationUnavailable ? null : mapboxMap.getLocationComponent().getLastKnownLocation();
    }

    public void setMapClicks(MapboxMap.OnMapLongClickListener longClickListener,
                             GeoShapesClickListener clickListener) {
        OnCircleDragListener onCircleDragListener = new OnCircleDragListener() {
            @Override
            public void onAnnotationDragStarted(Circle annotation) {
                clicksAllowed = false;
            }

            @Override
            public void onAnnotationDrag(Circle annotation) {
                //EMPTY
            }

            @Override
            public void onAnnotationDragFinished(Circle annotation) {
                circleManager.removeDragListener(this);
                clickListener.onGeoShapeMoved(annotation.getGeometry());
                clicksAllowed = true;
                circleManager.addDragListener(this);
            }
        };
        if (mapboxMap != null) {
            mapboxMap.addOnMapLongClickListener(longClickListener);
            mapboxMap.addOnMapClickListener(point -> {
                if (mapboxMap != null && clicksAllowed) {
                    clicksAllowed = false;
                    removeDragListener(onCircleDragListener);
                    Feature selected = findSelectedFeature(point);
                    if (selected != null) {
                        clickListener.onGeoShapeSelected(selected);
                    }
                    clicksAllowed = true;
                    addDragListener(onCircleDragListener);
                    return true;
                } else {
                    return false;
                }
            });
        }
        addDragListener(onCircleDragListener);
    }

    @org.jetbrains.annotations.Nullable
    private Feature findSelectedFeature(LatLng point) {
        Projection projection = mapboxMap.getProjection();
        List<Feature> features = mapboxMap.queryRenderedFeatures(projection.toScreenLocation(point),
                CIRCLE_LAYER_ID, SELECTED_FEATURE_POINT_LAYER_ID,
                LINE_LAYER_ID, FILL_LAYER_ID);
        return features.isEmpty() ? null : features.get(0);
    }

    private void removeDragListener(OnCircleDragListener onCircleDragListener) {
        if (circleManager != null){
            circleManager.removeDragListener(onCircleDragListener);
        }
    }

    private void addDragListener(OnCircleDragListener onCircleDragListener) {
        if (circleManager != null) {
            circleManager.addDragListener(onCircleDragListener);
        }
    }

    public boolean clicksAllowed() {
        return clicksAllowed;
    }

    public void displaySelectedPoint(LatLng point) {
        circleManager.deleteAll();
        circleManager.create(
                    new CircleOptions()
                            .withLatLng(point)
                            .withCircleRadius(10f)
                            .withCircleStrokeColor(SELECTED_POINT_BORDER_COLOR)
                            .withCircleStrokeWidth(1f)
                            .withDraggable(true)
                            .withCircleColor(SELECTED_POINT_FILL_COLOR));
    }

    public void clearSelected() {
        circleManager.deleteAll();
    }

    private boolean isLocationAllowed() {
        FragmentActivity activity = (FragmentActivity) getContext();
        return activity != null && ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED;
    }

    private void initFillLayer(@NonNull Style style) {
        FillLayer fillLayer = new FillLayer(FILL_LAYER_ID, FILL_SOURCE_ID);
        fillLayer.setProperties(
                fillColor(FILL_COLOR)
        );
        fillLayer.setFilter(has(FEATURE_POLYGON));
        style.addLayer(fillLayer);
    }

    private void initLineLayer(@NonNull Style style) {
        LineLayer lineLayer = new LineLayer(LINE_LAYER_ID, LINE_SOURCE_ID);
        lineLayer.setProperties(
                lineColor(LINE_COLOR),
                lineWidth(4f)
        );
        lineLayer.setFilter(any(has(FEATURE_POLYGON), has(FEATURE_LINE)));
        style.addLayerAbove(lineLayer, FILL_LAYER_ID);
    }

    private void initCircleLayer(@NonNull Style style) {
        CircleLayer circleLayer = new CircleLayer(CIRCLE_LAYER_ID, CIRCLE_SOURCE_ID);
        circleLayer.setProperties(
                circleRadius(6f),
                circleColor(CIRCLE_COLOR),
                circleStrokeWidth(1f),
                circleStrokeColor(CIRCLE_LINE_COLOR)
        );
        circleLayer.setFilter(
                all(not(has(GeoShapeConstants.POINT_SELECTED_PROPERTY)),
                        not(has(GeoShapeConstants.SHAPE_SELECTED_PROPERTY))));
        style.addLayerAbove(circleLayer, LINE_LAYER_ID);
    }

    private void initLineSource(@NonNull Style style, @NonNull List<Feature> features) {
        addJsonSourceToStyle(style, features, LINE_SOURCE_ID);
    }

    private void initFillSource(@NonNull Style style, @NonNull List<Feature> features) {
        addJsonSourceToStyle(style, features, FILL_SOURCE_ID);
    }

    private void initCircleSource(@NonNull Style style, @NonNull List<Feature> features) {
        addJsonSourceToStyle(style, features, CIRCLE_SOURCE_ID);
    }

    private void initCircleTextSource(@NonNull Style style, @NonNull List<Feature> features) {
        addJsonSourceToStyle(style, features, CIRCLE_SOURCE_ID_LABEL);
    }

    private void addJsonSourceToStyle(@NonNull Style style, @NonNull List<Feature> features,
                                      @NonNull String sourceId) {
        FeatureCollection featureCollection = FeatureCollection.fromFeatures(features);
        GeoJsonSource geoJsonSource = new GeoJsonSource(sourceId, featureCollection);
        style.addSource(geoJsonSource);
    }

    /**
     * Selecting a point, also selects it's shape, to show that a shape is selected, all its points
     * will be drawn in orange (except the actual selected point which is green)
     */
    private void initShapeSelectedCircleLayer(@NonNull Style style) {
        CircleLayer circleLayer = new CircleLayer(SELECTED_FEATURE_POINT_LAYER_ID,
                CIRCLE_SOURCE_ID);
        circleLayer.setProperties(
                circleRadius(8f),
                circleColor(SELECTED_SHAPE_COLOR),
                circleStrokeWidth(1f),
                circleStrokeColor(SELECTED_SHAPE_BORDER_COLOR)
        );
        circleLayer.setFilter(all(has(GeoShapeConstants.SHAPE_SELECTED_PROPERTY),
                not(has(GeoShapeConstants.POINT_SELECTED_PROPERTY))));
        style.addLayerAbove(circleLayer, CIRCLE_LAYER_ID);
    }

    /**
     * A selected point location will be drawn in a greenish color
     */
    private void initPointSelectedTextLayer(@NonNull Style style) {
        SymbolLayer symbolLayer = new SymbolLayer(SELECTED_POINT_TEXT_LAYER_ID,
                CIRCLE_SOURCE_ID_LABEL);
        symbolLayer.setProperties(
                textField(Expression.toString(get(GeoShapeConstants.LAT_LNG_PROPERTY))),
                textSize(12f),
                textOffset(new Float[]{0f, -2.0f}),
                textColor(SELECTED_POINT_COLOR),
                textAllowOverlap(true),
                textIgnorePlacement(true)
        );
        symbolLayer.setFilter(all(has(GeoShapeConstants.POINT_SELECTED_PROPERTY),
                not(has(GeoShapeConstants.SHAPE_SELECTED_PROPERTY))));
        style.addLayer(symbolLayer);
    }
}
