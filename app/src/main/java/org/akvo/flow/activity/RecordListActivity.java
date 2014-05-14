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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import org.akvo.flow.R;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.ui.fragment.MapFragment;
import org.akvo.flow.ui.fragment.SurveyedLocaleListFragment;
import org.akvo.flow.ui.fragment.SurveyedLocalesFragmentListener;
import org.akvo.flow.service.SurveyedLocaleSyncService;

public class RecordListActivity extends ActionBarActivity implements 
            SurveyedLocalesFragmentListener, ActionBar.TabListener {
    private static final String TAG = RecordListActivity.class.getSimpleName();
    
    public static final String EXTRA_SURVEY_GROUP = "survey_group";
    
    // Argument to be passed to list/map fragments
    public static final String EXTRA_SURVEY_GROUP_ID = "survey_group_id";
    
    private static final int POSITION_LIST = 0;
    private static final int POSITION_MAP  = 1;
    
    private SurveyGroup mSurveyGroup;
    private SurveyDbAdapter mDatabase;
    
    private ViewPager mPager;
    private TabsAdapter mAdapter;
    private String[] mTabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_list_activity);
        
        mTabs = getResources().getStringArray(R.array.records_activity_tabs);
        
        mAdapter = new TabsAdapter(getSupportFragmentManager());
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                getSupportActionBar().setSelectedNavigationItem(position);
            }
        });
        
        mSurveyGroup = (SurveyGroup) getIntent().getExtras().getSerializable(EXTRA_SURVEY_GROUP);
        setTitle(mSurveyGroup.getName());
        
        mDatabase = new SurveyDbAdapter(this);
        
        setupActionBar();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();
        // Delete empty Records, if any
        // TODO: For a more efficient cleanup, attempt to wipe ONLY the latest Record,
        // TODO: providing the id to RecordActivity, and reading it back on onActivityResult(...)
        mDatabase.deleteEmptyRecords();
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
    
    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        Tab listTab = actionBar.newTab()
                .setText(mTabs[POSITION_LIST])
                .setTabListener(this);
        Tab mapTab = actionBar.newTab()
                .setText(mTabs[POSITION_MAP])
                .setTabListener(this);
        
        actionBar.addTab(listTab);
        actionBar.addTab(mapTab);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.records_activity, menu);
        if (!mSurveyGroup.isMonitored()) {
            SubMenu subMenu = menu.findItem(R.id.more_submenu).getSubMenu();
            subMenu.removeItem(R.id.sync_records);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.search:
                return onSearchRequested();
            case R.id.new_record:
                // Create new record and return the ID
                String newLocaleId = mDatabase.createSurveyedLocale(mSurveyGroup.getId());
                onSurveyedLocaleSelected(newLocaleId);
                return true;
            case R.id.sync_records:
                Toast.makeText(RecordListActivity.this, R.string.syncing_records, 
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, SurveyedLocaleSyncService.class);
                intent.putExtra(SurveyedLocaleSyncService.SURVEY_GROUP, mSurveyGroup.getId());
                startService(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSurveyedLocaleSelected(String surveyedLocaleId) {
        // Start SurveysActivity, sending SurveyGroup + Record
        SurveyedLocale record = mDatabase.getSurveyedLocale(surveyedLocaleId);
        Intent intent = new Intent(this, RecordActivity.class);
        Bundle extras = new Bundle();
        extras.putSerializable(RecordActivity.EXTRA_SURVEY_GROUP, mSurveyGroup);
        extras.putString(RecordActivity.EXTRA_RECORD_ID, record.getId());
        intent.putExtras(extras);
        startActivity(intent);
    }
    
    class TabsAdapter extends FragmentPagerAdapter {
        
        public TabsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mTabs.length;
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
            Bundle extras = new Bundle();
            extras.putLong(EXTRA_SURVEY_GROUP_ID, mSurveyGroup.getId());
            switch (position) {
                case POSITION_LIST:
                    fragment =  new SurveyedLocaleListFragment();
                    break;
                case POSITION_MAP:
                    fragment = new MapFragment();
                    break;
            }
            
            if (fragment != null) {
                fragment.setArguments(extras);
            }
            
            return fragment;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return mTabs[position];
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
     * BroadcastReceiver to notify of records synchronisation. This should be
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
