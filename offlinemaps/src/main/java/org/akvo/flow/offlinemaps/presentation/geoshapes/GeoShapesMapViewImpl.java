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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.util.AttributeSet;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
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
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.akvo.flow.offlinemaps.presentation.MapReadyCallback;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.SELECTED_POINT_LAYER_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.SELECTED_POINT_TEXT_LAYER_ID;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.SELECTED_SHAPE_BORDER_COLOR;
import static org.akvo.flow.offlinemaps.presentation.geoshapes.GeoShapeConstants.SELECTED_SHAPE_COLOR;

public class GeoShapesMapViewImpl extends MapView implements OnMapReadyCallback {

    private MapboxMap mapboxMap;
    private MapReadyCallback mapReadyCallback;

    public GeoShapesMapViewImpl(@NonNull Context context) {
        super(context);
    }

    public GeoShapesMapViewImpl(@NonNull Context context,
            @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GeoShapesMapViewImpl(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public GeoShapesMapViewImpl(@NonNull Context context,
            @Nullable MapboxMapOptions options) {
        super(context, options);
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
            mapboxMap.setStyle(style, callback);
        }
    }

    public void initSources(FeatureCollection features, FeatureCollection pointFeatures) {
        Style style = mapboxMap.getStyle();
        if (style != null) {
            initFillSource(style, features);
            initLineSource(style, features);
            initCircleSource(style, pointFeatures);

            initFillLayer(style);
            initLineLayer(style);
            initCircleLayer(style);
        }
    }

    public void initCircleSelectionSources() {
        Style style = mapboxMap.getStyle();
        if (style != null) {
            initShapeSelectedCircleLayer(style);
            initPointSelectedCircleLayer(style);
            initPointSelectedTextLayer(style);
        }
    }

    public void centerMap(List<LatLng> listOfCoordinates) {
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

    public void setSource(FeatureCollection features, String sourceId) {
        if (mapboxMap != null && mapboxMap.getStyle() != null) {
            GeoJsonSource source = (GeoJsonSource) mapboxMap.getStyle().getSource(sourceId);
            if (source != null) {
                source.setGeoJson(features);
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
        if (mapboxMap != null && mapboxMap.getStyle() != null) {
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
        style.addLayer(lineLayer);
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
        style.addLayer(circleLayer);
    }

    private void initLineSource(@NonNull Style style, FeatureCollection featureCollection) {
        addJsonSourceToStyle(style, featureCollection, LINE_SOURCE_ID);
    }

    private void initFillSource(@NonNull Style style, FeatureCollection featureCollection) {
        addJsonSourceToStyle(style, featureCollection, FILL_SOURCE_ID);
    }

    private void initCircleSource(@NonNull Style style, FeatureCollection featureCollection) {
        addJsonSourceToStyle(style, featureCollection, CIRCLE_SOURCE_ID);
    }

    private void addJsonSourceToStyle(@NonNull Style style, @NonNull FeatureCollection collection,
            @NonNull String sourceId) {
        GeoJsonSource geoJsonSource = new GeoJsonSource(sourceId, collection);
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
                circleRadius(6f),
                circleColor(SELECTED_SHAPE_COLOR),
                circleStrokeWidth(1f),
                circleStrokeColor(SELECTED_SHAPE_BORDER_COLOR)
        );
        circleLayer.setFilter(all(has(GeoShapeConstants.SHAPE_SELECTED_PROPERTY),
                not(has(GeoShapeConstants.POINT_SELECTED_PROPERTY))));
        style.addLayer(circleLayer);
    }

    /**
     * A selected point will be drawn in a greenish color
     */
    private void initPointSelectedCircleLayer(@NonNull Style style) {
        CircleLayer circleLayer = new CircleLayer(SELECTED_POINT_LAYER_ID, CIRCLE_SOURCE_ID);
        circleLayer.setProperties(
                circleRadius(8f),
                circleColor(SELECTED_POINT_COLOR),
                circleStrokeWidth(1f),
                circleStrokeColor(SELECTED_POINT_BORDER_COLOR)
        );
        circleLayer.setFilter(all(has(GeoShapeConstants.POINT_SELECTED_PROPERTY),
                not(has(GeoShapeConstants.SHAPE_SELECTED_PROPERTY))));
        style.addLayer(circleLayer);
    }

    /**
     * A selected point location will be drawn in a greenish color
     */
    private void initPointSelectedTextLayer(@NonNull Style style) {
        SymbolLayer symbolLayer = new SymbolLayer(SELECTED_POINT_TEXT_LAYER_ID, CIRCLE_SOURCE_ID);
        symbolLayer.setProperties(
                textField(Expression.toString(get(GeoShapeConstants.LAT_LNG_PROPERTY))),
                textSize(12f),
                textOffset(new Float[] { 0f, -2.0f }),
                textColor(SELECTED_POINT_COLOR),
                textAllowOverlap(true),
                textIgnorePlacement(true)
        );
        symbolLayer.setFilter(all(has(GeoShapeConstants.POINT_SELECTED_PROPERTY),
                not(has(GeoShapeConstants.SHAPE_SELECTED_PROPERTY))));
        style.addLayer(symbolLayer);
    }

    @Nullable
    public Location getLocation() {
        boolean locationUnavailable = mapboxMap == null || !mapboxMap.getLocationComponent()
                .isLocationComponentActivated();
        return locationUnavailable ? null : mapboxMap.getLocationComponent().getLastKnownLocation();
    }

    public void setMapClicks(MapboxMap.OnMapLongClickListener longClickListener,
            GeoShapesClickListener clickListener) {
        if (mapboxMap != null) {
            mapboxMap.addOnMapLongClickListener(longClickListener);
            mapboxMap.addOnMapClickListener(point -> {
                if (mapboxMap != null) {
                    Projection projection = mapboxMap.getProjection();
                    List<Feature> features = mapboxMap
                            .queryRenderedFeatures(projection.toScreenLocation(point), CIRCLE_LAYER_ID);
                    Feature selected = features.isEmpty() ? null : features.get(0);
                    return selected != null && clickListener.onGeoShapeSelected(selected);
                } else {
                    return false;
                }
            });
        }
    }
}
