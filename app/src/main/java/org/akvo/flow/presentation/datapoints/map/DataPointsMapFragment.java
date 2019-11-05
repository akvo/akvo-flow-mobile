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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.geojson.FeatureCollection;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.offlinemaps.presentation.MapBoxMapItemListViewImpl;
import org.akvo.flow.offlinemaps.presentation.MapReadyCallback;
import org.akvo.flow.presentation.datapoints.DataPointSyncSnackBarManager;
import org.akvo.flow.tracking.TrackingListener;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.util.ConstantUtil;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class DataPointsMapFragment extends Fragment implements DataPointsMapView,
        MapReadyCallback {

    private static final int MAP_TAB = 1;

    @Inject
    DataPointSyncSnackBarManager dataPointSyncSnackBarManager;

    @Inject
    DataPointsMapPresenter presenter;

    @Inject
    Navigator navigator;

    @Nullable
    private ProgressBar progressBar;

    private boolean activityJustCreated;
    private Integer menuRes = null;

    private FloatingActionButton offlineMapsFab;
    private MapBoxMapItemListViewImpl mapView;

    private TrackingListener trackingListener;

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
        initializeInjector();
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        FragmentActivity activity = getActivity();
        if (! (activity instanceof TrackingListener)) {
            throw new IllegalArgumentException("Activity must implement TrackingListener");
        } else {
            trackingListener = (TrackingListener) activity;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map_box_map, container, false);
        progressBar = view.findViewById(R.id.progressBar);
        offlineMapsFab = view.findViewById(R.id.offline_maps_fab);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsyncWithCallback(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        presenter.setView(this);
        SurveyGroup surveyGroup = (SurveyGroup) getArguments()
                .getSerializable(ConstantUtil.SURVEY_GROUP_EXTRA);
        presenter.onSurveyGroupReady(surveyGroup);
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
    @SuppressWarnings("ConstantConditions")
    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getActivity().getApplication()).getApplicationComponent();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if (!activityJustCreated) {
            mapView.getMapAsyncWithCallback(this);
        }
        activityJustCreated = false;
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
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
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onDestroy() {
        presenter.destroy();
        super.onDestroy();
    }

    public void onNewSurveySelected(SurveyGroup surveyGroup) {
        Bundle arguments = getArguments();
        if (arguments == null) {
            arguments = new Bundle();
        }
        arguments.putSerializable(ConstantUtil.SURVEY_GROUP_EXTRA, surveyGroup);
        setArguments(arguments);
        presenter.onNewSurveySelected(surveyGroup);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
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
                if (trackingListener != null) {
                    trackingListener.logDownloadEvent(MAP_TAB);
                }
                return true;
            case R.id.upload:
                presenter.onUploadPressed();
                if (trackingListener != null) {
                    trackingListener.logUploadEvent(MAP_TAB);
                }
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
    public void displayDataPoints(FeatureCollection dataPoints) {
        mapView.displayDataPoints(dataPoints);
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
        dataPointSyncSnackBarManager.showErrorNoNetwork(getView(),
                v -> presenter.onSyncRecordsPressed());
    }

    @Override
    public void showErrorSync() {
        dataPointSyncSnackBarManager
                .showErrorSync(getView(), v -> presenter.onSyncRecordsPressed());
    }

    @Override
    public void showNoDataPointsToSync() {
        dataPointSyncSnackBarManager.showNoDataPointsToSync(getView());
    }

    public void showFab() {
        offlineMapsFab.show();
    }

    public void hideFab() {
        offlineMapsFab.hide();
    }

    public void refreshView() {
        mapView.refreshSelectedArea();
    }

    @Override
    public void onMapReady() {
        presenter.loadDataPoints();
    }
}
