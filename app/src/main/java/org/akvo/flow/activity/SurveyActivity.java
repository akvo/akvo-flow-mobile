/*
 *  Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.data.database.SurveyInstanceStatus;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.User;
import org.akvo.flow.domain.apkupdate.ApkUpdateStore;
import org.akvo.flow.domain.apkupdate.GsonMapper;
import org.akvo.flow.domain.apkupdate.ViewApkData;
import org.akvo.flow.service.BootstrapService;
import org.akvo.flow.service.DataSyncService;
import org.akvo.flow.service.SurveyDownloadService;
import org.akvo.flow.service.SurveyedDataPointSyncService;
import org.akvo.flow.service.TimeCheckService;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.fragment.DatapointsFragment;
import org.akvo.flow.ui.fragment.DrawerFragment;
import org.akvo.flow.ui.fragment.RecordListListener;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.StatusUtil;
import org.akvo.flow.util.ViewUtil;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class SurveyActivity extends AppCompatActivity implements RecordListListener,
        DrawerFragment.DrawerListener, DatapointsFragment.DatapointFragmentListener {

    private static final String DATA_POINTS_FRAGMENT_TAG = "datapoints_fragment";
    private static final String DRAWER_FRAGMENT_TAG = "f";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @BindView(R.id.add_data_point_fab)
    FloatingActionButton addDataPointFab;

    private SurveyDbAdapter mDatabase;

    @Nullable
    private SurveyGroup mSurveyGroup;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerFragment mDrawer;
    private Navigator navigator = new Navigator();
    private Prefs prefs;
    private ApkUpdateStore apkUpdateStore;

    private long selectedSurveyId;

    /**
     * BroadcastReceiver to notify of surveys synchronisation. This should be
     * fired from {@link SurveyDownloadService}.
     */
    private final BroadcastReceiver mSurveysSyncReceiver = new SurveySyncBroadcastReceiver(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_activity);

        ButterKnife.bind(this);

        initializeToolBar();

        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();

        prefs = new Prefs(getApplicationContext());
        selectedSurveyId = prefs.getLong(Prefs.KEY_SURVEY_GROUP_ID, SurveyGroup.ID_NONE);
        if (selectedSurveyId != SurveyGroup.ID_NONE) {
            mSurveyGroup = mDatabase.getSurveyGroup(selectedSurveyId);
        }
        apkUpdateStore = new ApkUpdateStore(new GsonMapper(), prefs);

        // Init navigation drawer
        initNavigationDrawer();

        initDataPointsFragment(savedInstanceState);

        // Start the setup Activity if necessary.
        boolean noDevIdYet = false;
        if (!prefs.getBoolean(Prefs.KEY_SETUP, false)) {
            noDevIdYet = true;
            navigator.navigateToAddUser(this);
        }

        startServices(noDevIdYet);

        //When the app is restarted we need to display the current user
        if (savedInstanceState == null) {
            displaySelectedUser();
        }

    }

    private void initializeToolBar() {
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initNavigationDrawer() {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        mDrawer = (DrawerFragment) supportFragmentManager.findFragmentByTag(DRAWER_FRAGMENT_TAG);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                 R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                mDrawer.onDrawerClosed();
                supportInvalidateOptionsMenu();
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //prevent the back icon from showing
                super.onDrawerSlide(drawerView, 0);
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                //disable drawer animation
                super.onDrawerSlide(drawerView, 0);
            }
        };

        mDrawerLayout.addDrawerListener(mDrawerToggle);

        // Automatically select the survey
        if (mSurveyGroup != null) {
            onSurveySelected(mSurveyGroup);
        } else {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private void initDataPointsFragment(Bundle savedInstanceState) {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        if (savedInstanceState == null
                || supportFragmentManager.findFragmentByTag(DATA_POINTS_FRAGMENT_TAG) == null) {
            DatapointsFragment datapointsFragment = DatapointsFragment.newInstance(mSurveyGroup);
            supportFragmentManager.beginTransaction()
                    .replace(R.id.content_frame, datapointsFragment, DATA_POINTS_FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case ConstantUtil.REQUEST_ADD_USER:
                if (resultCode == RESULT_OK) {
                    displaySelectedUser();
                    prefs.setBoolean(Prefs.KEY_SETUP, true);
                    // Trigger the delayed services, so the first
                    // backend connections uses the new Device ID
                    startService(new Intent(this, SurveyDownloadService.class));
                    startService(new Intent(this, DataSyncService.class));
                } else if (!prefs.getBoolean(Prefs.KEY_SETUP, false)) {
                    finish();
                }
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Delete empty responses, if any
        mDatabase.deleteEmptySurveyInstances();
        mDatabase.deleteEmptyRecords();
        registerReceiver(mSurveysSyncReceiver,
                new IntentFilter(getString(R.string.action_surveys_sync)));

        ViewApkData apkData = apkUpdateStore.getApkData();
        boolean shouldNotifyUpdate = apkUpdateStore.shouldNotifyNewVersion();
        if (apkData != null && shouldNotifyUpdate && PlatformUtil
                .isNewerVersion(BuildConfig.VERSION_NAME, apkData.getVersion())) {
            apkUpdateStore.saveAppUpdateNotifiedTime();
            navigator.navigateToAppUpdate(this, apkData);
        }
        updateAddDataPointFab();
    }

    private void updateAddDataPointFab() {
        if (mSurveyGroup != null) {
            addDataPointFab.setVisibility(View.VISIBLE);
            addDataPointFab.setEnabled(true);
        } else {
            addDataPointFab.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.START)) {
            mDrawerLayout.closeDrawer(Gravity.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mSurveysSyncReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    private void startServices(boolean waitForDeviceId) {
        if (!StatusUtil.hasExternalStorage()) {
            ViewUtil.showConfirmDialog(R.string.checksd, R.string.sdmissing, this,
                    false,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SurveyActivity.this.finish();
                        }
                    },
                    null);
        } else {
            if (!waitForDeviceId) {
                startService(new Intent(this, SurveyDownloadService.class));
                startService(new Intent(this, DataSyncService.class));
            }
            startService(new Intent(this, BootstrapService.class));
            startService(new Intent(this, TimeCheckService.class));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String surveyedLocaleId = intent.getDataString();
            onRecordSelected(surveyedLocaleId);
        }
    }

    @Override
    public void onUserSelected(User user) {
        FlowApp.getApp().setUser(user);
        mDrawer.load();
        mDrawerLayout.closeDrawers();
        displaySelectedUser();
    }

    @Override
    public void onSurveySelected(SurveyGroup surveyGroup) {
        mSurveyGroup = surveyGroup;

        CharSequence title =
                mSurveyGroup != null ? mSurveyGroup.getName() : getString(R.string.app_name);
        setTitle(title);

        selectedSurveyId = mSurveyGroup != null ? mSurveyGroup.getId() : SurveyGroup.ID_NONE;
        prefs.setLong(Prefs.KEY_SURVEY_GROUP_ID, selectedSurveyId);

        DatapointsFragment f = (DatapointsFragment) getSupportFragmentManager().findFragmentByTag(
                DATA_POINTS_FRAGMENT_TAG);
        if (f != null) {
            f.refresh(mSurveyGroup);
        } else {
            supportInvalidateOptionsMenu();
        }
        mDrawer.load();
        mDrawerLayout.closeDrawers();
        updateAddDataPointFab();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean showItems =
                !mDrawerLayout.isDrawerOpen(GravityCompat.START) && mSurveyGroup != null;
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(showItems);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onRecordSelected(String surveyedLocaleId) {
        final User user = FlowApp.getApp().getUser();
        // Ensure user is logged in
        if (user == null) {
            Toast.makeText(SurveyActivity.this, R.string.mustselectuser,
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (mSurveyGroup != null && mSurveyGroup.isMonitored()) {
            displayRecord(surveyedLocaleId);
        } else {
            displayForm(surveyedLocaleId, user);
        }
    }

    private void displayRecord(String surveyedLocaleId) {
        navigator.navigateToRecordActivity(this, surveyedLocaleId, mSurveyGroup);
    }

    private void displayForm(String surveyedLocaleId, User user) {
        Survey registrationForm = mDatabase.getRegistrationForm(mSurveyGroup);
        if (registrationForm == null) {
            Toast.makeText(this, R.string.error_missing_form, Toast.LENGTH_LONG).show();
            return;
        } else if (!registrationForm.isHelpDownloaded()) {
            Toast.makeText(this, R.string.error_missing_cascade, Toast.LENGTH_LONG).show();
            return;
        }

        final String registrationFormId = registrationForm.getId();
        long formInstanceId;
        boolean readOnly;
        Cursor c = mDatabase.getFormInstances(surveyedLocaleId);
        if (c.moveToFirst()) {
            formInstanceId = c.getLong(SurveyDbAdapter.FormInstanceQuery._ID);
            int status = c.getInt(SurveyDbAdapter.FormInstanceQuery.STATUS);
            readOnly = status != SurveyInstanceStatus.SAVED;
        } else {
            formInstanceId = mDatabase
                    .createSurveyRespondent(registrationForm.getId(), registrationForm.getVersion(),
                            user, surveyedLocaleId);
            readOnly = false;
        }
        c.close();

        navigator.navigateToFormActivity(this, surveyedLocaleId, registrationFormId,
                formInstanceId, readOnly, mSurveyGroup);
    }

    private void displaySelectedUser() {
        User user = FlowApp.getApp().getUser();
        if (user != null) {
            Toast.makeText(this, getString(R.string.logged_in_as) + " " + user.getName(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void refreshMenu() {
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onSyncRecordsTap(long surveyGroupId) {
        Toast.makeText(this, R.string.syncing_records, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, SurveyedDataPointSyncService.class);
        intent.putExtra(SurveyedDataPointSyncService.SURVEY_GROUP, surveyGroupId);
        startService(intent);
    }

    @Override
    public boolean onSearchTap() {
//        return onSearchRequested();
        return true;

    }

    private void reloadDrawer() {
        mDrawer.load();
    }

    @OnClick(R.id.add_data_point_fab)
    void onAddDataPointTap() {
        addDataPointFab.setEnabled(false);
        String newLocaleId = mDatabase.createSurveyedLocale(mSurveyGroup.getId());
        onRecordSelected(newLocaleId);
    }

    private static class SurveySyncBroadcastReceiver extends BroadcastReceiver {

        private final WeakReference<SurveyActivity> activityWeakReference;

        private SurveySyncBroadcastReceiver(SurveyActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.i("Surveys have been synchronised. Refreshing data...");
            SurveyActivity surveyActivity = activityWeakReference.get();
            if (surveyActivity != null) {
                surveyActivity.reloadDrawer();
            }
        }
    }

}
