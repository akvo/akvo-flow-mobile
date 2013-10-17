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

package com.gallatinsystems.survey.device.fragment;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.activity.SurveyedLocalesActivity;
import com.gallatinsystems.survey.device.async.loader.SurveyedLocaleLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends SupportMapFragment implements LoaderCallbacks<Cursor>, OnInfoWindowClickListener {
    private static final String TAG = MapFragment.class.getSimpleName();
    
    private int mSurveyGroupId;
    private SurveyDbAdapter mDatabase;
    private SurveyedLocalesFragmentListener mListener;
    
    private GoogleMap mMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurveyGroupId = getArguments().getInt(SurveyedLocalesActivity.EXTRA_SURVEY_GROUP_ID);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = (SurveyedLocalesFragmentListener)activity;
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
            getLoaderManager().restartLoader(0, null, this);
        }
    }
    
    private void displaySurveyedLocales(List<SurveyedLocale> surveyedLocales) {
        if (mMap != null) {
            mMap.clear();
            for (SurveyedLocale surveyedLocale : surveyedLocales) {
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(surveyedLocale.getLatitude(), surveyedLocale.getLongitude()))
                        .title(surveyedLocale.getName())
                        .snippet(surveyedLocale.getId()));
            }
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        final String surveyedLocaleId = marker.getSnippet();
        mListener.onSurveyedLocaleSelected(surveyedLocaleId);
    }

    // ==================================== //
    // ========= Loader Callbacks ========= //
    // ==================================== //

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SurveyedLocaleLoader(getActivity(), mDatabase, mSurveyGroupId);
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
