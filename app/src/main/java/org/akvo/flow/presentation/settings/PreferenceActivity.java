/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.SQLException;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.async.ClearDataAsyncTask;
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.service.DataSyncService;
import org.akvo.flow.service.SurveyDownloadService;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.ViewUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import timber.log.Timber;

public class PreferenceActivity extends BackActivity implements PreferenceView {

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

    private List<String> languages;
    private boolean trackChanges = false;

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
        Intent i = new Intent(this, DataSyncService.class);
        getApplicationContext().startService(i);
        finish();
    }

    @OnClick({R.id.preference_delete_collected_data_title,
            R.id.preference_delete_collected_data_subtitle
    })
    void onDeleteCollectedDataTap() {
        ViewUtil.showAdminAuthDialog(this, new ViewUtil.AdminAuthDialogListener() {
            @Override
            public void onAuthenticated() {
               deleteData(true);
            }
        });
    }

    @OnClick({R.id.preference_delete_everything_title,
            R.id.preference_delete_everything_subtitle
    })
    void onDeleteAllTap() {
        ViewUtil.showAdminAuthDialog(this, new ViewUtil.AdminAuthDialogListener() {
            @Override
            public void onAuthenticated() {
                deleteData(false);
            }
        });
    }

    @OnClick({R.id.preference_download_form_title,
            R.id.preference_download_form_subtitle
    })
    void onDownloadFormOptionTap() {
        ViewUtil.showAdminAuthDialog(this, new ViewUtil.AdminAuthDialogListener() {

            @Override
            public void onAuthenticated() {
                AlertDialog.Builder inputDialog = new AlertDialog.Builder(PreferenceActivity.this);
                inputDialog.setTitle(R.string.downloadsurveylabel);
                inputDialog.setMessage(R.string.downloadsurveyinstr);

                final EditText input = new EditText(PreferenceActivity.this);
                input.setKeyListener(new DigitsKeyListener(false, false));
                inputDialog.setView(input);
                inputDialog.setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String surveyId = input.getText().toString().trim();
                        if (!TextUtils.isEmpty(surveyId)) {
                            Intent i = new Intent(PreferenceActivity.this,
                                    SurveyDownloadService.class);
                            i.putExtra(SurveyDownloadService.EXTRA_SURVEY_ID, surveyId);
                            PreferenceActivity.this.startService(i);
                        }
                    }
                });

                inputDialog.setNegativeButton(R.string.cancelbutton, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });

                inputDialog.show();
            }
        });
    }

    @OnClick({R.id.preference_reload_forms_title,
            R.id.preference_reload_forms_subtitle
    })
    void onReloadAllSurveysOptionTap() {
        ViewUtil.showAdminAuthDialog(this, new ViewUtil.AdminAuthDialogListener() {
            @Override
            public void onAuthenticated() {
                AlertDialog.Builder builder = new AlertDialog.Builder(PreferenceActivity.this);
                builder.setTitle(R.string.conftitle);
                builder.setMessage(R.string.reloadconftext);
                builder.setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Context c = PreferenceActivity.this;
                        Intent i = new Intent(c, SurveyDownloadService.class);
                        i.putExtra(SurveyDownloadService.EXTRA_DELETE_SURVEYS, true);
                        c.startService(i);
                    }
                });
                builder.setNegativeButton(R.string.cancelbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                builder.show();
            }
        });
    }

    @OnClick(R.id.preference_gps_fixes)
    void onGpsFixesTap() {
        PackageManager packageManager = getPackageManager();
        Intent intent = new Intent(ConstantUtil.GPS_STATUS_INTENT);
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.nogpsstatus);
            builder.setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            builder.show();
        }
    }

    @OnClick(R.id.preference_storage)
    void onCheckSdCardStateOptionTap() {
        navigator.navigateToStorageSettings(this);
    }

    @OnCheckedChanged(R.id.switch_enable_data)
    void onDataCheckChanged(boolean checked) {
        if (trackChanges) {
            presenter.saveEnableMobileData(checked);
        }
    }

    @OnCheckedChanged(R.id.switch_screen_on)
    void onScreenOnCheckChanged(boolean checked) {
        if (trackChanges) {
            presenter.saveKeepScreenOn(checked);
        }
    }

    @OnItemSelected(R.id.preference_language)
    void onLanguageSelected(int position) {
        if (trackChanges) {
            presenter.saveAppLanguage(position, languages);
        }
    }

    @OnItemSelected(R.id.preference_image_size)
    void onImageSizeSelected(int position) {
        presenter.saveImageSize(position);
    }

    /**
     * Permanently deletes data from the device. If unsubmitted data is found on
     * the database, the user will be prompted with a message to confirm the
     * operation.
     *
     * @param responsesOnly Flag to specify a partial deletion (user generated
     *                      data).
     */
    private void deleteData(final boolean responsesOnly) throws SQLException {
        try {
            int messageId;
            if (unsentData()) {
                messageId = R.string.unsentdatawarning;
            } else if (responsesOnly) {
                messageId = R.string.delete_responses_warning;
            } else {
                messageId = R.string.deletealldatawarning;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(messageId)
                    .setCancelable(true)
                    .setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (!responsesOnly) {
                                // Delete everything implies logging the current user out (if any)
                                FlowApp.getApp().setUser(null);
                            }
                            new ClearDataAsyncTask(PreferenceActivity.this).execute(responsesOnly);
                        }
                    })
                    .setNegativeButton(R.string.cancelbutton,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
            builder.show();
        } catch (SQLException e) {
            Timber.e(e, e.getMessage());
            Toast.makeText(this, R.string.clear_data_error, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean unsentData() throws SQLException {
        SurveyDbDataSource db = new SurveyDbDataSource(this, null);
        try {
            db.open();
            return db.getUnSyncedTransmissions().size() > 0;
        } finally {
            db.close();
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
                trackChanges = true;
            }
        }, 500);
    }

    @Override
    public void displayLanguageChanged(String languageCode) {
        updateLocale(languageCode);
        Toast.makeText(this, R.string.please_restart, Toast.LENGTH_LONG)
                .show();
    }

    private void updateLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Resources resources = getBaseContext().getResources();
        Configuration config = resources.getConfiguration();
        config.locale = locale;
        resources.updateConfiguration(config, null);
    }
}
