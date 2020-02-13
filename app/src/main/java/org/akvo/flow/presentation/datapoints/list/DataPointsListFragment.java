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
 *
 */

package org.akvo.flow.presentation.datapoints.list;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
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
import org.akvo.flow.presentation.datapoints.list.entity.ListDataPoint;
import org.akvo.flow.service.DataPointUploadWorker;
import org.akvo.flow.tracking.TrackingListener;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.fragment.OrderByDialogFragment;
import org.akvo.flow.ui.fragment.OrderByDialogFragment.OrderByDialogListener;
import org.akvo.flow.ui.fragment.RecordListListener;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.WeakLocationListener;

import java.lang.ref.WeakReference;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class DataPointsListFragment extends Fragment implements LocationListener,
        OnItemClickListener, OrderByDialogListener, DataPointsListView {

    private static final int LIST_TAB = 0;

    @Inject
    DataPointSyncSnackBarManager dataPointSyncSnackBarManager;

    @Inject
    Navigator navigator;

    @Inject
    DataPointsListPresenter presenter;

    private LocationManager mLocationManager;
    private WeakLocationListener weakLocationListener;
    private Double mLatitude = null;
    private Double mLongitude = null;

    private DataPointListAdapter mAdapter;
    private RecordListListener mListener;
    private TrackingListener trackingListener;

    private TextView emptyTitleTv;
    private TextView emptySubTitleTv;
    private ImageView emptyIv;
    private ProgressBar progressBar;
    private SearchView searchView;
    private Integer menuRes = null;

    /**
     * BroadcastReceiver to notify of data synchronisation. This should be
     * fired from {@link DataPointUploadWorker}
     */
    private final BroadcastReceiver dataSyncReceiver = new DataSyncBroadcastReceiver(this);

    public static DataPointsListFragment newInstance(SurveyGroup surveyGroup) {
        DataPointsListFragment fragment = new DataPointsListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ConstantUtil.SURVEY_EXTRA, surveyGroup);
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

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        FragmentActivity activity = getActivity();
        try {
            mListener = (RecordListListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SurveyedLocalesFragmentListener");
        }

        if (! (activity instanceof TrackingListener)) {
            throw new IllegalArgumentException("Activity must implement TrackingListener");
        } else {
            trackingListener = (TrackingListener) activity;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.data_points_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLocationManager = (LocationManager) getActivity().getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
        weakLocationListener = new WeakLocationListener(this);
        View view = getView();
        ListView listView = view.findViewById(R.id.locales_lv);
        View emptyView = view.findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);
        emptyTitleTv = view.findViewById(R.id.empty_title_tv);
        emptySubTitleTv = view.findViewById(R.id.empty_subtitle_tv);
        emptyIv = view.findViewById(R.id.empty_iv);
        SurveyGroup surveyGroup = (SurveyGroup) getArguments()
                .getSerializable(ConstantUtil.SURVEY_EXTRA);
        mAdapter = new DataPointListAdapter(getActivity(), mLatitude, mLongitude, surveyGroup);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
        progressBar = view.findViewById(R.id.progress);
        updateProgressDrawable();

        presenter.setView(this);
        presenter.onDataReady(surveyGroup);
    }

    private void updateProgressDrawable() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Drawable progressDrawable = progressBar.getIndeterminateDrawable();
            if (progressDrawable != null) {
                progressDrawable
                        .setColorFilter(ContextCompat.getColor(getActivity(), R.color.colorAccent),
                                PorterDuff.Mode.MULTIPLY);
            }
        }
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

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(dataSyncReceiver,
                new IntentFilter(ConstantUtil.ACTION_DATA_SYNC));

        if (searchView == null || searchView.isIconified()) {
            updateLocation();
            presenter.loadDataPoints();
        }
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
            mLocationManager.requestLocationUpdates(provider, 1000, 0, weakLocationListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(dataSyncReceiver);
        mLocationManager.removeUpdates(weakLocationListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        mLocationManager.removeUpdates(weakLocationListener);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        trackingListener = null;
    }

    @Override
    public void onDestroy() {
        presenter.destroy();
        super.onDestroy();
    }

    public void onNewSurveySelected(SurveyGroup surveyGroup) {
        getArguments().putSerializable(ConstantUtil.SURVEY_EXTRA, surveyGroup);
        presenter.onNewSurveySelected(surveyGroup);
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
        emptyTitleTv.setText(R.string.no_survey_selected_text);
        emptySubTitleTv.setText("");
    }

    @Override
    public void showNoDataPoints(boolean monitored) {
        emptyTitleTv.setText(R.string.no_datapoints_error_text);
        int subtitleResource = monitored ?
                R.string.no_records_subtitle_monitored :
                R.string.no_records_subtitle_non_monitored;
        emptySubTitleTv.setText(subtitleResource);
        emptyIv.setImageResource(R.drawable.ic_format_list_bulleted);
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
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (menuRes != null) {
            inflater.inflate(menuRes, menu);
            setUpSearchView(menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void setUpSearchView(final Menu menu) {
        MenuItem searchMenuItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setIconifiedByDefault(true);
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Empty
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText)) {
                    presenter.getFilteredDataPoints(newText);
                } else {
                    presenter.loadDataPoints();
                }
                return false;
            }
        });
        searchMenuItem.setOnActionExpandListener(
                new MenuItem.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        if (trackingListener != null) {
                            trackingListener.logSearchEvent();
                        }
                        // EMPTY
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        presenter.loadDataPoints();
                        return true;
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.order_by:
                presenter.onOrderByClicked();
                if (trackingListener != null) {
                    trackingListener.logSortEvent();
                }
                return true;
            case R.id.download:
                presenter.onDownloadPressed();
                if (trackingListener != null) {
                    trackingListener.logDownloadEvent(LIST_TAB);
                }
                return true;
            case R.id.upload:
                presenter.onUploadPressed();
                if (trackingListener != null) {
                    trackingListener.logUploadEvent(LIST_TAB);
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onOrderByClick(int order) {
        presenter.onOrderByClick(order);
        if (trackingListener != null) {
            trackingListener.logOrderEvent(order);
        }
    }

    // ==================================== //
    // ======== Location Callbacks ======== //
    // ==================================== //

    @Override
    public void onLocationChanged(Location location) {
        // a single location is all we need
        mLocationManager.removeUpdates(weakLocationListener);
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
        presenter.loadDataPoints();
    }

    @Override
    public void displayData(List<ListDataPoint> listDataPoints) {
        if (mAdapter != null) {
            mAdapter.setDataPoints(listDataPoints);
        }
    }

    @Override
    public void showErrorMissingLocation() {
        //TODO: should we prompt the user to enable location?
        Toast.makeText(getActivity(), R.string.locale_list_error_unknown_location,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showNonMonitoredMenu() {
        menuRes = R.menu.datapoints_list;
        reloadMenu();
    }

    @Override
    public void showMonitoredMenu() {
        menuRes = R.menu.datapoints_list_monitored;
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
                presenter.onDownloadPressed();
            }
        });
    }

    @Override
    public void showErrorSync() {
        dataPointSyncSnackBarManager.showErrorSync(getView(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onDownloadPressed();
            }
        });
    }

    @Override
    public void displayNoSearchResultsFound() {
        if (emptyTitleTv != null) {
            emptyTitleTv.setText(R.string.no_search_results_error_text);
        }
        if (emptySubTitleTv != null) {
            emptySubTitleTv.setText("");
        }
        if (emptyIv != null) {
            emptyIv.setImageResource(R.drawable.ic_search_results_error);
        }
    }

    @Override
    public void showNoDataPointsToSync() {
        dataPointSyncSnackBarManager.showNoDataPointsToSync(getView());
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
