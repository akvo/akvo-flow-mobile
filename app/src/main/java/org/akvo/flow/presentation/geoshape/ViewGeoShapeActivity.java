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

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.MultiPoint;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
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
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeWidth;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class ViewGeoShapeActivity extends BackActivity {

    private static final String CIRCLE_SOURCE_ID = "circle-source-id";
    private static final String FILL_SOURCE_ID = "fill-source-id";
    private static final String LINE_SOURCE_ID = "line-source-id";
    private static final String CIRCLE_LAYER_ID = "circle-layer-id";
    private static final String FILL_LAYER_ID = "fill-layer-polygon-id";
    private static final String LINE_LAYER_ID = "line-layer-id";
    private static final int FILL_COLOR = 0x88736357;
    private static final int LINE_COLOR = 0xEE736357;
    private static final int POINT_COLOR = 0xFF736357;
    private static final int POINT_LINE_COLOR = 0xFF5B5048;
    public static final int ANIMATION_DURATION_MS = 400;
    public static final int ONE_POINT_ZOOM = 12;

    private MapView mapView;
    private MapboxMap mapboxMap;

    private Style.OnStyleLoaded callback = style -> {
        updateSources(style);
        centerMap();
    };

    private FeatureCollection features;
    private final List<LatLng> listOfCoordinates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_geo_shape);
        setupToolBar();
        setUpFeatures();
        setUpMapView(savedInstanceState);
    }

    private void setUpMapView(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> {
            this.mapboxMap = mapboxMap;
            mapboxMap.setStyle(Style.OUTDOORS, callback);
        });
    }

    private void setUpFeatures() {
        String geoJSON = getIntent().getStringExtra(ConstantUtil.GEOSHAPE_RESULT);
        Timber.d(geoJSON);
        features = geoJSON == null ?
                FeatureCollection.fromFeatures(new Feature[0]) :
                FeatureCollection.fromJson(geoJSON);

        List<Feature> features = this.features.features();
        if (features != null && !features.isEmpty()) {

            for (Feature feature : features) {
                Geometry geometry = feature.geometry();
                if (geometry instanceof Polygon) {
                    List<List<Point>> coordinates = ((Polygon) geometry).coordinates();
                    for (List<Point> points : coordinates) {
                        for (Point p : points) {
                            listOfCoordinates.add(new LatLng(p.latitude(), p.longitude()));
                        }
                    }
                } else if (geometry instanceof LineString) {
                    List<Point> coordinates = ((LineString) geometry).coordinates();
                    listOfCoordinates.addAll(getListOfCoordinates(coordinates));
                } else if (geometry instanceof MultiPoint) {
                    List<Point> coordinates = ((MultiPoint) geometry).coordinates();
                    listOfCoordinates.addAll(getListOfCoordinates(coordinates));
                }
            }
        }
    }

    private void updateSources(Style style) {
        GeoJsonSource circleSource = initCircleSource(style, features);
        GeoJsonSource fillSource = initFillSource(style, features);
        GeoJsonSource lineSource = initLineSource(style, features);

        initFillLayer(style);
        initLineLayer(style);
        initCircleLayer(style);

        fillSource.setGeoJson(features);
        lineSource.setGeoJson(features);
        circleSource.setGeoJson(features);
    }

    private void centerMap() {
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

    private List<LatLng> getListOfCoordinates(List<Point> coordinates) {
        List<LatLng> latLngs = new ArrayList<>();
        for (Point p : coordinates) {
            LatLng latLng = new LatLng(p.latitude(), p.longitude());
            latLngs.add(latLng);
        }
        return latLngs;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_geoshape_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mapboxMap != null) {
            switch (item.getItemId()) {
                case R.id.map_normal:
                    mapboxMap.setStyle(Style.LIGHT, callback);
                    break;
                case R.id.map_satellite:
                    mapboxMap.setStyle(Style.SATELLITE_STREETS, callback);
                    break;
                case R.id.map_terrain:
                    mapboxMap.setStyle(Style.OUTDOORS, callback);
                    break;
                case android.R.id.home:
                    onBackPressed();
                    break;
                default:
                    break;
            }
        }
        return super.onOptionsItemSelected(item);
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

    /**
     * Set up the FillLayer for showing the set boundaries' polygons
     */
    private void initFillLayer(@NonNull Style loadedMapStyle) {
        FillLayer fillLayer = new FillLayer(FILL_LAYER_ID, FILL_SOURCE_ID);
        fillLayer.setProperties(
                fillColor(FILL_COLOR)
        );
        loadedMapStyle.addLayer(fillLayer);
    }

    /**
     * Set up the LineLayer for showing the set boundaries' polygons
     */
    private void initLineLayer(@NonNull Style loadedMapStyle) {
        LineLayer lineLayer = new LineLayer(LINE_LAYER_ID, LINE_SOURCE_ID);
        lineLayer.setProperties(
                lineColor(LINE_COLOR),
                lineWidth(4f)
        );
        loadedMapStyle.addLayerAbove(lineLayer, FILL_LAYER_ID);
    }

    /**
     * Set up the CircleLayer for showing polygon click points
     */
    private void initCircleLayer(@NonNull Style loadedMapStyle) {
        CircleLayer circleLayer = new CircleLayer(CIRCLE_LAYER_ID, CIRCLE_SOURCE_ID);
        circleLayer.setProperties(
                circleRadius(6f),
                circleColor(POINT_COLOR),
                circleStrokeWidth(1f),
                circleStrokeColor(POINT_LINE_COLOR)
        );
        loadedMapStyle.addLayerAbove(circleLayer, LINE_LAYER_ID);
    }

    /**
     * Set up the LineLayer source for showing map click points
     */
    private GeoJsonSource initLineSource(@NonNull Style loadedMapStyle,
            FeatureCollection featureCollection) {
        GeoJsonSource lineGeoJsonSource = new GeoJsonSource(LINE_SOURCE_ID, featureCollection);
        loadedMapStyle.addSource(lineGeoJsonSource);
        return lineGeoJsonSource;
    }

    /**
     * Set up the FillLayer source for showing map click points
     */
    private GeoJsonSource initFillSource(@NonNull Style loadedMapStyle,
            FeatureCollection featureCollection) {
        GeoJsonSource fillGeoJsonSource = new GeoJsonSource(FILL_SOURCE_ID, featureCollection);
        loadedMapStyle.addSource(fillGeoJsonSource);
        return fillGeoJsonSource;
    }

    /**
     * Set up the CircleLayer source for showing map click points
     */
    private GeoJsonSource initCircleSource(@NonNull Style loadedMapStyle,
            FeatureCollection featureCollection) {
        GeoJsonSource circleGeoJsonSource = new GeoJsonSource(CIRCLE_SOURCE_ID, featureCollection);
        loadedMapStyle.addSource(circleGeoJsonSource);
        return circleGeoJsonSource;
    }
}
