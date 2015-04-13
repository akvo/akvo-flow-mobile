/*
 *  Copyright (C) 2013-2014 Stichting Akvo (Akvo Foundation)
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
import android.widget.TextView;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceStatus;
import org.akvo.flow.domain.Instance;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.domain.User;
import org.akvo.flow.ui.fragment.MapFragment;
import org.akvo.flow.ui.fragment.RecordListListener;
import org.akvo.flow.ui.fragment.ResponseListFragment;
import org.akvo.flow.ui.fragment.SurveyListFragment;
import org.akvo.flow.ui.fragment.SurveyListFragment.SurveyListListener;
import org.akvo.flow.service.BootstrapService;
import org.akvo.flow.util.ConstantUtil;

public class RecordActivity extends ActionBarActivity implements SurveyListListener, TabListener,
        RecordListListener {
    public static final String EXTRA_SURVEY_GROUP = "survey_group";
    public static final String EXTRA_RECORD_ID = "record";
    
    //private static final String TAG = RecordActivity.class.getSimpleName();
    
    private static final int POSITION_SURVEYS = 0;
    private static final int POSITION_RESPONSES = 1;
    private static final int POSITION_MAP = 2;

    private static final int REQUEST_FORM = 0;

    private User mUser;
    private Instance mInstance;
    private SurveyedLocale mRecord;
    private SurveyGroup mSurveyGroup;
    private SurveyDbAdapter mDatabase;
    
    private ViewPager mPager;
    private TextView mRecordTextView;
    
    private String[] mTabs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_activity);
        
        mTabs = getResources().getStringArray(R.array.record_tabs);
        mRecordTextView = (TextView) findViewById(R.id.record_text);
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(new TabsAdapter(getSupportFragmentManager()));
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
        Tab responsesTab = actionBar.newTab()
                .setText(mTabs[POSITION_RESPONSES])
                .setTabListener(this);
        Tab mapTab = actionBar.newTab()
                .setText(mTabs[POSITION_MAP])
                .setTabListener(this);
        
        actionBar.addTab(listTab);
        actionBar.addTab(responsesTab);
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
        mInstance = FlowApp.getApp().getInstance();
        // Record might have changed while answering a registration survey
        String recordId = getIntent().getStringExtra(EXTRA_RECORD_ID);
        mRecord = mDatabase.getSurveyedLocale(recordId);
        displayRecord();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDatabase.close();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FORM && resultCode == RESULT_OK) {
            finish();
        }
    }

    private void displayRecord() {
        mRecordTextView.setText(mRecord.getDisplayName(this) + ", " + mRecord.getId());
    }

    @Override
    public void onSurveyClick(final String surveyId) {
        if (BootstrapService.isProcessing) {
            Toast.makeText(this, R.string.pleasewaitforbootstrap, Toast.LENGTH_LONG).show();
            return;
        }
        if (!mDatabase.getSurvey(surveyId, mInstance.getAppId()).isHelpDownloaded()) {
            Toast.makeText(this, R.string.error_missing_cascade, Toast.LENGTH_LONG).show();
            return;
        }

        // Check if there are saved (non-submitted) responses for this Survey, and take the 1st one
        long[] instances = mDatabase.getSurveyInstances(mRecord.getId(), surveyId, mInstance.getAppId(),
                SurveyInstanceStatus.SAVED);
        long instance = instances.length > 0 ? instances[0]
                : mDatabase.createSurveyRespondent(surveyId, mUser, mRecord.getId(), mInstance.getAppId());

        Intent i = new Intent(this, SurveyActivity.class);
        i.putExtra(ConstantUtil.USER_ID_KEY, mUser.getId());
        i.putExtra(ConstantUtil.SURVEY_ID_KEY, surveyId);
        i.putExtra(ConstantUtil.SURVEY_GROUP, mSurveyGroup);
        i.putExtra(ConstantUtil.SURVEYED_LOCALE_ID, mRecord.getId());
        i.putExtra(ConstantUtil.RESPONDENT_ID_KEY, instance);
        startActivityForResult(i, REQUEST_FORM);
    }

    class TabsAdapter extends FragmentPagerAdapter {
        
        public TabsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mTabs.length;
        }
        
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case POSITION_SURVEYS:
                    return SurveyListFragment.instantiate(mSurveyGroup, mRecord);
                case POSITION_RESPONSES:
                    return ResponseListFragment.instantiate(mSurveyGroup, mRecord);
                case POSITION_MAP:
                    Fragment fragment = new MapFragment();
                    Bundle args = new Bundle();
                    args.putString(RecordActivity.EXTRA_RECORD_ID, mRecord.getId());
                    fragment.setArguments(args);
                    return fragment;
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
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

    @Override
    public void onRecordSelected(String recordId) {
    }
}
