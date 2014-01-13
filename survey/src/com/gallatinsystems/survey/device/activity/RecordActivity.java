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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.app.FlowApp;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.Survey;
import com.gallatinsystems.survey.device.domain.SurveyGroup;
import com.gallatinsystems.survey.device.domain.SurveyedLocale;
import com.gallatinsystems.survey.device.domain.User;
import com.gallatinsystems.survey.device.fragment.ResponseListFragment;
import com.gallatinsystems.survey.device.fragment.SurveyListFragment;
import com.gallatinsystems.survey.device.fragment.SurveyListFragment.SurveyListListener;
import com.gallatinsystems.survey.device.service.BootstrapService;
import com.gallatinsystems.survey.device.util.ConstantUtil;

public class RecordActivity extends ActionBarActivity implements SurveyListListener, TabListener {
    public static final String EXTRA_SURVEY_GROUP = "survey_group";
    public static final String EXTRA_RECORD = "record";
    
    //private static final String TAG = RecordActivity.class.getSimpleName();
    
    private static final int POSITION_SURVEYS = 0;
    private static final int POSITION_RESPONSES = 1;
    
    private User mUser;
    private SurveyedLocale mRecord;
    private SurveyGroup mSurveyGroup;
    private SurveyDbAdapter mDatabase;
    
    private ViewPager mPager;
    private TabsAdapter mAdapter;
    private View mRecordView;
    private TextView mRecordTextView;
    
    private String[] mTabs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_activity);
        
        mTabs = getResources().getStringArray(R.array.record_tabs);
        
        mRecordView = findViewById(R.id.record_view);
        mRecordTextView = (TextView) findViewById(R.id.record_text);
        mPager = (ViewPager)findViewById(R.id.pager);
        mAdapter = new TabsAdapter(getSupportFragmentManager());
        mPager.setAdapter(mAdapter);
        
        mDatabase = new SurveyDbAdapter(this);
        
        mSurveyGroup = (SurveyGroup) getIntent().getSerializableExtra(EXTRA_SURVEY_GROUP);
        mRecord = (SurveyedLocale) getIntent().getSerializableExtra(EXTRA_RECORD);
        setTitle(mSurveyGroup.getName());
        
        setupActionBar();
        displayRecord();
    }
    
    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        Tab listTab = actionBar.newTab()
                .setText(mTabs[POSITION_SURVEYS])
                .setTabListener(this);
        Tab mapTab = actionBar.newTab()
                .setText(mTabs[POSITION_RESPONSES])
                .setTabListener(this);
        
        actionBar.addTab(listTab);
        actionBar.addTab(mapTab);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();
        mUser = FlowApp.getApp().getUser();
        mAdapter.refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDatabase.open();
    }
    
    private void displayRecord() {
        // Display/Hide monitoring features
        if (mSurveyGroup.isMonitored() && mRecord != null) {
            mRecordView.setVisibility(View.VISIBLE);
            mRecordTextView.setText("Record: " + mRecord.getName() + " - " + mRecord.getId());
        } else {
            mRecordView.setVisibility(View.GONE);
        }
    }

    @Override
    public void startSurvey(Survey survey) {
        if (!BootstrapService.isProcessing) {
            Toast.makeText(this, R.string.pleasewaitforbootstrap, Toast.LENGTH_LONG).show();
        } else if (mUser == null) {
            // if the current user is null, we can't enter survey mode
            Toast.makeText(this, R.string.mustselectuser, Toast.LENGTH_LONG).show();
        } else {
            Intent i = new Intent(this, SurveyViewActivity.class);
            i.putExtra(ConstantUtil.USER_ID_KEY, mUser.getId());
            i.putExtra(ConstantUtil.SURVEY_ID_KEY, survey.getId());
            i.putExtra(ConstantUtil.SURVEY_GROUP_ID, mSurveyGroup.getId());
            if (mRecord != null) {
                // The record will automatically be managed in non monitored groups
                i.putExtra(ConstantUtil.SURVEYED_LOCALE_ID, mRecord.getId());
            }
            startActivity(i);
        }
    }
    
    class TabsAdapter extends FragmentPagerAdapter {
        
        public TabsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mTabs.length;
        }
        
        private String getFragmentTag(int pos){
            // Hell of a hack. This should be changed for a more reliable method
            return "android:switcher:" + R.id.pager + ":" + pos;
        }
        
        public void refresh() {
            SurveyListFragment surveyListFragment = (SurveyListFragment) getSupportFragmentManager().
                    findFragmentByTag(getFragmentTag(POSITION_SURVEYS));
            
            if (surveyListFragment != null && mSurveyGroup != null) {
                surveyListFragment.refresh();
            }
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case POSITION_SURVEYS:
                    return SurveyListFragment.instantiate(mSurveyGroup, mRecord);
                case POSITION_RESPONSES:
                    return ResponseListFragment.instantiate(mSurveyGroup, mRecord);
            }
            
            return null;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return mTabs[position];
        }
        
    }
    
    // ==================================== //
    // =========== Options Menu =========== //
    // ==================================== //
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.record_activity, menu);
        if (mRecord == null) {
            menu.removeItem(R.id.map_icon);
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
                startActivity(i);
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void viewOnMap() {
        final String uri = "geo:" + mRecord.getLatitude() + "," + mRecord.getLongitude() + "?q="
        		+ mRecord.getLatitude() + "," + mRecord.getLongitude() + "(" + mRecord.getName() + ")";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
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

}
