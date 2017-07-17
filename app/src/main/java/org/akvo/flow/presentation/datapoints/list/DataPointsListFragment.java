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

package org.akvo.flow.presentation.datapoints.list;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.datapoints.DataPointSyncSnackBarManager;
import org.akvo.flow.presentation.datapoints.DataPointSyncView;
import org.akvo.flow.presentation.datapoints.list.entity.ListDataPoint;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.fragment.OrderByDialogFragment;
import org.akvo.flow.ui.fragment.OrderByDialogFragment.OrderByDialogListener;
import org.akvo.flow.ui.fragment.RecordListListener;
import org.akvo.flow.util.ConstantUtil;

import java.lang.ref.WeakReference;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class DataPointsListFragment extends Fragment implements LocationListener,
        OnItemClickListener, OrderByDialogListener, DataPointsListView, DataPointSyncView {

    private LocationManager mLocationManager;
    private Double mLatitude = null;
    private Double mLongitude = null;

    private DataPointListAdapter mAdapter;
    private RecordListListener mListener;

    private TextView emptyTextView;
    private ProgressBar progressBar;

    /**
     * BroadcastReceiver to notify of data synchronisation. This should be
     * fired from {@link org.akvo.flow.service.DataSyncService}
     */
    private final BroadcastReceiver dataSyncReceiver = new DataSyncBroadcastReceiver(this);

    private final DataPointSyncSnackBarManager dataPointSyncSnackBarManager = new DataPointSyncSnackBarManager(
            this);

    @Inject
    Navigator navigator;

    @Inject
    DataPointsListPresenter presenter;

    private boolean displayMonitoredMenu;

    public static DataPointsListFragment newInstance(SurveyGroup surveyGroup) {
        DataPointsListFragment fragment = new DataPointsListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ConstantUtil.EXTRA_SURVEY_GROUP, surveyGroup);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            throw new ClassCastException(activity.toString()
                    + " must implement SurveyedLocalesFragmentListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.surveyed_locales_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLocationManager = (LocationManager) getActivity()
                .getSystemService(Context.LOCATION_SERVICE);
        View view = getView();
        ListView listView = (ListView) view.findViewById(R.id.locales_lv);
        emptyTextView = (TextView) view.findViewById(R.id.empty_tv);
        listView.setEmptyView(emptyTextView);
        SurveyGroup surveyGroup = (SurveyGroup) getArguments()
                .getSerializable(ConstantUtil.EXTRA_SURVEY_GROUP);
        if (mAdapter == null) {
            mAdapter = new DataPointListAdapter(getActivity(), mLatitude, mLongitude,
                    surveyGroup);
            listView.setAdapter(mAdapter);
        }
        listView.setOnItemClickListener(this);
        progressBar = (ProgressBar) view.findViewById(R.id.progress);
        initializeInjector();
        presenter.setView(this);
        presenter.onDataReady(surveyGroup);
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
    public void onResume() {
        super.onResume();

        // try to find out where we are
        updateLocation();

        // Listen for data sync updates, so we can update the UI accordingly
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(dataSyncReceiver,
                new IntentFilter(ConstantUtil.ACTION_DATA_SYNC));

        presenter.refresh();
    }

    private void updateLocation() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String provider = mLocationManager.getBestProvider(criteria, true);
        if (provider != null) {
            Location loc = mLocationManager.getLastKnownLocation(provider);
            if (loc != null) {
                mLatitude = loc.getLatitude();
                mLongitude = loc.getLongitude();
                mAdapter.updateLocation(mLatitude, mLongitude);
                presenter.onLocationReady(mLatitude, mLongitude);
            }
            mLocationManager.requestLocationUpdates(provider, 1000, 0, this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(dataSyncReceiver);
        mLocationManager.removeUpdates(this);
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

    public void refresh(SurveyGroup surveyGroup) {
        presenter.onDataReady(surveyGroup);
        presenter.refresh();
    }

    @Override
    public void showOrderByDialog(int orderBy) {
        DialogFragment dialogFragment = OrderByDialogFragment.instantiate(orderBy);
        dialogFragment.setTargetFragment(this, 0);
        dialogFragment
                .show(getFragmentManager(), OrderByDialogFragment.FRAGMENT_ORDER_BY_TAG);
    }

    @Override
    public void showNoSurveySelected() {
        emptyTextView.setText(R.string.no_survey_selected_text);
    }

    @Override
    public void showNoDataPoints() {
        emptyTextView.setText(R.string.no_records_text);
    }

    @Override
    public void showLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ListDataPoint surveyedLocale = mAdapter.getItem(position);
        final String localeId = surveyedLocale == null ? null : surveyedLocale.getId();
        if (localeId != null) {
            mListener.onRecordSelected(localeId);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (displayMonitoredMenu) {
            inflater.inflate(R.menu.datapoints_list_monitored, menu);
        } else {
            inflater.inflate(R.menu.datapoints_list, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.order_by:
                presenter.onOrderByClicked();
                return true;
            case R.id.sync_records:
                presenter.onSyncRecordsPressed();
                return true;
        }
        return false;
    }

    @Override
    public void onOrderByClick(int order) {
        presenter.onOrderByClick(order);
    }

    // ==================================== //
    // ======== Location Callbacks ======== //
    // ==================================== //

    @Override
    public void onLocationChanged(Location location) {
        // a single location is all we need
        mLocationManager.removeUpdates(this);
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        presenter.onLocationReady(mLatitude, mLongitude);
    }

    @Override
    public void onProviderDisabled(String provider) {
        // EMPTY
    }

    @Override
    public void onProviderEnabled(String provider) {
        // EMPTY
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // EMPTY
    }

    private void refreshLocalData() {
        presenter.refresh();
    }

    @Override
    public void displayData(List<ListDataPoint> listDataPoints) {
        if (mAdapter != null) {
            mAdapter.setLocales(listDataPoints);
        }
    }

    @Override
    public void showErrorMissingLocation() {
        //TODO: should we prompt the user to enable location?
        Toast.makeText(getActivity(), R.string.locale_list_error_unknown_location,
                Toast.LENGTH_SHORT).show();
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
        dataPointSyncSnackBarManager.showSyncedResults(numberOfSyncedItems);
    }

    @Override
    public void showErrorAssignmentMissing() {
        dataPointSyncSnackBarManager.showErrorAssignmentMissing();
    }

    @Override
    public void showErrorSyncNotAllowed() {
        dataPointSyncSnackBarManager.showErrorSyncNotAllowed();
    }

    @Override
    public void showErrorNoNetwork() {
        dataPointSyncSnackBarManager.showErrorNoNetwork();
    }

    @Override
    public void showErrorSync() {
        dataPointSyncSnackBarManager.showErrorSync();
    }

    @Override
    public void onRetryRequested() {
        presenter.onSyncRecordsPressed();
    }

    @Override
    public View getRootView() {
        return getView();
    }

    @Override
    public void onSettingsPressed() {
        navigator.navigateToPreferences(getActivity());
    }

    //TODO: once we insert data using brite database this will no longer be necessary either
    public static class DataSyncBroadcastReceiver extends BroadcastReceiver {

        private final WeakReference<DataPointsListFragment> fragmentWeakRef;

        public DataSyncBroadcastReceiver(DataPointsListFragment fragment) {
            this.fragmentWeakRef = new WeakReference<>(fragment);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.i("Survey Instance status has changed. Refreshing UI...");
            DataPointsListFragment fragment = fragmentWeakRef.get();
            if (fragment != null) {
                fragment.refreshLocalData();
            }
        }
    }
}
