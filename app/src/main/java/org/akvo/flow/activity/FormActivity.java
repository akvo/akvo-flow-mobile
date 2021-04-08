/*
 * Copyright (C) 2010-2021 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.activity;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.dao.SurveyDao;
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.database.SurveyDbAdapter;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.database.SurveyLanguagesDataSource;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.entity.User;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.QuestionInteractionListener;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.form.FormPresenter;
import org.akvo.flow.presentation.form.FormView;
import org.akvo.flow.presentation.form.mobiledata.MobileDataSettingDialog;
import org.akvo.flow.service.DataPointUploadWorker;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.adapter.LanguageAdapter;
import org.akvo.flow.ui.adapter.SurveyTabAdapter;
import org.akvo.flow.ui.model.Language;
import org.akvo.flow.ui.model.LanguageMapper;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.ui.view.geolocation.GeoFieldsResetConfirmDialogFragment;
import org.akvo.flow.ui.view.geolocation.GeoQuestionView;
import org.akvo.flow.uicomponents.BackActivity;
import org.akvo.flow.uicomponents.SnackBarManager;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.MediaFileHelper;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.StorageHelper;
import org.akvo.flow.util.ViewUtil;
import org.akvo.flow.util.files.FormFileBrowser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import timber.log.Timber;

import static org.akvo.flow.util.ViewUtil.showConfirmDialog;

public class FormActivity extends BackActivity implements SurveyListener,
        QuestionInteractionListener, FormView,
        GeoFieldsResetConfirmDialogFragment.GeoFieldsResetConfirmListener,
        MobileDataSettingDialog.MobileDataSettingListener {

    public static final int INVALID_INSTANCE_ID = -1;
    @Inject
    FormFileBrowser formFileBrowser;

    @Inject
    MediaFileHelper mediaFileHelper;

    @Inject
    SurveyDbDataSource mDatabase;

    @Inject
    Prefs prefs;

    @Inject
    FormPresenter presenter;

    @Inject
    SnackBarManager snackBarManager;

    @Inject
    Navigator navigator;

    @Inject
    StorageHelper storageHelper;

    @Inject
    LanguageMapper languageMapper;

    @Inject
    SurveyLanguagesDataSource surveyLanguagesDataSource;

    /**
     * When a request is done to perform photo, video, barcode scan, etc we store
     * the question id, so we can notify later the result of such operation.
     */
    private String mRequestQuestionId;

    private ViewPager mPager;
    private SurveyTabAdapter mAdapter;
    private ProgressBar progressBar;
    private View rootView;

    /**
     * flag to represent whether the Survey can be edited or not
     */
    private boolean readOnly;
    private long formInstanceId;
    private long mSessionStartTime;
    private String dataPointId;
    private SurveyGroup survey;
    private Survey form;

    private String[] mLanguages;

    // QuestionId - QuestionResponse
    private Map<String, QuestionResponse> mQuestionResponses = new HashMap<>();
    private String formId;

    private Uri imagePath;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.form_activity);
        initializeInjector();
        presenter.setView(this);
        setupToolBar();
        extractExtras();
        restoreState(savedInstanceState);
        mDatabase.open();

        //TODO: move all loading to worker thread
        loadSurvey(formId);
        loadLanguages();
        if (form == null) {
            showSurveyError();
        } else {
            setTitle();
            initViews();
            loadResponseData();
            spaceLeftOnCard();
        }
    }

    private void loadResponseData() {
        // Initialize new survey or load previous responses
        Map<String, QuestionResponse> responses = mDatabase.getResponses(formInstanceId);
        if (!responses.isEmpty()) {
            displayResponses(responses);
        }
    }

    private void showSurveyError() {
        Timber.e("mSurvey is null. Finishing the Activity...");
        Toast.makeText(getApplicationContext(), R.string.error_missing_form, Toast.LENGTH_LONG)
                .show();
        finish();
    }

    private void setTitle() {
        // Set the survey name as Activity title
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setTitle(form.getName());
            supportActionBar.setSubtitle("v " + getVersion());
        }
    }

    private void initViews() {
        mPager = findViewById(R.id.pager);
        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mPager);
        mAdapter = new SurveyTabAdapter(this, mPager, this, this);
        mPager.setAdapter(mAdapter);

        progressBar = findViewById(R.id.progressBar);
        rootView = findViewById(R.id.coordinator_layout);
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mRequestQuestionId = savedInstanceState
                    .getString(ConstantUtil.REQUEST_QUESTION_ID_EXTRA);
            imagePath = savedInstanceState.getParcelable(ConstantUtil.IMAGE_FILE_KEY);
        }
    }

    private void extractExtras() {
        // Read all the params. Note that the survey instance id is now mandatory
        Intent intent = getIntent();
        formId = intent.getStringExtra(ConstantUtil.FORM_ID_EXTRA);
        readOnly = intent.getBooleanExtra(ConstantUtil.READ_ONLY_EXTRA, false);
        formInstanceId = intent.getLongExtra(ConstantUtil.RESPONDENT_ID_EXTRA, INVALID_INSTANCE_ID);
        survey = (SurveyGroup) intent.getSerializableExtra(ConstantUtil.SURVEY_GROUP_EXTRA);
        dataPointId = intent.getStringExtra(ConstantUtil.DATA_POINT_ID_EXTRA);
        user = intent.getParcelableExtra(ConstantUtil.VIEW_USER_EXTRA);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(ConstantUtil.REQUEST_QUESTION_ID_EXTRA, mRequestQuestionId);
        outState.putParcelable(ConstantUtil.IMAGE_FILE_KEY, imagePath);
        super.onSaveInstanceState(outState);
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

    /**
     * Display prefill option dialog, if applies. This feature is only available
     * for monitored groups, when a new survey instance is created, allowing users
     * to 'clone' responses from the previous response.
     */
    private void displayPreFillDialog() {
        final Long lastSurveyInstance = mDatabase.getLastSurveyInstance(dataPointId, form.getId());
        if (lastSurveyInstance != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.prefill_title);
            builder.setMessage(R.string.prefill_text);
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                preFillSurvey(lastSurveyInstance);
                dialog.dismiss();
            });
            builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());

            builder.show();
        }
    }

    private void preFillSurvey(long preFilledSurveyInstance) {
        Map<String, QuestionResponse> responses = mDatabase
                .getResponsesForPreFilledSurvey(preFilledSurveyInstance, formInstanceId);
        displayResponses(responses);
    }

    private void loadSurvey(String surveyId) {
        Survey surveyMeta = mDatabase.getSurvey(surveyId);
        InputStream in = null;
        try {
            File file = formFileBrowser
                    .findFile(getApplicationContext(), surveyMeta.getFileName());
            in = new FileInputStream(file);
            form = SurveyDao.loadSurvey(surveyMeta, in);
            form.setId(surveyId);
        } catch (FileNotFoundException e) {
            Timber.e(e, "Could not load survey xml file");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    //EMPTY
                }
            }
        }
    }

    private double getVersion() {
        if (readOnly) {
            double version = 0.0;
            Cursor c = mDatabase.getFormInstance(formInstanceId);
            if (c.moveToFirst()) {
                version = c.getDouble(SurveyDbAdapter.FormInstanceQuery.VERSION);
            }
            c.close();

            if (version == 0.0) {
                version = form.getVersion();// Default to current value
            }

            return version;
        } else {
            return form.getVersion();
        }
    }

    /**
     * Load state for the current survey instance
     */
    private void loadResponses() {
        Map<String, QuestionResponse> responses = mDatabase.getResponses(formInstanceId);
        displayResponses(responses);
    }

    /**
     * Load state with the provided responses map
     */
    private void displayResponses(Map<String, QuestionResponse> responses) {
        mQuestionResponses = responses;
        mAdapter.reset();// Propagate the change
    }

    /**
     * Handle survey session duration. Only 'active' survey time will be consider, that is,
     * the time range between onResume() and onPause() callbacks. Survey submission will also
     * stop the recording. This feature is only used if the mReadOnly flag is not active.
     *
     * @param start true if the call is to start recording, false to stop and save the duration.
     */
    private void recordDuration(boolean start) {
        if (readOnly) {
            return;
        }

        final long time = System.currentTimeMillis();

        if (!start) {
            mDatabase.addSurveyDuration(formInstanceId, time - mSessionStartTime);
            // Restart the current session timer, in case we receive subsequent calls
            // to record the time, w/o setting up the timer first.
        }
        mSessionStartTime = time;
    }

    private void saveState() {
        if (!readOnly) {
            mDatabase.updateSurveyInstanceStatus(formInstanceId, SurveyInstanceStatus.SAVED);
            mDatabase.updateRecordModifiedDate(dataPointId, System.currentTimeMillis());

            // Record meta-data, if applies
            if (!survey.isMonitored() || form.getId()
                    .equals(survey.getRegisterSurveyId())) {
                saveRecordMetaData();
            }
        }
    }

    private void saveRecordMetaData() {
        saveRecordName();
        saveRecordLocation();
    }

    private void saveRecordLocation() {
        String localeGeoQuestion = form.getLocaleGeoQuestion();
        if (localeGeoQuestion != null) {
            QuestionResponse response = mDatabase.getResponse(formInstanceId, localeGeoQuestion);
            if (response != null) {
                mDatabase.updateSurveyedLocale(formInstanceId, response.getValue(),
                        SurveyDbAdapter.SurveyedLocaleMeta.GEOLOCATION);
            }
        }
    }

    private void saveRecordName() {
        StringBuilder builder = new StringBuilder();
        List<String> localeNameQuestions = form.getLocaleNameQuestions();

        // Check the responses given to these questions (marked as name)
        // and concatenate them so it becomes the Locale name.
        if (!localeNameQuestions.isEmpty()) {
            boolean first = true;
            for (String questionId : localeNameQuestions) {
                QuestionResponse questionResponse = mDatabase
                        .getResponse(formInstanceId, questionId);
                String answer =
                        questionResponse != null ? questionResponse.getDatapointNameValue() : null;

                if (!TextUtils.isEmpty(answer)) {
                    if (!first) {
                        builder.append(" - ");
                    }
                    builder.append(answer);
                    first = false;
                }
            }
            // Make sure the value is not larger than 500 chars
            builder.setLength(Math.min(builder.length(), 500));
            mDatabase.updateSurveyedLocale(formInstanceId, builder.toString(),
                    SurveyDbAdapter.SurveyedLocaleMeta.NAME);
        }
    }

    private void resetRecordName() {
        if (!survey.isMonitored() || isRegistrationForm()) {
            mDatabase.clearSurveyedLocaleName(formInstanceId);
        }
    }

    private boolean isRegistrationForm() {
        Survey registrationForm = mDatabase.getRegistrationForm(survey);
        return registrationForm != null && registrationForm.getId() != null && registrationForm
                .getId().equals(formId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.onResume();
        recordDuration(true);// Keep track of this session's duration.
        mPager.setKeepScreenOn(
                prefs.getBoolean(Prefs.KEY_SCREEN_ON, Prefs.DEFAULT_VALUE_SCREEN_ON));
        presenter.updateInstanceVersion(readOnly, form, formInstanceId);
    }

    @Override
    public void onPause() {
        super.onPause();
        mPager.setKeepScreenOn(false);
        mAdapter.onPause();
        recordDuration(false);
        saveState();
    }

    @Override
    public void onDestroy() {
        if (mAdapter != null) {
            mAdapter.onDestroy();
        }
        if (mDatabase != null) {
            mDatabase.close();
        }
        presenter.destroy();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.form_activity, menu);
        SubMenu subMenu = menu.findItem(R.id.more_submenu).getSubMenu();
        if (isReadOnly()) {
            subMenu.removeItem(R.id.clear);
            subMenu.removeItem(R.id.prefill);
        } else {
            subMenu.removeItem(R.id.view_map);
            subMenu.removeItem(R.id.transmission);
            if (!survey.isMonitored() ||
                    mDatabase.getLastSurveyInstance(dataPointId, form.getId()) == null) {
                subMenu.removeItem(R.id.prefill);
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_lang:
                displayLanguagesDialog();
                return true;
            case R.id.clear:
                clearSurvey();
                return true;
            case R.id.prefill:
                displayPreFillDialog();
                return true;
            case R.id.view_map:
                navigator.navigateToMapActivity(this, dataPointId);
                return true;
            case R.id.transmission:
                navigator.navigateToTransmissionActivity(this, formInstanceId);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearSurvey() {
        showConfirmDialog(R.string.cleartitle, R.string.cleardesc, this, true,
                (dialog, which) -> {
                    mDatabase.deleteResponses(String.valueOf(formInstanceId));
                    resetRecordName();
                    loadResponses();
                    spaceLeftOnCard();
                });
    }

    private void displayLanguagesDialog() {
        final ListView listView = createLanguagesList();
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.surveylanglabel)
                .setView(listView)
                .setPositiveButton(R.string.okbutton, (dialog, which) -> {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    useSelectedLanguages((LanguageAdapter) listView.getAdapter());
                }).create();
        alertDialog.show();
    }

    private void useSelectedLanguages(LanguageAdapter languageAdapter) {
        Set<String> selectedLanguages = languageAdapter.getSelectedLanguages();
        if (selectedLanguages != null && selectedLanguages.size() > 0) {
            saveLanguages(selectedLanguages);
        } else {
            displayError();
        }
    }

    private void displayError() {
        ViewUtil.showConfirmDialog(R.string.langmandatorytitle,
                R.string.langmandatorytext, this, false,
                (dialog, which) -> {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    displayLanguagesDialog();
                });
    }

    private void saveLanguages(Set<String> selectedLanguages) {
        surveyLanguagesDataSource.saveLanguagePreferences(survey.getId(),
                selectedLanguages);
        loadLanguages();
        mAdapter.notifyOptionsChanged();
    }

    @NonNull
    private ListView createLanguagesList() {
        List<Language> languages = languageMapper
                .transform(mLanguages, form.getAvailableLanguageCodes());
        final LanguageAdapter languageAdapter = new LanguageAdapter(this, languages);

        final ListView listView = (ListView) LayoutInflater.from(this)
                .inflate(R.layout.languages_list, null);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setAdapter(languageAdapter);
        listView.setOnItemClickListener(
                (parent, view, position, id) -> languageAdapter.updateSelected(position));
        return listView;
    }

    @Override
    public void onActivityResult(final int requestCode, int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (mRequestQuestionId == null || resultCode != RESULT_OK) {
            mRequestQuestionId = null;
            return;
        }

        if (mAdapter.getQuestionView(mRequestQuestionId) == null) {
            // Set the result only after the QuestionView is loaded
            mAdapter.setOnTabLoadedListener(() -> {
                setResult(requestCode, intent, mRequestQuestionId);
                mAdapter.setOnTabLoadedListener(null);
            });
        } else {
            setResult(requestCode, intent, mRequestQuestionId);
        }
    }

    private void setResult(int requestCode, Intent intent, String requestQuestionId) {
        switch (requestCode) {
            case ConstantUtil.PHOTO_ACTIVITY_REQUEST:
                onImageTaken();
                break;
            case ConstantUtil.VIDEO_ACTIVITY_REQUEST:
                onVideoTaken(intent.getData());
                break;
            case ConstantUtil.GET_PHOTO_ACTIVITY_REQUEST:
                onImageAcquired(intent.getData());
                break;
            case ConstantUtil.GET_VIDEO_ACTIVITY_REQUEST:
                onVideoAcquired(intent.getData());
                break;
            case ConstantUtil.CADDISFLY_REQUEST:
            case ConstantUtil.SCAN_ACTIVITY_REQUEST:
            case ConstantUtil.PLOTTING_REQUEST:
            case ConstantUtil.SIGNATURE_REQUEST:
            default:
                mAdapter.onQuestionResultReceived(requestQuestionId, intent.getExtras());
                break;
        }
        mRequestQuestionId = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (mRequestQuestionId == null) {
            return;
        }
        mAdapter.onRequestPermissionsResult(requestCode, mRequestQuestionId, permissions,
                grantResults);
        mRequestQuestionId = null;
    }

    public void requestPermissions(@NonNull String[] permissions, int requestCode,
            String questionId) {
        mRequestQuestionId = questionId;
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    private void onVideoAcquired(Uri uri) {
        Bundle mediaData = new Bundle();
        mediaData.putParcelable(ConstantUtil.VIDEO_FILE_KEY, uri);
        mAdapter.onQuestionResultReceived(mRequestQuestionId, mediaData);
    }

    private void onImageAcquired(Uri imageUri) {
        Bundle mediaData = new Bundle();
        mediaData.putParcelable(ConstantUtil.IMAGE_FILE_KEY, imageUri);
        mAdapter.onQuestionResultReceived(mRequestQuestionId, mediaData);
    }

    private void onImageTaken() {
        Bundle mediaData = new Bundle();
        mediaData.putParcelable(ConstantUtil.IMAGE_FILE_KEY, imagePath);
        mediaData.putBoolean(ConstantUtil.PARAM_REMOVE_ORIGINAL, true);
        mAdapter.onQuestionResultReceived(mRequestQuestionId, mediaData);
    }

    private void onVideoTaken(Uri uri) {
        Bundle mediaData = new Bundle();
        mediaData.putBoolean(ConstantUtil.PARAM_REMOVE_ORIGINAL, true);
        mediaData.putParcelable(ConstantUtil.VIDEO_FILE_KEY, uri);
        mAdapter.onQuestionResultReceived(mRequestQuestionId, mediaData);
    }

    private void loadLanguages() {
        Set<String> languagePreferences = surveyLanguagesDataSource
                .getLanguagePreferences(survey.getId());
        mLanguages = languagePreferences.toArray(new String[0]);
    }

    @Override
    public List<QuestionGroup> getQuestionGroups() {
        return form.getQuestionGroups();
    }

    @Override
    public String getDefaultLanguage() {
        return form.getDefaultLanguageCode();
    }

    @Override
    public String[] getLanguages() {
        return mLanguages;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void onSurveySubmit() {
        recordDuration(false);
        saveState();
        presenter.onSubmitPressed(formInstanceId, formId, survey);
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
    public void startSync(boolean isMobileSyncAllowed) {
        DataPointUploadWorker.scheduleUpload(getApplicationContext(), isMobileSyncAllowed);
    }

    @Override
    public void dismiss() {
        readOnly = true;
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void GoToListOfForms() {
        readOnly = true;
        navigator.navigateToRecordActivity(this, dataPointId, survey);
        finish();
        Toast.makeText(getApplicationContext(), R.string.snackbar_submitted, Toast.LENGTH_LONG)
                .show();
    }

    @Override
    public void showErrorExport() {
        snackBarManager.displaySnackBar(rootView, R.string.form_submit_error, this);
    }

    @Override
    public void showFormUpdated() {
        snackBarManager.displaySnackBar(rootView, R.string.form_updated, this);
    }

    @Override
    public void showMobileUploadSetting(long surveyInstanceId) {
        DialogFragment fragment = MobileDataSettingDialog.newInstance(surveyInstanceId);
        fragment.show(getSupportFragmentManager(), MobileDataSettingDialog.TAG);
    }

    @Override
    public void onMobileUploadSet(long instanceId) {
        presenter.onSubmitPressed(instanceId, formId, survey);
    }

    @Override
    public void openQuestion(String questionId) {
        int tab = mAdapter.displayQuestion(questionId);
        if (tab != -1) {
            mPager.setCurrentItem(tab, true);
        }
    }

    @Override
    public Map<String, QuestionResponse> getResponses() {
        return mQuestionResponses;
    }

    @Override
    public void deleteResponse(String questionId) {
        QuestionResponse questionResponse = mQuestionResponses.remove(questionId);
        if (questionResponse != null && questionResponse.isAnswerToRepeatableGroup()) {
            mDatabase.deleteResponse(formInstanceId, questionResponse.getQuestionId(),
                    questionResponse.getIteration() + "");
        } else {
            mDatabase.deleteResponse(formInstanceId, questionId);
        }
    }

    @Override
    public QuestionView getQuestionView(String questionId) {
        return mAdapter.getQuestionView(questionId);
    }

    @Override
    public String getDataPointId() {
        return dataPointId;
    }

    @Override
    public String getFormId() {
        return form.getId();
    }

    /**
     * event handler that can be used to handle events fired by individual
     * questions at the Activity level. Because we can't launch the photo
     * activity from a view (we need to launch it from the activity), the photo
     * question view fires a QuestionInteractionEvent (to which this activity
     * listens). When we get the event, we can then spawn the camera activity.
     * Currently, this method supports handing TAKE_PHOTO_EVENT and
     * VIDEO_TIP_EVENT types
     */
    public void onQuestionInteraction(QuestionInteractionEvent event) {
        if (QuestionInteractionEvent.TAKE_PHOTO_EVENT.equals(event.getEventType())) {
            takePhoto(event);
        } else if (QuestionInteractionEvent.GET_PHOTO_EVENT.equals(event.getEventType())) {
            navigateToGetPhoto(event);
        } else if (QuestionInteractionEvent.TAKE_VIDEO_EVENT.equals(event.getEventType())) {
            navigateToTakeVideo(event);
        } else if (QuestionInteractionEvent.GET_VIDEO_EVENT.equals(event.getEventType())) {
            navigateToGetVideo(event);
        } else if (QuestionInteractionEvent.SCAN_BARCODE_EVENT.equals(event.getEventType())) {
            navigateToBarcodeScanner(event);
        } else if (QuestionInteractionEvent.QUESTION_CLEAR_EVENT.equals(event.getEventType())) {
            clearQuestion(event);
        } else if (QuestionInteractionEvent.QUESTION_ANSWER_EVENT.equals(event.getEventType())) {
            storeAnswer(event);
        } else if (QuestionInteractionEvent.CADDISFLY.equals(event.getEventType())) {
            navigateToCaddisfly(event);
        } else if (QuestionInteractionEvent.PLOTTING_EVENT.equals(event.getEventType())) {
            navigateToGeoShapeActivity(event);
        } else if (QuestionInteractionEvent.ADD_SIGNATURE_EVENT.equals(event.getEventType())) {
            navigateToSignatureActivity(event);
        }
    }

    private void navigateToGetVideo(QuestionInteractionEvent event) {
        recordSourceId(event);
        navigator.navigateToGetVideo(this);
    }

    private void navigateToGetPhoto(QuestionInteractionEvent event) {
        recordSourceId(event);
        navigator.navigateToGetPhoto(this);
    }

    private void navigateToSignatureActivity(QuestionInteractionEvent event) {
        mRequestQuestionId = event.getSource().getQuestion().getId();
        navigator.navigateToSignatureActivity(this, event.getData());
    }

    private void navigateToGeoShapeActivity(QuestionInteractionEvent event) {
        mRequestQuestionId = event.getSource().getQuestion().getId();
        navigator.navigateToCreateGeoShapeActivity(this, event.getData());
    }

    private void navigateToCaddisfly(QuestionInteractionEvent event) {
        mRequestQuestionId = event.getSource().getQuestion().getId();
        navigator.navigateToCaddisfly(this, event.getData(), getString(R.string.caddisfly_test));
    }

    private void storeAnswer(QuestionInteractionEvent event) {
        //TODO: move to presenter + add loading
        if (dataPointId == null) {
            //create datapoint if doesn't exist yet
            dataPointId = mDatabase.createSurveyedLocale(survey.getId());
            formInstanceId = mDatabase.createSurveyRespondent(form.getId(), form.getVersion(), user, dataPointId);
        } else if (formInstanceId == INVALID_INSTANCE_ID) {
            formInstanceId = mDatabase.createSurveyRespondent(form.getId(), form.getVersion(), user, dataPointId);
        }
        String questionIdKey = event.getSource().getQuestion().getId();
        QuestionResponse eventResponse = event.getSource().getResponse();

        // Store the response if it contains a value. Otherwise, delete it
        if (eventResponse != null && eventResponse.hasValue()) {
            Long id = mQuestionResponses.containsKey(questionIdKey) ?
                    mQuestionResponses.get(questionIdKey).getId() : null;
            QuestionResponse responseToSave = new QuestionResponse.QuestionResponseBuilder()
                    .setValue(eventResponse.getValue())
                    .setType(eventResponse.getType())
                    .setId(id)
                    .setSurveyInstanceId(formInstanceId)
                    .setQuestionId(eventResponse.getQuestionId())
                    .setFilename(eventResponse.getFilename())
                    .setIncludeFlag(eventResponse.getIncludeFlag())
                    .setIteration(eventResponse.getIteration())
                    .createQuestionResponse();
            responseToSave = mDatabase.createOrUpdateSurveyResponse(responseToSave);
            mQuestionResponses.put(questionIdKey, responseToSave);
        } else {
            event.getSource().setResponse(null, true);// Invalidate previous response
            deleteResponse(questionIdKey);
        }
    }

    private void clearQuestion(QuestionInteractionEvent event) {
        String questionId = event.getSource().getQuestion().getId();
        deleteResponse(questionId);
    }

    private void navigateToBarcodeScanner(QuestionInteractionEvent event) {
        recordSourceId(event);
        navigator.navigateToBarcodeScanner(this);
    }

    private void recordSourceId(QuestionInteractionEvent event) {
        if (event.getSource() != null) {
            mRequestQuestionId = event.getSource().getQuestion().getId();
        } else {
            Timber.e("Question source was null in the event");
        }
    }

    private void navigateToTakeVideo(QuestionInteractionEvent event) {
        recordSourceId(event);
        navigator.navigateToTakeVideo(this);
    }

    private void takePhoto(QuestionInteractionEvent event) {
        recordSourceId(event);
        File imageTmpFile = mediaFileHelper.getTemporaryImageFile();
        if (imageTmpFile != null) {
            imagePath = FileProvider.getUriForFile(this, ConstantUtil.FILE_PROVIDER_AUTHORITY,
                    imageTmpFile);
            navigator.navigateToTakePhoto(this, imagePath);
        }
        //TODO: notify error taking pictures
    }

    /*
     * Check SD card space. Warn by dialog popup if it is getting low. Return to
     * home screen if completely full.
     */
    private void spaceLeftOnCard() {
        if (PlatformUtil.isEmulator()) {
            return;
        }
        long megaAvailable = storageHelper.getExternalStorageAvailableSpace();

        // keep track of changes
        // assume we had space before
        long lastMegaAvailable = prefs
                .getLong(Prefs.KEY_SPACE_AVAILABLE, Prefs.DEF_VALUE_SPACE_AVAILABLE);
        prefs.setLong(Prefs.KEY_SPACE_AVAILABLE, megaAvailable);

        if (megaAvailable <= 0L) {// All out, OR media not mounted
            // Bounce user
            showConfirmDialog(R.string.nocardspacetitle, R.string.nocardspacedialog, this, false,
                    (dialog, which) -> {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        finish();
                    }
            );
            return;
        }

        // just issue a warning if we just descended to or past a number on the list
        if (megaAvailable < lastMegaAvailable) {
            for (long l = megaAvailable; l < lastMegaAvailable; l++) {
                if (ConstantUtil.SPACE_WARNING_MB_LEVELS.contains(Long.toString(l))) {
                    // display how much space is left
                    //TODO: replace "%%%" by "%s" and use String formatting
                    String message = getResources().getString(R.string.lowcardspacedialog);
                    message = message.replace("%%%", Long.toString(megaAvailable));
                    showConfirmDialog(R.string.lowcardspacetitle, message, this, false,
                            (dialog, which) -> {
                                if (dialog != null) {
                                    dialog.dismiss();
                                }
                            },
                            null);
                    return; // only one warning per survey, even of we passed >1 limit
                }
            }
        }
    }

    @Override
    public void confirmGeoFieldReset(String questionId) {
        View viewWithTag = mPager.findViewWithTag(questionId);
        if (viewWithTag instanceof GeoQuestionView) {
            ((GeoQuestionView) viewWithTag).startListeningToLocation();
        }
    }
}
