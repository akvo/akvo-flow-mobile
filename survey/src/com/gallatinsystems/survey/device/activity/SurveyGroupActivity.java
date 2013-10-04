package com.gallatinsystems.survey.device.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.async.loader.SurveyGroupLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.SurveyGroup;
import com.gallatinsystems.survey.device.fragment.ResponseListFragment;
import com.gallatinsystems.survey.device.fragment.SurveyListFragment;
import com.gallatinsystems.survey.device.service.BootstrapService;
import com.gallatinsystems.survey.device.service.DataSyncService;
import com.gallatinsystems.survey.device.service.ExceptionReportingService;
import com.gallatinsystems.survey.device.service.LocationService;
import com.gallatinsystems.survey.device.service.SurveyDownloadService;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.StatusUtil;
import com.gallatinsystems.survey.device.util.ViewUtil;
import com.viewpagerindicator.TabPageIndicator;

public class SurveyGroupActivity extends ActionBarActivity implements 
            LoaderCallbacks<Cursor>, OnNavigationListener {
    private static final String TAG = SurveyGroupActivity.class.getSimpleName();
    
    // Loader IDs
    private static final int ID_SURVEY_GROUP_LIST = 0;
    
    // Activity IDs
    private static final int ID_ACTIVITY_USERS       = 0;
    private static final int ID_SURVEYED_LOCALE_LIST = 1;
    
    private static final String[] TABS = {"Surveys", "Responses"};// TODO: localized strings
    
    // Active user info
    private String mUserId;
    private String mUserName;
    
    private String mLocaleId;
    
    private SurveyGroup mSurveyGroup;// Active SurveyGroup
    private List<SurveyGroup> mSurveyGroups;// Available surveyGroups
    private ArrayAdapter<String> mNavigationAdapter;
    private ViewPager mPager;
    private TabsAdapter mAdapter;
    private TextView mUserTextView;
    private TextView mLocaleTextView;
    
    private MenuItem mLocalesIcon;
    
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
        display();// Configure navigation and display surveys
    }
    
    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mSurveysSyncReceiver,
                new IntentFilter(getString(R.string.action_surveys_sync)));
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
        mLocaleTextView.setText("Record: " + (mLocaleId != null ? mLocaleId : "New Record"));
    }
    
    private void display() {
        displayUser();
        
        // Now the navigation...
        final ActionBar actionBar = getSupportActionBar();
        final Context context = actionBar.getThemedContext();
        
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        
        final int activeSurveyGroupId = mSurveyGroup != null ? mSurveyGroup.getId() : 0;
        
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
    
            mNavigationAdapter = new ArrayAdapter<String>(context, R.layout.spinner_item, android.R.id.text1, names);
            mNavigationAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
            actionBar.setListNavigationCallbacks(mNavigationAdapter, this);
            actionBar.setSelectedNavigationItem(currentIndex);
        }
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        mSurveyGroup = mSurveyGroups.get(position);
        
        mLocaleId = null;// Start over again
        //TODO: cleanup
        displayRecord();
        // Enable/Disable monitoring features
        if (mSurveyGroup.isMonitored()) {
            // Show locale selection icon
            mLocalesIcon.setVisible(true);
            mLocaleTextView.setVisibility(View.VISIBLE);
        } else {
            mLocalesIcon.setVisible(false);
            mLocaleTextView.setVisibility(View.GONE);
        }
        
        mAdapter.onSurveyGroupChanged();
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
                    mLocaleId = intent != null ? 
                            intent.getStringExtra(SurveyedLocaleListActivity.EXTRA_SURVEYED_LOCALE_ID)
                            : null;
                    displayRecord();
                    mAdapter.onSurveyedLocaleChange();
                }
            break;
                
        }
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
            if (surveyListFragment != null)
            ((SurveyListFragment)surveyListFragment).setUserId(mUserId);
        }
        
        private String getFragmentTag(int pos){
            return "android:switcher:" + R.id.pager + ":" + pos;
        }
        
        // TODO: DRY - Cleanup these methods
        public void onSurveyGroupChanged() {
            SurveyListFragment surveyListFragment = (SurveyListFragment) getSupportFragmentManager().
                    findFragmentByTag(getFragmentTag(POSITION_SURVEYS));
            ResponseListFragment responseListFragment = (ResponseListFragment) getSupportFragmentManager().
                    findFragmentByTag(getFragmentTag(POSITION_RESPONSES));
            if (surveyListFragment != null && mSurveyGroup != null) {
                surveyListFragment.refresh(mSurveyGroup);
            }
            if (responseListFragment != null) {
                responseListFragment.refresh(mSurveyGroup, mLocaleId);
            }
        }
        
        public void onSurveyedLocaleChange() {
            SurveyListFragment surveyListFragment = (SurveyListFragment) getSupportFragmentManager().
                    findFragmentByTag(getFragmentTag(POSITION_SURVEYS));
            ResponseListFragment responseListFragment = (ResponseListFragment) getSupportFragmentManager().
                    findFragmentByTag(getFragmentTag(POSITION_RESPONSES));
            
            if (surveyListFragment != null && mSurveyGroup != null) {
                surveyListFragment.refresh(mSurveyGroup, mLocaleId);
            }
            if (responseListFragment != null) {
                responseListFragment.refresh(mSurveyGroup, mLocaleId);
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
                display();
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
        getMenuInflater().inflate(R.menu.survey_group_activity, menu);
        mLocalesIcon = menu.findItem(R.id.locales_icon);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.users:
                Intent i = new Intent(this, ListUserActivity.class);
                startActivityForResult(i, ID_ACTIVITY_USERS);
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.locales_icon:
                Intent intent = new Intent(this, SurveyedLocaleListActivity.class);
                Bundle extras = new Bundle();
                extras.putInt(SurveyedLocaleListActivity.EXTRA_SURVEY_GROUP_ID, mSurveyGroup.getId());
                intent.putExtras(extras);
                startActivityForResult(intent, ID_SURVEYED_LOCALE_LIST);
                return true;
            default:
                return super.onOptionsItemSelected(item);
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
