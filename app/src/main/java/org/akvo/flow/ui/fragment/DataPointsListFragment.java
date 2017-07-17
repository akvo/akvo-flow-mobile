/*
 *  Copyright (C) 2013-2017 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.ui.fragment;

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
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.data.loader.SurveyedLocalesLoader;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.ui.fragment.OrderByDialogFragment.OrderByDialogListener;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.GeoUtil;
import org.akvo.flow.util.PlatformUtil;
import org.ocpsoft.prettytime.PrettyTime;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

public class DataPointsListFragment extends Fragment implements LocationListener,
        OnItemClickListener, LoaderCallbacks<List<SurveyedLocale>>, OrderByDialogListener,
        DataPointsSyncListener {

    private LocationManager mLocationManager;
    private double mLatitude = 0.0d;
    private double mLongitude = 0.0d;

    private int mOrderBy;
    private SurveyGroup mSurveyGroup;

    private SurveyedLocaleListAdapter mAdapter;
    private RecordListListener mListener;

    private TextView emptyTextView;
    private ProgressBar progressBar;

    /**
     * BroadcastReceiver to notify of data synchronisation. This should be
     * fired from {@link org.akvo.flow.service.DataSyncService}
     */
    private final BroadcastReceiver dataSyncReceiver = new DataSyncBroadcastReceiver(this);

    /**
     * BroadcastReceiver to notify of records synchronisation. This should be
     * fired from {@link org.akvo.flow.service.SurveyedDataPointSyncService}.
     */
    private final BroadcastReceiver dataPointSyncReceiver = new DataPointSyncBroadcastReceiver(
            this);

    public static DataPointsListFragment newInstance(SurveyGroup surveyGroup) {
        DataPointsListFragment fragment = new DataPointsListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ConstantUtil.SURVEY_GROUP_EXTRA, surveyGroup);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurveyGroup = (SurveyGroup) getArguments()
                .getSerializable(ConstantUtil.SURVEY_GROUP_EXTRA);
        mOrderBy = ConstantUtil.ORDER_BY_DATE;// Default case
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
        if (mAdapter == null) {
            mAdapter = new SurveyedLocaleListAdapter(getActivity(), mLatitude, mLongitude,
                    mSurveyGroup);
            listView.setAdapter(mAdapter);
        }
        emptyTextView.setText(R.string.no_datapoints_error_text);
        listView.setOnItemClickListener(this);
        progressBar = (ProgressBar) view.findViewById(R.id.progress);
    }

    @Override
    public void onResume() {
        super.onResume();

        // try to find out where we are
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String provider = mLocationManager.getBestProvider(criteria, true);
        if (provider != null) {
            Location loc = mLocationManager.getLastKnownLocation(provider);
            if (loc != null) {
                mLatitude = loc.getLatitude();
                mLongitude = loc.getLongitude();
                mAdapter.updateLocation(mLatitude, mLongitude);
            }
            mLocationManager.requestLocationUpdates(provider, 1000, 0, this);
        }

        // Listen for data sync updates, so we can update the UI accordingly
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(dataSyncReceiver,
                new IntentFilter(ConstantUtil.ACTION_DATA_SYNC));

        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(dataPointSyncReceiver,
                        new IntentFilter(ConstantUtil.ACTION_LOCALE_SYNC_UPDATE));

        refreshLocalData();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(dataSyncReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(dataPointSyncReceiver);
        mLocationManager.removeUpdates(this);
    }

    public void refresh(SurveyGroup surveyGroup) {
        mSurveyGroup = surveyGroup;
        refreshLocalData();
        getArguments().putSerializable(ConstantUtil.SURVEY_GROUP_EXTRA, surveyGroup);
        refreshLocalData();
    }

    /**
     * Ideally, we should build a ContentProvider, so this notifications are handled
     * automatically, and the loaders restarted without this explicit dependency.
     */
    private void refreshLocalData() {
        if (!isResumed()) {
            return;
        }

        if (mSurveyGroup == null) {
            emptyTextView.setText(R.string.no_survey_selected_text);
        } else {
            emptyTextView.setText(R.string.no_datapoints_error_text);
        }

        if (mOrderBy == ConstantUtil.ORDER_BY_DISTANCE && mLatitude == 0.0d && mLongitude == 0.0d) {
            // Warn user that the location is unknown
            Toast.makeText(getActivity(), R.string.locale_list_error_unknown_location,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        mAdapter.updateLocation(mLatitude, mLongitude);
        showLoading();
        getLoaderManager().restartLoader(0, null, this);
    }

    private void showLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SurveyedLocale surveyedLocale = mAdapter.getItem(position);
        final String localeId = surveyedLocale.getId();
        mListener.onRecordSelected(localeId);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mSurveyGroup != null) {
            if (mSurveyGroup.isMonitored()) {
                inflater.inflate(R.menu.datapoints_list_monitored, menu);
            } else {
                inflater.inflate(R.menu.datapoints_list, menu);
            }
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.order_by:
                DialogFragment dialogFragment = OrderByDialogFragment.instantiate(mOrderBy);
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getFragmentManager(), "order_by");
                return true;
            case R.id.sync_records:
                requestRemoteDataRefresh();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onOrderByClick(int order) {
        if (mOrderBy != order) {
            mOrderBy = order;
            refreshLocalData();
        }
    }

    // ==================================== //
    // ========= Loader Callbacks ========= //
    // ==================================== //

    @Override
    public Loader<List<SurveyedLocale>> onCreateLoader(int id, Bundle args) {
        long surveyId = mSurveyGroup != null ? mSurveyGroup.getId() : SurveyGroup.ID_NONE;
        return new SurveyedLocalesLoader(getActivity(), surveyId, mLatitude, mLongitude,
                mOrderBy);
    }

    @Override
    public void onLoadFinished(Loader<List<SurveyedLocale>> loader,
            List<SurveyedLocale> surveyedLocales) {
        hideLoading();
        if (surveyedLocales == null) {
            Timber.w("onFinished() - Loader returned no data");
            return;
        }

        mAdapter.setLocales(surveyedLocales);
    }

    @Override
    public void onLoaderReset(Loader<List<SurveyedLocale>> loader) {
        // EMPTY
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
        refreshLocalData();
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

    private void requestRemoteDataRefresh() {
        if (mListener != null && mSurveyGroup != null) {
            showLoading();
            mListener.onSyncRecordsRequested(mSurveyGroup.getId());
        } else {
            hideLoading();
        }
    }

    @Override
    public void onNewDataAvailable() {
        refreshLocalData();
    }

    /**
     * List Adapter to bind the Surveyed Locales into the list items
     */
    public static class SurveyedLocaleListAdapter extends ArrayAdapter<SurveyedLocale> {

        private double mLatitude;
        private double mLongitude;
        private final SurveyGroup mSurveyGroup;
        private final LayoutInflater inflater;

        public SurveyedLocaleListAdapter(Context context, double mLatitude, double mLongitude,
                SurveyGroup mSurveyGroup) {
            super(context, R.layout.surveyed_locale_item);
            this.mLatitude = mLatitude;
            this.mLongitude = mLongitude;
            this.mSurveyGroup = mSurveyGroup;
            this.inflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = inflater.inflate(R.layout.surveyed_locale_item, parent, false);
            } else {
                view = convertView;
            }
            TextView nameView = (TextView) view.findViewById(R.id.locale_name);
            TextView idView = (TextView) view.findViewById(R.id.locale_id);
            TextView dateView = (TextView) view.findViewById(R.id.last_modified);
            TextView distanceView = (TextView) view.findViewById(R.id.locale_distance);
            TextView statusView = (TextView) view.findViewById(R.id.status);
            ImageView statusImage = (ImageView) view.findViewById(R.id.status_img);
            final SurveyedLocale surveyedLocale = getItem(position);
            Context context = parent.getContext();
            int status = surveyedLocale.getStatus();
            nameView.setText(surveyedLocale.getDisplayName(context));
            idView.setText(surveyedLocale.getId());

            displayDistanceText(distanceView, getDistanceText(surveyedLocale, context));
            displayDateText(dateView, surveyedLocale.getLastModified());

            int statusRes = 0;
            String statusText = null;
            switch (status) {
                case SurveyInstanceStatus.SAVED:
                    statusRes = R.drawable.record_saved_icn;
                    statusText = context.getString(R.string.status_saved);
                    break;
                case SurveyInstanceStatus.SUBMITTED:
                case SurveyInstanceStatus.EXPORTED:
                    statusRes = R.drawable.record_exported_icn;
                    statusText = context.getString(R.string.status_exported);
                    break;
                case SurveyInstanceStatus.SYNCED:
                case SurveyInstanceStatus.DOWNLOADED:
                    statusRes = R.drawable.record_synced_icn;
                    statusText = context.getString(R.string.status_synced);
                    break;
                default:
                    //wrong state
                    break;
            }

            statusImage.setImageResource(statusRes);
            statusView.setText(statusText);

            // Alternate background
            int attr = position % 2 == 0 ? R.attr.listitem_bg1 : R.attr.listitem_bg2;
            final int res = PlatformUtil.getResource(context, attr);
            view.setBackgroundResource(res);
            return view;
        }

        public void setCurrentLocation(Location location) {
            mLatitude = location.getLatitude();
            mLongitude = location.getLongitude();
        }

        private String getDistanceText(SurveyedLocale surveyedLocale, Context context) {
            StringBuilder builder = new StringBuilder(
                    context.getString(R.string.distance_label) + " ");

            if (surveyedLocale.getLatitude() != null && surveyedLocale.getLongitude() != null
                    && (mLatitude != 0.0d || mLongitude != 0.0d)) {
                float[] results = new float[1];
                Location.distanceBetween(mLatitude, mLongitude, surveyedLocale.getLatitude(),
                        surveyedLocale.getLongitude(), results);
                final double distance = results[0];

                builder.append(GeoUtil.getDisplayLength(distance));
                return builder.toString();
            }

            return null;
        }

        private void displayDateText(TextView tv, Long time) {
            if (time != null && time > 0) {
                tv.setVisibility(View.VISIBLE);
                int labelRes = R.string.last_modified_regular;
                if (mSurveyGroup != null && mSurveyGroup.isMonitored()) {
                    labelRes = R.string.last_modified_monitored;
                }
                tv.setText(tv.getContext().getString(labelRes) + " " + new PrettyTime()
                        .format(new Date(time)));
            } else {
                tv.setVisibility(View.GONE);
            }
        }

        private void displayDistanceText(TextView tv, String distance) {
            if (!TextUtils.isEmpty(distance)) {
                tv.setVisibility(View.VISIBLE);
                tv.setText(distance);
            } else {
                tv.setVisibility(View.GONE);
            }
        }

        void setLocales(List<SurveyedLocale> surveyedLocales) {
            clear();
            for (SurveyedLocale sl : surveyedLocales) {
                add(sl);
            }
            notifyDataSetChanged();
        }

        void updateLocation(double latitude, double longitude) {
            this.mLatitude = latitude;
            this.mLongitude = longitude;
        }
    }

    static class DataSyncBroadcastReceiver extends BroadcastReceiver {

        private final WeakReference<DataPointsListFragment> fragmentWeakRef;

        DataSyncBroadcastReceiver(DataPointsListFragment fragment) {
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
