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
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
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
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.database.SurveyDbAdapter;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.User;
import org.akvo.flow.domain.apkupdate.ApkUpdateStore;
import org.akvo.flow.domain.apkupdate.GsonMapper;
import org.akvo.flow.domain.apkupdate.ViewApkData;
import org.akvo.flow.presentation.EditUserDialog;
import org.akvo.flow.presentation.UserDeleteConfirmationDialog;
import org.akvo.flow.presentation.navigation.FlowNavigation;
import org.akvo.flow.presentation.navigation.SurveyDeleteConfirmationDialog;
import org.akvo.flow.presentation.navigation.UserOptionsDialog;
import org.akvo.flow.presentation.navigation.ViewUser;
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

import static org.akvo.flow.util.ConstantUtil.ACTION_LOCALE_SYNC_RESULT;
import static org.akvo.flow.util.ConstantUtil.ACTION_SURVEY_SYNC;

public class SurveyActivity extends AppCompatActivity implements RecordListListener,
        DrawerFragment.DrawerListener, DatapointsFragment.DatapointFragmentListener,
        FlowNavigation.DrawerNavigationListener,
        SurveyDeleteConfirmationDialog.SurveyDeleteListener, UserOptionsDialog.UserOptionListener,
        UserDeleteConfirmationDialog.UserDeleteListener, EditUserDialog.EditUserListener {

    private static final String DATA_POINTS_FRAGMENT_TAG = "datapoints_fragment";
    //    private static final String DRAWER_FRAGMENT_TAG = "f";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @BindView(R.id.add_data_point_fab)
    FloatingActionButton addDataPointFab;

    @BindView(R.id.nav_view)
    FlowNavigation navigationView;

    @Nullable
    private SurveyDbDataSource mDatabase;
    private SurveyGroup mSurveyGroup;
    private ActionBarDrawerToggle mDrawerToggle;
    //    private DrawerFragment mDrawer;
    private Navigator navigator = new Navigator();
    private View rootView;
    private Prefs prefs;
    private ApkUpdateStore apkUpdateStore;

    private long selectedSurveyId;

    /**
     * BroadcastReceiver to notify of surveys synchronisation. This should be
     * fired from {@link SurveyDownloadService}.
     */
    private final BroadcastReceiver mSurveysSyncReceiver = new SurveySyncBroadcastReceiver(this);
    private final BroadcastReceiver dataPointSyncReceiver = new DatapointsSyncResultBroadcastReceiver(
            this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_activity);

        ButterKnife.bind(this);

        initializeToolBar();

        mDatabase = new SurveyDbDataSource(this, null);
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

        rootView = findViewById(R.id.content_frame);

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
        navigationView.setSurveyListener(this);
    }

    private void initializeToolBar() {
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initNavigationDrawer() {
        //        FragmentManager supportFragmentManager = getSupportFragmentManager();
        //        mDrawer = (DrawerFragment) supportFragmentManager.findFragmentByTag(DRAWER_FRAGMENT_TAG);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                //                mDrawer.onDrawerClosed();
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
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mSurveysSyncReceiver, new IntentFilter(ACTION_SURVEY_SYNC));
        LocalBroadcastManager.getInstance(this).registerReceiver(dataPointSyncReceiver,
                new IntentFilter(ACTION_LOCALE_SYNC_RESULT));

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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSurveysSyncReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataPointSyncReceiver);
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
        //        mDrawer.load();
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

        DatapointsFragment f = (DatapointsFragment) getSupportFragmentManager().findFragmentByTag(
                DATA_POINTS_FRAGMENT_TAG);
        if (f != null) {
            f.refresh(mSurveyGroup);
        }
        supportInvalidateOptionsMenu();

        mDrawerLayout.closeDrawers();
        updateAddDataPointFab();
    }

    @Override
    public void onSurveyDeleted(long surveyGroupId) {
        if (selectedSurveyId == surveyGroupId) {
            onSurveySelected(null);
        }
    }

    @Override
    public void onSurveyDeleteConfirmed(long surveyGroupId) {
        navigationView.onSurveyDeleteConfirmed(surveyGroupId);
    }

    @Override
    public void onEditUser(ViewUser viewUser) {
        DialogFragment fragment = EditUserDialog.newInstance(viewUser);
        fragment.show(getSupportFragmentManager(), EditUserDialog.TAG);
    }

    @Override
    public void editUser(ViewUser viewUser) {
        navigationView.editUser(viewUser);
    }

    @Override
    public void onDeleteUser(ViewUser viewUser) {
        DialogFragment fragment = UserDeleteConfirmationDialog.newInstance(viewUser);
        fragment.show(getSupportFragmentManager(), UserDeleteConfirmationDialog.TAG);
    }

    @Override
    public void onUserDeleteConfirmed(ViewUser viewUser) {
        navigationView.deleteUser(viewUser);
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
    public void onSyncRecordsRequested(long surveyGroupId) {
        Toast.makeText(this, R.string.syncing_records, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, SurveyedDataPointSyncService.class);
        intent.putExtra(SurveyedDataPointSyncService.SURVEY_GROUP, surveyGroupId);
        startService(intent);
    }

    @Override
    public boolean onSearchTap() {
        return onSearchRequested();
    }

    private void reloadDrawer() {
        //        mDrawer.load();
    }

    @OnClick(R.id.add_data_point_fab)
    void onAddDataPointTap() {
        addDataPointFab.setEnabled(false);
        String newLocaleId = mDatabase.createSurveyedLocale(mSurveyGroup.getId());
        onRecordSelected(newLocaleId);
    }

    private void displayResult(Intent intent) {
        if (intent != null) {
            int resultCode = intent.getIntExtra(ConstantUtil.EXTRA_DATAPOINT_SYNC_RESULT,
                    ConstantUtil.DATA_SYNC_RESULT_SUCCESS);
            int numberSynced = intent.getIntExtra(ConstantUtil.EXTRA_DATAPOINT_NUMBER, 0);
            switch (resultCode) {
                case ConstantUtil.DATA_SYNC_RESULT_SUCCESS:
                    if (numberSynced > 0) {
                        displaySuccess(numberSynced);
                    }
                    break;
                case ConstantUtil.DATA_SYNC_RESULT_ERROR_MISSING_ASSIGNMENT:
                    displayErrorAssignment();
                    break;
                case ConstantUtil.DATA_SYNC_RESULT_ERROR_NETWORK:
                    displayErrorNetwork();
                    break;
                default:
                    displayDefaultError();
                    break;
            }
        }
    }

    private void displayDefaultError() {
        displaySnackBarWithRetry(R.string.data_points_sync_error_message_default);
    }

    private void displayErrorNetwork() {
        displaySnackBarWithRetry(R.string.data_points_sync_error_message_network);
    }

    private void onDataPointRetryPressed() {
        if (mSurveyGroup != null) {
            onSyncRecordsRequested(mSurveyGroup.getId());
        }
    }

    private void displayErrorAssignment() {
        displaySnackBar(getString(R.string.data_points_sync_error_message_assignment));
    }

    private void displaySuccess(int numberSynced) {
        displaySnackBar(getString(R.string.data_points_sync_success_message, numberSynced));
    }

    private void displaySnackBar(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
    }

    private void displaySnackBarWithRetry(@StringRes int errorMessage) {
        Snackbar.make(rootView, getString(errorMessage), Snackbar.LENGTH_LONG)
                .setAction(R.string.retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onDataPointRetryPressed();
                    }
                })
                .show();
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

    static class DatapointsSyncResultBroadcastReceiver extends BroadcastReceiver {

        private final WeakReference<SurveyActivity> activityWeakReference;

        DatapointsSyncResultBroadcastReceiver(SurveyActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            SurveyActivity surveyActivity = activityWeakReference.get();
            if (surveyActivity != null) {
                surveyActivity.displayResult(intent);
            }
        }
    }
}
