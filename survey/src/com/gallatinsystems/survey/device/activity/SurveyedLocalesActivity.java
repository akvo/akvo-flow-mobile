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


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.fragment.MapFragment;
import com.gallatinsystems.survey.device.fragment.SurveyedLocaleListFragment;
import com.gallatinsystems.survey.device.fragment.SurveyedLocalesFragmentListener;
import com.gallatinsystems.survey.device.service.SurveyedLocaleSyncService;

public class SurveyedLocalesActivity extends ActionBarActivity implements SurveyedLocalesFragmentListener {
    public static final String EXTRA_SURVEY_GROUP_ID = "survey_group_id";
    public static final String EXTRA_SURVEYED_LOCALE_ID = "surveyed_locale_id";
    
    private int mSurveyGroupId;
    
    private SurveyDbAdapter mDatabase;
    
    // False for MapFragment. List by default.
    private boolean mListResults = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.surveyed_locales_activity);
        
        mSurveyGroupId = getIntent().getExtras().getInt(EXTRA_SURVEY_GROUP_ID);
        
        if (savedInstanceState != null) {
            mListResults = savedInstanceState.getBoolean("list_results", true);
        }

        mDatabase = new SurveyDbAdapter(this);
        
        display();
    }
    
    protected void onSaveInstanceState (Bundle outState) {
        outState.putBoolean("list_results", mListResults);
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
    
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String surveyedLocaleId = intent.getDataString();
            onSurveyedLocaleSelected(surveyedLocaleId);
        }
    }
    
    private void display() {
        Fragment fragment = mListResults ? new SurveyedLocaleListFragment() : new MapFragment();
        // Pass the arguments on to let the fragment retrieve the survey group
        fragment.setArguments(getIntent().getExtras());
            
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // Add the fragment to the 'fragment_container' FrameLayout
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
    
    private void switchFragment() {
        mListResults = !mListResults;
        supportInvalidateOptionsMenu();
        display();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.surveyed_locales_activity, menu);
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
                return onSearchRequested();
            case R.id.new_record:
                setResult(RESULT_OK);// Return null locale (new record will be created)
                finish();
                return true;
            case R.id.sync_records:
                Toast.makeText(SurveyedLocalesActivity.this, "Syncing...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, SurveyedLocaleSyncService.class);
                intent.putExtra(SurveyedLocaleSyncService.SURVEY_GROUP, mSurveyGroupId);
                startService(intent);
                //new DownloadRecordsTask().execute();
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

    /*
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
            MapFragment fragment = (MapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            fragment.refresh();
        }
    }
    */
        
}
