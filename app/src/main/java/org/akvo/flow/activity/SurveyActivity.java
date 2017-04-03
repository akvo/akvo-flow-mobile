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
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.database.SurveyDbAdapter;
import org.akvo.flow.database.SurveyInstanceStatus;
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

import timber.log.Timber;

import static org.akvo.flow.util.ConstantUtil.ACTION_SURVEY_SYNC;

public class SurveyActivity extends AppCompatActivity implements RecordListListener,
        DrawerFragment.DrawerListener, DatapointsFragment.DatapointFragmentListener {

    private static final String DATA_POINTS_FRAGMENT_TAG = "datapoints_fragment";
    private static final String DRAWER_FRAGMENT_TAG = "f";

    private SurveyDbDataSource mDatabase;
    private SurveyGroup mSurveyGroup;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerFragment mDrawer;
    private CharSequence mDrawerTitle, mTitle;
    private Navigator navigator = new Navigator();
    private Prefs prefs;
    private ApkUpdateStore apkUpdateStore;

    /**
     * BroadcastReceiver to notify of surveys synchronisation. This should be
     * fired from {@link SurveyDownloadService}.
     */
    private final BroadcastReceiver mSurveysSyncReceiver = new SurveySyncBroadcastReceiver(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_activity);

        initializeToolBar();

        mDatabase = new SurveyDbDataSource(this, null);
        mDatabase.open();

        mTitle = mDrawerTitle = getString(R.string.app_name);

        // Init navigation drawer
        initNavigationDrawer();

        initDataPointsFragment(savedInstanceState);

        prefs = new Prefs(getApplicationContext());
        apkUpdateStore = new ApkUpdateStore(new GsonMapper(), prefs);
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
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initNavigationDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        mDrawer = (DrawerFragment) supportFragmentManager.findFragmentByTag(DRAWER_FRAGMENT_TAG);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                mDrawer.onDrawerClosed();
                getSupportActionBar().setTitle(mTitle);
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
                getSupportActionBar().setTitle(mDrawerTitle);
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
        SurveyGroup sg = mDatabase.getSurveyGroup(FlowApp.getApp().getSurveyGroupId());
        if (sg != null) {
            onSurveySelected(sg);
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
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mSurveysSyncReceiver, new IntentFilter(ACTION_SURVEY_SYNC));

        ViewApkData apkData = apkUpdateStore.getApkData();
        boolean shouldNotifyUpdate = apkUpdateStore.shouldNotifyNewVersion();
        if (apkData != null && shouldNotifyUpdate && PlatformUtil
                .isNewerVersion(BuildConfig.VERSION_NAME, apkData.getVersion())) {
            apkUpdateStore.saveAppUpdateNotifiedTime();
            navigator.navigateToAppUpdate(this, apkData);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSurveysSyncReceiver);
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

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
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

        CharSequence title = mSurveyGroup != null ? mSurveyGroup.getName() : mDrawerTitle;
        long id = mSurveyGroup != null ? mSurveyGroup.getId() : SurveyGroup.ID_NONE;

        setTitle(title);

        FlowApp.getApp().setSurveyGroupId(id);

        DatapointsFragment f = (DatapointsFragment) getSupportFragmentManager().findFragmentByTag(
                DATA_POINTS_FRAGMENT_TAG);
        if (f != null) {
            f.refresh(mSurveyGroup);
        }
        supportInvalidateOptionsMenu();
        mDrawer.load();
        mDrawerLayout.closeDrawers();
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

        // Non-monitored surveys display the form directly
        if (!mSurveyGroup.isMonitored()) {
            Survey registrationForm = mDatabase.getRegistrationForm(mSurveyGroup);
            if (registrationForm == null) {
                Toast.makeText(this, R.string.error_missing_form, Toast.LENGTH_LONG).show();
                return;
            } else if (!registrationForm.isHelpDownloaded()) {
                Toast.makeText(this, R.string.error_missing_cascade, Toast.LENGTH_LONG).show();
                return;
            }

            final String formId = registrationForm.getId();
            long formInstanceId;
            boolean readOnly = false;
            Cursor c = mDatabase.getFormInstances(surveyedLocaleId);
            if (c.moveToFirst()) {
                formInstanceId = c.getLong(SurveyDbAdapter.FormInstanceQuery._ID);
                int status = c.getInt(SurveyDbAdapter.FormInstanceQuery.STATUS);
                readOnly = status != SurveyInstanceStatus.SAVED;
            } else {
                formInstanceId = mDatabase
                        .createSurveyRespondent(formId, registrationForm.getVersion(), user,
                                surveyedLocaleId);
            }
            c.close();

            navigator.navigateToFormActivity(this, surveyedLocaleId, user, formId, formInstanceId,
                    readOnly, mSurveyGroup);
        } else {
            navigator.navigateToRecordActivity(this, surveyedLocaleId, mSurveyGroup);

        }
    }

    private void displaySelectedUser() {
        User user = FlowApp.getApp().getUser();
        if (user != null) {
            Toast.makeText(this, getString(R.string.logged_in_as) + " " + user.getName(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSearchTap() {
        return onSearchRequested();
    }

    private void reloadDrawer() {
        mDrawer.load();
    }

    static class SurveySyncBroadcastReceiver extends BroadcastReceiver {

        private final WeakReference<SurveyActivity> activityWeakReference;

        SurveySyncBroadcastReceiver(SurveyActivity activity) {
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
