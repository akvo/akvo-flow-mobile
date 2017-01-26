/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.fragment;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.akvo.flow.R;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.util.ConstantUtil;

public class SingleDataPointMapFragment extends SupportMapFragment implements OnMapReadyCallback {

    public static final int MAP_ZOOM_LEVEL = 10;

    private String mRecordId;
    private SurveyedLocale item;

    @Nullable
    private GoogleMap mMap;

    private boolean justCreated;

    public static SingleDataPointMapFragment newInstance(String dataPointId) {
        SingleDataPointMapFragment fragment = new SingleDataPointMapFragment();
        Bundle args = new Bundle();
        args.putString(ConstantUtil.EXTRA_RECORD_ID, dataPointId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRecordId = getArguments().getString(ConstantUtil.EXTRA_RECORD_ID);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getMapAsync(this);
        justCreated = true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        loadItem();
    }

    /**
     * Center the map in the given record's coordinates. If no record is provided,
     * the user's location will be used.
     */
    private void centerMap(@Nullable SurveyedLocale record) {
        if (mMap == null) {
            return; // Not ready yet
        }
        mMap.setMyLocationEnabled(true);
        if (record != null && record.getLatitude() != null && record.getLongitude() != null) {
            centerMapOnDataPoint(record);
        } else {
            centerMapOnUserLocation();
        }
    }

    private void centerMapOnUserLocation() {
        LatLng position = null;
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

    private void centerMapOnDataPoint(@NonNull SurveyedLocale record) {
        LatLng position = new LatLng(record.getLatitude(), record.getLongitude());
        if (position != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, MAP_ZOOM_LEVEL));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!justCreated) {
            loadItem();
        }
        justCreated = false;
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
    public void loadItem() {
        SurveyDbAdapter mDatabase = new SurveyDbAdapter(getActivity());
        mDatabase.open();
        item = mDatabase.getSurveyedLocale(mRecordId);
        mDatabase.close();
        if (mMap != null && item != null && item.getLatitude() != null
                && item.getLongitude() != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(item.getLatitude(), item.getLongitude()))
                    .title(item.getDisplayName(getActivity()))
                    .snippet(item.getId()));
            centerMap(item);
        }
    }

}
