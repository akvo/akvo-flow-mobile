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
import java.util.List;

import org.apache.http.HttpException;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;
import com.gallatinsystems.survey.device.exception.PersistentUncaughtExceptionHandler;
import com.gallatinsystems.survey.device.fragment.MapFragment;
import com.gallatinsystems.survey.device.fragment.SurveyedLocaleListFragment;
import com.gallatinsystems.survey.device.fragment.SurveyedLocalesFragmentListener;
import com.gallatinsystems.survey.device.parser.json.SurveyedLocaleParser;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.HttpUtil;
import com.gallatinsystems.survey.device.util.PropertyUtil;
import com.gallatinsystems.survey.device.util.StatusUtil;

public class SurveyedLocalesActivity extends ActionBarActivity implements SurveyedLocalesFragmentListener {
    
    public static final String TAG = SurveyedLocalesActivity.class.getSimpleName();
    public static final String EXTRA_SURVEY_GROUP_ID = "survey_group_id";
    public static final String EXTRA_SURVEYED_LOCALE_ID = "surveyed_locale_id";
    
    // API parameters
    private static final String SURVEYED_LOCALE_SERVICE_PATH = "/surveyedlocale?surveyGroupId=";
    private static final String PARAM_PHONE_NUMBER = "&phoneNumber=";
    private static final String PARAM_IMEI = "&imei=";
    private static final String PARAM_ONLY_COUNT = "&checkAvailable=";
    
    private int mSurveyGroupId;
    private String mServerBase;
    
    private SurveyDbAdapter mDatabase;
    
    // False for MapFragment. List by default.
    private boolean mListResults = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.surveyed_locales_activity);
        
        mSurveyGroupId = getIntent().getExtras().getInt(EXTRA_SURVEY_GROUP_ID);

        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();
        mServerBase = getServerBase();
        
        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }
            
            display();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mDatabase.close();
    }
    
    private void display() {
        Fragment fragment = mListResults ? new SurveyedLocaleListFragment() : MapFragment.newInstance();
        // Pass the arguments on to let the fragment retrieve the survey group
        fragment.setArguments(getIntent().getExtras());
            
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // Add the fragment to the 'fragment_container' FrameLayout
        transaction.replace(R.id.fragment_container, fragment);
        //transaction.addToBackStack(null);
        transaction.commit();
    }
    
    private void switchFragment() {
        mListResults = !mListResults;
        supportInvalidateOptionsMenu();
        display();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.surveyed_locale_list_activity, menu);
        // We must hide list/map results option depending on the current fragment
        if (mListResults) {
            menu.removeItem(R.id.list_results);
        } else {
            menu.removeItem(R.id.map_results);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                // TODO
                Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.new_record:
                setResult(RESULT_OK);// Return null locale (new record will be created)
                finish();
                return true;
            case R.id.sync_records:
                Toast.makeText(SurveyedLocalesActivity.this, "Syncing...", Toast.LENGTH_LONG).show();
                new DownloadRecordsTask().execute();
                return true;
            case R.id.list_results:
            case R.id.map_results:
                switchFragment();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSurveyedLocaleSelected(String surveyedLocaleId) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SURVEYED_LOCALE_ID, surveyedLocaleId);
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
                        + PARAM_PHONE_NUMBER + URLEncoder.encode(StatusUtil.getPhoneNumber(SurveyedLocalesActivity.this), "UTF-8")
                        + PARAM_IMEI + URLEncoder.encode(StatusUtil.getImei(SurveyedLocalesActivity.this), "UTF-8")
                        + PARAM_ONLY_COUNT + false;
                response = HttpUtil.httpGet(url);
                if (response != null) {
                    List<SurveyedLocale> surveyedLocales = new SurveyedLocaleParser().parseList(response);
                    if (surveyedLocales != null) {
                        SurveyDbAdapter database = new SurveyDbAdapter(SurveyedLocalesActivity.this).open();
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
            Toast.makeText(SurveyedLocalesActivity.this, "Synced " + result.toString()  + " records", 
                    Toast.LENGTH_LONG).show();
            // Refresh the list with synced records
            if (mListResults) {
                SurveyedLocaleListFragment fragment = (SurveyedLocaleListFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
                fragment.refresh();
            } else {
                // TODO   
            }
        }
        
    }

}
