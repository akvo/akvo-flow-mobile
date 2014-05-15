/*
 *  Copyright (C) 2013 Stichting Akvo (Akvo Foundation)
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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends SupportMapFragment implements LoaderCallbacks<Cursor>, OnInfoWindowClickListener {
    private static final String TAG = MapFragment.class.getSimpleName();

    private long mSurveyGroupId;
    private String mRecordId; // If set, load a single record
    private SurveyDbAdapter mDatabase;
    private RecordListListener mListener;

    private boolean mSingleRecord = false;
    
    private GoogleMap mMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            
            LocationManager manager = (LocationManager)getActivity()
                    .getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            String provider = manager.getBestProvider(criteria, true);
            Location location = manager.getLastKnownLocation(provider);
            if (location != null) {
                LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 10));
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();
        refresh();
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
                if (mMap != null && record != null) {
                    mMap.clear();
                    displayRecord(record);
                }
            } else {
                getLoaderManager().restartLoader(0, null, this);
            }
        }
    }
    
    private void displaySurveyedLocales(List<SurveyedLocale> surveyedLocales) {
        if (mMap != null) {
            mMap.clear();
            for (SurveyedLocale surveyedLocale : surveyedLocales) {
                displayRecord(surveyedLocale);
            }
        }
    }

    private void displayRecord(SurveyedLocale record) {
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(record.getLatitude(), record.getLongitude()))
                .title(record.getName())
                .snippet(record.getId()));
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
        
        List<SurveyedLocale> surveyedLocales = new ArrayList<SurveyedLocale>();
        if (cursor.moveToFirst()) {
            do {
                surveyedLocales.add(SurveyDbAdapter.getSurveyedLocale(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        displaySurveyedLocales(surveyedLocales);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
    
}
