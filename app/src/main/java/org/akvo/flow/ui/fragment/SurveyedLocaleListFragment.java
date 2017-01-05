/*
 *  Copyright (C) 2013-2015 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.ui.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.activity.SurveyActivity;
import org.akvo.flow.data.loader.SurveyedLocaleLoader;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.data.database.RecordColumns;
import org.akvo.flow.data.database.SurveyInstanceColumns;
import org.akvo.flow.data.database.SurveyInstanceStatus;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.ui.fragment.OrderByDialogFragment.OrderByDialogListener;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.GeoUtil;
import org.akvo.flow.util.PlatformUtil;
import org.ocpsoft.prettytime.PrettyTime;

import java.lang.ref.WeakReference;
import java.util.Date;

public class SurveyedLocaleListFragment extends ListFragment implements LocationListener,
        OnItemClickListener, LoaderCallbacks<Cursor>, OrderByDialogListener {
    private static final String TAG = SurveyedLocaleListFragment.class.getSimpleName();

    private LocationManager mLocationManager;
    private double mLatitude = 0.0d;
    private double mLongitude = 0.0d;

    private int mOrderBy;
    private SurveyGroup mSurveyGroup;
    private SurveyDbAdapter mDatabase;

    private SurveyedLocaleListAdapter mAdapter;
    private RecordListListener mListener;

    public static SurveyedLocaleListFragment newInstance(SurveyGroup surveyGroup) {
        SurveyedLocaleListFragment fragment = new SurveyedLocaleListFragment();
        Bundle args = new Bundle();
        args.putSerializable(SurveyActivity.EXTRA_SURVEY_GROUP, surveyGroup);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurveyGroup = (SurveyGroup) getArguments()
                .getSerializable(SurveyActivity.EXTRA_SURVEY_GROUP);
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLocationManager = (LocationManager) getActivity()
                .getSystemService(Context.LOCATION_SERVICE);
        mDatabase = new SurveyDbAdapter(getActivity());
        if (mAdapter == null) {
            mAdapter = new SurveyedLocaleListAdapter(getActivity(), mLatitude, mLongitude,
                    mSurveyGroup);
            setListAdapter(mAdapter);
        }
        setEmptyText(getString(R.string.no_records_text));
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();

        // try to find out where we are
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String provider = mLocationManager.getBestProvider(criteria, true);
        if (provider != null) {
            Location loc = mLocationManager.getLastKnownLocation(provider);
            if (loc != null) {
                mLatitude = loc.getLatitude();
                mLongitude = loc.getLongitude();
            }
            mLocationManager.requestLocationUpdates(provider, 1000, 0, this);
        }

        // Listen for data sync updates, so we can update the UI accordingly
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(dataSyncReceiver,
                new IntentFilter(ConstantUtil.ACTION_DATA_SYNC));

        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(dataSyncReceiver);
        mLocationManager.removeUpdates(this);
        mDatabase.close();
    }

    public void refresh(SurveyGroup surveyGroup) {
        mSurveyGroup = surveyGroup;
        refresh();
    }

    /**
     * Ideally, we should build a ContentProvider, so this notifications are handled
     * automatically, and the loaders restarted without this explicit dependency.
     */
    private void refresh() {
        if (!isResumed()) {
            return;
        }

        if (mSurveyGroup == null) {
            setEmptyText(getString(R.string.no_survey_selected_text));
        } else {
            setEmptyText(getString(R.string.no_records_text));
        }

        if (mOrderBy == ConstantUtil.ORDER_BY_DISTANCE && mLatitude == 0.0d && mLongitude == 0.0d) {
            // Warn user that the location is unknown
            Toast.makeText(getActivity(), R.string.locale_list_error_unknown_location, Toast.LENGTH_SHORT).show();
            return;
        }
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        final String localeId = cursor.getString(cursor.getColumnIndexOrThrow(
                RecordColumns.RECORD_ID));

        mListener.onRecordSelected(localeId);// Notify the host activity
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
        }

        return false;
    }

    @Override
    public void onOrderByClick(int order) {
        if (mOrderBy != order) {
            mOrderBy = order;
            refresh();
        }
    }

    // ==================================== //
    // ========= Loader Callbacks ========= //
    // ==================================== //

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        long surveyId = mSurveyGroup != null ? mSurveyGroup.getId() : SurveyGroup.ID_NONE;
        return new SurveyedLocaleLoader(getActivity(), mDatabase, surveyId, mLatitude, mLongitude,
                mOrderBy);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            Log.e(TAG, "onFinished() - Loader returned no data");
            return;
        }

        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
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
        refresh();
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    /**
     * BroadcastReceiver to notify of data synchronisation. This should be
     * fired from DataSyncService.
     */
    private final BroadcastReceiver dataSyncReceiver = new DataSyncBroadcastReceiver(this);

    /**
     * List Adapter to bind the Surveyed Locales into the list items
     */
    private static class SurveyedLocaleListAdapter extends CursorAdapter {

        private final double mLatitude;
        private final double mLongitude;
        private final SurveyGroup mSurveyGroup;

        public SurveyedLocaleListAdapter(Context context, double mLatitude, double mLongitude,
                SurveyGroup mSurveyGroup) {
            super(context, null, false);
            this.mLatitude = mLatitude;
            this.mLongitude = mLongitude;
            this.mSurveyGroup = mSurveyGroup;
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return inflater.inflate(R.layout.surveyed_locale_item, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor c) {
            TextView nameView = (TextView) view.findViewById(R.id.locale_name);
            TextView idView = (TextView) view.findViewById(R.id.locale_id);
            TextView dateView = (TextView) view.findViewById(R.id.last_modified);
            TextView distanceView = (TextView) view.findViewById(R.id.locale_distance);
            TextView statusView = (TextView) view.findViewById(R.id.status);
            ImageView statusImage = (ImageView) view.findViewById(R.id.status_img);
            final SurveyedLocale surveyedLocale = SurveyDbAdapter.getSurveyedLocale(c);

            // This cursor contains extra info about the Record status
            int status = c.getInt(c.getColumnIndexOrThrow(SurveyInstanceColumns.STATUS));
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
            int attr = c.getPosition() % 2 == 0 ? R.attr.listitem_bg1 : R.attr.listitem_bg2;
            final int res = PlatformUtil.getResource(context, attr);
            view.setBackgroundResource(res);
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

    }

    private static class DataSyncBroadcastReceiver extends BroadcastReceiver {

        private final WeakReference<SurveyedLocaleListFragment> fragmentWeakRef;

        private DataSyncBroadcastReceiver(SurveyedLocaleListFragment fragment) {
            this.fragmentWeakRef = new WeakReference<>(fragment);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Survey Instance status has changed. Refreshing UI...");
            SurveyedLocaleListFragment fragment = fragmentWeakRef.get();
            if (fragment != null) {
                fragment.refresh();
            }
        }
    }
}
