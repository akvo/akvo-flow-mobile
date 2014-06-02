/*
 *  Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

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
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.ImageUtil;
import org.akvo.flow.util.LangsPreferenceData;
import org.akvo.flow.util.LangsPreferenceUtil;
import org.akvo.flow.util.ViewUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurveyActivity extends ActionBarActivity implements SurveyListener,
        QuestionInteractionListener {
    private static final String TAG = SurveyActivity.class.getSimpleName();

    private static final int PHOTO_ACTIVITY_REQUEST = 1;
    private static final int VIDEO_ACTIVITY_REQUEST = 2;
    private static final int SCAN_ACTIVITY_REQUEST  = 3;

    private static final String TEMP_PHOTO_NAME_PREFIX = "image";
    private static final String TEMP_VIDEO_NAME_PREFIX = "video";
    private static final String IMAGE_SUFFIX = ".jpg";
    private static final String VIDEO_SUFFIX = ".mp4";

    /**
     * When a request is done to perform photo, video, barcode scan, etc we store
     * the question id, so we can notify later the result of such operation.
     */
    private String mRequestQuestionId;

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
        setContentView(R.layout.survey_activity);

        // Read all the params. Note that the survey instance id is now mandatory
        final String surveyId = getIntent().getStringExtra(ConstantUtil.SURVEY_ID_KEY);
        mReadOnly = getIntent().getBooleanExtra(ConstantUtil.READONLY_KEY, false);
        mSurveyInstanceId = getIntent().getLongExtra(ConstantUtil.RESPONDENT_ID_KEY, 0);
        mSurveyGroup = (SurveyGroup)getIntent().getSerializableExtra(ConstantUtil.SURVEY_GROUP);
        mRecordId = getIntent().getStringExtra(ConstantUtil.SURVEYED_LOCALE_ID);

        mQuestionResponses = new HashMap<String, QuestionResponse>();
        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();

        loadSurvey(surveyId);// Load Survey. This task would be better off if executed in a worker thread
        loadLanguages();

        if (mSurvey == null) {
            Log.e(TAG, "mSurvey is null. Finishing the Activity...");
            finish();
        }

        // Set the survey name as Activity title
        setTitle(mSurvey.getName());
        ViewPager pager = (ViewPager)findViewById(R.id.pager);
        mAdapter = new SurveyTabAdapter(this, getSupportActionBar(), pager, this, this);
        mAdapter.load();// Instantiate tabs and views. TODO: Lazy loading with fragments!
        pager.setAdapter(mAdapter);

        // Initialize new survey or load previous responses
        Map<String, QuestionResponse> responses = mDatabase.getResponses(mSurveyInstanceId);
        if (responses.isEmpty()) {
            displayPrefillDialog();
        } else {
            loadState(responses);
        }

        spaceLeftOnCard();
    }

    /**
     * Display prefill option dialog, if applies. This feature is only available
     * for monitored groups, when a new survey instance is created, allowing users
     * to 'clone' responses from the previous response.
     */
    private void displayPrefillDialog() {
        if (!mSurveyGroup.isMonitored()) {
            return;// Do nothing, as prefill option is not available in the current context
        }

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
        loadState(responses);
    }

    private void loadSurvey(String surveyId) {
        Survey surveyMeta = mDatabase.getSurvey(surveyId);
        InputStream in = null;
        try {
            // load from file
            in = FileUtil.getFileInputStream(surveyMeta.getFileName(),
                    ConstantUtil.DATA_DIR, false, this);
            mSurvey = SurveyDao.loadSurvey(surveyMeta, in);
            mSurvey.setId(surveyId);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not load survey xml file");
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException e) {}
            }
        }
    }

    /**
     * Load state for the current survey instance
     */
    private void loadState() {
        Map<String, QuestionResponse> responses = mDatabase.getResponses(mSurveyInstanceId);
        loadState(responses);
    }

    /**
     * Load state with the provided responses map
     */
    private void loadState(Map<String, QuestionResponse> responses) {
        mQuestionResponses = responses;
        mAdapter.loadState();
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
            for (QuestionResponse response : mQuestionResponses.values()) {
                // Store the response if it contains a value. Otherwise, delete it
                if (response.hasValue()) {
                    response.setRespondentId(mSurveyInstanceId);
                    mDatabase.createOrUpdateSurveyResponse(response);
                } else if (response.getId() != null && response.getId() > 0) {
                    // if we don't have a value BUT there is an ID, we need to
                    // remove it since the user blanked out their response
                    mDatabase.deleteResponse(mSurveyInstanceId, response.getQuestionId());
                }
            }
            mDatabase.updateSurveyStatus(mSurveyInstanceId, SurveyInstanceStatus.SAVED);
            mDatabase.updateRecordModifiedDate(mRecordId, System.currentTimeMillis());

            // Record meta-data, if applies
            if (mSurvey.getId().equals(mSurveyGroup.getRegisterSurveyId())) {
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
        if (localeNameQuestions.size() > 0) {
            for (int i=0; i<localeNameQuestions.size(); i++) {
                QuestionResponse questionResponse = mDatabase.getResponse(mSurveyInstanceId,
                        localeNameQuestions.get(i));

                String answer = questionResponse != null ? questionResponse.getValue() : null;

                if (!TextUtils.isEmpty(answer)) {
                    if (i > 0) {
                        builder.append(" - ");
                    }
                    builder.append(answer);
                }
            }
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
        recordDuration(true);// Keep track of this session's duration.
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.onPause();
        recordDuration(false);
        saveState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.survey_activity, menu);
        if (isReadOnly()) {
            SubMenu subMenu = menu.findItem(R.id.more_submenu).getSubMenu();
            subMenu.removeItem(R.id.new_survey);
            subMenu.removeItem(R.id.clear);
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearSurvey() {
        ViewUtil.showConfirmDialog(R.string.cleartitle, R.string.cleardesc, this, true,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDatabase.deleteResponses(String.valueOf(mSurveyInstanceId));
                        loadState();
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
        final int[] langsSelectedMasterIndexArray = langsPrefData.getLangsSelectedMasterIndexArray();

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
        if (mRequestQuestionId == null) {
            return;// Move along, nothing to see here
        }

        if (requestCode == PHOTO_ACTIVITY_REQUEST || requestCode == VIDEO_ACTIVITY_REQUEST) {
            if (resultCode == RESULT_OK) {
                String filePrefix, fileSuffix;
                if (requestCode == PHOTO_ACTIVITY_REQUEST) {
                    filePrefix = TEMP_PHOTO_NAME_PREFIX;
                    fileSuffix = IMAGE_SUFFIX;
                } else {
                    filePrefix = TEMP_VIDEO_NAME_PREFIX;
                    fileSuffix = VIDEO_SUFFIX;
                }

                File f = new File(Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + File.separator + filePrefix + fileSuffix);

                // Ensure no image is saved in the DCIM folder
                FileUtil.cleanDCIM(this, f.getAbsolutePath());

                String newFilename = filePrefix + System.nanoTime() + fileSuffix;
                String newPath = FileUtil.getStorageDirectory(ConstantUtil.SURVEYAL_DIR,
                        newFilename, false);
                FileUtil.findOrCreateDir(newPath);
                String absoluteFile = newPath + File.separator + newFilename;

                int maxImgSize = ConstantUtil.IMAGE_SIZE_320_240;
                String maxImgSizePref = mDatabase.getPreference(ConstantUtil.MAX_IMG_SIZE);
                if (!TextUtils.isEmpty(maxImgSizePref)) {
                    maxImgSize = Integer.valueOf(maxImgSizePref);
                }

                String sizeTxt = getResources().getStringArray(R.array.max_image_size_pref)[maxImgSize];

                if (ImageUtil.resizeImage(f.getAbsolutePath(), absoluteFile, maxImgSize)) {
                    Toast.makeText(this, "Image resized to " + sizeTxt, Toast.LENGTH_LONG).show();
                    if (!f.delete()) { // must check return value to know if it failed
                        Log.e(TAG, "Media file delete failed");
                    }
                } else if (!f.renameTo(new File(absoluteFile))) {
                    // must check  return  value to  know if it  failed!
                    Log.e(TAG, "Media file resize failed");
                }

                Bundle photoData = new Bundle();
                photoData.putString(ConstantUtil.MEDIA_FILE_KEY, absoluteFile);
                mAdapter.onQuestionComplete(mRequestQuestionId, photoData);
            } else {
                Log.e(TAG, "Result of camera op was not ok: " + resultCode);
            }
        } else if (requestCode == SCAN_ACTIVITY_REQUEST && resultCode == RESULT_OK) {
            mAdapter.onQuestionComplete(mRequestQuestionId, data.getExtras());
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
    public Map<String, QuestionResponse> getResponses() {
        return mQuestionResponses;
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
        mDatabase.updateSurveyStatus(mSurveyInstanceId, SurveyDbAdapter.SurveyInstanceStatus.SUBMITTED);

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
                            finish();
                        }
                    }
                });
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
            i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(Environment
                    .getExternalStorageDirectory().getAbsolutePath() + File.separator
                    + TEMP_PHOTO_NAME_PREFIX + IMAGE_SUFFIX)));
            if (event.getSource() != null) {
                mRequestQuestionId = event.getSource().getQuestion().getId();
            } else {
                Log.e(TAG, "Question source was null in the event");
            }

            startActivityForResult(i, PHOTO_ACTIVITY_REQUEST);
        } else if (QuestionInteractionEvent.TAKE_VIDEO_EVENT.equals(event.getEventType())) {
            // fire off the intent
            Intent i = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
            i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(Environment
                    .getExternalStorageDirectory().getAbsolutePath() + File.separator
                    + TEMP_VIDEO_NAME_PREFIX + VIDEO_SUFFIX)));
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
            mQuestionResponses.remove(questionId);
            mDatabase.deleteResponse(mSurveyInstanceId, questionId);
        } else if (QuestionInteractionEvent.QUESTION_ANSWER_EVENT.equals(event.getEventType())) {
            String questionId = event.getSource().getQuestion().getId();
            QuestionResponse response = event.getSource().getResponse();
            if (response != null) {
                mQuestionResponses.put(questionId, response);
            } else {
                mQuestionResponses.remove(questionId);
            }
            // TODO: Should we save this Response to the DB straightaway?
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

}
