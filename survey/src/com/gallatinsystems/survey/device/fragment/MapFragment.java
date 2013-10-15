package com.gallatinsystems.survey.device.fragment;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends SupportMapFragment implements LocationListener, LoaderCallbacks<Cursor> {
    private static final String TAG = MapFragment.class.getSimpleName();
    
    private LocationManager mLocationManager;
    
    private int mSurveyGroupId;
    private SurveyDbAdapter mDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurveyGroupId = getArguments().getInt(SurveyedLocalesActivity.EXTRA_SURVEY_GROUP_ID);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        mDatabase = new SurveyDbAdapter(getActivity());
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        refresh();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mDatabase.close();
        mLocationManager.removeUpdates(this);
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
        GoogleMap map = getMap();
        
        for (SurveyedLocale surveyedLocale : surveyedLocales) {
            map.addMarker(new MarkerOptions()
                    .position(new LatLng(surveyedLocale.getLatitude(), surveyedLocale.getLongitude()))
                    .title(surveyedLocale.getId()));
        }
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
    
    // ==================================== //
    // ======== Location Callbacks ======== //
    // ==================================== //

    @Override
    public void onLocationChanged(Location location) {
        // a single location is all we need
        mLocationManager.removeUpdates(this);
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        // TODO: center map
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

}
