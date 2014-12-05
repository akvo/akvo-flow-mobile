/*
 *  Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.activity;

import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.akvo.flow.R;
import org.akvo.flow.ui.map.Feature;
import org.akvo.flow.ui.map.PointsFeature;
import org.akvo.flow.ui.map.PolygonFeature;
import org.akvo.flow.ui.map.PolylineFeature;

import java.util.ArrayList;
import java.util.List;

public class PlotActivity extends ActionBarActivity {
    // public static final String EXTRA__PLOT_ID = "plot_id";

    private static final String TAG = PlotActivity.class.getSimpleName();
    // private static final float ACCURACY_THRESHOLD = 20f;

    private List<Feature> mFeatures;// Saved features
    private Feature mCurrentFeature;// Ongoing feature

    private View mFeatureMenu;
    private View mClearPointBtn;
    private TextView mFeatureName;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plot_activity);

        mFeatures = new ArrayList<Feature>();
        mMap = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map)).getMap();

        mFeatureMenu = findViewById(R.id.feature_menu);
        mFeatureName = (TextView)findViewById(R.id.feature_name);
        mClearPointBtn = findViewById(R.id.clear_point_btn);
        mClearPointBtn.setOnClickListener(mFeatureMenuListener);
        findViewById(R.id.add_point_btn).setOnClickListener(mFeatureMenuListener);
        findViewById(R.id.clear_feature_btn).setOnClickListener(mFeatureMenuListener);

        initMap();
    }

    private void initMap() {
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    addPoint(latLng);
                }
            });
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    // We need to figure out which feature contains this marker.
                    // For now, a naive linear search will do the trick
                    // TODO: Select marker index, extending the feature from this marker
                    for (Feature feature : mFeatures) {
                        if (feature.contains(marker)) {
                            selectFeature(feature, marker);
                            break;
                        }
                    }
                    return false;
                }
            });
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    selectFeature(null, null);
                }
            });
        }
    }

    private void selectFeature(Feature feature, Marker marker) {
        // Remove current selection, if any
        if (mCurrentFeature != null && mCurrentFeature != feature) {
            mCurrentFeature.setSelected(false, null);
        }

        mCurrentFeature = feature;
        if (mCurrentFeature != null) {
            mCurrentFeature.setSelected(true, marker);
            mFeatureName.setText(mCurrentFeature.getTitle());
            mFeatureMenu.setVisibility(View.VISIBLE);
        } else {
            mFeatureMenu.setVisibility(View.GONE);
        }

        mClearPointBtn.setVisibility(marker != null ? View.VISIBLE : View.GONE);
    }

    private void addPoint(LatLng point) {
        if (mCurrentFeature == null) {
            return;
        }
        mCurrentFeature.addPoint(point);
        mClearPointBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.plot_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mMap == null) {
            return false;
        }
        switch (item.getItemId()) {
            case R.id.add_line:
                Toast.makeText(this, "Adding new line", Toast.LENGTH_LONG).show();
                selectFeature(new PolylineFeature(mMap), null);
                mFeatures.add(mCurrentFeature);
                break;
            case R.id.add_points:
                Toast.makeText(this, "Adding points", Toast.LENGTH_LONG).show();
                selectFeature(new PointsFeature(mMap), null);
                mFeatures.add(mCurrentFeature);
                break;
            case R.id.add_polygon:
                Toast.makeText(this, "Adding new area", Toast.LENGTH_LONG).show();
                selectFeature(new PolygonFeature(mMap), null);
                mFeatures.add(mCurrentFeature);
                break;
            case R.id.save:
                selectFeature(null, null);
                Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show();
                break;
            case R.id.clear:
                mFeatures.clear();
                mMap.clear();
                mCurrentFeature = null;
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private View.OnClickListener mFeatureMenuListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCurrentFeature == null) {
                return;
            }

            switch (v.getId()) {
                case R.id.add_point_btn:
                    Location location = mMap.getMyLocation();
                    // TODO: Check accuracy
                    if (location != null) {
                        addPoint(new LatLng(location.getLatitude(), location.getLongitude()));
                    }
                    break;
                case R.id.clear_point_btn:
                    mCurrentFeature.removePoint();
                    selectFeature(mCurrentFeature, null);
                    break;
                case R.id.clear_feature_btn:
                    mCurrentFeature.delete();
                    selectFeature(null, null);
                    break;
            }
        }
    };

}
