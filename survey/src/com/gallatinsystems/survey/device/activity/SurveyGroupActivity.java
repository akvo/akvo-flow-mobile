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

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.async.loader.SurveyGroupLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.Survey;
import com.gallatinsystems.survey.device.domain.SurveyGroup;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;
import com.gallatinsystems.survey.device.fragment.ResponseListFragment;
import com.gallatinsystems.survey.device.fragment.SurveyListFragment;
import com.gallatinsystems.survey.device.fragment.SurveyListFragment.SurveyListListener;
import com.gallatinsystems.survey.device.service.BootstrapService;
import com.gallatinsystems.survey.device.service.DataSyncService;
import com.gallatinsystems.survey.device.service.ExceptionReportingService;
import com.gallatinsystems.survey.device.service.LocationService;
import com.gallatinsystems.survey.device.service.SurveyDownloadService;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.StatusUtil;
import com.gallatinsystems.survey.device.util.ViewUtil;
import com.viewpagerindicator.TabPageIndicator;

public class SurveyGroupActivity extends ActionBarActivity implements SurveyListListener,
            LoaderCallbacks<Cursor>, OnNavigationListener {
    private static final String TAG = SurveyGroupActivity.class.getSimpleName();
    
    // Loader IDs
    private static final int ID_SURVEY_GROUP_LIST = 0;
    
    // Activity IDs
    private static final int ID_ACTIVITY_USERS       = 0;
    private static final int ID_SURVEYED_LOCALE_LIST = 1;
    
    private static final String[] TABS = {"SURVEYS", "RESPONSES"};// TODO: localized strings
    
    // Active user info
    private String mUserId;
    private String mUserName;
    
    private SurveyedLocale mLocale;
    
    private SurveyGroup mSurveyGroup;// Active SurveyGroup
    
    private List<SurveyGroup> mSurveyGroups;// Available surveyGroups
    private ListNavigationAdapter mNavigationAdapter;
    private ViewPager mPager;
    private TabsAdapter mAdapter;
    private TextView mUserTextView;
    private TextView mLocaleTextView;
    
    private SurveyDbAdapter mDatabase;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_group_activity);
        
        mUserTextView = (TextView) findViewById(R.id.username_text);
        mLocaleTextView = (TextView) findViewById(R.id.locale_text);
        mPager = (ViewPager)findViewById(R.id.pager);
        mAdapter = new TabsAdapter(getSupportFragmentManager());
        mPager.setAdapter(mAdapter);
        
        TabPageIndicator indicator = (TabPageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
        
        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();
        mSurveyGroups = new ArrayList<SurveyGroup>();
        
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        
        init();// No external storage will finish the application
        setupNavigationList();
        loadLastUser();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mSurveysSyncReceiver,
                new IntentFilter(getString(R.string.action_surveys_sync)));
        
        // A survey might have changed the name of the locale we're in,
        // thus we refresh it. TODO: You can do this way better...
        if (mLocale != null) {
            mLocale = mDatabase.getSurveyedLocale(mLocale.getId());
            displayRecord();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mSurveysSyncReceiver);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }
    
    /**
     * Checks if the user preference to persist logged-in users is set and, if
     * so, loads the last logged-in user from the DB
     */
    private void loadLastUser() {
        // First check if they want to keep users logged in
        String val = mDatabase.findPreference(ConstantUtil.USER_SAVE_SETTING_KEY);
        if (val != null && Boolean.parseBoolean(val)) {
            val = mDatabase.findPreference(ConstantUtil.LAST_USER_SETTING_KEY);
            if (val != null && val.trim().length() > 0) {
                Cursor cur = mDatabase.findUser(Long.valueOf(val));
                if (cur != null) {
                    mUserId = val;
                    mUserName = cur.getString(cur.getColumnIndexOrThrow(SurveyDbAdapter.DISP_NAME_COL));
                    cur.close();
                }
            }
        }
        displayUser();
    }
    
    private void init() {
        if (!StatusUtil.hasExternalStorage()) {
            ViewUtil.showConfirmDialog(R.string.checksd, R.string.sdmissing, this,
                false, 
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SurveyGroupActivity.this.finish();
                    }
                }, 
                null);
        } else {
            startService(new Intent(this, SurveyDownloadService.class));
            startService(new Intent(this, LocationService.class));
            startService(new Intent(this, BootstrapService.class));
            startService(new Intent(this, ExceptionReportingService.class));
            
            Intent i = new Intent(this, DataSyncService.class);
            i.putExtra(ConstantUtil.OP_TYPE_KEY, ConstantUtil.SEND);
            startService(i);
            
            getSupportLoaderManager().restartLoader(ID_SURVEY_GROUP_LIST, null, this);
        }
    }
    
    private void displayUser() {
        final String name = mUserName != null ? mUserName : "";
        mUserTextView.setText(getString(R.string.currentuser) + " " + name);
    }
    
    private void displayRecord() {
        // Enable/Disable monitoring features
        if (mSurveyGroup.isMonitored()) {
            mLocaleTextView.setVisibility(View.VISIBLE);
            if (mLocale != null) {
                mLocaleTextView.setText("Record: " + mLocale.getName() + " - " + mLocale.getId());
            } else {
                mLocaleTextView.setText("New Record");
            }
        } else {
            mLocaleTextView.setVisibility(View.GONE);
        }
    }
    
    private void setupNavigationList() {
        // Now the navigation...
        final ActionBar actionBar = getSupportActionBar();
        final Context context = actionBar.getThemedContext();
        
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        
        // Determine which survey group will be preselected
        int activeSurveyGroupId = SurveyGroup.ID_NONE;
        if (mSurveyGroup != null) {
            // If we already have a group selected, no doubt it's that one
            activeSurveyGroupId = mSurveyGroup.getId();
        } else {
            // Otherwise, try and get the cached survey group from the database
            String surveyGroupPref = mDatabase.findPreference(ConstantUtil.SURVEY_GROUP_KEY);
            if (!TextUtils.isEmpty(surveyGroupPref)) {
                activeSurveyGroupId = Integer.valueOf(surveyGroupPref);
            }
        }
        
        if (mSurveyGroups.size() > 0) {
            String[] names = new String[mSurveyGroups.size()];
            int currentIndex = 0;
            for (int i = 0; i < mSurveyGroups.size(); i++) {
                final SurveyGroup surveyGroup = mSurveyGroups.get(i);
                names[i] = surveyGroup.getName();
                // Select current survey group (if any)
                if (surveyGroup.getId() == activeSurveyGroupId) {
                    currentIndex = i;
                }
            }
    
            mNavigationAdapter = new ListNavigationAdapter(context, R.layout.spinner_item, android.R.id.text1, mSurveyGroups);
            mNavigationAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
            actionBar.setListNavigationCallbacks(mNavigationAdapter, this);
            actionBar.setSelectedNavigationItem(currentIndex);
        }
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        mSurveyGroup = mSurveyGroups.get(position);
        mLocale = null;// Start over again
        
        displayRecord();// Or hide it
        supportInvalidateOptionsMenu();
        mAdapter.refreshFragments();
        
        // Cache the survey group
        mDatabase.savePreference(ConstantUtil.SURVEY_GROUP_KEY, String.valueOf(mSurveyGroup.getId()));
        return true;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case ID_ACTIVITY_USERS:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        mUserId = bundle.getString(ConstantUtil.ID_KEY);
                        mUserName = bundle.getString(ConstantUtil.DISPLAY_NAME_KEY);
                    }
                } else if (resultCode == RESULT_CANCELED && intent != null) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null
                            && bundle.getBoolean(ConstantUtil.DELETED_SAVED_USER)) {
                        mUserId = null;
                        mUserName = null;
                    }
                }
                displayUser();
                mAdapter.onUserChanged();
                break;
            case ID_SURVEYED_LOCALE_LIST:
                if (resultCode == RESULT_OK) {
                    if (intent != null) {
                        String localeId = intent.getStringExtra(SurveyedLocalesActivity.EXTRA_SURVEYED_LOCALE_ID);
                        mLocale = mDatabase.getSurveyedLocale(localeId);
                    } else {
                        mLocale = null;
                    }
                    displayRecord();
                    mAdapter.refreshFragments();
                }
                break;
        }
    }

    @Override
    public void startSurvey(Survey survey) {
        // We might need to create a new record
        if (mLocale == null && mSurveyGroup.isMonitored()) {
            String newLocaleId = mDatabase.createSurveyedLocale(mSurveyGroup.getId());
            mLocale = mDatabase.getSurveyedLocale(newLocaleId);
            
            // we have to notify our fragments of this new locale
            mAdapter.refreshFragments();
        }
                
        Intent i = new Intent(this, SurveyViewActivity.class);
        i.putExtra(ConstantUtil.USER_ID_KEY, mUserId);
        i.putExtra(ConstantUtil.SURVEY_ID_KEY, survey.getId());
        i.putExtra(ConstantUtil.SURVEY_GROUP_ID, mSurveyGroup.getId());
        if (mSurveyGroup.isMonitored()) {
            // The locale will automatically be managed in non monitored groups
            i.putExtra(ConstantUtil.SURVEYED_LOCALE_ID, mLocale.getId());
        }
        startActivity(i);
    }
    
    class TabsAdapter extends FragmentPagerAdapter {
        private static final int POSITION_SURVEYS = 0;
        private static final int POSITION_RESPONSES = 1;
        
        ResponseListFragment mResponseListFragment;

        public TabsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return TABS.length;
        }
        
        public void onUserChanged() {
            Fragment surveyListFragment = getSupportFragmentManager().findFragmentByTag(getFragmentTag(POSITION_SURVEYS));
            if (surveyListFragment != null) {
                ((SurveyListFragment)surveyListFragment).setUserId(mUserId);
            }
        }
        
        private String getFragmentTag(int pos){
            // Hell of a hack. This should be changed for a more reliable method
            return "android:switcher:" + R.id.pager + ":" + pos;
        }
        
        public void refreshFragments() {
            SurveyListFragment surveyListFragment = (SurveyListFragment) getSupportFragmentManager().
                    findFragmentByTag(getFragmentTag(POSITION_SURVEYS));
            ResponseListFragment responseListFragment = (ResponseListFragment) getSupportFragmentManager().
                    findFragmentByTag(getFragmentTag(POSITION_RESPONSES));
            
            final String localeId = mLocale != null ? mLocale.getId() : null;
            
            if (surveyListFragment != null && mSurveyGroup != null) {
                surveyListFragment.refresh(mSurveyGroup, localeId);
            }
            if (responseListFragment != null) {
                responseListFragment.refresh(mSurveyGroup, localeId);
            }
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case POSITION_SURVEYS:
                    return SurveyListFragment.instantiate();
                case POSITION_RESPONSES:
                    return ResponseListFragment.instantiate();
            }
            
            return null;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return TABS[position];
        }
        
    }
    
    // ==================================== //
    // ========= Loader Callbacks ========= //
    // ==================================== //

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ID_SURVEY_GROUP_LIST:
                return new SurveyGroupLoader(this, mDatabase);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            Log.e(TAG, "onFinished() - Loader returned no data");
            return;
        }
        
        switch (loader.getId()) {
            case ID_SURVEY_GROUP_LIST:
                mSurveyGroups.clear();
                if (cursor.moveToFirst()) {
                    do {
                        mSurveyGroups.add(SurveyDbAdapter.getSurveyGroup(cursor));
                    } while (cursor.moveToNext());
                }
                cursor.close();
                setupNavigationList();
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    // ==================================== //
    // =========== Options Menu =========== //
    // ==================================== //
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mSurveyGroup != null && mSurveyGroup.isMonitored()) {
            getMenuInflater().inflate(R.menu.survey_group_activity_monitored, menu);
        } else {
            getMenuInflater().inflate(R.menu.survey_group_activity, menu);
        }
        
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.map_icon:
                viewOnMap();
                return true;
            case R.id.users:
                Intent i = new Intent(this, ListUserActivity.class);
                startActivityForResult(i, ID_ACTIVITY_USERS);
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.locales_icon:
                Intent intent = new Intent(this, SurveyedLocalesActivity.class);
                Bundle extras = new Bundle();
                extras.putInt(SurveyedLocalesActivity.EXTRA_SURVEY_GROUP_ID, mSurveyGroup.getId());
                intent.putExtras(extras);
                startActivityForResult(intent, ID_SURVEYED_LOCALE_LIST);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void viewOnMap() {
        // Will only show the position of existing records
        if (mLocale != null) {
            final String uri = "geo:" + mLocale.getLatitude() + "," + mLocale.getLongitude() + "?q="
            		+ mLocale.getLatitude() + "," + mLocale.getLongitude() + "(" + mLocale.getName() + ")";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(intent);
        } else {
            Toast.makeText(this, "No locale selected", Toast.LENGTH_SHORT).show();
        }
    }
    
    class ListNavigationAdapter extends ArrayAdapter<SurveyGroup> {

        public ListNavigationAdapter(Context context, int resource, int textViewResourceId,
                List<SurveyGroup> objects) {
            super(context, resource, textViewResourceId, objects);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            TextView nameView;
            TextView monitoredView;
            
            if (convertView == null) {
                view = getLayoutInflater().inflate(R.layout.spinner_item, parent, false);
            } else {
                view = convertView;
            }
            
            final SurveyGroup surveyGroup = getItem(position);
            
            nameView = (TextView) view.findViewById(R.id.text1);
            monitoredView = (TextView) view.findViewById(R.id.text2);
            nameView.setText(surveyGroup.getName());
            
            if (surveyGroup.isMonitored()) {
                monitoredView.setVisibility(View.VISIBLE);
            } else {
                monitoredView.setVisibility(View.GONE);
            }
            
            return view;
        }
        
    }

    /**
     * BroadcastReceiver to notify of surveys synchronisation. This should be
     * fired from SurveyDownloadService.
     */
    private BroadcastReceiver mSurveysSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Surveys have been synchronised. Refreshing data...");
            getSupportLoaderManager().restartLoader(ID_SURVEY_GROUP_LIST, null, SurveyGroupActivity.this);
        }
    };
    
}
