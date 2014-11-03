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

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.akvo.flow.R;
import org.akvo.flow.activity.RecordActivity;
import org.akvo.flow.activity.RecordListActivity;
import org.akvo.flow.async.loader.SurveyedLocaleLoader;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.util.ConstantUtil;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends SupportMapFragment implements LoaderCallbacks<Cursor>, OnInfoWindowClickListener {
    private static final String TAG = MapFragment.class.getSimpleName();

    private long mSurveyGroupId;
    private String mRecordId; // If set, load a single record
    private SurveyDbAdapter mDatabase;
    private RecordListListener mListener;

    private List<SurveyedLocale> mItems;

    private boolean mSingleRecord = false;

    private GoogleMap mMap;
    private ClusterManager<SurveyedLocale> mClusterManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItems = new ArrayList<SurveyedLocale>();
        Bundle args = getArguments();
        if (args.containsKey(RecordActivity.EXTRA_RECORD_ID)) {
            // Single record mode.
            mSingleRecord = true;
            mRecordId = args.getString(RecordActivity.EXTRA_RECORD_ID);
        } else {
            mSingleRecord = false;
            mSurveyGroupId = args.getLong(RecordListActivity.EXTRA_SURVEY_GROUP_ID);
        }
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
        mDatabase = new SurveyDbAdapter(getActivity());
        if (mMap == null) {
            mMap = getMap();
            configMap();
        }
    }

    private void configMap() {
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
            mMap.setOnInfoWindowClickListener(this);
            mClusterManager = new ClusterManager<SurveyedLocale>(getActivity(), mMap);
            mClusterManager.setRenderer(new PointRenderer());
            mMap.setOnMarkerClickListener(mClusterManager);
            mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    cluster();
                }
            });
            centerMap(null);
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
        LatLngBounds newBounds = bounds
                .including(new LatLng(ne.latitude + latDst/scale, ne.longitude + lonDst/scale))
                .including(new LatLng(sw.latitude - latDst/scale, ne.longitude + lonDst/scale))
                .including(new LatLng(sw.latitude - latDst/scale, sw.longitude - lonDst/scale))
                .including(new LatLng(ne.latitude + latDst/scale, sw.longitude - lonDst/scale));

        new DynamicallyAddMarkerTask().execute(newBounds);
    }

    /**
     * Center the map in the given record's coordinates. If no record is provided,
     * the user's location will be used.
     *
     * @param record
     */
    private void centerMap(SurveyedLocale record) {
        if (mMap == null) {
            return; // Not ready yet
        }

        LatLng position = null;

        if (record != null && record.getLatitude() != null && record.getLongitude() != null) {
            // Center the map in the data point
            position = new LatLng(record.getLatitude(), record.getLongitude());
        } else {
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
        }

        if (position != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 10));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();
        if (mItems.isEmpty()) {
            // Make sure we only fetch the data and center the map once
            refresh();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mDatabase.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        View mapView = super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.map_fragment, container, false);
        FrameLayout layout = (FrameLayout) v.findViewById(R.id.map_container);

        layout.addView(mapView, 0);

        return v;
    }

    /**
     * Ideally, we should build a ContentProvider, so this notifications are handled
     * automatically, and the loaders restarted without this explicit dependency.
     */
    public void refresh() {
        if (isResumed()) {
            if (mSingleRecord) {
                // Just get it from the DB
                SurveyedLocale record = mDatabase.getSurveyedLocale(mRecordId);
                if (mMap != null && record != null && record.getLatitude() != null
                        && record.getLongitude() != null) {
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(record.getLatitude(), record.getLongitude()))
                            .title(record.getDisplayName(getActivity()))
                            .snippet(record.getId()));
                    centerMap(record);
                }
            } else {
                getLoaderManager().restartLoader(0, null, this);
            }
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (mSingleRecord) {
            return; // Do nothing. We are already inside the record Activity
        }
        final String surveyedLocaleId = marker.getSnippet();
        mListener.onRecordSelected(surveyedLocaleId);
    }

    // ==================================== //
    // ========= Loader Callbacks ========= //
    // ==================================== //

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SurveyedLocaleLoader(getActivity(), mDatabase, mSurveyGroupId,
                ConstantUtil.ORDER_BY_NONE);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            Log.e(TAG, "onFinished() - Loader returned no data");
            return;
        }

        if (cursor.moveToFirst()) {
            mItems.clear();
            do {
                SurveyedLocale item = SurveyDbAdapter.getSurveyedLocale(cursor);
                //mClusterManager.addItem(item);
                mItems.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        cluster();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * This custom renderer overrides original 'bucketed' names, in order to display the accurate
     * number of markers within a cluster.
     */
    class PointRenderer extends DefaultClusterRenderer<SurveyedLocale> {

        public PointRenderer() {
            super(getActivity(), getMap(), mClusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(SurveyedLocale item, MarkerOptions markerOptions) {
            markerOptions
                    .title(item.getDisplayName(getActivity()))
                    .snippet(item.getId());
            super.onBeforeClusterItemRendered(item, markerOptions);
        }

        @Override
        protected int getBucket(Cluster<SurveyedLocale> cluster) {
            return cluster.getSize();
        }

        @Override
        protected String getClusterText(int bucket) {
            return String.valueOf(bucket);
        }

    }

    private class DynamicallyAddMarkerTask extends AsyncTask<LatLngBounds, Void, Void> {

        @Override
        protected Void doInBackground(LatLngBounds... bounds) {
            mClusterManager.clearItems();
            for (SurveyedLocale item : mItems) {
                if (item.getPosition() != null && bounds[0].contains(item.getPosition())) {
                    mClusterManager.addItem(item);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mClusterManager.cluster();
        }
    }

}
