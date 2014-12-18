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

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

import org.akvo.flow.R;
import org.akvo.flow.ui.map.Feature;
import org.akvo.flow.ui.map.PointsFeature;
import org.akvo.flow.ui.map.PolygonFeature;
import org.akvo.flow.ui.map.PolylineFeature;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ViewUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlotActivity extends ActionBarActivity {
    private static final String JSON_TYPE = "type";
    private static final String JSON_GEOMETRY = "geometry";
    private static final String JSON_COORDINATES = "coordinates";
    private static final String JSON_FEATURES = "features";
    // private static final String JSON_PROPERTIES = "properties";
    private static final String TYPE_FEATURE = "Feature";
    private static final String TYPE_FEATURE_COLLECTION = "FeatureCollection";

    private static final String TAG = PlotActivity.class.getSimpleName();
    // private static final float ACCURACY_THRESHOLD = 20f;

    private List<Feature> mFeatures;// Saved features
    private Feature mCurrentFeature;// Ongoing feature

    private boolean mAllowPoints, mAllowLine, mAllowPolygon;

    private View mFeatureMenu;
    private View mClearPointBtn;
    private TextView mFeatureName;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plot_activity);

        mFeatures = new ArrayList<>();
        mMap = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map)).getMap();

        mFeatureMenu = findViewById(R.id.feature_menu);
        mFeatureName = (TextView)findViewById(R.id.feature_name);
        mClearPointBtn = findViewById(R.id.clear_point_btn);
        mClearPointBtn.setOnClickListener(mFeatureMenuListener);
        findViewById(R.id.add_point_btn).setOnClickListener(mFeatureMenuListener);
        findViewById(R.id.clear_feature_btn).setOnClickListener(mFeatureMenuListener);

        initMap();

        mAllowPoints = getIntent().getBooleanExtra(ConstantUtil.EXTRA_ALLOW_POINTS, true);
        mAllowLine = getIntent().getBooleanExtra(ConstantUtil.EXTRA_ALLOW_LINE, true);
        mAllowPolygon = getIntent().getBooleanExtra(ConstantUtil.EXTRA_ALLOW_POLYGON, true);
        String geoJSON = getIntent().getStringExtra(ConstantUtil.GEOSHAPE_RESULT);
        if (!TextUtils.isEmpty(geoJSON)) {
            load(geoJSON);
        }
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
            mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {
                }

                @Override
                public void onMarkerDrag(Marker marker) {
                }

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    for (Feature feature : mFeatures) {
                        if (feature.contains(marker)) {
                            feature.onDrag(marker);
                            break;
                        }
                    }
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

        mClearPointBtn.setEnabled(marker != null);
    }

    private void addPoint(LatLng point) {
        if (mCurrentFeature == null) {
            return;
        }
        mCurrentFeature.addPoint(point);
        mClearPointBtn.setEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.plot_activity, menu);
        if (!mAllowPoints) {
            menu.findItem(R.id.add_points).setVisible(false);
        }
        if (!mAllowLine) {
            menu.findItem(R.id.add_line).setVisible(false);
        }
        if (!mAllowPolygon) {
            menu.findItem(R.id.add_polygon).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mMap == null) {
            return false;
        }
        switch (item.getItemId()) {
            case R.id.add_line:
                selectFeature(new PolylineFeature(mMap), null);
                mFeatures.add(mCurrentFeature);
                break;
            case R.id.add_points:
                selectFeature(new PointsFeature(mMap), null);
                mFeatures.add(mCurrentFeature);
                break;
            case R.id.add_polygon:
                selectFeature(new PolygonFeature(mMap), null);
                mFeatures.add(mCurrentFeature);
                break;
            case R.id.save:
                Intent intent = new Intent();
                intent.putExtra(ConstantUtil.GEOSHAPE_RESULT, geoJson());
                setResult(RESULT_OK, intent);
                finish();
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
                    ViewUtil.showConfirmDialog(R.string.clear_point_title,
                            R.string.clear_point_text, PlotActivity.this, true,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mCurrentFeature.removePoint();
                                    selectFeature(mCurrentFeature, null);
                                }
                            });
                    break;
                case R.id.clear_feature_btn:
                    ViewUtil.showConfirmDialog(R.string.clear_feature_title,
                            R.string.clear_feature_text, PlotActivity.this, true,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mCurrentFeature.delete();
                                    selectFeature(null, null);
                                }
                            });
                    break;
            }
        }
    };

    private String geoJson() {
        JSONObject jObject = new JSONObject();
        try {
            jObject.put(JSON_TYPE, TYPE_FEATURE_COLLECTION);
            JSONArray jFeatures = new JSONArray();
            for (Feature feature : mFeatures) {
                if (feature.getPoints().isEmpty()) {
                    continue;
                }

                JSONObject jFeature = new JSONObject();
                jFeature.put(JSON_TYPE, TYPE_FEATURE);// Top level type (always "Feature")

                // Geometry
                JSONObject jGeometry = new JSONObject();
                jGeometry.put(JSON_TYPE, feature.geoGeometryType());

                // Coordinates
                JSONArray jCoordinates = new JSONArray();
                for (LatLng point : feature.getPoints()) {
                    JSONArray jCoordinate = new JSONArray();
                    jCoordinate.put(point.longitude);
                    jCoordinate.put(point.latitude);
                    jCoordinates.put(jCoordinate);
                }
                if (PolygonFeature.GEOMETRY_TYPE.equals(feature.geoGeometryType())) {
                    // Polygon features enclose coordinates in a 'LinearRing'
                    JSONArray ring = jCoordinates;
                    jCoordinates = new JSONArray();
                    jCoordinates.put(ring);
                }
                jGeometry.put(JSON_COORDINATES, jCoordinates);
                jFeature.put(JSON_GEOMETRY, jGeometry);
                // TODO: handle properties
                jFeatures.put(jFeature);
            }
            jObject.put(JSON_FEATURES, jFeatures);
        } catch (JSONException e) {
            Log.e(TAG, "geoJSON() - " + e.getMessage());
            return null;
        }
        return jObject.toString();
    }

    private void load(String geoJSON) {
        try {
            // Keep track of all points, so we can later center the map
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            JSONObject jObject = new JSONObject(geoJSON);
            JSONArray jFeatures = jObject.getJSONArray(JSON_FEATURES);
            if (jFeatures == null || jFeatures.length() == 0) {
                return;
            }
            for (int i=0; i<jFeatures.length(); i++) {
                JSONObject jFeature = jFeatures.getJSONObject(i);
                JSONObject jGeometry = jFeature.getJSONObject(JSON_GEOMETRY);
                JSONArray jCoordinates = jGeometry.getJSONArray(JSON_COORDINATES);
                if (PolygonFeature.GEOMETRY_TYPE.equals(jGeometry.getString(JSON_TYPE))) {
                    // Polygon features enclose coordinates in a 'LinearRing'
                    jCoordinates = jCoordinates.getJSONArray(0);
                }
                // Load point list
                List<LatLng> points = new ArrayList<>();
                for (int j=0; j<jCoordinates.length(); j++) {
                    JSONArray jPoint = jCoordinates.getJSONArray(j);
                    LatLng point = new LatLng(jPoint.getDouble(1), jPoint.getDouble(0));// [lon, lat] -> LatLng(lat, lon)
                    points.add(point);
                    builder.include(point);
                }

                Feature feature;
                switch (jGeometry.getString(JSON_TYPE)) {
                    case PointsFeature.GEOMETRY_TYPE:
                        feature = new PointsFeature(mMap);
                        break;
                    case PolylineFeature.GEOMETRY_TYPE:
                        feature = new PolylineFeature(mMap);
                        break;
                    case PolygonFeature.GEOMETRY_TYPE:
                        feature = new PolygonFeature(mMap);
                        break;
                    default:
                        continue;// Unknown geometry type.
                }
                feature.load(points);
                mFeatures.add(feature);
            }
            final LatLngBounds bounds = builder.build();
            mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 60));
                }
            });
        } catch (JSONException e) {
            Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
            Log.e(TAG, "geoJSON() - " + e.getMessage());
            // TODO: Remove features?
        }
    }

}
