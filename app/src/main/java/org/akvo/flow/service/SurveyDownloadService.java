/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import org.akvo.flow.R;
import org.akvo.flow.api.FlowApi;
import org.akvo.flow.api.S3Api;
import org.akvo.flow.data.dao.SurveyDao;
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionHelp;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.util.ConnectivityStateManager;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FileUtil.FileType;
import org.akvo.flow.util.HttpUtil;
import org.akvo.flow.util.NotificationHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;

import timber.log.Timber;

import static org.akvo.flow.util.ConstantUtil.ACTION_SURVEY_SYNC;

/**
 * This activity will check for new surveys on the device and install as needed
 *
 * @author Christopher Fagiani
 */
public class SurveyDownloadService extends IntentService {

    private static final String TAG = "SURVEY_DOWNLOAD_SERVICE";

    /**
     * Intent parameter to specify which survey needs to be downloaded
     */
    public static final String EXTRA_SURVEY_ID = "survey";
    public static final String EXTRA_DELETE_SURVEYS = "delete_surveys";

    private static final String DEFAULT_TYPE = "Survey";
    public static final String TEST_SURVEY_ID = "0";

    private SurveyDbDataSource databaseAdaptor;
    private Prefs prefs;
    private ConnectivityStateManager connectivityStateManager;

    public SurveyDownloadService() {
        super(TAG);
    }

    public void onHandleIntent(@Nullable Intent intent) {
        try {
            databaseAdaptor = new SurveyDbDataSource(this, null);
            databaseAdaptor.open();
            prefs = new Prefs(getApplicationContext());
            connectivityStateManager = new ConnectivityStateManager(getApplicationContext());
            if (intent != null && intent.hasExtra(EXTRA_SURVEY_ID)) {
                downloadSurvey(intent);
            } else if (intent != null && intent.getBooleanExtra(EXTRA_DELETE_SURVEYS, false)) {
                reDownloadAllSurveys(intent);
            } else {
                checkAndDownload(null);
            }
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        } finally {
            databaseAdaptor.close();
            sendBroadcastNotification();
        }
    }

    private void reDownloadAllSurveys(@NonNull Intent intent) {
        intent.removeExtra(EXTRA_DELETE_SURVEYS);
        String[] surveyIds = databaseAdaptor.getSurveyIds();
        databaseAdaptor.deleteAllSurveys();
        checkAndDownload(surveyIds);
    }

    private void downloadSurvey(@NonNull Intent intent) {
        String surveyId = intent.getStringExtra(EXTRA_SURVEY_ID);
        intent.removeExtra(EXTRA_SURVEY_ID);
        if (TEST_SURVEY_ID.equals(surveyId)) {
            databaseAdaptor.reinstallTestSurvey();
        } else {
            checkAndDownload(new String[] { surveyId });
        }
    }

    /**
     * if no surveyIds are passed in, this will check for new surveys and, if
     * there are some new ones, downloads them to the DATA_DIR. If surveyIds are
     * passed in, then those specific surveys will be downloaded. If they're already
     * on the device, the surveys will be replaced with the new ones.
     */
    private void checkAndDownload(@Nullable String[] surveyIds) {
        if (!connectivityStateManager.isConnectionAvailable(
                prefs.getBoolean(Prefs.KEY_CELL_UPLOAD, Prefs.DEFAULT_VALUE_CELL_UPLOAD))) {
            //No internet or not allowed to sync
            return;
        }

        List<Survey> surveys;
        if (surveyIds != null && surveyIds.length > 0) {
            surveys = getSurveyHeaders(surveyIds);
        } else {
            surveys = checkForSurveys();
        }

        // Update all survey groups
        syncSurveyGroups(surveys);

        // Check synced versions, and omit up-to-date surveys
        surveys = databaseAdaptor.checkSurveyVersions(surveys);

        if (!surveys.isEmpty()) {
            int synced = 0, failed = 0;
            displayNotification(synced, failed, surveys.size());
            for (Survey survey : surveys) {
                try {
                    downloadSurvey(survey);
                    databaseAdaptor.saveSurvey(survey);
                    downloadResources(survey);
                    synced++;
                } catch (IOException e) {
                    failed++;
                    Timber.e(e, "Error downloading survey: " + survey.getId());
                    displayErrorNotification(ConstantUtil.NOTIFICATION_FORM_ERROR,
                            getString(R.string.error_form_download));
                }
                displayNotification(synced, failed, surveys.size());
            }
        }

        // now check if any previously downloaded surveys still need
        // don't have their help media pre-cached
        surveys = databaseAdaptor.getSurveyList(SurveyGroup.ID_NONE);
        for (Survey survey : surveys) {
            if (!survey.isHelpDownloaded()) {
                downloadResources(survey);
            }
        }
    }

    private void syncSurveyGroups(@NonNull List<Survey> surveys) {
        for (Survey s : surveys) {
            // Assign registration form id, if missing.
            SurveyGroup sg = s.getSurveyGroup();
            if (sg != null && sg.getRegisterSurveyId() == null) {
                sg.setRegisterSurveyId(s.getId());
            }
            databaseAdaptor.addSurveyGroup(sg);
        }
    }

    /**
     * Downloads the survey based on the ID and then updates the survey object
     * with the filename and location
     */
    private void downloadSurvey(@NonNull Survey survey) throws IOException {
        final String filename = survey.getId() + ConstantUtil.ARCHIVE_SUFFIX;
        final String objectKey = ConstantUtil.S3_SURVEYS_DIR + filename;
        final File file = new File(FileUtil.getFilesDir(FileType.FORMS), filename);

        S3Api s3Api = new S3Api(this);
        s3Api.get(objectKey, file); // Download zip file

        FileUtil.extract(new ZipInputStream(new FileInputStream(file)),
                FileUtil.getFilesDir(FileType.FORMS));

        // Compressed file is not needed any more
        if (!file.delete()) {
            Timber.e("Could not delete survey zip file: " + filename);
        }

        survey.setFileName(survey.getId() + ConstantUtil.XML_SUFFIX);
        survey.setType(DEFAULT_TYPE);
        survey.setLocation(ConstantUtil.FILE_LOCATION);
    }

    @Nullable
    private Survey loadSurvey(@NonNull Survey survey) {
        InputStream in = null;
        Survey hydratedDurvey = null;
        try {
            if (ConstantUtil.RESOURCE_LOCATION.equalsIgnoreCase(survey.getLocation())) {
                // load from resource
                Resources res = getResources();
                in = res.openRawResource(res.getIdentifier(survey.getFileName(),
                        ConstantUtil.RAW_RESOURCE, ConstantUtil.RESOURCE_PACKAGE));
            } else {
                // load from file
                File f = new File(FileUtil.getFilesDir(FileType.FORMS), survey.getFileName());
                in = new FileInputStream(f);
            }
            hydratedDurvey = SurveyDao.loadSurvey(survey, in);
        } catch (FileNotFoundException e) {
            Timber.e(e, "Could not parse survey survey file");
        } finally {
            FileUtil.close(in);
        }
        return hydratedDurvey;
    }

    /**
     * checks to see if we should pre-cache help media files (based on the
     * property in the settings db) and, if we should, downloads the files
     *
     * @param survey
     */
    private void downloadResources(@NonNull Survey survey) {
        Survey hydratedSurvey = loadSurvey(survey);
        if (hydratedSurvey != null) {
            // collect files in a set just in case the same binary is
            // used in multiple questions we only need to download once
            Set<String> resources = new HashSet<>();
            for (QuestionGroup group : hydratedSurvey.getQuestionGroups()) {
                for (Question question : group.getQuestions()) {
                    if (!question.getHelpByType(ConstantUtil.VIDEO_HELP_TYPE).isEmpty()) {
                        resources.add(question.getHelpByType(ConstantUtil.VIDEO_HELP_TYPE)
                                .get(0).getValue());
                    }
                    for (QuestionHelp help : question.getHelpByType(ConstantUtil.IMAGE_HELP_TYPE)) {
                        resources.add(help.getValue());
                    }
                    // Question src data (i.e. cascading question resources)
                    if (question.getSrc() != null) {
                        resources.add(question.getSrc());
                    }
                }
            }
            // Download help media files (images & videos) and common resources
            downloadResources(survey.getId(), resources);
        }
    }

    private void downloadResources(@NonNull final String sid,
            @NonNull final Set<String> resources) {
        databaseAdaptor.markSurveyHelpDownloaded(sid, false);
        boolean ok = true;
        for (String resource : resources) {
            Timber.i("Downloading resource: " + resource);
            try {
                // Handle both absolute URL (media help files) and S3 object IDs (survey resources)
                // Naive check to determine whether or not this is an absolute filename
                if (resource.startsWith("http")) {
                    downloadGaeResource(sid, resource);
                } else {
                    downloadS3Resource(resource);
                }
            } catch (Exception e) {
                ok = false;
                // Display cascade-specific error message. If at any point we include support for
                // more resource types, this message should be accordingly customized.
                displayErrorNotification(ConstantUtil.NOTIFICATION_RESOURCE_ERROR,
                        getString(R.string.error_missing_cascade));
                Timber.e(e, "Could not download resource " + resource + " for survey " + sid);
            }
        }
        // Mark help (survey resources) as downloaded if ALL files succeeded.
        if (ok) {
            databaseAdaptor.markSurveyHelpDownloaded(sid, true);
        }
    }

    private void downloadS3Resource(String resource) throws IOException {
        // resource is just a filename
        final String filename = resource + ConstantUtil.ARCHIVE_SUFFIX;
        final String objectKey = ConstantUtil.S3_SURVEYS_DIR + filename;
        final File resDir = FileUtil.getFilesDir(FileType.RES);
        final File file = new File(resDir, filename);
        S3Api s3 = new S3Api(SurveyDownloadService.this);
        s3.syncFile(objectKey, file);
        FileUtil.extract(new ZipInputStream(new FileInputStream(file)), resDir);
        if (!file.delete()) {
            Timber.e("Error deleting resource zip file");
        }
    }

    private void downloadGaeResource(@NonNull String sid, @NonNull String url) throws IOException {
        final String filename = new File(url).getName();
        final File surveyDir = new File(FileUtil.getFilesDir(FileType.FORMS), sid);
        if (!surveyDir.exists()) {
            surveyDir.mkdir();
        }
        HttpUtil.httpGet(url, new File(surveyDir, filename));
    }

    /**
     * invokes a service call to get the header information for multiple surveys
     */
    @NonNull
    private List<Survey> getSurveyHeaders(@NonNull String[] surveyIds) {
        List<Survey> surveys = new ArrayList<>();
        FlowApi flowApi = new FlowApi(getApplicationContext());
        for (String id : surveyIds) {
            try {
                surveys.addAll(flowApi.getSurveyHeader(id));
            } catch (IllegalArgumentException | IOException e) {
                if (e instanceof IllegalArgumentException) {
                    Timber.e(e, e.getMessage());
                }
                displayErrorNotification(ConstantUtil.NOTIFICATION_HEADER_ERROR,
                        getString(R.string.error_form_header, id));
            }
        }
        return surveys;
    }

    /**
     * invokes a service call to list all surveys that have been designated for
     * this device (based on phone number).
     *
     * @return - an arrayList of Survey objects with the id and version populated
     * TODO: Move this feature to FLOWApi
     */
    private List<Survey> checkForSurveys() {
        List<Survey> surveys = new ArrayList<>();
        FlowApi api = new FlowApi(getApplicationContext());
        try {
            surveys = api.getSurveys();
        } catch (@NonNull IllegalArgumentException | IOException e) {
            if (e instanceof IllegalArgumentException) {
                Timber.e(e, e.getMessage());
            }
            displayErrorNotification(ConstantUtil.NOTIFICATION_ASSIGNMENT_ERROR,
                    getString(R.string.error_assignment_read));
        }
        return surveys;
    }

    private void displayErrorNotification(int id, String msg) {
        NotificationHelper
                .displayErrorNotification(getString(R.string.error_form_sync_title), msg, this, id);
    }

    private void displayNotification(int synced, int failed, int total) {
        boolean finished = synced + failed >= total;
        String title = getString(R.string.downloading_forms);
        // Do not show failed if there is none
        String text = failed > 0 ? String.format(getString(R.string.data_sync_all),
                synced, failed)
                : String.format(getString(R.string.data_sync_synced), synced);

        NotificationHelper.displayNotification(this, total, title, text,
                ConstantUtil.NOTIFICATION_FORMS_SYNCED, !finished,
                synced + failed);
    }

    /**
     * Dispatch a Broadcast notification to notify of surveys synchronization.
     * This notification will be received in SurveyHomeActivity, in order to
     * refresh its data
     */
    private void sendBroadcastNotification() {
        Intent intentBroadcast = new Intent(ACTION_SURVEY_SYNC);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentBroadcast);
    }

}
