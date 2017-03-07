/*
 *  Copyright (C) 2015-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.activity;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.akvo.flow.R;
import org.akvo.flow.data.loader.SurveyedLocaleItemLoader;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.util.ConstantUtil;

public class MapActivity extends BackActivity implements OnMapReadyCallback,
        LoaderManager.LoaderCallbacks<SurveyedLocale> {

    public static final int MAP_ZOOM_LEVEL = 10;

    private SurveyedLocale datapoint;
    private String datapointId;
    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        datapointId = getIntent().getStringExtra(ConstantUtil.SURVEYED_LOCALE_ID);
        loadDataPoint();
    }

    private void loadDataPoint() {
        getSupportLoaderManager().restartLoader(0, null, this);
    }

    private void updateTitle() {
        if (datapoint != null) {
            setTitle(datapoint.getDisplayName(this));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        googleMap.setMyLocationEnabled(true);
        loadDataPoint();
    }

    private void addDataPointMarker() {
        if (map != null) {
            map.clear();
            if (isDataPointLocationAvailable()) {
                map.addMarker(new MarkerOptions()
                        .position(new LatLng(datapoint.getLatitude(), datapoint.getLongitude()))
                        .title(datapoint.getDisplayName(this))
                        .snippet(datapoint.getId()));
            }
        }
    }

    private boolean isDataPointLocationAvailable() {
        return datapoint != null && datapoint.getLatitude() != null
                && datapoint.getLongitude() != null;
    }

    /**
     * Center the map in the given record's coordinates. If no record is provided,
     * the user's location will be used.
     */
    private void centerMap() {
        if (isDataPointLocationAvailable()) {
            centerMapOnDataPoint(datapoint);
        } else {
            centerMapOnUserLocation();
        }
    }

    private void centerMapOnUserLocation() {
        LatLng position = null;
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        String provider = manager.getBestProvider(criteria, true);
        if (provider != null) {
            Location location = manager.getLastKnownLocation(provider);
            if (location != null) {
                position = new LatLng(location.getLatitude(), location.getLongitude());
            }
        }
        positionMap(position);
    }

    private void positionMap(LatLng position) {
        if (position != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, MAP_ZOOM_LEVEL));
        }
    }

    private void centerMapOnDataPoint(@NonNull SurveyedLocale record) {
        LatLng position = new LatLng(record.getLatitude(), record.getLongitude());
        positionMap(position);
    }

    @Override
    public Loader<SurveyedLocale> onCreateLoader(int id, Bundle args) {
        return new SurveyedLocaleItemLoader(this, datapointId);
    }

    @Override
    public void onLoadFinished(Loader<SurveyedLocale> loader, SurveyedLocale data) {
        this.datapoint  = data;
        updateTitle();
        addDataPointMarker();
        centerMap();
    }

    @Override
    public void onLoaderReset(Loader<SurveyedLocale> loader) {
        // EMPTY
    }
}
