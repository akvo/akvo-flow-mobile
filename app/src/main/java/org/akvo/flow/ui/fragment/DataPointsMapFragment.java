/*
 *  Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.ClusterManager;

import org.akvo.flow.R;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.data.loader.SurveyedLocalesLoader;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class DataPointsMapFragment extends SupportMapFragment
        implements LoaderCallbacks<List<SurveyedLocale>>, OnInfoWindowClickListener, OnMapReadyCallback {

    public static final int MAP_ZOOM_LEVEL = 10;
    public static final String MAP_OPTIONS = "MapOptions";

    private SurveyGroup mSurveyGroup;
    private SurveyDbAdapter mDatabase;
    private RecordListListener mListener;
    private List<SurveyedLocale> mItems;

    @Nullable
    private ProgressBar progressBar;

    @Nullable
    private GoogleMap mMap;

    private ClusterManager<SurveyedLocale> mClusterManager;

    public static DataPointsMapFragment newInstance(SurveyGroup surveyGroup) {
        DataPointsMapFragment fragment = new DataPointsMapFragment();
        Bundle args = new Bundle();
        args.putSerializable(ConstantUtil.EXTRA_SURVEY_GROUP, surveyGroup);
        GoogleMapOptions options = new GoogleMapOptions();
        options.zOrderOnTop(true);
        args.putParcelable(MAP_OPTIONS, options);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItems = new ArrayList<>();
        mSurveyGroup = (SurveyGroup) getArguments()
                .getSerializable(ConstantUtil.EXTRA_SURVEY_GROUP);
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
            throw new ClassCastException(
                    activity.toString() + " must implement SurveyedLocalesFragmentListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            View.inflate(getActivity(), R.layout.map_progress_bar, viewGroup);
            progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        }
        return view;
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
            centerMap();
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
                bounds.including(
                        new LatLng(ne.latitude + latDst / scale, ne.longitude + lonDst / scale))
                        .including(new LatLng(sw.latitude - latDst / scale,
                                ne.longitude + lonDst / scale))
                        .including(new LatLng(sw.latitude - latDst / scale,
                                sw.longitude - lonDst / scale))
                        .including(new LatLng(ne.latitude + latDst / scale,
                                sw.longitude - lonDst / scale));

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
    private void centerMap() {
        if (mMap == null) {
            return; // Not ready yet
        }

        LatLng position = null;
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
            showProgress();
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.sync_records:
                if (mListener != null && mSurveyGroup != null) {
                    showProgress();
                    mListener.onSyncRecordsSyncRequested(mSurveyGroup.getId());
                }
                return true;
        }
        return false;
    }

    private void showProgress() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        final String surveyedLocaleId = marker.getSnippet();
        mListener.onRecordSelected(surveyedLocaleId);
    }

    // ==================================== //
    // ========= Loader Callbacks ========= //
    // ==================================== //

    @Override
    public Loader<List<SurveyedLocale>> onCreateLoader(int id, Bundle args) {
        long surveyId = mSurveyGroup != null ? mSurveyGroup.getId() : SurveyGroup.ID_NONE;
        return new SurveyedLocalesLoader(getActivity(), surveyId, ConstantUtil.ORDER_BY_NONE);
    }

    @Override
    public void onLoadFinished(Loader<List<SurveyedLocale>> loader,
            List<SurveyedLocale> surveyedLocales) {
        hideProgress();
        if (surveyedLocales == null) {
            Timber.w("onFinished() - Loader returned no data");
            return;
        }
        mItems.clear();
        mItems.addAll(surveyedLocales);
        cluster();
    }

    private void hideProgress() {
        if (progressBar != null) {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<SurveyedLocale>> loader) {
        //EMPTY
    }
}
