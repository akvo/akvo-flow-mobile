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

package org.akvo.flow.presentation.datapoints.map;

import android.Manifest;
import android.app.Activity;
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
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.constants.MapboxConstants;
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

public class DataPointsMapBoxFragment extends SupportMapFragment implements
        MapboxMap.OnMapClickListener, OnMapReadyCallback, DataPointsMapView {

    private static final String MARKER_IMAGE = "custom-marker";
    private static final String MAP_OPTIONS = "MapOptions";
    private static final String SOURCE_ID = "datapoints";
    public static final String UNCLUSTERED_POINTS = "unclustered-points";
    public static final String POINT_COUNT = "point_count";
    private static final String PROPERTY_SELECTED = "selected";

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
    private FeatureCollection featureCollection;
    private GeoJsonSource source;
    private MarkerView markerView;
    private MarkerViewManager markerViewManager;
    private View customView;

    public static DataPointsMapBoxFragment newInstance(SurveyGroup surveyGroup) {
        DataPointsMapBoxFragment fragment = new DataPointsMapBoxFragment();
        Bundle args = new Bundle();
        args.putSerializable(ConstantUtil.SURVEY_GROUP_EXTRA, surveyGroup);
        MapboxMapOptions options = new MapboxMapOptions()
                .camera(new CameraPosition.Builder()
                        .target(new LatLng(41.6082045, 2.654562)) //for debugging
                        .zoom(12)
                        .build());
        args.putParcelable(MapboxConstants.FRAG_ARG_MAPBOXMAPOPTIONS, options);

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = (RecordListListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    activity.toString() + " must implement RecordListListener");
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
                //TODO: refactor this to do this only once
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

    //TODO: too long method refactor
    private void addClusteredGeoJsonSource(@NonNull Style loadedMapStyle) {
        featureCollection = getFeatureCollection();
        addGeoJsonSource(loadedMapStyle, featureCollection);
        addUnClusteredLayer(loadedMapStyle);

        // Use the datapoints GeoJSON source to create three layers: One layer for each cluster category.
        // Each point range gets a different fill color.
        //TODO: extract color resources
        int[][] layers = new int[][] {
                new int[] { 50, Color.parseColor("#009954") },
                new int[] { 20, Color.parseColor("#007B99") },
                new int[] { 0, Color.parseColor("#005899") }
        };

        for (int i = 0; i < layers.length; i++) {
            //Add clusters' circles
            addClusterLayer(loadedMapStyle, layers, i);
        }

        //Add the count labels
        addCountLabels(loadedMapStyle);
    }

    private FeatureCollection getFeatureCollection() {
        //TODO: make datapoint a feature
        List<Feature> features = new ArrayList<>();
        for (MapDataPoint item : mItems) {
            com.google.android.gms.maps.model.LatLng position = item.getPosition();
            if (position != null) {
                Feature feature = Feature.fromGeometry(
                        Point.fromLngLat(position.longitude, position.latitude));
                feature.addStringProperty("id", item.getId());
                feature.addStringProperty("name", item.getName());
                feature.addBooleanProperty(PROPERTY_SELECTED, false);
                features.add(feature);
            }
        }

        return FeatureCollection.fromFeatures(features);
    }

    private void addGeoJsonSource(@NonNull Style loadedMapStyle,
            FeatureCollection featureCollection) {
        source = new GeoJsonSource(SOURCE_ID,
                featureCollection,
                new GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
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
            return handleClickIcon(projection.toScreenLocation(point));
        } else {
            return false;
        }
    }

    /**
     * This method handles click events for SymbolLayer symbols.
     * <p>
     * When a SymbolLayer icon is clicked, we moved that feature to the selected state.
     * </p>
     *
     * @param screenPoint the point on screen clicked
     */
    private boolean handleClickIcon(PointF screenPoint) {
        List<Feature> features = mapboxMap == null ?
                Collections.<Feature>emptyList() :
                mapboxMap.queryRenderedFeatures(screenPoint, UNCLUSTERED_POINTS);
        if (!features.isEmpty() && features.get(0) != null) {
            Feature feature = features.get(0);
            if (feature.hasNonNullValueForProperty("id")) {
                String id = feature.getStringProperty("id");
                List<Feature> featureList = featureCollection.features();
                if (featureList != null) {
                    for (int i = 0; i < featureList.size(); i++) {
                        Feature selectedFeature = featureList.get(i);
                        String selectedItemId = selectedFeature.getStringProperty("id");
                        if (selectedItemId.equals(id)) {
                            MapDataPoint item = getItem(selectedItemId);
                            onFeaturePressed(item);
                        }
                    }
                }
            }
            return true;
        } else {
            return false;
        }
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
        if (markerView == null) {
            createNewMarker(selectedDataPoint);
        } else {
            markerViewManager.removeMarker(markerView);
            markerView = null;
            if (customView == null || !selectedDataPoint.getId().equals(customView.getTag())) {
                createNewMarker(selectedDataPoint);
            }
        }
    }

    private void createNewMarker(MapDataPoint selectedDataPoint) {
        // Use an XML layout to create a View object
        customView = LayoutInflater.from(getActivity()).inflate(
                R.layout.symbol_layer_info_window_layout_callout, null);
        customView.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        customView.setTag(selectedDataPoint.getId());

        // Set the View's TextViews with content
        TextView titleTextView = customView.findViewById(R.id.info_window_title);
        TextView snippetTextView = customView.findViewById(R.id.info_window_description);

        titleTextView.setText(selectedDataPoint.getName());
        snippetTextView.setText(selectedDataPoint.getId());
        customView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onInfoWindowClick((String) customView.getTag());
            }
        });

        // Use the View to create a MarkerView which will eventually be given to
        // the plugin's MarkerViewManager class
        com.google.android.gms.maps.model.LatLng position = selectedDataPoint.getPosition();
        markerView = new MarkerView(new LatLng(position.latitude, position.longitude), customView);
        markerViewManager.addMarker(markerView);
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
        featureCollection = getFeatureCollection();
        source.setGeoJson(featureCollection);
        //TODO: check if the selected datapoint still exists
        if (markerView != null) {
            markerViewManager.removeMarker(markerView);
            markerView = null;
        }
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
            activity.supportInvalidateOptionsMenu();
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
