/*
 *  Copyright (C) 2010-2020 Stichting Akvo (Akvo Foundation)
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

import android.Manifest;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.perf.metrics.AddTrace;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.database.SurveyDbAdapter;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.entity.User;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.offlinemaps.domain.entity.DomainOfflineArea;
import org.akvo.flow.offlinemaps.presentation.OfflineMapSelectedListener;
import org.akvo.flow.offlinemaps.presentation.dialog.OfflineMapsDialog;
import org.akvo.flow.offlinemaps.presentation.infowindow.InfoWindowLayout;
import org.akvo.flow.presentation.UserDeleteConfirmationDialog;
import org.akvo.flow.presentation.datapoints.map.DataPointsMapFragment;
import org.akvo.flow.presentation.entity.ViewApkData;
import org.akvo.flow.presentation.navigation.CreateUserDialog;
import org.akvo.flow.presentation.navigation.EditUserDialog;
import org.akvo.flow.presentation.navigation.FlowNavigationView;
import org.akvo.flow.presentation.navigation.SurveyDeleteConfirmationDialog;
import org.akvo.flow.presentation.navigation.UserOptionsDialog;
import org.akvo.flow.presentation.navigation.ViewSurvey;
import org.akvo.flow.presentation.navigation.ViewUser;
import org.akvo.flow.presentation.survey.CustomDrawerArrowDrawable;
import org.akvo.flow.presentation.survey.SurveyPresenter;
import org.akvo.flow.presentation.survey.SurveyView;
import org.akvo.flow.service.SurveyDownloadWorker;
import org.akvo.flow.service.bootstrap.BootstrapWorker;
import org.akvo.flow.service.time.TimeCheckWorker;
import org.akvo.flow.tracking.TrackingHelper;
import org.akvo.flow.tracking.TrackingListener;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.fragment.RecordListListener;
import org.akvo.flow.ui.fragment.StatsDialogFragment;
import org.akvo.flow.ui.fragment.TabsAdapter;
import org.akvo.flow.uicomponents.SnackBarManager;
import org.akvo.flow.util.AppPermissionsHelper;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ViewUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SurveyActivity extends AppCompatActivity implements RecordListListener,
        FlowNavigationView.DrawerNavigationListener,
        SurveyDeleteConfirmationDialog.SurveyDeleteListener, UserOptionsDialog.UserOptionListener,
        UserDeleteConfirmationDialog.UserDeleteListener, EditUserDialog.EditUserListener,
        CreateUserDialog.CreateUserListener, SurveyView, TrackingListener,
        OfflineMapSelectedListener, InfoWindowLayout.InfoWindowSelectionListener {

    public static final int NAVIGATION_DRAWER_DELAY_MILLIS = 250;
    private static final String STATS_DIALOG_FRAGMENT_TAG = "stats";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @BindView(R.id.add_data_point_fab)
    FloatingActionButton addDataPointFab;

    @BindView(R.id.nav_view)
    FlowNavigationView navigationView;

    @BindView(R.id.survey_root_layout)
    View rootLayout;

    @Inject
    SurveyDbDataSource mDatabase;

    @Inject
    Prefs prefs;

    @Inject
    Navigator navigator;

    @Inject
    SnackBarManager snackBarManager;

    @Inject
    AppPermissionsHelper appPermissionsHelper;

    @Inject
    SurveyPresenter presenter;

    private SurveyGroup mSurveyGroup;

    private TabsAdapter mTabsAdapter;
    private ViewPager mPager;
    private ActionBarDrawerToggle mDrawerToggle;
    private long selectedSurveyId;
    private boolean activityJustCreated;
    private boolean permissionsResults;
    private TrackingHelper trackingHelper;
    private boolean servicesStarted = false;
    private CustomDrawerArrowDrawable customDrawerArrowDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_activity);

        initializeInjector();
        ButterKnife.bind(this);

        initializeToolBar();
        presenter.setView(this);
        trackingHelper = new TrackingHelper(this);
        servicesStarted = false;
        if (!deviceSetUpCompleted()) {
            navigateToSetUp();
        } else {

            mDatabase.open();

            updateSelectedSurvey();

            initNavigationDrawer();
            selectSurvey();

            //When the app is restarted we need to display the current user
            if (savedInstanceState == null) {
                presenter.checkSelectedUser();
            }
            activityJustCreated = true;
            setNavigationView();
            mPager = findViewById(R.id.pager);
            TabLayout tabs = findViewById(R.id.tabs);

            mTabsAdapter = new TabsAdapter(getSupportFragmentManager(), getResources().getStringArray(R.array.records_activity_tabs), mSurveyGroup);
            mPager.setAdapter(mTabsAdapter);
            tabs.setupWithViewPager(mPager);
            mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset,
                                           int positionOffsetPixels) {
                    //EMPTY
                }

                @Override
                public void onPageSelected(int position) {
                    if (position == TabsAdapter.POSITION_MAP && mSurveyGroup != null) {
                        displayMapFab();
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    switch (state) {
                        case ViewPager.SCROLL_STATE_SETTLING:
                        case ViewPager.SCROLL_STATE_IDLE:
                            showFabs();
                            break;
                        default:
                            hideFabs();
                            break;
                    }
                }
            });
        }
    }

    private void hideFabs() {
        if (mSurveyGroup != null) {
            addDataPointFab.hide();
        }
        if (mSurveyGroup != null) {
            hideMapsFab();
        }
    }

    private void hideMapsFab() {
        DataPointsMapFragment mapFragment = mTabsAdapter.getMapFragment();
        if (mapFragment != null) {
            mapFragment.hideFab();
        }
    }

    private void showFabs() {
        if (mSurveyGroup != null) {
            addDataPointFab.show();
            addDataPointFab.setEnabled(true);
        }

        if (mPager.getCurrentItem() == TabsAdapter.POSITION_MAP && mSurveyGroup != null) {
            displayMapFab();
        }
    }

    private void displayMapFab() {
        DataPointsMapFragment mapFragment = mTabsAdapter.getMapFragment();
        if (mapFragment != null) {
            mapFragment.showFab();
        }
    }

    public void refreshMap() {
        mTabsAdapter.refreshMap();
    }

    public void refresh(SurveyGroup surveyGroup) {
        mSurveyGroup = surveyGroup;
        refreshView();
    }

    private void refreshView() {
        if (mTabsAdapter != null) {
            mTabsAdapter.refreshFragments(mSurveyGroup);
        }
    }

    private boolean deviceSetUpCompleted() {
        return prefs.getBoolean(Prefs.KEY_SETUP, false);
    }

    private void navigateToSetUp() {
        navigator.navigateToAddUser(this);
        finish();
    }

    private void updateSelectedSurvey() {
        selectedSurveyId = prefs.getLong(Prefs.KEY_SURVEY_GROUP_ID, SurveyGroup.ID_NONE);
        if (selectedSurveyId != SurveyGroup.ID_NONE) {
            mSurveyGroup = mDatabase.getSurveyGroup(selectedSurveyId);
        }
    }

    private void setNavigationView() {
        navigationView.setDrawerNavigationListener(this);
    }

    private void initializeInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return {@link ApplicationComponent}
     */
    protected ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getApplication()).getApplicationComponent();
    }

    private void initializeToolBar() {
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initNavigationDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close);
        customDrawerArrowDrawable = new CustomDrawerArrowDrawable(getSupportActionBar().getThemedContext());
        mDrawerToggle.setDrawerArrowDrawable(customDrawerArrowDrawable);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        if (mSurveyGroup == null) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private void selectSurvey() {
        // Automatically select the survey
        if (mSurveyGroup != null) {
            onSurveySelected(mSurveyGroup);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ConstantUtil.FORM_FILLING_REQUEST && resultCode == RESULT_OK) {
            snackBarManager.displaySnackBar(rootLayout, R.string.snackbar_submitted, this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFinishing()) {
            return;
        }
        if (!activityJustCreated && !deviceSetUpCompleted()) {
            navigateToSetUp();
        } else {
            activityJustCreated = false;
            presenter.verifyApkUpdate();
            updateAddDataPointFab();
            if (!permissionsResults) {
                handlePermissions();
            }
            permissionsResults = false;
        }
    }

    private void handlePermissions() {
        List<String> permissionsList = new ArrayList<>(2);
        if (!appPermissionsHelper.isStorageAllowed()) {
            permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!appPermissionsHelper.isPhoneStateAllowed()) {
            permissionsList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (permissionsList.isEmpty()) {
            startServicesIfPossible();
        } else {
            final String[] permissions = permissionsList.toArray(new String[0]);
            ActivityCompat
                    .requestPermissions(this, permissions,
                            ConstantUtil.STORAGE_AND_PHONE_STATE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        permissionsResults = true;
        if (requestCode == ConstantUtil.STORAGE_AND_PHONE_STATE_PERMISSION_CODE) {
            if (appPermissionsHelper.allPermissionsGranted(permissions, grantResults)) {
                startServicesIfPossible();
            } else {
                permissionsNotGranted();
            }
        }
    }

    private void permissionsNotGranted() {
        final View.OnClickListener retryListener = v -> {
            if (appPermissionsHelper.userPressedDoNotShowAgain(SurveyActivity.this)) {
                navigator.navigateToAppSystemSettings(SurveyActivity.this);
            } else {
                handlePermissions();
            }
        };
        snackBarManager
                .displaySnackBarWithAction(rootLayout,
                        R.string.survey_permissions_missing,
                        R.string.action_retry, retryListener, this);
    }

    private void updateAddDataPointFab() {
        if (mSurveyGroup != null) {
            addDataPointFab.show();
            addDataPointFab.setEnabled(true);
        } else {
            addDataPointFab.hide();
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        presenter.destroy();
        super.onDestroy();
        if (mDatabase != null) {
            mDatabase.close();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    private void startServicesIfPossible() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            startServicesOnce();
        } else {
            displayExternalStorageMissing();
        }
    }

    private void startServicesOnce() {
        if (!servicesStarted) {
            SurveyDownloadWorker.scheduleWork(getApplicationContext());
            BootstrapWorker.scheduleWork(getApplicationContext());
            TimeCheckWorker.scheduleWork(getApplicationContext());
            servicesStarted = true;
        }
    }

    private void displayExternalStorageMissing() {
        ViewUtil.showConfirmDialog(R.string.checksd, R.string.sdmissing, this,
                false,
                (dialog, which) -> SurveyActivity.this.finish(),
                null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String surveyedLocaleId = intent.getDataString();
            onDatapointSelected(surveyedLocaleId);
        }
    }

    @Override
    public void onSurveySelected(SurveyGroup surveyGroup) {
        mSurveyGroup = surveyGroup;

        updateActivityTitle();

        selectedSurveyId = mSurveyGroup != null ? mSurveyGroup.getId() : SurveyGroup.ID_NONE;

        refresh(mSurveyGroup);

        mDrawerLayout.closeDrawers();
        invalidateOptionsMenu();

        updateAddDataPointFab();
    }

    private void updateActivityTitle() {
        CharSequence title =
                mSurveyGroup != null ? mSurveyGroup.getName() : getString(R.string.app_name);
        setTitle(title);
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
    public void createUser(String userName) {
        navigationView.createUser(userName);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.stats) {
            StatsDialogFragment dialogFragment = StatsDialogFragment
                    .newInstance(mSurveyGroup.getId());
            dialogFragment.show(getSupportFragmentManager(), STATS_DIALOG_FRAGMENT_TAG);
            int selectedTab = mPager.getCurrentItem();

            if (trackingHelper != null) {
                String fromTab = selectedTab == 0 ? "list" : "map";
                trackingHelper.logStatsEvent(fromTab);
            }
            return true;
        }
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onDatapointSelected(final String datapointId) {
        presenter.onDatapointSelected(datapointId);
    }

    @Override
    public void showMissingUserError() {
        Toast.makeText(this, R.string.mustselectuser, Toast.LENGTH_LONG).show();
    }

    @AddTrace(name = "openDataPoint")
    @Override
    public void openDataPoint(String datapointId, User user) {
        if (mSurveyGroup != null && mSurveyGroup.isMonitored()) {
            displayRecord(datapointId);
        } else {
            displayForm(datapointId, user);
        }
    }

    @Override
    public void openEmptyForm(@NotNull User user, String formId) {
        navigator.navigateToFormActivity(this, mSurveyGroup, formId, user);
    }

    @Override
    public void showMissingFormError() {
        Toast.makeText(this, R.string.error_missing_registration_form, Toast.LENGTH_LONG).show();
        addDataPointFab.setEnabled(true);
    }

    @Override
    public void showMissingCascadeError() {
        Toast.makeText(this, R.string.error_missing_cascade, Toast.LENGTH_LONG).show();
        addDataPointFab.setEnabled(true);
    }

    private void displayRecord(String datapointId) {
        navigator.navigateToRecordActivity(this, datapointId, mSurveyGroup);
    }

    @AddTrace(name = "displayForm")
    private void displayForm(String datapointId, User user) {
        Survey registrationForm =
                mDatabase != null ? mDatabase.getRegistrationForm(mSurveyGroup) : null;
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
        Cursor c = mDatabase.getFormInstances(datapointId);
        if (c.moveToFirst()) {
            formInstanceId = c.getLong(SurveyDbAdapter.FormInstanceQuery._ID);
            int status = c.getInt(SurveyDbAdapter.FormInstanceQuery.STATUS);
            readOnly = status != SurveyInstanceStatus.SAVED;
        } else {
            formInstanceId = mDatabase
                    .createSurveyRespondent(registrationForm.getId(), registrationForm.getVersion(),
                            user, datapointId);
            readOnly = false;
        }
        c.close();

        navigator.navigateToFormActivity(this, datapointId, registrationFormId,
                formInstanceId, readOnly, mSurveyGroup);
    }

    @Override
    public void displaySelectedUser(@NotNull String name) {
        showMessage(getString(R.string.logged_in_as) + " " + name);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void navigateToHelp() {
        navigate(() -> navigator.navigateToHelp(SurveyActivity.this));
    }

    @Override
    public void navigateToAbout() {
        navigate(() -> navigator.navigateToAbout(SurveyActivity.this));
    }

    @Override
    public void navigateToSettings() {
        navigate(() -> navigator.navigateToAppSettings(SurveyActivity.this));
    }

    @Override
    public void navigateToOfflineMaps() {
        navigate(() -> navigator.navigateToOfflineAreasList(SurveyActivity.this));
    }

    @Override
    public void updateDrawerIcon(@NotNull List<ViewSurvey> newSurveys) {
        if (customDrawerArrowDrawable != null) {
            customDrawerArrowDrawable.setEnabled(newSurveys.size() > 0);
        }
    }

    private void navigate(Runnable runnable) {
        mDrawerLayout.closeDrawers();
        mDrawerLayout.postDelayed(runnable, NAVIGATION_DRAWER_DELAY_MILLIS);
    }

    @AddTrace(name = "onAddDataPointTap")
    @OnClick(R.id.add_data_point_fab)
    void onAddDataPointTap() {
        if (mDatabase != null) {
            addDataPointFab.setEnabled(false);
            presenter.onAddDataPointTap(mSurveyGroup);
        }
    }

    @Override
    public void showNewVersionAvailable(ViewApkData apkData) {
        navigator.navigateToAppUpdate(this, apkData);
    }

    @Override
    public void logSearchEvent() {
        if (trackingHelper != null) {
            trackingHelper.logSearchEvent();
        }
    }

    @Override
    public void logSortEvent() {
        if (trackingHelper != null) {
            trackingHelper.logSortEvent();
        }
    }

    @Override
    public void logDownloadEvent(int selectedTab) {
        if (trackingHelper != null) {
            String fromTab = selectedTab == 0 ? "list" : "map";
            trackingHelper.logDownloadEvent(fromTab);
        }
    }

    @Override
    public void logUploadEvent(int selectedTab) {
        if (trackingHelper != null) {
            String fromTab = selectedTab == 0 ? "list" : "map";
            trackingHelper.logUploadEvent(fromTab);
        }
    }

    @Override
    public void logOrderEvent(int order) {
        if (trackingHelper != null) {
            String orderSuffix = null;
            switch (order) {
                case ConstantUtil.ORDER_BY_DATE:
                    orderSuffix = "date";
                    break;
                case ConstantUtil.ORDER_BY_DISTANCE:
                    orderSuffix = "distance";
                    break;
                case ConstantUtil.ORDER_BY_STATUS:
                    orderSuffix = "status";
                    break;
                case ConstantUtil.ORDER_BY_NAME:
                    orderSuffix = "name";
                    break;
                default:
                    break;
            }
            if (orderSuffix != null) {
                trackingHelper.logSortEventChosen(orderSuffix);
            }
        }
    }

    @Override
    public void onOfflineAreaPressed(DomainOfflineArea offlineArea) {
        OfflineMapsDialog fragment = (OfflineMapsDialog) getSupportFragmentManager()
                .findFragmentByTag(OfflineMapsDialog.TAG);
        if (fragment != null) {
            fragment.onOfflineAreaSelected(offlineArea);
        }
    }

    @Override
    public void onNewMapAreaSaved() {
        refreshMap();
    }

    @Override
    public void onWindowSelected(String id) {
        onDatapointSelected(id);
    }

}
