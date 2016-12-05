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
import android.content.Context;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.akvo.flow.R;
import org.akvo.flow.activity.RecordActivity;
import org.akvo.flow.activity.SurveyActivity;
import org.akvo.flow.async.loader.SurveyedLocaleLoader;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.util.ConstantUtil;

//TODO: separate single data point and multiple into different classes for clarity
public class MapFragment extends SupportMapFragment
    implements LoaderCallbacks<Cursor>, OnInfoWindowClickListener, OnMapReadyCallback {

    private static final String TAG = MapFragment.class.getSimpleName();
    public static final int MAP_ZOOM_LEVEL = 10;

    private SurveyGroup mSurveyGroup;
    private SurveyDbAdapter mDatabase;
    private RecordListListener mListener;

    private String mRecordId; // If set, load a single record
    private List<SurveyedLocale> mItems;
    private boolean mSingleRecord = false;

    @Nullable
    private GoogleMap mMap;

    private ClusterManager<SurveyedLocale> mClusterManager;

    public static MapFragment newInstance(SurveyGroup surveyGroup, String dataPointId) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putSerializable(SurveyActivity.EXTRA_SURVEY_GROUP, surveyGroup);
        args.putString(RecordActivity.EXTRA_RECORD_ID, dataPointId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItems = new ArrayList<>();

        mSurveyGroup = (SurveyGroup) getArguments().getSerializable(SurveyActivity.EXTRA_SURVEY_GROUP);
        mRecordId = getArguments().getString(RecordActivity.EXTRA_RECORD_ID);
        mSingleRecord = !TextUtils.isEmpty(mRecordId);// Single datapoint mode?
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = (RecordListListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement SurveyedLocalesFragmentListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDatabase = new SurveyDbAdapter(getActivity());
        getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        configMap();
        refresh();
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
        LatLngBounds newBounds =
            bounds.including(new LatLng(ne.latitude + latDst / scale, ne.longitude + lonDst / scale))
                  .including(new LatLng(sw.latitude - latDst / scale, ne.longitude + lonDst / scale))
                  .including(new LatLng(sw.latitude - latDst / scale, sw.longitude - lonDst / scale))
                  .including(new LatLng(ne.latitude + latDst / scale, sw.longitude - lonDst / scale));

        mClusterManager.clearItems();
        for (SurveyedLocale item : mItems) {
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
    private void centerMap(@Nullable SurveyedLocale record) {
        if (mMap == null) {
            return; // Not ready yet
        }

        LatLng position = null;

        if (record != null && record.getLatitude() != null && record.getLongitude() != null) {
            // Center the map in the data point
            position = new LatLng(record.getLatitude(), record.getLongitude());
        } else {
            // When multiple points are shown, center the map in user's location
            LocationManager manager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
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
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, MAP_ZOOM_LEVEL));
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mapView = super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.map_fragment, container, false);
        FrameLayout layout = (FrameLayout) v.findViewById(R.id.map_container);

        layout.addView(mapView, 0);

        return v;
    }

    public void refresh(SurveyGroup surveyGroup) {
        mSurveyGroup = surveyGroup;
        refresh();
    }

    /**
     * Ideally, we should build a ContentProvider, so this notifications are handled
     * automatically, and the loaders restarted without this explicit dependency.
     */
    public void refresh() {
        if (isResumed()) {
            if (mSingleRecord) {
                // Just get it from the DB
                updateSingleRecord();
            } else {
                getLoaderManager().restartLoader(0, null, this);
            }
        }
    }

    private void updateSingleRecord() {
        SurveyedLocale record = mDatabase.getSurveyedLocale(mRecordId);
        if (mMap != null && record != null && record.getLatitude() != null && record.getLongitude() != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(new LatLng(record.getLatitude(), record.getLongitude()))
                                              .title(record.getDisplayName(getActivity()))
                                              .snippet(record.getId()));
            centerMap(record);
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
        long surveyId = mSurveyGroup != null ? mSurveyGroup.getId() : SurveyGroup.ID_NONE;
        return new SurveyedLocaleLoader(getActivity(), mDatabase, surveyId, ConstantUtil.ORDER_BY_NONE);
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
    private static class PointRenderer extends DefaultClusterRenderer<SurveyedLocale> {

        private final WeakReference<Context> activityContextWeakRef;

        public PointRenderer(GoogleMap map, Context context, ClusterManager<SurveyedLocale> clusterManager) {
            super(context, map, clusterManager);
            this.activityContextWeakRef = new WeakReference<>(context);
        }

        @Override
        protected void onBeforeClusterItemRendered(SurveyedLocale item, MarkerOptions markerOptions) {
            Context context = activityContextWeakRef.get();
            if (context != null) {
                markerOptions.title(item.getDisplayName(context)).snippet(item.getId());
            }
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
}
