/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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
 *
 */

package org.akvo.flow.presentation.settings;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.StringRes;
import com.google.android.material.appbar.AppBarLayout;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.SwitchCompat;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.SnackBarManager;
import org.akvo.flow.presentation.settings.passcode.PassCodeDeleteAllDialog;
import org.akvo.flow.presentation.settings.passcode.PassCodeDeleteCollectedDialog;
import org.akvo.flow.presentation.settings.passcode.PassCodeDownloadFormDialog;
import org.akvo.flow.presentation.settings.passcode.PassCodeReloadFormsDialog;
import org.akvo.flow.service.DataPointUploadWorker;
import org.akvo.flow.tracking.TrackingHelper;
import org.akvo.flow.ui.Navigator;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnItemSelected;

public class PreferenceActivity extends BackActivity implements PreferenceView,
        PassCodeDeleteCollectedDialog.PassCodeDeleteCollectedListener,
        PassCodeDeleteAllDialog.PassCodeDeleteAllListener,
        PassCodeDownloadFormDialog.PassCodeDownloadFormListener,
        PassCodeReloadFormsDialog.PassCodeReloadFormsListener,
        DeleteResponsesWarningDialog.DeleteResponsesListener,
        DeleteAllWarningDialog.DeleteAllListener, DownloadFormDialog.DownloadFormListener,
        ReloadFormsConfirmationDialog.ReloadFormsListener {

    @Inject
    Navigator navigator;

    @BindView(R.id.appbar_layout)
    AppBarLayout appBarLayout;

    @BindView(R.id.toolbar_shadow)
    View toolbarShadow;

    @BindView(R.id.preference_identifier_value)
    TextView deviceIdentifierTv;

    @BindView(R.id.preference_instance_value)
    TextView instanceNameTv;

    @BindView(R.id.progress)
    ProgressBar progressBar;

    @BindView(R.id.switch_screen_on)
    SwitchCompat screenOnSc;

    @BindView(R.id.switch_enable_data)
    SwitchCompat enableDataSc;

    @BindView(R.id.preference_language)
    Spinner appLanguageSp;

    @BindView(R.id.preference_image_size)
    Spinner imageSizeSp;

    @Inject
    PreferencePresenter presenter;

    @Inject
    SnackBarManager snackBarManager;

    private List<String> languages;
    private boolean listenersEnabled = false;
    private TrackingHelper trackingHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);
        ButterKnife.bind(this);
        initializeInjector();
        setupToolBar();
        setUpToolBarAnimationListener();
        updateProgressDrawable();
        languages = Arrays.asList(getResources().getStringArray(R.array.app_language_codes));
        presenter.setView(this);
        trackingHelper = new TrackingHelper(this);
        presenter.loadPreferences(languages);
    }

    private void setUpToolBarAnimationListener() {
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                    onToolBarCollapsed();
                } else if (verticalOffset == 0) {
                    onToolbarExpanded();
                } else {
                    onToolbarMove();
                }
            }

            private void onToolbarMove() {
                toolbarShadow.setVisibility(View.GONE);
            }

            private void onToolbarExpanded() {
                toolbarShadow.setVisibility(View.VISIBLE);
            }

            private void onToolBarCollapsed() {
                toolbarShadow.setVisibility(View.GONE);
            }
        });
    }

    private void updateProgressDrawable() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Drawable progressDrawable = progressBar.getIndeterminateDrawable();
            if (progressDrawable != null) {
                progressDrawable
                        .setColorFilter(ContextCompat.getColor(this, R.color.colorAccent),
                                PorterDuff.Mode.MULTIPLY);
            }
        }
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent()).build();
        viewComponent.inject(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.destroy();
    }

    @OnClick(R.id.send_data_points)
    void onDataPointSendTap() {
        if (trackingHelper != null) {
            trackingHelper.logUploadDataEvent();
        }
        DataPointUploadWorker.scheduleUpload(getApplicationContext(), enableDataSc.isChecked());
        finish();
    }

    @OnClick({ R.id.preference_delete_collected_data_title,
            R.id.preference_delete_collected_data_subtitle
    })
    void onDeleteCollectedDataTap() {
        if (trackingHelper != null) {
            trackingHelper.logDeleteDataPressed();
        }
        DialogFragment newFragment = PassCodeDeleteCollectedDialog.newInstance();
        newFragment.show(getSupportFragmentManager(), PassCodeDeleteCollectedDialog.TAG);
    }

    @OnClick({ R.id.preference_delete_everything_title,
            R.id.preference_delete_everything_subtitle
    })
    void onDeleteAllTap() {
        if (trackingHelper != null) {
            trackingHelper.logDeleteAllPressed();
        }
        DialogFragment newFragment = PassCodeDeleteAllDialog.newInstance();
        newFragment.show(getSupportFragmentManager(), PassCodeDeleteAllDialog.TAG);
    }

    @OnClick({ R.id.preference_download_form_title,
            R.id.preference_download_form_subtitle
    })
    void onDownloadFormOptionTap() {
        if (trackingHelper != null) {
            trackingHelper.logDownloadFormPressed();
        }
        DialogFragment newFragment = PassCodeDownloadFormDialog.newInstance();
        newFragment.show(getSupportFragmentManager(), PassCodeDownloadFormDialog.TAG);
    }

    @OnClick({ R.id.preference_reload_forms_title,
            R.id.preference_reload_forms_subtitle
    })
    void onReloadAllSurveysOptionTap() {
        if (trackingHelper != null) {
            trackingHelper.logDownloadFormsPressed();
        }
        DialogFragment newFragment = PassCodeReloadFormsDialog.newInstance();
        newFragment.show(getSupportFragmentManager(), PassCodeReloadFormsDialog.TAG);
    }

    @OnClick(R.id.preference_gps_fixes)
    void onGpsFixesTap() {
        if (trackingHelper != null) {
            trackingHelper.logGpsFixesEvent();
        }
        navigator.navigateToGpsFixes(this);
    }

    @OnClick(R.id.preference_storage)
    void onCheckSdCardStateOptionTap() {
        if (trackingHelper != null) {
            trackingHelper.logStorageEvent();
        }
        navigator.navigateToStorageSettings(this);
    }

    @OnCheckedChanged(R.id.switch_enable_data)
    void onDataCheckChanged(boolean checked) {
        if (listenersEnabled) {
            if (trackingHelper != null) {
                trackingHelper.logMobileDataChanged(checked);
            }
            presenter.saveEnableMobileData(checked);
        }
    }

    @OnCheckedChanged(R.id.switch_screen_on)
    void onScreenOnCheckChanged(boolean checked) {
        if (listenersEnabled) {
            if (trackingHelper != null) {
                trackingHelper.logScreenOnChanged(checked);
            }
            presenter.saveKeepScreenOn(checked);
        }
    }

    @OnItemSelected(R.id.preference_language)
    void onLanguageSelected(int position) {
        if (listenersEnabled) {
            if (trackingHelper != null) {
                trackingHelper.logLanguageChanged(languages.get(position));
            }
            presenter.saveAppLanguage(position, languages);
        }
    }

    @OnItemSelected(R.id.preference_image_size)
    void onImageSizeSelected(int position) {
        if (listenersEnabled) {
            if (trackingHelper != null) {
                trackingHelper.logImageSizeChanged(position);
            }
            presenter.saveImageSize(position);
        }
    }

    @Override
    public void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void displaySettings(ViewUserSettings viewUserSettings) {
        instanceNameTv.setText(BuildConfig.INSTANCE_URL);
        deviceIdentifierTv.setText(viewUserSettings.getIdentifier());
        screenOnSc.setChecked(viewUserSettings.isScreenOn());
        enableDataSc.setChecked(viewUserSettings.isDataEnabled());
        appLanguageSp.setSelection(viewUserSettings.getLanguage());
        imageSizeSp.setSelection(viewUserSettings.getImageSize());
        delayListeners();
    }

    /**
     * Delay enabling listeners in order to give ui time to draw spinners
     */
    private void delayListeners() {
        appLanguageSp.postDelayed(new Runnable() {
            @Override
            public void run() {
                listenersEnabled = true;
            }
        }, 500);
    }

    @Override
    public void displayLanguageChanged(String languageCode) {
        updateLocale(languageCode);
        showMessage(R.string.please_restart);
    }

    private void updateLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Resources resources = getBaseContext().getResources();
        Configuration config = resources.getConfiguration();
        config.locale = locale;
        resources.updateConfiguration(config, null);
    }

    @Override
    public void deleteCollectedData() {
        presenter.deleteCollectedData();
    }

    @Override
    public void deleteAllData() {
        presenter.deleteAllData();
    }

    @Override
    public void downloadForm() {
        DialogFragment newFragment = DownloadFormDialog.newInstance();
        newFragment.show(getSupportFragmentManager(), DownloadFormDialog.TAG);
    }

    @Override
    public void reloadForms() {
        DialogFragment newFragment = ReloadFormsConfirmationDialog.newInstance();
        newFragment.show(getSupportFragmentManager(), ReloadFormsConfirmationDialog.TAG);
    }

    @Override
    public void showDeleteCollectedData() {
        DialogFragment newFragment = DeleteResponsesWarningDialog.newInstance(false);
        newFragment.show(getSupportFragmentManager(), DeleteResponsesWarningDialog.TAG);
    }

    @Override
    public void showDeleteCollectedDataWithPending() {
        DialogFragment newFragment = DeleteResponsesWarningDialog.newInstance(true);
        newFragment.show(getSupportFragmentManager(), DeleteResponsesWarningDialog.TAG);
    }

    @Override
    public void showDeleteAllData() {
        DialogFragment newFragment = DeleteAllWarningDialog.newInstance(false);
        newFragment.show(getSupportFragmentManager(), DeleteAllWarningDialog.TAG);
    }

    @Override
    public void showDeleteAllDataWithPending() {
        DialogFragment newFragment = DeleteAllWarningDialog.newInstance(true);
        newFragment.show(getSupportFragmentManager(), DeleteAllWarningDialog.TAG);
    }

    @Override
    public void showClearDataError() {
        showMessage(R.string.clear_data_error);
    }

    @Override
    public void showClearDataSuccess() {
        showMessage(R.string.clear_data_success);
    }

    @Override
    public void deleteResponsesConfirmed() {
        if (trackingHelper != null) {
            trackingHelper.logDeleteDataConfirmed();
        }
        presenter.deleteResponsesConfirmed();
    }

    @Override
    public void deleteAllConfirmed() {
        if (trackingHelper != null) {
            trackingHelper.logDeleteAllConfirmed();
        }
        presenter.deleteAllConfirmed();
    }

    @Override
    public void dismiss() {
        finish();
    }

    @Override
    public void downloadForm(String formId) {
        if (trackingHelper != null) {
            trackingHelper.logDownloadFormConfirmed(formId);
        }
        presenter.downloadForm(formId);
    }

    @Override
    public void reloadFormsConfirmed() {
        if (trackingHelper != null) {
            trackingHelper.logDownloadFormsConfirmed();
        }
        presenter.reloadForms();
    }

    @Override
    public void showDownloadFormsError(int numberOfForms) {
        showQuantityMessage(R.plurals.download_forms_error, numberOfForms);
    }

    @Override
    public void showDownloadFormsSuccess(int numberOfForms) {
        showQuantityMessage(R.plurals.download_forms_success, numberOfForms);
    }

    private void showMessage(@StringRes int resId) {
        snackBarManager.displaySnackBar(instanceNameTv, resId, this);
    }

    private void showQuantityMessage(int resId, int quantity) {
        String message = getResources().getQuantityString(resId, quantity);
        snackBarManager.displaySnackBar(instanceNameTv, message, this);
    }
}
