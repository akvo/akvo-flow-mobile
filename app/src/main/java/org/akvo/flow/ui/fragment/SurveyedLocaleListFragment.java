/*
 *  Copyright (C) 2013-2014 Stichting Akvo (Akvo Foundation)
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

import java.text.DecimalFormat;
import java.util.Date;

import org.akvo.flow.util.PlatformUtil;
import org.ocpsoft.prettytime.PrettyTime;

import android.app.Activity;
import android.content.Context;
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
import android.support.v4.widget.CursorAdapter;
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
import org.akvo.flow.activity.RecordListActivity;
import org.akvo.flow.async.loader.SurveyedLocaleLoader;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.dao.SurveyDbAdapter.RecordColumns;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceColumns;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceStatus;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.ui.fragment.OrderByDialogFragment.OrderByDialogListener;
import org.akvo.flow.util.ConstantUtil;

public class SurveyedLocaleListFragment extends ListFragment implements LocationListener, 
            OnItemClickListener, LoaderCallbacks<Cursor>, OrderByDialogListener {
    private static final String TAG = SurveyedLocaleListFragment.class.getSimpleName();

    private LocationManager mLocationManager;
    private double mLatitude = 0.0d;
    private double mLongitude = 0.0d;

    private int mOrderBy;
    private long mSurveyGroupId;
    private SurveyDbAdapter mDatabase;
    
    private SurveyedLocaleListAdapter mAdapter;
    private RecordListListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurveyGroupId = getArguments().getLong(RecordListActivity.EXTRA_SURVEY_GROUP_ID);
        mOrderBy = ConstantUtil.ORDER_BY_DATE;// Default case
        setHasOptionsMenu(true);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = (RecordListListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SurveyedLocalesFragmentListener");
        }
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        mDatabase = new SurveyDbAdapter(getActivity());
        if(mAdapter == null) {
            mAdapter = new SurveyedLocaleListAdapter(getActivity());
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
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        refresh();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(this);
        mDatabase.close();
    }

    /**
     * Ideally, we should build a ContentProvider, so this notifications are handled
     * automatically, and the loaders restarted without this explicit dependency.
     */
    public void refresh() {
        if (isResumed()) {
            if (mOrderBy == ConstantUtil.ORDER_BY_DISTANCE && mLatitude == 0.0d && mLongitude == 0.0d) {
                // Warn user that the location is unknown
                Toast.makeText(getActivity(), "Unknown Location", Toast.LENGTH_SHORT).show();
                return;
            }
            getLoaderManager().restartLoader(0, null, this);
        }
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
                DialogFragment dialogFragment = new OrderByDialogFragment();
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
        return new SurveyedLocaleLoader(getActivity(), mDatabase, mSurveyGroupId, mLatitude, mLongitude, mOrderBy);
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
     * List Adapter to bind the Surveyed Locales into the list items
     */
    class SurveyedLocaleListAdapter extends CursorAdapter {

        public SurveyedLocaleListAdapter(Context context) {
            super(context, null, false);
        }

        private String getDistanceText(SurveyedLocale surveyedLocale) {
            StringBuilder builder = new StringBuilder("Distance: ");
            
            if (surveyedLocale.getLatitude() != null && surveyedLocale.getLongitude() != null
                    && (mLatitude != 0.0d || mLongitude != 0.0d)) {
                float[] results = new float[1];
                Location.distanceBetween(mLatitude, mLongitude, surveyedLocale.getLatitude(), surveyedLocale.getLongitude(), results);
                final double distance = results[0];
            
                // default: no decimal point, km as unit
                DecimalFormat df = new DecimalFormat("#.#");
                String unit = "km";
                Double factor = 0.001; // convert from meters to km
    
                // for distances smaller than 1 km, use meters as unit
                if (distance < 1000.0) {
                    factor = 1.0;
                    unit = "m";
                    df = new DecimalFormat("#"); // only whole meters
                }
                double dist = distance * factor;
                builder.append(df.format(dist)).append(" ").append(unit);
            } else {
                builder.append(getString(R.string.unknown));
            }
            
            return builder.toString();
        }
        
        private void displayDateText(TextView tv, Long time) {
            if (time != null && time > 0) {
                tv.setVisibility(View.VISIBLE);
                tv.setText(getString(R.string.last_modified) + " " +
                        new PrettyTime().format(new Date(time)));
            } else {
                tv.setVisibility(View.GONE);
            }
        }

        @Override
        public void bindView(View view, Context context, Cursor c) {
            TextView nameView = (TextView) view.findViewById(R.id.locale_name);
            TextView idView = (TextView) view.findViewById(R.id.locale_id);
            TextView dateView = (TextView) view.findViewById(R.id.last_modified);
            TextView distanceView = (TextView) view.findViewById(R.id.locale_distance);
            ImageView statusImage = (ImageView) view.findViewById(R.id.status_img);
            final SurveyedLocale surveyedLocale = SurveyDbAdapter.getSurveyedLocale(c);

            // This cursor contains extra info about the Record status
            int status = c.getInt(c.getColumnIndexOrThrow(SurveyInstanceColumns.STATUS));

            nameView.setText(surveyedLocale.getDisplayName(context));
            idView.setText(surveyedLocale.getId());
            distanceView.setText(getDistanceText(surveyedLocale));
            displayDateText(dateView, surveyedLocale.getLastModified());

            int statusRes = 0;
            switch (status) {
                case SurveyInstanceStatus.SAVED:
                    statusRes = R.drawable.record_saved_icn;
                    break;
                case SurveyInstanceStatus.SUBMITTED:
                case SurveyInstanceStatus.EXPORTED:
                    statusRes = R.drawable.record_exported_icn;
                    break;
                case SurveyInstanceStatus.SYNCED:
                case SurveyInstanceStatus.DOWNLOADED:
                    statusRes = R.drawable.record_synced_icn;
                    break;
            }

            statusImage.setImageResource(statusRes);

            // Alternate background
            int attr = c.getPosition() % 2 == 0 ? R.attr.listitem_bg1 : R.attr.listitem_bg2;
            final int res= PlatformUtil.getResource(context, attr);
            view.setBackgroundResource(res);
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            return inflater.inflate(R.layout.surveyed_locale_item, null);
        }

    }
    
}
