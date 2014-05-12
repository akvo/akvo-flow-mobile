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

package org.akvo.flow.activity;

import android.content.Intent;
import android.database.Cursor;
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

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceStatus;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.domain.User;
import org.akvo.flow.ui.fragment.ResponseListFragment;
import org.akvo.flow.ui.fragment.ResponsesDialogFragment;
import org.akvo.flow.ui.fragment.ResponsesDialogFragment.ResponsesDialogListener;
import org.akvo.flow.ui.fragment.SurveyListFragment;
import org.akvo.flow.ui.fragment.SurveyListFragment.SurveyListListener;
import org.akvo.flow.service.BootstrapService;
import org.akvo.flow.util.ConstantUtil;

public class RecordActivity extends ActionBarActivity implements SurveyListListener, TabListener,
        ResponsesDialogListener{
    public static final String EXTRA_SURVEY_GROUP = "survey_group";
    public static final String EXTRA_RECORD_ID = "record";
    
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
        mAdapter = new TabsAdapter(getSupportFragmentManager());
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                getSupportActionBar().setSelectedNavigationItem(position);
            }
        });
        
        mDatabase = new SurveyDbAdapter(this);
        
        mSurveyGroup = (SurveyGroup) getIntent().getSerializableExtra(EXTRA_SURVEY_GROUP);
        setTitle(mSurveyGroup.getName());
        
        setupActionBar();
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

        // Delete empty SurveyInstances, if any
        // TODO: For a more efficient cleanup, attempt to wipe ONLY the latest SurveyInstance,
        // TODO: providing the id to SurveyActivity, and reading it back on onActivityResult(...)
        mDatabase.deleteEmptySurveyInstances();

        mUser = FlowApp.getApp().getUser();
        // Record might have changed while answering a registration survey
        String recordId = getIntent().getStringExtra(EXTRA_RECORD_ID);
        mRecord = mDatabase.getSurveyedLocale(recordId);
        displayRecord();
        mAdapter.refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDatabase.close();
    }

    private void displayRecord() {
        // Display/Hide monitoring features
        if (mSurveyGroup.isMonitored() && mRecord != null) {
            mRecordView.setVisibility(View.VISIBLE);
            mRecordTextView.setText("Record: " + mRecord.getName() + ", " + mRecord.getId());
        } else {
            mRecordView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSurveyClick(String surveyId) {
        if (BootstrapService.isProcessing) {
            Toast.makeText(this, R.string.pleasewaitforbootstrap, Toast.LENGTH_LONG).show();
            return;
        }

        final String recordId = mRecord != null ? mRecord.getId() : null;

        // Check if there are ongoing (non-submitted) responses for this Survey
        boolean createNew = true;
        Cursor c =  mDatabase.getSurveyInstances(recordId, surveyId, SurveyInstanceStatus.SAVED);
        if (c != null) {
            createNew = c.getCount() == 0;
            c.close();
        }

        if (createNew) {
            startSurvey(surveyId);
        } else {
            // Display ResponsesDialogFragment
            ResponsesDialogFragment dialogFragment = ResponsesDialogFragment.instantiate(
                    mSurveyGroup, surveyId, recordId);
            dialogFragment.show(getSupportFragmentManager(), "responses");
        }
    }

    private void startSurvey(String surveyId) {
        String recordId = mRecord != null ? mRecord.getId() : null;
        if (recordId == null) {
            // Non-monitored group. Create a new Record for the locale
            recordId = mDatabase.createSurveyedLocale(mSurveyGroup.getId());
        }
        long surveyInstanceId = mDatabase.createSurveyRespondent(surveyId,
                String.valueOf(mUser.getId()), recordId);

        startSurvey(surveyId, recordId, surveyInstanceId);
    }

    private void startSurvey(String surveyId, String recordId, long surveyInstanceId) {
        Intent i = new Intent(this, SurveyActivity.class);
        i.putExtra(ConstantUtil.USER_ID_KEY, mUser.getId());
        i.putExtra(ConstantUtil.SURVEY_ID_KEY, surveyId);
        i.putExtra(ConstantUtil.SURVEY_GROUP, mSurveyGroup);
        i.putExtra(ConstantUtil.SURVEYED_LOCALE_ID, recordId);
        i.putExtra(ConstantUtil.RESPONDENT_ID_KEY, surveyInstanceId);
        startActivity(i);
    }

    // **************************** //
    // * DialogFragment callbacks * //
    // **************************** //

    @Override
    public void onResponseClick(String surveyId, String recordId, long surveyInstanceId) {
        startSurvey(surveyId, recordId, surveyInstanceId);
    }

    @Override
    public void onNewResponse(String surveyId) {
        startSurvey(surveyId);
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
            case android.R.id.home:
                onBackPressed();
                return true;
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
