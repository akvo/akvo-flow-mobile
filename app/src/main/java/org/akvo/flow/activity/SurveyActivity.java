/*
 *  Copyright (C) 2010-2018 Stichting Akvo (Akvo Foundation)
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
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
import org.akvo.flow.domain.apkupdate.ApkUpdateStore;
import org.akvo.flow.domain.apkupdate.ViewApkData;
import org.akvo.flow.domain.entity.User;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.util.GsonMapper;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.SnackBarManager;
import org.akvo.flow.presentation.UserDeleteConfirmationDialog;
import org.akvo.flow.presentation.navigation.CreateUserDialog;
import org.akvo.flow.presentation.navigation.EditUserDialog;
import org.akvo.flow.presentation.navigation.FlowNavigationView;
import org.akvo.flow.presentation.navigation.SurveyDeleteConfirmationDialog;
import org.akvo.flow.presentation.navigation.UserOptionsDialog;
import org.akvo.flow.presentation.navigation.ViewUser;
import org.akvo.flow.service.BootstrapService;
import org.akvo.flow.service.DataSyncService;
import org.akvo.flow.service.SurveyDownloadService;
import org.akvo.flow.service.TimeCheckService;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.fragment.DatapointsFragment;
import org.akvo.flow.ui.fragment.RecordListListener;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.StatusUtil;
import org.akvo.flow.util.ViewUtil;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class SurveyActivity extends AppCompatActivity implements RecordListListener,
        FlowNavigationView.DrawerNavigationListener,
        SurveyDeleteConfirmationDialog.SurveyDeleteListener, UserOptionsDialog.UserOptionListener,
        UserDeleteConfirmationDialog.UserDeleteListener, EditUserDialog.EditUserListener,
        CreateUserDialog.CreateUserListener {

    public static final int NAVIGATION_DRAWER_DELAY_MILLIS = 250;
    private static final String DATA_POINTS_FRAGMENT_TAG = "datapoints_fragment";

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
    @Named("getSelectedUser")
    UseCase getSelectedUser;

    private SurveyGroup mSurveyGroup;

    private ActionBarDrawerToggle mDrawerToggle;
    private ApkUpdateStore apkUpdateStore;
    private long selectedSurveyId;
    private boolean activityJustCreated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_activity);
        initializeInjector();
        ButterKnife.bind(this);

        initializeToolBar();

        if (!deviceSetUpCompleted()) {
            navigateToSetUp();
        } else {

            mDatabase.open();

            updateSelectedSurvey();
            apkUpdateStore = new ApkUpdateStore(new GsonMapper(), prefs);

            initNavigationDrawer();
            selectSurvey();
            initDataPointsFragment(savedInstanceState);

            startServicesIfPossible();

            //When the app is restarted we need to display the current user
            if (savedInstanceState == null) {
                showSelectedUser();
            }
            activityJustCreated = true;
            setNavigationView();
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
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
            }
        };

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
            // Delete empty responses, if any
            if (mDatabase != null) {
                mDatabase.deleteEmptySurveyInstances();
                mDatabase.deleteEmptyRecords();
            }

            showApkUpdateIfNeeded();
            updateAddDataPointFab();
            //TODO: broken
            handlePermissions();
        }
    }

    private void handlePermissions() {
        if (!isStorageAllowed()) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    ConstantUtil.STORAGE_PERMISSION_CODE);
        }
    }

    private boolean isStorageAllowed() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
       if (grantResults.length > 0 && requestCode == ConstantUtil.STORAGE_PERMISSION_CODE
                && Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[0])
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
           startServices();
       } else {
           snackBarManager.displaySnackBarWithAction(rootLayout,
                   R.string.storage_permission_missing, R.string.action_retry,
                   new View.OnClickListener() {
                       @Override
                       public void onClick(View v) {
                           handlePermissions();
                       }
                   }, this);
       }
    }

    private void showApkUpdateIfNeeded() {
        ViewApkData apkData = apkUpdateStore.getApkData();
        boolean shouldNotifyUpdate = apkUpdateStore.shouldNotifyNewVersion();
        if (apkData != null && shouldNotifyUpdate && PlatformUtil
                .isNewerVersion(BuildConfig.VERSION_NAME, apkData.getVersion())) {
            apkUpdateStore.saveAppUpdateNotifiedTime();
            navigator.navigateToAppUpdate(this, apkData);
        }
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
        if (mDrawerLayout.isDrawerOpen(Gravity.START)) {
            mDrawerLayout.closeDrawer(Gravity.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
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
        if (!StatusUtil.hasExternalStorage()) {
            checkStorage();
        } else {
            startServices();
        }
    }

    private void startServices() {
        startService(new Intent(this, SurveyDownloadService.class));
        startService(new Intent(this, DataSyncService.class));
        startService(new Intent(this, BootstrapService.class));
        startService(new Intent(this, TimeCheckService.class));
    }

    private void checkStorage() {
        ViewUtil.showConfirmDialog(R.string.checksd, R.string.sdmissing, this,
                false,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SurveyActivity.this.finish();
                    }
                },
                null);
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
    public void onSurveySelected(SurveyGroup surveyGroup) {
        mSurveyGroup = surveyGroup;

        updateActivityTitle();

        selectedSurveyId = mSurveyGroup != null ? mSurveyGroup.getId() : SurveyGroup.ID_NONE;

        DatapointsFragment f = (DatapointsFragment) getSupportFragmentManager().findFragmentByTag(
                DATA_POINTS_FRAGMENT_TAG);
        if (f != null) {
            f.refresh(mSurveyGroup);
        }
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onRecordSelected(final String surveyedLocaleId) {
        getSelectedUser.execute(new DefaultObserver<User>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                showMissingUserError();
            }

            @Override
            public void onNext(User user) {
                if (user.getName() == null) {
                    showMissingUserError();
                } else {
                    if (mSurveyGroup != null && mSurveyGroup.isMonitored()) {
                        displayRecord(surveyedLocaleId);
                    } else {
                        displayForm(surveyedLocaleId, user);
                    }
                }
            }
        }, null);
    }

    private void showMissingUserError() {
        Toast.makeText(this, R.string.mustselectuser, Toast.LENGTH_LONG).show();
    }

    private void displayRecord(String surveyedLocaleId) {
        navigator.navigateToRecordActivity(this, surveyedLocaleId, mSurveyGroup);
    }

    private void displayForm(String surveyedLocaleId, User user) {
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

    private void showSelectedUser() {
        getSelectedUser.execute(new DefaultObserver<User>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }

            @Override
            public void onNext(User user) {
                String userName = user.getName();
                if (!TextUtils.isEmpty(userName)) {
                    showMessage(getString(R.string.logged_in_as) + " " + userName);
                }
            }
        }, null);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void navigateToHelp() {
        navigate(new Runnable() {
            @Override
            public void run() {
                navigator.navigateToHelp(SurveyActivity.this);
            }
        });
    }

    @Override
    public void navigateToAbout() {
        navigate(new Runnable() {
            @Override
            public void run() {
                navigator.navigateToAbout(SurveyActivity.this);
            }
        });
    }

    @Override
    public void navigateToSettings() {
        navigate(new Runnable() {
            @Override
            public void run() {
                navigator.navigateToAppSettings(SurveyActivity.this);
            }
        });
    }

    private void navigate(Runnable runnable) {
        mDrawerLayout.closeDrawers();
        mDrawerLayout.postDelayed(runnable, NAVIGATION_DRAWER_DELAY_MILLIS);
    }

    @OnClick(R.id.add_data_point_fab)
    void onAddDataPointTap() {
        if (mDatabase != null) {
            addDataPointFab.setEnabled(false);
            String newLocaleId = mDatabase.createSurveyedLocale(mSurveyGroup.getId());
            onRecordSelected(newLocaleId);
        }
    }
}
