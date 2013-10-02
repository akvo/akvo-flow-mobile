package com.gallatinsystems.survey.device.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.async.loader.SurveyGroupListLoader;
import com.gallatinsystems.survey.device.async.loader.base.AsyncResult;
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
            LoaderCallbacks<AsyncResult<List<SurveyGroup>>>, OnNavigationListener {
    private static final String TAG = SurveyGroupActivity.class.getSimpleName();
    
    // Loader IDs
    private static final int ID_SURVEY_GROUP_LIST = 0;
    
    // Activity IDs
    private static final int ID_ACTIVITY_USERS = 0;
    
    private static final String[] TABS = {"Surveys", "Responses"};// TODO: localized strings
    
    // Active user info
    private String mUserId;
    private String mUserName;
    
    private SurveyGroup mSurveyGroup;// Active SurveyGroup
    private List<SurveyGroup> mSurveyGroups;// Available surveyGroups
    private ArrayAdapter<String> mNavigationAdapter;
    private ViewPager mPager;
    private TabsAdapter mAdapter;
    private TextView mUserTextView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_group_activity);
        
        mUserTextView = (TextView) findViewById(R.id.username_text);
        mPager = (ViewPager)findViewById(R.id.pager);
        mAdapter = new TabsAdapter(getSupportFragmentManager());
        mPager.setAdapter(mAdapter);
        
        TabPageIndicator indicator = (TabPageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
        
        mSurveyGroups = new ArrayList<SurveyGroup>();
        
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        
        init();// No external storage will finish the application
        display();// Configure navigation and display surveys
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
        }
    }
    
    private int getActiveSurveyGroupId() {
        return mSurveyGroup != null ? mSurveyGroup.getId() : SurveyGroup.ID_NONE;
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
        
        public void onSurveyGroupChanged() {
            Fragment surveyListFragment = getSupportFragmentManager().findFragmentByTag(getFragmentTag(POSITION_SURVEYS));
            if (surveyListFragment != null)
            ((SurveyListFragment)surveyListFragment).setSurveyGroup(getActiveSurveyGroupId());
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case POSITION_SURVEYS:
                    return SurveyListFragment.instantiate(getActiveSurveyGroupId());
                case POSITION_RESPONSES:
                    return ResponseListFragment.instantiate(getActiveSurveyGroupId());
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
    public Loader<AsyncResult<List<SurveyGroup>>> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ID_SURVEY_GROUP_LIST:
                return new SurveyGroupListLoader(this);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<AsyncResult<List<SurveyGroup>>> loader,
            AsyncResult<List<SurveyGroup>> result) {
        Exception e = result.getException();
        if (e != null) {
            Log.e(TAG, e.getMessage());
            return;
        }
        
        switch (loader.getId()) {
            case ID_SURVEY_GROUP_LIST:
                if (result.getData() != null) {
                    mSurveyGroups = result.getData();
                    display();
                } else {
                    Log.e(TAG, "onFinished() - Loader returned no data");
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<AsyncResult<List<SurveyGroup>>> loader) {
        loader.reset();
    }

    // ==================================== //
    // =========== Options Menu =========== //
    // ==================================== //
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.survey_group_activity, menu);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
}
