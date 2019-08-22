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

package org.akvo.flow.presentation.geoshape;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.geoshape.create.FeatureMapper;
import org.akvo.flow.presentation.geoshape.create.ViewFeatures;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.expressions.Expression.any;
import static com.mapbox.mapboxsdk.style.expressions.Expression.has;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeWidth;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.ANIMATION_DURATION_MS;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.CIRCLE_COLOR;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.CIRCLE_LAYER_ID;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.CIRCLE_SOURCE_ID;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.FILL_COLOR;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.FILL_LAYER_ID;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.FILL_SOURCE_ID;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.LINE_COLOR;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.LINE_LAYER_ID;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.LINE_SOURCE_ID;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.ONE_POINT_ZOOM;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.POINT_LINE_COLOR;

public class ViewGeoShapeActivity extends BackActivity {
    private MapView mapView;
    private MapboxMap mapboxMap;

    private Style.OnStyleLoaded callback = style -> {
        initSources(style);
        centerMap();
    };

    private ViewFeatures viewFeatures = new ViewFeatures(new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>());

    @Inject
    FeatureMapper featureMapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_geo_shape);
        initializeInjector();
        setupToolBar();
        setUpFeatures();
        setUpMapView(savedInstanceState);
    }

    private void initializeInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder()
                        .applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    private void setUpMapView(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> {
            this.mapboxMap = mapboxMap;
            updateMapStyle(Style.OUTDOORS);
        });
    }

    private void setUpFeatures() {
        String geoJSON = getIntent().getStringExtra(ConstantUtil.GEOSHAPE_RESULT);
        Timber.d(geoJSON);
        viewFeatures = featureMapper.toViewFeatures(geoJSON);
    }

    private void initSources(Style style) {
        FeatureCollection features = FeatureCollection.fromFeatures(viewFeatures.getFeatures());
        initFillSource(style, features);
        initCircleSource(style, features);
        initLineSource(style, features);

        initFillLayer(style);
        initCircleLayer(style);
        initLineLayer(style);
    }

    private void centerMap() {
        if (mapboxMap != null) {
            List<LatLng> listOfCoordinates = viewFeatures.getListOfCoordinates();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_geoshape_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.map_normal:
                updateMapStyle(Style.LIGHT);
                break;
            case R.id.map_satellite:
                updateMapStyle(Style.SATELLITE_STREETS);
                break;
            case R.id.map_terrain:
                updateMapStyle(Style.OUTDOORS);
                break;
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateMapStyle(String light) {
        if (mapboxMap != null) {
            mapboxMap.setStyle(light, callback);
        }
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

    private void initFillLayer(@NonNull Style style) {
        FillLayer fillLayer = new FillLayer(FILL_LAYER_ID, FILL_SOURCE_ID);
        fillLayer.setProperties(
                fillColor(FILL_COLOR)
        );
        fillLayer.setFilter(has(ViewFeatures.FEATURE_POLYGON));
        style.addLayer(fillLayer);
    }

    private void initLineLayer(@NonNull Style style) {
        LineLayer lineLayer = new LineLayer(LINE_LAYER_ID,  LINE_SOURCE_ID);
        lineLayer.setProperties(
                lineColor(LINE_COLOR),
                lineWidth(4f)
        );
        lineLayer.setFilter(any(has(ViewFeatures.FEATURE_POLYGON), has(ViewFeatures.FEATURE_LINE)));
        style.addLayer(lineLayer);
    }

    private void initCircleLayer(@NonNull Style style) {
        CircleLayer circleLayer = new CircleLayer(CIRCLE_LAYER_ID, CIRCLE_SOURCE_ID);
        circleLayer.setProperties(
                circleRadius(6f),
                circleColor(CIRCLE_COLOR),
                circleStrokeWidth(1f),
                circleStrokeColor(POINT_LINE_COLOR)
        );
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
}
