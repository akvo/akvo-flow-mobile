/*
 *  Copyright (C) 2013-2016 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.data.database.SurveyInstanceStatus;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.domain.User;
import org.akvo.flow.ui.fragment.RecordListListener;
import org.akvo.flow.ui.fragment.ResponseListFragment;
import org.akvo.flow.ui.fragment.FormListFragment;
import org.akvo.flow.ui.fragment.FormListFragment.SurveyListListener;
import org.akvo.flow.service.BootstrapService;
import org.akvo.flow.util.ConstantUtil;

public class RecordActivity extends BackActivity implements SurveyListListener, TabListener,
        RecordListListener {
    public static final String EXTRA_SURVEY_GROUP = "survey_group";
    public static final String EXTRA_RECORD_ID = "record";

    private static final int POSITION_SURVEYS = 0;
    private static final int POSITION_RESPONSES = 1;

    private static final int REQUEST_FORM = 0;

    private User mUser;
    private SurveyedLocale mRecord;
    private SurveyGroup mSurveyGroup;
    private SurveyDbAdapter mDatabase;

    private ViewPager mPager;

    private String[] mTabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_activity);

        mTabs = getResources().getStringArray(R.array.record_tabs);
        mPager = (ViewPager) findViewById(R.id.pager);
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

    //TODO: replace deprecated Tabs
    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        Tab listTab = actionBar.newTab()
                .setText(mTabs[POSITION_SURVEYS])
                .setTabListener(this);
        Tab responsesTab = actionBar.newTab()
                .setText(mTabs[POSITION_RESPONSES])
                .setTabListener(this);

        actionBar.addTab(listTab);
        actionBar.addTab(responsesTab);
    }

    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();

        mUser = FlowApp.getApp().getUser();
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
        setTitle(mRecord.getDisplayName(this));
    }

    @Override
    public void onSurveyClick(final String surveyId) {
        if (BootstrapService.isProcessing) {
            Toast.makeText(this, R.string.pleasewaitforbootstrap, Toast.LENGTH_LONG).show();
            return;
        }
        Survey survey = mDatabase.getSurvey(surveyId);
        if (!survey.isHelpDownloaded()) {
            Toast.makeText(this, R.string.error_missing_cascade, Toast.LENGTH_LONG).show();
            return;
        }

        // Check if there are saved (non-submitted) responses for this Survey, and take the 1st one
        long[] instances = mDatabase.getFormInstances(mRecord.getId(), surveyId,
                SurveyInstanceStatus.SAVED);
        long instance = instances.length > 0 ?
                instances[0]
                :
                mDatabase.createSurveyRespondent(surveyId, survey.getVersion(), mUser,
                        mRecord.getId());

        Intent i = new Intent(this, FormActivity.class);
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
                    return FormListFragment.newInstance(mSurveyGroup, mRecord);
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
        getMenuInflater().inflate(R.menu.datapoint_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view_map:
                startActivity(new Intent(this, MapActivity.class)
                        .putExtra(ConstantUtil.SURVEYED_LOCALE_ID, mRecord.getId()));
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
