/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.datapoints.map;

import android.Manifest;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Projection;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.SupportMapFragment;
import com.mapbox.mapboxsdk.plugins.markerview.MarkerView;
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.datapoints.DataPointSyncSnackBarManager;
import org.akvo.flow.presentation.datapoints.map.entity.MapDataPoint;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.fragment.RecordListListener;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

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

public class DataPointsMapFragment extends SupportMapFragment implements
        MapboxMap.OnMapClickListener, OnMapReadyCallback, DataPointsMapView {

    private static final String MARKER_IMAGE = "custom-marker";
    private static final String SOURCE_ID = "datapoints";
    private static final String UNCLUSTERED_POINTS = "unclustered-points";
    private static final String POINT_COUNT = "point_count";
    private static final String ID_PROPERTY = "id";

    @Inject
    DataPointSyncSnackBarManager dataPointSyncSnackBarManager;

    @Inject
    DataPointsMapPresenter presenter;

    @Inject
    Navigator navigator;

    @Nullable
    private RecordListListener mListener;

    private List<MapDataPoint> mItems;

    @Nullable
    private ProgressBar progressBar;

    @Nullable
    private MapboxMap mapboxMap;

    private boolean activityJustCreated;
    private Integer menuRes = null;
    private GeoJsonSource source;
    private MarkerView markerView;
    private MarkerViewManager markerViewManager;
    private View customView;
    private TextView titleTextView;
    private TextView snippetTextView;
    private MapDataPoint currentSelected = null;

    public static DataPointsMapFragment newInstance(SurveyGroup surveyGroup) {
        DataPointsMapFragment fragment = new DataPointsMapFragment();
        Bundle args = new Bundle();
        args.putSerializable(ConstantUtil.SURVEY_GROUP_EXTRA, surveyGroup);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItems = new ArrayList<>();
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = (RecordListListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    context.toString() + " must implement RecordListListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            View.inflate(getActivity(), R.layout.map_progress_bar, viewGroup);
            progressBar = view.findViewById(R.id.progressBar);
        }
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initializeInjector();
        presenter.setView(this);
        SurveyGroup surveyGroup = (SurveyGroup) getArguments()
                .getSerializable(ConstantUtil.SURVEY_GROUP_EXTRA);
        presenter.onDataReady(surveyGroup);
        customView = LayoutInflater.from(getActivity()).inflate(
                R.layout.symbol_layer_info_window_layout_callout, null);
        customView.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        titleTextView = customView.findViewById(R.id.info_window_title);
        snippetTextView = customView.findViewById(R.id.info_window_description);
        customView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onInfoWindowClick((String) customView.getTag());
            }
        });
        getMapAsync(this);
        activityJustCreated = true;
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent())
                .build();
        viewComponent.inject(this);
    }

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return {@link ApplicationComponent}
     */
    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getActivity().getApplication()).getApplicationComponent();
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        this.mapboxMap.addOnMapClickListener(this);
        markerViewManager = new MarkerViewManager((MapView) getView(), mapboxMap);
        this.mapboxMap.setStyle(new Style.Builder()
                .fromUrl("mapbox://styles/mapbox/light-v10"), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                style.addImage(MARKER_IMAGE, BitmapFactory.decodeResource(
                        getResources(), R.drawable.marker), true);
                enableLocationComponent(style);
                addClusteredGeoJsonSource(style);
                presenter.onViewReady();
            }
        });
    }

    @SuppressWarnings({ "MissingPermission" })
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        Context context = getContext();
        if (isLocationAllowed() && mapboxMap != null && context != null) {
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(context, loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.NORMAL);
        }
    }

    private boolean isLocationAllowed() {
        FragmentActivity activity = getActivity();
        return activity != null && ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED;
    }

    private void addClusteredGeoJsonSource(@NonNull Style loadedMapStyle) {
        addGeoJsonSource(loadedMapStyle, getFeatureCollection());
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

    private FeatureCollection getFeatureCollection() {
        List<Feature> features = new ArrayList<>();
        for (MapDataPoint item : mItems) {
            Feature feature = Feature.fromGeometry(
                    Point.fromLngLat(item.getLongitude(), item.getLatitude()));
            feature.addStringProperty(ID_PROPERTY, item.getId());
            features.add(feature);
        }
        return FeatureCollection.fromFeatures(features);
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
        List<Feature> features = mapboxMap == null ?
                Collections.<Feature>emptyList() :
                mapboxMap.queryRenderedFeatures(screenPoint, UNCLUSTERED_POINTS);
        Feature feature = features.isEmpty() ? null : features.get(0);
        if (feature != null && feature.hasNonNullValueForProperty(ID_PROPERTY)) {
            String id = feature.getStringProperty(ID_PROPERTY);
            MapDataPoint item = getItem(id);
            if (item != null) {
                onFeaturePressed(item);
                return true;
            }
        }
        if (currentSelected != null) {
            currentSelected = null;
            markerViewManager.removeMarker(markerView);
            return true;
        }
        return false;
    }

    @Nullable
    private MapDataPoint getItem(String selectedItemId) {
        if (mItems == null || selectedItemId == null) {
            return null;
        }
        for (MapDataPoint dp: mItems) {
            if (selectedItemId.equals(dp.getId())) {
                return dp;
            }
        }
        return null;
    }

    private void onFeaturePressed(MapDataPoint selectedDataPoint) {
        if (currentSelected != null && currentSelected.getId().equals(selectedDataPoint.getId())) {
            unSelectDataPoint();
        } else {
            if (currentSelected != null) {
                markerViewManager.removeMarker(markerView);
            }
            customView.setTag(selectedDataPoint.getId());
            titleTextView.setText(selectedDataPoint.getName());
            snippetTextView.setText(selectedDataPoint.getId());

            LatLng latLng = new LatLng(selectedDataPoint.getLatitude(),
                    selectedDataPoint.getLongitude());
            if (markerView == null) {
                markerView = new MarkerView(latLng, customView);
            } else {
                markerView.setLatLng(latLng);
            }
            markerViewManager.addMarker(markerView);
            currentSelected = selectedDataPoint;
        }
    }

    private void unSelectDataPoint() {
        if (markerView != null) {
            markerViewManager.removeMarker(markerView);
            currentSelected = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!activityJustCreated) {
            presenter.loadDataPoints();
        }
        activityJustCreated = false;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        presenter.destroy();
        super.onDestroy();
    }

    public void onNewSurveySelected(SurveyGroup surveyGroup) {
        getArguments().putSerializable(ConstantUtil.SURVEY_GROUP_EXTRA, surveyGroup);
        presenter.onNewSurveySelected(surveyGroup);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (menuRes != null) {
            inflater.inflate(menuRes, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.download:
                presenter.onSyncRecordsPressed();
                return true;
            case R.id.upload:
                presenter.onUploadPressed();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void showProgress() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideProgress() {
        if (progressBar != null) {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    public void onInfoWindowClick(String surveyedLocaleId) {
        if (mListener != null) {
            mListener.onRecordSelected(surveyedLocaleId);
        }
    }

    @Override
    public void displayData(List<MapDataPoint> surveyedLocales) {
        mItems.clear();
        mItems.addAll(surveyedLocales);
        source.setGeoJson(getFeatureCollection());
        unSelectDataPoint();
    }

    @Override
    public void showNonMonitoredMenu() {
        menuRes = R.menu.datapoints_map;
        reloadMenu();
    }

    @Override
    public void showMonitoredMenu() {
        menuRes = R.menu.datapoints_map_monitored;
        reloadMenu();
    }

    @Override
    public void hideMenu() {
        menuRes = null;
        reloadMenu();
    }

    private void reloadMenu() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    @Override
    public void showSyncedResults(int numberOfSyncedItems) {
        dataPointSyncSnackBarManager.showSyncedResults(numberOfSyncedItems, getView());
    }

    @Override
    public void showErrorAssignmentMissing() {
        dataPointSyncSnackBarManager.showErrorAssignmentMissing(getView());
    }

    @Override
    public void showErrorNoNetwork() {
        dataPointSyncSnackBarManager.showErrorNoNetwork(getView(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onSyncRecordsPressed();
            }
        });
    }

    @Override
    public void showErrorSync() {
        dataPointSyncSnackBarManager.showErrorSync(getView(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onSyncRecordsPressed();
            }
        });
    }

    @Override
    public void showNoDataPointsToSync() {
        dataPointSyncSnackBarManager.showNoDataPointsToSync(getView());
    }
}
