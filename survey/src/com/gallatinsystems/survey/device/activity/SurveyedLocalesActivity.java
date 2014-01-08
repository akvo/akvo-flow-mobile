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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.SurveyGroup;
import com.gallatinsystems.survey.device.fragment.MapFragment;
import com.gallatinsystems.survey.device.fragment.SurveyedLocaleListFragment;
import com.gallatinsystems.survey.device.fragment.SurveyedLocalesFragmentListener;
import com.gallatinsystems.survey.device.service.SurveyedLocaleSyncService;

public class SurveyedLocalesActivity extends ActionBarActivity implements 
            SurveyedLocalesFragmentListener, ActionBar.TabListener {
    private static final String TAG = SurveyedLocalesActivity.class.getSimpleName();
    
    public static final String EXTRA_SURVEY_GROUP_ID = "survey_group_id";
    public static final String EXTRA_SURVEYED_LOCALE_ID = "surveyed_locale_id";
    
    private SurveyGroup mSurveyGroup;
    
    private SurveyDbAdapter mDatabase;
    
    private ViewPager mPager;
    private TabsAdapter mAdapter;
    
    // False for MapFragment. List by default.
    private boolean mListResults = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.surveyed_locales_activity);
        
        mPager = (ViewPager)findViewById(R.id.pager);
        mAdapter = new TabsAdapter(getSupportFragmentManager());
        mPager.setAdapter(mAdapter);
        
        final int surveyGroupId = getIntent().getExtras().getInt(EXTRA_SURVEY_GROUP_ID);
        
        if (savedInstanceState != null) {
            mListResults = savedInstanceState.getBoolean("list_results", true);
        }

        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();
        
        mSurveyGroup = loadSurveyGroup(surveyGroupId);
        
        setTitle(mSurveyGroup.getName());
        
        setupTabs();
        //display();
    }
    
    private void setupTabs() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        Tab listTab = actionBar.newTab()
                .setText("RECORDS")
                .setTabListener(this);
        Tab mapTab = actionBar.newTab()
                .setText("MAP")
                .setTabListener(this);
        
        actionBar.addTab(listTab);
        actionBar.addTab(mapTab);
    }
    
    private SurveyGroup loadSurveyGroup(int surveyGroupId) {
        // C'mon... you can do this better
        Cursor cursor = mDatabase.getSurveyGroup(surveyGroupId);
        cursor.moveToFirst();
        return SurveyDbAdapter.getSurveyGroup(cursor);
    }
    
    protected void onSaveInstanceState (Bundle outState) {
        outState.putBoolean("list_results", mListResults);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();
        registerReceiver(surveyedLocalesSyncReceiver,
                new IntentFilter(getString(R.string.action_locales_sync)));
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mDatabase.close();
        unregisterReceiver(surveyedLocalesSyncReceiver);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String surveyedLocaleId = intent.getDataString();
            onSurveyedLocaleSelected(surveyedLocaleId);
        }
    }
    
    /*
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
    */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.surveyed_locales_activity, menu);
        // We must hide list/map results option depending on the current fragment
        SubMenu submenu = menu.findItem(R.id.more_submenu).getSubMenu();
        if (mListResults) {
            submenu.removeItem(R.id.list_results);
        } else {
            submenu.removeItem(R.id.map_results);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                return onSearchRequested();
            case R.id.new_record:
                // Create new record and return the ID
                String newLocaleId = mDatabase.createSurveyedLocale(mSurveyGroup.getId());
                onSurveyedLocaleSelected(newLocaleId);
                return true;
            case R.id.sync_records:
                Toast.makeText(SurveyedLocalesActivity.this, R.string.syncing_records, 
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, SurveyedLocaleSyncService.class);
                intent.putExtra(SurveyedLocaleSyncService.SURVEY_GROUP, mSurveyGroup.getId());
                startService(intent);
                return true;
            case R.id.list_results:
            case R.id.map_results:
                //switchFragment();
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
    
    class TabsAdapter extends FragmentPagerAdapter {
        static final int POSITION_LIST = 0;
        static final int POSITION_MAP  = 1;
    
        final String[] TABS = {"RECORDS", "MAP"};
        
        public TabsAdapter(FragmentManager fm) {
            super(fm);
            //TABS = getResources().getStringArray(R.array.survey_group_tabs);
        }

        @Override
        public int getCount() {
            return TABS.length;
        }
        
        private Fragment getFragment(int pos){
            // Hell of a hack. This should be changed for a more reliable method
            String tag = "android:switcher:" + R.id.pager + ":" + pos;
            return getSupportFragmentManager().findFragmentByTag(tag);
        }
        
        public void refreshFragments() {
            SurveyedLocaleListFragment listFragment = (SurveyedLocaleListFragment) getFragment(POSITION_LIST);
            MapFragment mapFragment = (MapFragment) getFragment(POSITION_MAP);
            
            if (listFragment != null && mSurveyGroup != null) {
                listFragment.refresh();
            }
            if (mapFragment != null) {
                mapFragment.refresh();
            }
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;
            switch (position) {
                case POSITION_LIST:
                    fragment =  new SurveyedLocaleListFragment();
                    break;
                case POSITION_MAP:
                    fragment = new MapFragment();
                    break;
            }
            
            if (fragment != null) {
                fragment.setArguments(getIntent().getExtras());
            }
            
            return fragment;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return TABS[position];
        }
        
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction fragmentTransaction) {
        mPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction fragmentTransaction) {
    }
    
    /**
     * BroadcastReceiver to notify of locales synchronisation. This should be
     * fired from SurveyedLocalesSyncService.
     */
    private BroadcastReceiver surveyedLocalesSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "New Records have been synchronised. Refreshing fragments...");
            mAdapter.refreshFragments();
        }
    };

}
