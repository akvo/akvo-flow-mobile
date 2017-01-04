/*
 *  Copyright (C) 2014-2016 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.activity;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import org.akvo.flow.R;
import org.akvo.flow.dao.SurveyDao;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceStatus;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyedLocaleMeta;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.event.QuestionInteractionEvent;
import org.akvo.flow.event.QuestionInteractionListener;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.ui.adapter.SurveyTabAdapter;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FileUtil.FileType;
import org.akvo.flow.util.ImageUtil;
import org.akvo.flow.util.LangsPreferenceData;
import org.akvo.flow.util.LangsPreferenceUtil;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.ViewUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormActivity extends BackActivity implements SurveyListener,
        QuestionInteractionListener {
    private static final String TAG = FormActivity.class.getSimpleName();

    private static final int PHOTO_ACTIVITY_REQUEST = 1;
    private static final int VIDEO_ACTIVITY_REQUEST = 2;
    private static final int SCAN_ACTIVITY_REQUEST = 3;
    private static final int EXTERNAL_SOURCE_REQUEST = 4;
    private static final int CADDISFLY_REQUEST = 5;
    private static final int PLOTTING_REQUEST = 6;
    private static final int SIGNATURE_REQUEST = 7;

    private static final String TEMP_PHOTO_NAME_PREFIX = "image";
    private static final String TEMP_VIDEO_NAME_PREFIX = "video";
    private static final String IMAGE_SUFFIX = ".jpg";
    private static final String VIDEO_SUFFIX = ".mp4";

    /**
     * When a request is done to perform photo, video, barcode scan, etc we store
     * the question id, so we can notify later the result of such operation.
     */
    private String mRequestQuestionId;

    private ViewPager mPager;
    private SurveyTabAdapter mAdapter;

    private boolean mReadOnly;//flag to represent whether the Survey can be edited or not
    private long mSurveyInstanceId;
    private long mSessionStartTime;
    private String mRecordId;
    private SurveyGroup mSurveyGroup;
    private Survey mSurvey;
    private SurveyDbAdapter mDatabase;

    private String[] mLanguages;

    private Map<String, QuestionResponse> mQuestionResponses;// QuestionId - QuestionResponse

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.form_activity);

        // Read all the params. Note that the survey instance id is now mandatory
        final String surveyId = getIntent().getStringExtra(ConstantUtil.SURVEY_ID_KEY);
        mReadOnly = getIntent().getBooleanExtra(ConstantUtil.READONLY_KEY, false);
        mSurveyInstanceId = getIntent().getLongExtra(ConstantUtil.RESPONDENT_ID_KEY, 0);
        mSurveyGroup = (SurveyGroup) getIntent().getSerializableExtra(ConstantUtil.SURVEY_GROUP);
        mRecordId = getIntent().getStringExtra(ConstantUtil.SURVEYED_LOCALE_ID);

        mQuestionResponses = new HashMap<>();
        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();

        loadSurvey(
                surveyId);// Load Survey. This task would be better off if executed in a worker thread
        loadLanguages();

        if (mSurvey == null) {
            Log.e(TAG, "mSurvey is null. Finishing the Activity...");
            finish();
        }

        // Set the survey name as Activity title
        getSupportActionBar().setTitle(mSurvey.getName());
        getSupportActionBar().setSubtitle("v " + getVersion());

        mPager = (ViewPager) findViewById(R.id.pager);
        mAdapter = new SurveyTabAdapter(this, getSupportActionBar(), mPager, this, this);
        mPager.setAdapter(mAdapter);

        // Initialize new survey or load previous responses
        Map<String, QuestionResponse> responses = mDatabase.getResponses(mSurveyInstanceId);
        if (!responses.isEmpty()) {
            displayResponses(responses);
        }

        spaceLeftOnCard();
        Log.d(TAG, "form activity");
    }

    /**
     * Display prefill option dialog, if applies. This feature is only available
     * for monitored groups, when a new survey instance is created, allowing users
     * to 'clone' responses from the previous response.
     */
    private void displayPrefillDialog() {
        final Long lastSurveyInstance = mDatabase.getLastSurveyInstance(mRecordId, mSurvey.getId());
        if (lastSurveyInstance != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.prefill_title);
            builder.setMessage(R.string.prefill_text);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    prefillSurvey(lastSurveyInstance);
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            builder.show();
        }
    }

    private void prefillSurvey(long prefillSurveyInstance) {
        Map<String, QuestionResponse> responses = mDatabase.getResponses(prefillSurveyInstance);
        for (QuestionResponse response : responses.values()) {
            // Adapt(clone) responses for the current survey instance:
            // Get rid of its Id and update the SurveyInstance Id
            response.setId(null);
            response.setRespondentId(mSurveyInstanceId);
        }
        displayResponses(responses);
    }

    private void loadSurvey(String surveyId) {
        Survey surveyMeta = mDatabase.getSurvey(surveyId);
        InputStream in = null;
        try {
            // load from file
            File file = new File(FileUtil.getFilesDir(FileType.FORMS), surveyMeta.getFileName());
            in = new FileInputStream(file);
            mSurvey = SurveyDao.loadSurvey(surveyMeta, in);
            mSurvey.setId(surveyId);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not load survey xml file");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private double getVersion() {
        double version = 0.0;
        Cursor c = mDatabase.getFormInstance(mSurveyInstanceId);
        if (c.moveToFirst()) {
            version = c.getDouble(SurveyDbAdapter.FormInstanceQuery.VERSION);
        }
        c.close();

        if (version == 0.0) {
            version = mSurvey.getVersion();// Default to current value
        }

        return version;
    }

    /**
     * Load state for the current survey instance
     */
    private void loadResponses() {
        Map<String, QuestionResponse> responses = mDatabase.getResponses(mSurveyInstanceId);
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
        if (mReadOnly) {
            return;
        }

        final long time = System.currentTimeMillis();

        if (start) {
            mSessionStartTime = time;
        } else {
            mDatabase.addSurveyDuration(mSurveyInstanceId, time - mSessionStartTime);
            // Restart the current session timer, in case we receive subsequent calls
            // to record the time, w/o setting up the timer first.
            mSessionStartTime = time;
        }
    }

    private void saveState() {
        if (!mReadOnly) {
            mDatabase.updateSurveyStatus(mSurveyInstanceId, SurveyInstanceStatus.SAVED);
            mDatabase.updateRecordModifiedDate(mRecordId, System.currentTimeMillis());

            // Record meta-data, if applies
            if (!mSurveyGroup.isMonitored() ||
                    mSurvey.getId().equals(mSurveyGroup.getRegisterSurveyId())) {
                saveRecordMetaData();
            }
        }
    }

    private void saveRecordMetaData() {
        // META_NAME
        StringBuilder builder = new StringBuilder();
        List<String> localeNameQuestions = mSurvey.getLocaleNameQuestions();

        // Check the responses given to these questions (marked as name)
        // and concatenate them so it becomes the Locale name.
        if (!localeNameQuestions.isEmpty()) {
            boolean first = true;
            for (String questionId : localeNameQuestions) {
                QuestionResponse questionResponse = mDatabase
                        .getResponse(mSurveyInstanceId, questionId);
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

            mDatabase.updateSurveyedLocale(mSurveyInstanceId, builder.toString(),
                    SurveyedLocaleMeta.NAME);
        }

        // META_GEO
        String localeGeoQuestion = mSurvey.getLocaleGeoQuestion();
        if (localeGeoQuestion != null) {
            QuestionResponse response = mDatabase.getResponse(mSurveyInstanceId, localeGeoQuestion);
            if (response != null) {
                mDatabase.updateSurveyedLocale(mSurveyInstanceId, response.getValue(),
                        SurveyedLocaleMeta.GEOLOCATION);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.onResume();
        recordDuration(true);// Keep track of this session's duration.
        if (Boolean.valueOf(mDatabase.getPreference(ConstantUtil.SCREEN_ON_KEY))) {
            mPager.setKeepScreenOn(true);
        }
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
        super.onDestroy();
        mAdapter.onDestroy();
        mDatabase.close();
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
            if (!mSurveyGroup.isMonitored() ||
                    mDatabase.getLastSurveyInstance(mRecordId, mSurvey.getId()) == null) {
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
                displayPrefillDialog();
                return true;
            case R.id.view_map:
                startActivity(new Intent(this, MapActivity.class)
                        .putExtra(ConstantUtil.SURVEYED_LOCALE_ID, mRecordId));
                return true;
            case R.id.transmission:
                startActivity(new Intent(this, TransmissionHistoryActivity.class)
                        .putExtra(ConstantUtil.RESPONDENT_ID_KEY, mSurveyInstanceId));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearSurvey() {
        ViewUtil.showConfirmDialog(R.string.cleartitle, R.string.cleardesc, this, true,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDatabase.deleteResponses(String.valueOf(mSurveyInstanceId));
                        loadResponses();
                        spaceLeftOnCard();
                    }
                });
    }

    private void displayLanguagesDialog() {
        // TODO: language management should be simplified
        LangsPreferenceData langsPrefData = LangsPreferenceUtil.createLangPrefData(this,
                mDatabase.getPreference(ConstantUtil.SURVEY_LANG_SETTING_KEY),
                mDatabase.getPreference(ConstantUtil.SURVEY_LANG_PRESENT_KEY));

        final String[] langsSelectedNameArray = langsPrefData.getLangsSelectedNameArray();
        final boolean[] langsSelectedBooleanArray = langsPrefData.getLangsSelectedBooleanArray();
        final int[] langsSelectedMasterIndexArray = langsPrefData
                .getLangsSelectedMasterIndexArray();

        ViewUtil.displayLanguageSelector(this, langsSelectedNameArray,
                langsSelectedBooleanArray,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int clicked) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }

                        mDatabase.savePreference(ConstantUtil.SURVEY_LANG_SETTING_KEY,
                                LangsPreferenceUtil.formLangPreferenceString(
                                        langsSelectedBooleanArray,
                                        langsSelectedMasterIndexArray));

                        loadLanguages();
                        mAdapter.notifyOptionsChanged();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mRequestQuestionId == null || resultCode != RESULT_OK) {
            mRequestQuestionId = null;
            return;// Move along, nothing to see here
        }

        switch (requestCode) {
            case PHOTO_ACTIVITY_REQUEST:
            case VIDEO_ACTIVITY_REQUEST:
                String fileSuffix =
                        requestCode == PHOTO_ACTIVITY_REQUEST ? IMAGE_SUFFIX : VIDEO_SUFFIX;
                File tmp = getTmpFile(requestCode == PHOTO_ACTIVITY_REQUEST);

                // Ensure no image is saved in the DCIM folder
                FileUtil.cleanDCIM(this, tmp.getAbsolutePath());

                String filename = PlatformUtil.uuid() + fileSuffix;
                File imgFile = new File(FileUtil.getFilesDir(FileType.MEDIA), filename);

                int maxImgSize = ConstantUtil.IMAGE_SIZE_320_240;
                String maxImgSizePref = mDatabase.getPreference(ConstantUtil.MAX_IMG_SIZE);
                if (!TextUtils.isEmpty(maxImgSizePref)) {
                    maxImgSize = Integer.valueOf(maxImgSizePref);
                }

                if (ImageUtil.resizeImage(tmp.getAbsolutePath(), imgFile.getAbsolutePath(),
                        maxImgSize)) {
                    Log.i(TAG, "Image resized to: " +
                            getResources().getStringArray(R.array.max_image_size_pref)[maxImgSize]);
                    if (!tmp.delete()) { // must check return value to know if it failed
                        Log.e(TAG, "Media file delete failed");
                    }
                } else if (!tmp.renameTo(imgFile)) {
                    // must check  return  value to  know if it  failed!
                    Log.e(TAG, "Media file resize failed");
                }

                Bundle photoData = new Bundle();
                photoData.putString(ConstantUtil.MEDIA_FILE_KEY, imgFile.getAbsolutePath());
                mAdapter.onQuestionComplete(mRequestQuestionId, photoData);
                break;
            case EXTERNAL_SOURCE_REQUEST:
            case CADDISFLY_REQUEST:
            case SCAN_ACTIVITY_REQUEST:
            case PLOTTING_REQUEST:
            case SIGNATURE_REQUEST:
            default:
                mAdapter.onQuestionComplete(mRequestQuestionId, data.getExtras());
                break;
        }

        mRequestQuestionId = null;// Reset the tmp reference
    }

    private String getDefaultLang() {
        String lang = mSurvey.getLanguage();
        if (TextUtils.isEmpty(lang)) {
            lang = ConstantUtil.ENGLISH_CODE;
        }
        return lang;
    }

    private void loadLanguages() {
        String langsSelection = mDatabase.getPreference(ConstantUtil.SURVEY_LANG_SETTING_KEY);
        String langsPresentIndexes = mDatabase.getPreference(ConstantUtil.SURVEY_LANG_PRESENT_KEY);
        LangsPreferenceData langsPrefData = LangsPreferenceUtil.createLangPrefData(this,
                langsSelection, langsPresentIndexes);
        mLanguages = LangsPreferenceUtil.getSelectedLangCodes(this,
                langsPrefData.getLangsSelectedMasterIndexArray(),
                langsPrefData.getLangsSelectedBooleanArray(),
                R.array.alllanguagecodes);
    }

    @Override
    public List<QuestionGroup> getQuestionGroups() {
        return mSurvey.getQuestionGroups();
    }

    @Override
    public String getDefaultLanguage() {
        return getDefaultLang();
    }

    @Override
    public String[] getLanguages() {
        return mLanguages;
    }

    @Override
    public boolean isReadOnly() {
        return mReadOnly;
    }

    @Override
    public void onSurveySubmit() {
        recordDuration(false);
        saveState();

        // if we have no missing responses, submit the survey
        mDatabase.updateSurveyStatus(mSurveyInstanceId,
                SurveyDbAdapter.SurveyInstanceStatus.SUBMITTED);

        // Make the current survey immutable
        mReadOnly = true;

        // send a broadcast message indicating new data is available
        Intent i = new Intent(ConstantUtil.DATA_AVAILABLE_INTENT);
        sendBroadcast(i);

        ViewUtil.showConfirmDialog(R.string.submitcompletetitle, R.string.submitcompletetext,
                this, false,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (dialog != null) {
                            setResult(RESULT_OK);
                            finish();
                        }
                    }
                });
    }

    @Override
    public void nextTab() {
        mPager.setCurrentItem(mPager.getCurrentItem() + 1, true);
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
        mQuestionResponses.remove(questionId);
        mDatabase.deleteResponse(mSurveyInstanceId, questionId);
    }

    @Override
    public QuestionView getQuestionView(String questionId) {
        return mAdapter.getQuestionView(questionId);
    }

    @Override
    public String getDatapointId() {
        return mRecordId;
    }

    @Override
    public String getFormId() {
        return mSurvey.getId();
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
            // fire off the intent
            Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(getTmpFile(true)));
            if (event.getSource() != null) {
                mRequestQuestionId = event.getSource().getQuestion().getId();
            } else {
                Log.e(TAG, "Question source was null in the event");
            }

            startActivityForResult(i, PHOTO_ACTIVITY_REQUEST);
        } else if (QuestionInteractionEvent.TAKE_VIDEO_EVENT.equals(event.getEventType())) {
            // fire off the intent
            Intent i = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
            i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(getTmpFile(false)));
            if (event.getSource() != null) {
                mRequestQuestionId = event.getSource().getQuestion().getId();
            } else {
                Log.e(TAG, "Question source was null in the event");
            }

            startActivityForResult(i, VIDEO_ACTIVITY_REQUEST);
        } else if (QuestionInteractionEvent.SCAN_BARCODE_EVENT.equals(event.getEventType())) {
            Intent intent = new Intent(ConstantUtil.BARCODE_SCAN_INTENT);
            try {
                startActivityForResult(intent, SCAN_ACTIVITY_REQUEST);
                if (event.getSource() != null) {
                    mRequestQuestionId = event.getSource().getQuestion().getId();
                } else {
                    Log.e(TAG, "Question source was null in the event");
                }
            } catch (ActivityNotFoundException ex) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.barcodeerror);
                builder.setPositiveButton(R.string.okbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                builder.show();
            }
        } else if (QuestionInteractionEvent.QUESTION_CLEAR_EVENT.equals(event.getEventType())) {
            String questionId = event.getSource().getQuestion().getId();
            deleteResponse(questionId);
        } else if (QuestionInteractionEvent.QUESTION_ANSWER_EVENT.equals(event.getEventType())) {
            String questionId = event.getSource().getQuestion().getId();
            QuestionResponse response = event.getSource().getResponse();

            // Store the response if it contains a value. Otherwise, delete it
            if (response != null && response.hasValue()) {
                mQuestionResponses.put(questionId, response);
                response.setRespondentId(mSurveyInstanceId);
                mDatabase.createOrUpdateSurveyResponse(response);
            } else {
                event.getSource().setResponse(null, true);// Invalidate previous response
                deleteResponse(questionId);
            }
        } else if (QuestionInteractionEvent.EXTERNAL_SOURCE_EVENT.equals(event.getEventType())) {
            mRequestQuestionId = event.getSource().getQuestion().getId();
            Intent intent = new Intent(ConstantUtil.EXTERNAL_SOURCE_ACTION);
            intent.putExtras(event.getData());
            intent.setType(ConstantUtil.CADDISFLY_MIME);
            startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.use_external_source)),
                    +EXTERNAL_SOURCE_REQUEST);
        } else if (QuestionInteractionEvent.CADDISFLY.equals(event.getEventType())) {
            mRequestQuestionId = event.getSource().getQuestion().getId();
            Intent intent = new Intent(ConstantUtil.CADDISFLY_ACTION);
            intent.putExtras(event.getData());
            intent.setType(ConstantUtil.CADDISFLY_MIME);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.caddisfly_test)),
                    +CADDISFLY_REQUEST);
        } else if (QuestionInteractionEvent.PLOTTING_EVENT.equals(event.getEventType())) {
            Intent i = new Intent(this, GeoshapeActivity.class);
            if (event.getData() != null) {
                i.putExtras(event.getData());
            }
            mRequestQuestionId = event.getSource().getQuestion().getId();
            startActivityForResult(i, PLOTTING_REQUEST);
        } else if (QuestionInteractionEvent.ADD_SIGNATURE_EVENT.equals(event.getEventType())) {
            Intent i = new Intent(this, SignatureActivity.class);
            mRequestQuestionId = event.getSource().getQuestion().getId();
            startActivityForResult(i, SIGNATURE_REQUEST);
        }
    }

    /*
     * Check SD card space. Warn by dialog popup if it is getting low. Return to
     * home screen if completely full.
     */
    public void spaceLeftOnCard() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // TODO: more specific warning if card not mounted?
        }
        // compute space left
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        double sdAvailSize = (double) stat.getAvailableBlocks()
                * (double) stat.getBlockSize();
        // One binary gigabyte equals 1,073,741,824 bytes.
        // double gigaAvailable = sdAvailSize / 1073741824;
        // One binary megabyte equals 1 048 576 bytes.
        long megaAvailable = (long) Math.floor(sdAvailSize / 1048576.0);

        // keep track of changes
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        // assume we had space before
        long lastMegaAvailable = settings.getLong("cardMBAvaliable", 101L);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("cardMBAvaliable", megaAvailable);
        // Commit the edits!
        editor.commit();

        if (megaAvailable <= 0L) {// All out, OR media not mounted
            // Bounce user
            ViewUtil.showConfirmDialog(R.string.nocardspacetitle,
                    R.string.nocardspacedialog, this, false,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (dialog != null) {
                                dialog.dismiss();
                            }
                            finish();
                        }
                    }
            );
            return;
        }

        // just issue a warning if we just descended to or past a number on the list
        if (megaAvailable < lastMegaAvailable) {
            for (long l = megaAvailable; l < lastMegaAvailable; l++) {
                if (ConstantUtil.SPACE_WARNING_MB_LEVELS.contains(Long.toString(l))) {
                    // display how much space is left
                    String s = getResources().getString(R.string.lowcardspacedialog);
                    s = s.replace("%%%", Long.toString(megaAvailable));
                    ViewUtil.showConfirmDialog(
                            R.string.lowcardspacetitle,
                            s,
                            this,
                            false,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (dialog != null) {
                                        dialog.dismiss();
                                    }
                                }
                            },
                            null);
                    return; // only one warning per survey, even of we passed >1 limit
                }
            }
        }
    }

    private File getTmpFile(boolean image) {
        String filename = image ? TEMP_PHOTO_NAME_PREFIX + IMAGE_SUFFIX
                : TEMP_VIDEO_NAME_PREFIX + VIDEO_SUFFIX;
        return new File(FileUtil.getFilesDir(FileType.TMP), filename);
    }

}
