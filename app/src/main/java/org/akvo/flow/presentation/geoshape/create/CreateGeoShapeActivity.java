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

package org.akvo.flow.presentation.geoshape.create;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomappbar.BottomAppBar;
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
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.expressions.Expression.all;
import static com.mapbox.mapboxsdk.style.expressions.Expression.any;
import static com.mapbox.mapboxsdk.style.expressions.Expression.has;
import static com.mapbox.mapboxsdk.style.expressions.Expression.not;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeWidth;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.ACCURACY_THRESHOLD;
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
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.SELECTED_FEATURE_POINT_LAYER_ID;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.SELECTED_POINT_COLOR;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.SELECTED_POINT_LAYER_ID;
import static org.akvo.flow.presentation.geoshape.GeoShapeConstants.SELECTED_SHAPE_COLOR;

public class CreateGeoShapeActivity extends BackActivity {

    private MapView mapView;
    private MapboxMap mapboxMap;
    private boolean changed = false;
    private boolean allowPoints;
    private boolean allowLine;
    private boolean allowPolygon;

    private DrawMode drawMode = DrawMode.NONE;

    private Style.OnStyleLoaded callback = style -> {
        initSources(style);
        centerMap();
        setMapClicks();
        displayUserLocation();
    };

    private boolean manualInputEnabled; //TODO:
    private TextView bottomBarTitle;
    private BottomAppBar bottomAppBar;

    private ViewFeatures viewFeatures = new ViewFeatures(new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>());

    @Inject
    FeatureMapper featureMapper;

    @Inject
    CoordinatesMapper coordinatesMapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_geo_shape);
        initializeInjector();
        setupToolBar();
        setUpBottomBar();
        setUpFeatures();
        setUpMapView(savedInstanceState);

        allowPoints = getIntent().getBooleanExtra(ConstantUtil.EXTRA_ALLOW_POINTS, true);
        allowLine = getIntent().getBooleanExtra(ConstantUtil.EXTRA_ALLOW_LINE, true);
        allowPolygon = getIntent().getBooleanExtra(ConstantUtil.EXTRA_ALLOW_POLYGON, true);
        manualInputEnabled = getIntent().getBooleanExtra(ConstantUtil.EXTRA_MANUAL_INPUT, true);
    }

    private void initializeInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder()
                        .applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    private void setUpBottomBar() {
        bottomAppBar = findViewById(R.id.bottomBar);
        bottomBarTitle = findViewById(R.id.bottomBarTitle);

        bottomAppBar.replaceMenu(R.menu.create_geoshape_activity_bottom);
        bottomAppBar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.add_point:
                    addLocationPoint(); //TODO: check selected shape
                    break;
                default:
                    break;
            }
            return true;
        });
    }

    private void addLocationPoint() {
        Location location =
                mapboxMap == null || !isLocationAllowed() || !mapboxMap.getLocationComponent()
                        .isLocationComponentActivated() ?
                        null :
                        mapboxMap.getLocationComponent().getLastKnownLocation();
        if (location != null && location.getAccuracy() <= ACCURACY_THRESHOLD) {
            addPoint(new LatLng(location.getLatitude(), location.getLongitude()));
            updateChanged();
        } else {
            //TODO: use snackbar
            Toast.makeText(CreateGeoShapeActivity.this,
                    location != null ?
                            R.string.location_inaccurate :
                            R.string.location_unknown,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setUpMapView(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> {
            this.mapboxMap = mapboxMap;
            updateMapStyle(Style.OUTDOORS);
            updateAttributionMargin();
        });
    }

    private void updateAttributionMargin() {
        View view = findViewById(R.id.toolbar);
        int height = view.getHeight();
        if (height > 0) {
            setBottomMargin(height);
        } else {
            view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                        int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    int toolBarHeight = bottom - top;
                    if (toolBarHeight > 0) {
                        view.removeOnLayoutChangeListener(this);
                        setBottomMargin(toolBarHeight);
                    }
                }
            });
        }
    }

    private void setBottomMargin(int height) {
        UiSettings uiSettings = mapboxMap.getUiSettings();
        uiSettings.setAttributionMargins(uiSettings.getAttributionMarginLeft(),
                uiSettings.getAttributionMarginTop(), uiSettings.getAttributionMarginRight(),
                uiSettings.getAttributionMarginBottom() + height);
        uiSettings.setLogoMargins(uiSettings.getLogoMarginLeft(), uiSettings.getLogoMarginTop(),
                uiSettings.getLogoMarginRight(), uiSettings.getLogoMarginBottom() + height);
    }

    private void setUpFeatures() {
        String geoJSON = getIntent().getStringExtra(ConstantUtil.GEOSHAPE_RESULT);
        Timber.d(geoJSON);
        viewFeatures = featureMapper.toViewFeatures(geoJSON);
    }

    private void initSources(Style style) {
        FeatureCollection features = FeatureCollection.fromFeatures(viewFeatures.getFeatures());
        FeatureCollection pointList = FeatureCollection
                .fromFeatures(viewFeatures.getPointFeatures());
        initFillSource(style, features);
        initLineSource(style, features);
        initCircleSource(style, pointList);

        initFillLayer(style);
        initLineLayer(style);
        initCircleLayer(style);
        initShapeSelectedCircleLayer(style);
        initPointSelectedCircleLayer(style);
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

    @SuppressLint("MissingPermission")
    public void displayUserLocation() {
        if (isLocationAllowed() && mapboxMap != null && mapboxMap.getStyle() != null) {
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, mapboxMap.getStyle())
                            .build());
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.NORMAL);
        }
    }

    private void setMapClicks() {
        if (mapboxMap != null) {
            mapboxMap.addOnMapLongClickListener(point -> {
                if (!manualInputEnabled) {
                    //TODO: should notify user?
                    return false;
                }
                if (drawMode == DrawMode.POINT || drawMode == DrawMode.AREA
                        || drawMode == DrawMode.LINE) {
                    addPoint(point);
                    updateChanged();
                }
                //TODO: should notify user?
                return true;
            });
        }
    }

    private void updateChanged() {
        if (!changed) {
            changed = true;
            invalidateOptionsMenu();
        }
    }

    private void addPoint(LatLng latLng) {
        Point mapTargetPoint = Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude());
        String featureId = updateFeature(mapTargetPoint);
        updatePointsList(mapTargetPoint, featureId);
        updateSources(mapboxMap.getStyle());
    }

    private String updateFeature(Point mapTargetPoint) {
        Feature selectedFeature = viewFeatures.getSelectedFeature();
        List<Point> points;
        String featureId;
        if (isValidFeature(selectedFeature)) {
            points = getCoordinates(selectedFeature);
            points.add(mapTargetPoint);
            featureId = selectedFeature.getStringProperty(ViewFeatures.FEATURE_ID);
        } else {
            points = new ArrayList<>();
            points.add(mapTargetPoint);
            selectedFeature = createFeatureFromGeometry(points);
            selectedFeature.addBooleanProperty(ViewFeatures.POINT_SELECTED_PROPERTY, true);
            featureId = UUID.randomUUID().toString();
            selectedFeature.addStringProperty(ViewFeatures.FEATURE_ID, featureId);
            viewFeatures.setSelectedFeature(selectedFeature);
            viewFeatures.getFeatures().add(selectedFeature);
        }
        return featureId;
    }

    @NonNull
    private Feature createFeatureFromGeometry(List<Point> points) {
        Feature feature;
        switch (drawMode) {
            case LINE:
                feature = Feature.fromGeometry(LineString.fromLngLats(points));
                feature.addBooleanProperty(ViewFeatures.FEATURE_LINE, true);
                break;
            case AREA:
                List<List<Point>> es = new ArrayList<>();
                es.add(points);
                feature = Feature.fromGeometry(Polygon.fromLngLats(es));
                feature.addBooleanProperty(ViewFeatures.FEATURE_POLYGON, true);
                break;
            case POINT:
            default:
                feature = Feature.fromGeometry(MultiPoint.fromLngLats(points));
                feature.addBooleanProperty(ViewFeatures.FEATURE_POINT, true);
                break;
        }
        return feature;
    }

    @NonNull
    private List<Point> getCoordinates(Feature selectedFeature) {
        Geometry geometry = selectedFeature.geometry();
        if (geometry == null) {
            return Collections.emptyList();
        }
        switch (drawMode) {
            case POINT:
                return ((MultiPoint) geometry).coordinates();
            case LINE:
                return ((LineString) geometry).coordinates();
            case AREA:
                return ((Polygon) geometry).coordinates().get(0);
            default:
                return Collections.emptyList();
        }
    }

    private boolean isValidFeature(Feature selectedFeature) {
        if (selectedFeature == null) {
            return false;
        } else {
            switch (drawMode) {
                case POINT:
                    return selectedFeature.geometry() instanceof MultiPoint;
                case LINE:
                    return selectedFeature.geometry() instanceof LineString;
                case AREA:
                    return selectedFeature.geometry() instanceof Polygon;
                default:
                    return false;
            }
        }
    }

    private void updatePointsList(Point mapTargetPoint, String featureId) {
        Feature feature = Feature.fromGeometry(mapTargetPoint);
        feature.addBooleanProperty(ViewFeatures.POINT_SELECTED_PROPERTY, true);
        feature.addStringProperty(ViewFeatures.FEATURE_ID, featureId);
        feature.addStringProperty(ViewFeatures.POINT_ID, UUID.randomUUID().toString());
        List<Feature> pointFeatureList = viewFeatures.getPointFeatures();
        for (Feature f : pointFeatureList) {
            if (f.getStringProperty(ViewFeatures.FEATURE_ID).equals(featureId)) {
                f.addBooleanProperty(ViewFeatures.SHAPE_SELECTED_PROPERTY, true);
            } else {
                f.removeProperty(ViewFeatures.SHAPE_SELECTED_PROPERTY);
            }
            f.removeProperty(ViewFeatures.POINT_SELECTED_PROPERTY);
        }
        pointFeatureList.add(feature);
    }

    private void updateSources(Style style) {
        FeatureCollection features = FeatureCollection.fromFeatures(viewFeatures.getFeatures());
        FeatureCollection pointList = FeatureCollection
                .fromFeatures(viewFeatures.getPointFeatures());
        setSource(style, features, FILL_SOURCE_ID);
        setSource(style, features, LINE_SOURCE_ID);
        setSource(style, pointList, CIRCLE_SOURCE_ID);
    }

    private void setSource(Style style, FeatureCollection features, String sourceId) {
        GeoJsonSource source = (GeoJsonSource) style.getSource(sourceId);
        if (source != null) {
            source.setGeoJson(features);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_geoshape_activity, menu);
        if (!allowPoints) {
            hideMenuItem(menu, R.id.add_points);
        }
        if (!allowLine) {
            hideMenuItem(menu, R.id.add_line);
        }
        if (!allowPolygon) {
            hideMenuItem(menu, R.id.add_polygon);
        }
        MenuItem item = menu.findItem(R.id.save);
        if (item != null) {
            item.setVisible(isValidShape());
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void hideMenuItem(Menu menu, int itemId) {
        menu.findItem(itemId).setVisible(false);
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
            case R.id.add_points:
                bottomAppBar.setVisibility(View.VISIBLE);
                bottomBarTitle.setText(R.string.geoshape_points);
                drawMode = DrawMode.POINT;
                break;
            case R.id.add_line:
                bottomAppBar.setVisibility(View.VISIBLE);
                bottomBarTitle.setText(R.string.geoshape_line);
                drawMode = DrawMode.LINE;
                break;
            case R.id.add_polygon:
                bottomAppBar.setVisibility(View.VISIBLE);
                bottomBarTitle.setText(R.string.geoshape_area);
                drawMode = DrawMode.AREA;
                break;
            case R.id.save:
                setShapeResult();
                finish();
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

    private void setShapeResult() {
        Intent intent = new Intent();
        if (isValidShape() && changed) {
            FeatureCollection features = FeatureCollection.fromFeatures(viewFeatures.getFeatures());
            //TODO: shall we remove the id property?
            intent.putExtra(ConstantUtil.GEOSHAPE_RESULT, features.toJson());
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED, intent);
        }
    }

    //TODO: validate shapes
    private boolean isValidShape() {
        List<Feature> features = viewFeatures.getFeatures();
        return features.size() > 0 && changed;
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
        LineLayer lineLayer = new LineLayer(LINE_LAYER_ID, LINE_SOURCE_ID);
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
        circleLayer.setFilter(
                all(not(has(ViewFeatures.POINT_SELECTED_PROPERTY)),
                        not(has(ViewFeatures.SHAPE_SELECTED_PROPERTY))));
        style.addLayer(circleLayer);
    }

    /**
     * Selecting a point, also selects it's shape, to show that a shape is selected, all its points
     * will be drawn in orange (except the actual selected point which is green)
     */
    private void initShapeSelectedCircleLayer(@NonNull Style style) {
        CircleLayer circleLayer = new CircleLayer(SELECTED_FEATURE_POINT_LAYER_ID, CIRCLE_SOURCE_ID);
        circleLayer.setProperties(
                circleRadius(6f),
                circleColor(SELECTED_SHAPE_COLOR)
        );
        circleLayer.setFilter(all(has(ViewFeatures.SHAPE_SELECTED_PROPERTY),
                not(has(ViewFeatures.POINT_SELECTED_PROPERTY))));
        style.addLayer(circleLayer);
    }

    /**
     * A selected point will be drawn in a greenish color
     */
    private void initPointSelectedCircleLayer(@NonNull Style style) {
        CircleLayer circleLayer = new CircleLayer(SELECTED_POINT_LAYER_ID, CIRCLE_SOURCE_ID);
        circleLayer.setProperties(
                circleRadius(8f),
                circleColor(SELECTED_POINT_COLOR)
        );
        circleLayer.setFilter(all(has(ViewFeatures.POINT_SELECTED_PROPERTY),
                not(has(ViewFeatures.SHAPE_SELECTED_PROPERTY))));
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
        GeoJsonSource fillGeoJsonSource = new GeoJsonSource(sourceId, collection);
        style.addSource(fillGeoJsonSource);
    }
}
