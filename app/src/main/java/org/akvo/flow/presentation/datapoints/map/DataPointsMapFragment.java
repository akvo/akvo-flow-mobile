/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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
 *
 */

package org.akvo.flow.presentation.datapoints.map;

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.ClusterManager;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.datapoints.DataPointSyncListener;
import org.akvo.flow.presentation.datapoints.map.entity.MapDataPoint;
import org.akvo.flow.ui.fragment.RecordListListener;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class DataPointsMapFragment extends SupportMapFragment
        implements OnInfoWindowClickListener, OnMapReadyCallback,
        DataPointsMapView {

    public static final int MAP_ZOOM_LEVEL = 10;
    public static final String MAP_OPTIONS = "MapOptions";

    @Nullable
    private RecordListListener mListener;

    @Nullable
    private DataPointSyncListener dataPointsSyncListener;

    private List<MapDataPoint> mItems;

    private boolean displayMonitoredMenu;

    @Nullable
    private ProgressBar progressBar;

    @Nullable
    private GoogleMap mMap;

    @Inject
    DataPointsMapPresenter presenter;

    private ClusterManager<MapDataPoint> mClusterManager;

    public static DataPointsMapFragment newInstance(SurveyGroup surveyGroup) {
        DataPointsMapFragment fragment = new DataPointsMapFragment();
        Bundle args = new Bundle();
        args.putSerializable(ConstantUtil.EXTRA_SURVEY_GROUP, surveyGroup);
        GoogleMapOptions options = new GoogleMapOptions();
        options.zOrderOnTop(true);
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
        if (!(activity instanceof DataPointSyncListener)) {
            throw new IllegalArgumentException("Activity must implement DataPointsSyncListener");
        }
        this.dataPointsSyncListener = (DataPointSyncListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            View.inflate(getActivity(), R.layout.map_progress_bar, viewGroup);
            progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        }
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initializeInjector();
        presenter.setView(this);
        SurveyGroup surveyGroup = (SurveyGroup) getArguments()
                .getSerializable(ConstantUtil.EXTRA_SURVEY_GROUP);
        presenter.onDataReady(surveyGroup);
        getMapAsync(this);
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
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        configMap();
        presenter.onViewReady();
    }

    private void configMap() {
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
            mMap.setOnInfoWindowClickListener(this);
            mClusterManager = new ClusterManager<>(getActivity(), mMap);
            mClusterManager.setRenderer(new PointRenderer(mMap, getActivity(), mClusterManager));
            mMap.setOnMarkerClickListener(mClusterManager);
            mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    cluster();
                }
            });
            centerMap();
        }
    }

    private void cluster() {
        if (mMap == null) {
            return;
        }

        final LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        LatLng ne = bounds.northeast, sw = bounds.southwest;
        double latDst = Math.abs(ne.latitude - sw.latitude);
        double lonDst = Math.abs(ne.longitude - sw.longitude);

        final double scale = 1d;
        LatLngBounds newBounds =
                bounds.including(
                        new LatLng(ne.latitude + latDst / scale, ne.longitude + lonDst / scale))
                        .including(new LatLng(sw.latitude - latDst / scale,
                                ne.longitude + lonDst / scale))
                        .including(new LatLng(sw.latitude - latDst / scale,
                                sw.longitude - lonDst / scale))
                        .including(new LatLng(ne.latitude + latDst / scale,
                                sw.longitude - lonDst / scale));

        mClusterManager.clearItems();
        for (MapDataPoint item : mItems) {
            if (item.getPosition() != null && newBounds.contains(item.getPosition())) {
                mClusterManager.addItem(item);
            }
        }
        mClusterManager.cluster();
    }

    /**
     * Center the map in the given record's coordinates. If no record is provided,
     * the user's location will be used.
     */
    private void centerMap() {
        if (mMap == null) {
            return; // Not ready yet
        }

        LatLng position = null;
        // When multiple points are shown, center the map in user's location
        LocationManager manager = (LocationManager) getActivity()
                .getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String provider = manager.getBestProvider(criteria, true);
        if (provider != null) {
            Location location = manager.getLastKnownLocation(provider);
            if (location != null) {
                position = new LatLng(location.getLatitude(), location.getLongitude());
            }
        }

        if (position != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, MAP_ZOOM_LEVEL));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mItems.isEmpty()) {
            // Make sure we only fetch the data and center the map once
            refresh();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        dataPointsSyncListener = null;
    }

    @Override
    public void onDestroy() {
        presenter.onViewDestroyed();
        super.onDestroy();
    }

    public void refresh(SurveyGroup surveyGroup) {
        presenter.onDataReady(surveyGroup);
        refresh();
    }

    /**
     * Ideally, we should build a ContentProvider, so this notifications are handled
     * automatically, and the loaders restarted without this explicit dependency.
     */
    public void refresh() {
        if (isResumed()) {
            presenter.refresh();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (displayMonitoredMenu) {
            inflater.inflate(R.menu.datapoints_map_monitored, menu);
        } else {
            inflater.inflate(R.menu.datapoints_map, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.sync_records:
                presenter.onSyncRecordsPressed();
                return true;
        }
        return false;
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
    public void onInfoWindowClick(Marker marker) {
        final String surveyedLocaleId = marker.getSnippet();
        if (mListener != null) {
            mListener.onRecordSelected(surveyedLocaleId);
        }
    }

    @Override
    public void displayData(List<MapDataPoint> surveyedLocales) {
        mItems.clear();
        mItems.addAll(surveyedLocales);
        cluster();
    }

    @Override
    public void displayMenu(boolean monitored) {
        displayMonitoredMenu = monitored;
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void showSyncedResults(int numberOfSyncedItems) {
        if (dataPointsSyncListener != null) {
            dataPointsSyncListener.showSyncedResults(numberOfSyncedItems);
        }
    }

    @Override
    public void showSyncNotAllowed() {
        if (dataPointsSyncListener != null) {
            dataPointsSyncListener.showSyncNotAllowed();
        }
    }

    @Override
    public void showNoNetwork() {
        if (dataPointsSyncListener != null) {
            dataPointsSyncListener.showNoNetwork();
        }
    }

    @Override
    public void showErrorSync() {
        if (dataPointsSyncListener != null) {
            dataPointsSyncListener.showErrorSync();
        }
    }
}
