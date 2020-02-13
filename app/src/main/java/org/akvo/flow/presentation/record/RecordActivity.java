/*
 * Copyright (C) 2013-2020 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.presentation.record;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.data.loader.SurveyedLocaleItemLoader;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.SurveyedLocale;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.adapter.RecordTabsAdapter;
import org.akvo.flow.ui.fragment.FormListFragment;
import org.akvo.flow.ui.fragment.ResponseListFragment;
import org.akvo.flow.uicomponents.BackActivity;
import org.akvo.flow.uicomponents.SnackBarManager;
import org.akvo.flow.util.ConstantUtil;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import androidx.annotation.StringRes;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;

public class RecordActivity extends BackActivity implements FormListFragment.SurveyListListener,
        ResponseListFragment.ResponseListListener, LoaderManager.LoaderCallbacks<SurveyedLocale>,
        RecordView {

    private SurveyGroup mSurveyGroup;
    private String recordId;

    @Inject
    SurveyDbDataSource mDatabase;

    @Inject
    Navigator navigator;

    @Inject
    SnackBarManager snackBarManager;

    @Inject
    RecordPresenter presenter;

    @BindView(R.id.record_root_layout)
    View rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_activity);
        initializeInjector();
        ButterKnife.bind(this);
        presenter.setView(this);
        ViewPager viewPager = findViewById(R.id.pager);
        RecordTabsAdapter recordTabsAdapter = new RecordTabsAdapter(getSupportFragmentManager(),
                getResources().getStringArray(R.array.record_tabs));
        viewPager.setAdapter(recordTabsAdapter);

        mSurveyGroup = (SurveyGroup) getIntent().getSerializableExtra(
                ConstantUtil.SURVEY_GROUP_EXTRA);
        setupToolBar();
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
    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getApplication()).getApplicationComponent();
    }

    @Override
    public void onResume() {
        super.onResume();
        mDatabase.open();

        recordId = getIntent().getStringExtra(ConstantUtil.DATA_POINT_ID_EXTRA);
        getSupportLoaderManager().restartLoader(0, null, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDatabase.close();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ConstantUtil.FORM_FILLING_REQUEST && resultCode == RESULT_OK) {
            snackBarManager.displaySnackBar(rootLayout, R.string.snackbar_submitted, this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) {
            presenter.destroy();
        }
    }

    @Override
    public void onSurveyClick(final String formId) {
        presenter.onFormClick(formId, recordId);
    }

    private void showErrorMessage(@StringRes int stringResId) {
        Toast.makeText(this, stringResId, Toast.LENGTH_LONG).show();
    }



    @Override
    public void onNamedRecordDeleted() {
        getSupportLoaderManager().restartLoader(0, null, this);
    }

    // ==================================== //
    // =========== Options Menu =========== //
    // ==================================== //

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.record_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view_map:
                navigator.navigateToMapActivity(this, recordId);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<SurveyedLocale> onCreateLoader(int id, Bundle args) {
        return new SurveyedLocaleItemLoader(this, recordId);
    }

    @Override
    public void onLoadFinished(Loader<SurveyedLocale> loader, SurveyedLocale data) {
        if (data != null) {
            setTitle(data.getDisplayName(this));
        } else {
            setTitle(getString(R.string.unknown));
        }
    }

    @Override
    public void onLoaderReset(Loader<SurveyedLocale> loader) {
        // EMPTY
    }

    @Override
    public void showBootStrapPendingError() {
        showErrorMessage(R.string.pleasewaitforbootstrap);
    }

    @Override
    public void showMissingCascadeError() {
        showErrorMessage(R.string.error_missing_cascade);
    }

    @Override
    public void navigateToForm(@NotNull String formId, long formInstanceId) {
        navigator.navigateToFormActivity(this, recordId, formId, formInstanceId, false,
                mSurveyGroup);
    }
}
