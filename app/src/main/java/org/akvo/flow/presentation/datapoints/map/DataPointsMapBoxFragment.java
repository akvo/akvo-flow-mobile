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
import android.widget.ProgressBar;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.SupportMapFragment;
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
import java.util.List;

import javax.inject.Inject;

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

public class DataPointsMapBoxFragment extends SupportMapFragment implements
        MapboxMap.OnInfoWindowClickListener, OnMapReadyCallback, DataPointsMapView {

    private static final String MARKER_IMAGE = "custom-marker";
    private static final String MAP_OPTIONS = "MapOptions";
    private static final String SOURCE_ID = "datapoints";

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

    public static DataPointsMapBoxFragment newInstance(SurveyGroup surveyGroup) {
        DataPointsMapBoxFragment fragment = new DataPointsMapBoxFragment();
        Bundle args = new Bundle();
        args.putSerializable(ConstantUtil.SURVEY_GROUP_EXTRA, surveyGroup);
        MapboxMapOptions options = new MapboxMapOptions();
        //options.zOrderOnTop(true);
        args.putParcelable(MAP_OPTIONS, options);
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
        presenter.onViewReady();
    }

    //TODO: too long method refactor
    private void addClusteredGeoJsonSource(@NonNull Style loadedMapStyle) {
        List<Feature> features = new ArrayList<>();
        for (MapDataPoint item : mItems) {
            com.google.android.gms.maps.model.LatLng position = item.getPosition();
            if (position != null) {
                features.add(Feature.fromGeometry(
                        Point.fromLngLat(position.longitude, position.latitude)));
            }
        }

        GeoJsonSource source = (GeoJsonSource) loadedMapStyle.getSource(SOURCE_ID);
        if (source == null) {
            loadedMapStyle.addSource(
                    new GeoJsonSource(SOURCE_ID,
                            FeatureCollection.fromFeatures(features),
                            new GeoJsonOptions()
                                    .withCluster(true)
                                    .withClusterMaxZoom(14)
                                    .withClusterRadius(50)
                    )
            );

            // Use the datapoints GeoJSON source to create three layers: One layer for each cluster category.
            // Each point range gets a different fill color.
            int[][] layers = new int[][] {
                    new int[] { 50, Color.parseColor("#009954") },
                    new int[] { 20, Color.parseColor("#007B99") },
                    new int[] { 0, Color.parseColor("#005899") }
            };

            //Creating a marker layer for single data points
            SymbolLayer unclustered = new SymbolLayer("unclustered-points", SOURCE_ID);

            unclustered.setProperties(
                    iconImage(MARKER_IMAGE),
                    iconColor(
                            rgb(255, 119, 77)
                              /*  interpolate(exponential(1), get("mag"),
                                        stop(2.0, rgb(0, 255, 0)),
                                        stop(4.5, rgb(0, 0, 255)),
                                        stop(7.0, rgb(255, 0, 0))
                                )*/
                    )
            );
            loadedMapStyle.addLayer(unclustered);

            for (int i = 0; i < layers.length; i++) {
                //Add clusters' circles
                CircleLayer circles = new CircleLayer("cluster-" + i, SOURCE_ID);
                circles.setProperties(
                        circleColor(layers[i][1]),
                        circleRadius(18f)
                );

                Expression pointCount = toNumber(get("point_count"));

                // Add a filter to the cluster layer that hides the circles based on "point_count"
                circles.setFilter(
                        i == 0
                                ? all(has("point_count"),
                                gte(pointCount, literal(layers[i][0]))
                        ) : all(has("point_count"),
                                gt(pointCount, literal(layers[i][0])),
                                lt(pointCount, literal(layers[i - 1][0]))
                        )
                );
                loadedMapStyle.addLayer(circles);
            }

            //Add the count labels
            SymbolLayer count = new SymbolLayer("count", SOURCE_ID);
            count.setProperties(
                    textField(Expression.toString(get("point_count"))),
                    textSize(12f),
                    textColor(Color.WHITE),
                    textIgnorePlacement(true),
                    textAllowOverlap(true)
            );
            loadedMapStyle.addLayer(count);
        } else {
            source.setGeoJson(FeatureCollection.fromFeatures(features));
        }
    }

    private boolean isLocationAllowed() {
        FragmentActivity activity = getActivity();
        return activity != null && ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED;
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

    @Override
    public boolean onInfoWindowClick(@NonNull Marker marker) {
        final String surveyedLocaleId = marker.getSnippet();
        if (mListener != null) {
            mListener.onRecordSelected(surveyedLocaleId);
        }
        return false;
    }

    @Override
    public void displayData(List<MapDataPoint> surveyedLocales) {
        mItems.clear();
        mItems.addAll(surveyedLocales);

        if (mapboxMap != null) {
            mapboxMap.setStyle(new Style.Builder()
                    .fromUrl("mapbox://styles/mapbox/light-v10"), new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    //TODO: refactor this to do this only once
                    style.addImage(MARKER_IMAGE, BitmapFactory.decodeResource(
                            getResources(), R.drawable.marker), true);
                    enableLocationComponent(style);
                    addClusteredGeoJsonSource(style);
                }
            });
        }
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
