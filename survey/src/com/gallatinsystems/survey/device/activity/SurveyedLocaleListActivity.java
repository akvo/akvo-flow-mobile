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

package com.gallatinsystems.survey.device.activity;

import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.List;

import org.apache.http.HttpException;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.async.loader.SurveyedLocaleLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter.SurveyedLocaleAttrs;
import com.gallatinsystems.survey.device.domain.SurveyGroup;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;
import com.gallatinsystems.survey.device.exception.PersistentUncaughtExceptionHandler;
import com.gallatinsystems.survey.device.parser.json.SurveyedLocaleParser;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.HttpUtil;
import com.gallatinsystems.survey.device.util.PropertyUtil;
import com.gallatinsystems.survey.device.util.StatusUtil;

public class SurveyedLocaleListActivity extends ActionBarActivity implements
        LoaderCallbacks<Cursor>,
        OnItemClickListener, LocationListener {
    public static final String TAG = SurveyedLocaleListActivity.class.getSimpleName();
    public static final String EXTRA_SURVEY_GROUP_ID = "survey_group_id";
    public static final String EXTRA_SURVEYED_LOCALE_ID = "surveyed_locale_id";
    
    // API parameters
    private static final String SURVEYED_LOCALE_SERVICE_PATH = "/surveyedlocale?surveyGroupId=";
    private static final String PARAM_PHONE_NUMBER = "&phoneNumber=";
    private static final String PARAM_IMEI = "&imei=";
    private static final String PARAM_ONLY_COUNT = "&checkAvailable=";

    private LocationManager mLocationManager;
    private double mLatitude = 0.0d;
    private double mLongitude = 0.0d;
    private static final double RADIUS = 100000d;// Meters

    private int mSurveyGroupId;
    private String mServerBase;
    private SurveyDbAdapter mDatabase;

    private ListView mListView;
    private CursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.surveyed_locale_list_activity);

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mSurveyGroupId = getIntent().getIntExtra(EXTRA_SURVEY_GROUP_ID, SurveyGroup.ID_NONE);

        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();
        mServerBase = getServerBase();
        
        mAdapter = new SurveyedLocaleListAdapter(this);
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        mListView.setEmptyView(findViewById(android.R.id.empty));
    }
    
    
    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();
        // try to find out where we are
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String provider = mLocationManager.getBestProvider(criteria, true);
        if (provider != null) {
            Location loc = mLocationManager.getLastKnownLocation(provider);
            if (loc != null) {
                mLatitude = loc.getLatitude();
                mLongitude = loc.getLongitude();
            }
        }
        loadData();
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(this);
        mDatabase.close();
    }

    private void loadData() {
        getSupportLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.surveyed_locale_list_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_record:
                setResult(RESULT_OK);// Return null locale (new record will be created)
                finish();
                return true;
            case R.id.map_results:
                // TODO
                Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.sync_records:
                Toast.makeText(SurveyedLocaleListActivity.this, "Syncing...", Toast.LENGTH_LONG).show();
                new DownloadRecordsTask().execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        final String localeId = cursor.getString(cursor.getColumnIndexOrThrow(
                SurveyedLocaleAttrs.SURVEYED_LOCALE_ID));
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SURVEYED_LOCALE_ID, localeId);
        setResult(RESULT_OK, intent);
        finish();
    }
            
    private String getServerBase() {
        String serverBase = mDatabase.findPreference(ConstantUtil.SERVER_SETTING_KEY);
        if (serverBase != null && serverBase.trim().length() > 0) {
            serverBase = getResources().getStringArray(R.array.servers)[Integer
                    .parseInt(serverBase)];
        } else {
            serverBase = new PropertyUtil(getResources()).
                    getProperty(ConstantUtil.SERVER_BASE);
        }
            
        return serverBase;
    }
    
    // ==================================== //
    // ========= Loader Callbacks ========= //
    // ==================================== //

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SurveyedLocaleLoader(this, mDatabase, mSurveyGroupId, mLatitude, mLongitude, RADIUS);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            Log.e(TAG, "onFinished() - Loader returned no data");
            return;
        }

        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
    
    // ==================================== //
    // ========= Location Callbacks ========= //
    // ==================================== //

    @Override
    public void onLocationChanged(Location location) {
        // a single location is all we need
        mLocationManager.removeUpdates(this);
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        loadData();
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

    /**
     * List Adapter to bind the Surveyed Locales into the list items
     */
    class SurveyedLocaleListAdapter extends CursorAdapter {

        public SurveyedLocaleListAdapter(Context context) {
            super(context, null, false);
        }

        private String getDistanceText(SurveyedLocale surveyedLocale) {
            StringBuilder builder = new StringBuilder("Distance: ");
            
            if (mLatitude != 0.0d || mLongitude != 0.0d) {
                float[] results = new float[1];
                Location.distanceBetween(mLatitude, mLongitude, surveyedLocale.getLatitude(), surveyedLocale.getLongitude(), results);
                final double distance = results[0];
            
                // default: no decimal point, km as unit
                DecimalFormat df = new DecimalFormat("#.#");
                String unit = "km";
                Double factor = 0.001; // convert from meters to km
    
                // for distances smaller than 1 km, use meters as unit
                if (distance < 1000.0) {
                    factor = 1.0;
                    unit = "m";
                    df = new DecimalFormat("#"); // only whole meters
                }
                double dist = distance * factor;
                builder.append(df.format(dist)).append(" ").append(unit);
            } else {
                builder.append("Unknown");
            }
            
            return builder.toString();
        }

        @Override
        public void bindView(View view, Context context, Cursor c) {
            TextView idView = (TextView) view.findViewById(R.id.locale_id);
            TextView distanceView = (TextView) view.findViewById(R.id.locale_distance);
            final SurveyedLocale surveyedLocale = SurveyDbAdapter.getSurveyedLocale(c);

            idView.setText(surveyedLocale.getId());
            distanceView.setText(getDistanceText(surveyedLocale));
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(SurveyedLocaleListActivity.this);
            return inflater.inflate(R.layout.surveyed_locale_item, null);
        }

    }
    
    /**
     * Worker thread to download the records.
     */
    class DownloadRecordsTask extends AsyncTask<Void, Void, Integer> {
        
        @Override
        protected Integer doInBackground(Void... params) {
            String response = null;
            int syncedRecords = 0;
            try {
                final String url = mServerBase
                        + SURVEYED_LOCALE_SERVICE_PATH + mSurveyGroupId
                        + PARAM_PHONE_NUMBER + URLEncoder.encode(StatusUtil.getPhoneNumber(SurveyedLocaleListActivity.this), "UTF-8")
                        + PARAM_IMEI + URLEncoder.encode(StatusUtil.getImei(SurveyedLocaleListActivity.this), "UTF-8")
                        + PARAM_ONLY_COUNT + false;
                response = HttpUtil.httpGet(url);
                if (response != null) {
                    List<SurveyedLocale> surveyedLocales = new SurveyedLocaleParser().parseList(response);
                    if (surveyedLocales != null) {
                        SurveyDbAdapter database = new SurveyDbAdapter(SurveyedLocaleListActivity.this).open();
                        database.syncSurveyedLocales(surveyedLocales);
                        database.close();
                        syncedRecords = surveyedLocales.size();
                    }
                }
            } catch (HttpException e) {
                Log.e(TAG, "Server returned an unexpected response", e);
                PersistentUncaughtExceptionHandler.recordException(e);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                PersistentUncaughtExceptionHandler.recordException(e);
            }
            
            return syncedRecords;
        }
        
        @Override
        protected void onPostExecute(Integer result) {
            Toast.makeText(SurveyedLocaleListActivity.this, "Synced " + result.toString()  + " records", 
                    Toast.LENGTH_LONG).show();
            // Refresh the list with synced records
            loadData();
        }
        
    }
    
}
