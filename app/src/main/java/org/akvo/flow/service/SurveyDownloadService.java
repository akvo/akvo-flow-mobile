/*
 *  Copyright (C) 2010-2015 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.service;

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

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.akvo.flow.R;
import org.akvo.flow.api.FlowApi;
import org.akvo.flow.api.S3Api;
import org.akvo.flow.api.parser.csv.SurveyMetaParser;
import org.akvo.flow.dao.SurveyDao;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionHelp;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.exception.PersistentUncaughtExceptionHandler;
import org.akvo.flow.exception.TransferException;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FileUtil.FileType;
import org.akvo.flow.util.HttpUtil;
import org.akvo.flow.util.LangsPreferenceUtil;
import org.akvo.flow.util.StatusUtil;
import org.akvo.flow.util.ViewUtil;

/**
 * This activity will check for new surveys on the device and install as needed
 * 
 * @author Christopher Fagiani
 */
public class SurveyDownloadService extends IntentService {
    private static final String TAG = "SURVEY_DOWNLOAD_SERVICE";

    public static final String EXTRA_SURVEYS = "surveys";// Intent parameter to specify which surveys need to be updated

    private static final String DEFAULT_TYPE = "Survey";

    private static final String SURVEY_LIST_SERVICE_PATH = "/surveymanager?action=getAvailableSurveysDevice";
    private static final String SURVEY_HEADER_SERVICE_PATH = "/surveymanager?action=getSurveyHeader&surveyId=";

    private SurveyDbAdapter databaseAdaptor;

    public SurveyDownloadService() {
        super(TAG);
    }

    public void onHandleIntent(Intent intent) {
        if (StatusUtil.hasDataConnection(this)) {
            try {
                databaseAdaptor = new SurveyDbAdapter(this);
                databaseAdaptor.open();

                String[] surveyIds = intent != null ? intent.getStringArrayExtra(EXTRA_SURVEYS) : null;
                checkAndDownload(surveyIds);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                PersistentUncaughtExceptionHandler.recordException(e);
            } finally {
                databaseAdaptor.close();
            }
        }

        sendBroadcastNotification();
    }

    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler.getInstance());
    }

    /**
     * if no surveyIds are passed in, this will check for new surveys and, if
     * there are some new ones, downloads them to the DATA_DIR. If surveyIds are
     * passed in, then those specific surveys will be downloaded. If they're already
     * on the device, the surveys will be replaced with the new ones.
     */
    private void checkAndDownload(String[] surveyIds) throws IOException {
        // Load preferences
        final String serverBase = StatusUtil.getServerBase(this);

        List<Survey> surveys;
        if (surveyIds != null) {
            surveys = getSurveyHeaders(serverBase, surveyIds);
        } else {
            surveys = checkForSurveys(serverBase);
        }

        // if there are surveys for this device, see if we need them
        surveys = databaseAdaptor.checkSurveyVersions(surveys);

        if (!surveys.isEmpty()) {
            int synced = 0, failed = 0;
            displayNotification(synced, failed, surveys.size());
            for (Survey survey : surveys) {
                try {
                    downloadSurvey(survey);
                    databaseAdaptor.addSurveyGroup(survey.getSurveyGroup());
                    databaseAdaptor.saveSurvey(survey);
                    String[] langs = LangsPreferenceUtil.determineLanguages(this, survey);
                    databaseAdaptor.addLanguages(langs);
                    downloadResources(survey);
                    synced++;
                } catch (Exception e) {
                    failed++;
                    Log.e(TAG, "Error downloading survey: " + survey.getId(), e);
                    displayErrorNotification(ConstantUtil.NOTIFICATION_FORM_ERROR,
                            getString(R.string.error_form_download));
                    PersistentUncaughtExceptionHandler
                            .recordException(new TransferException(survey.getId(), null, e));
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

    /**
     * Downloads the survey based on the ID and then updates the survey object
     * with the filename and location
     */
    private void downloadSurvey(Survey survey) throws IOException {
        final String filename = survey.getId() + ConstantUtil.ARCHIVE_SUFFIX;
        final String objectKey = ConstantUtil.S3_SURVEYS_DIR + filename;
        final File file = new File(FileUtil.getFilesDir(FileType.FORMS), filename);

        S3Api s3Api = new S3Api(this);
        s3Api.get(objectKey, file); // Download zip file

        FileUtil.extract(new ZipInputStream(new FileInputStream(file)),
                FileUtil.getFilesDir(FileType.FORMS));

        // Compressed file is not needed any more
        if (!file.delete()) {
            Log.e(TAG, "Could not delete survey zip file: " + filename);
        }

        survey.setFileName(survey.getId() + ConstantUtil.XML_SUFFIX);
        survey.setType(DEFAULT_TYPE);
        survey.setLocation(ConstantUtil.FILE_LOCATION);
    }

    private Survey loadSurvey(Survey survey) {
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
            Log.e(TAG, "Could not parse survey survey file", e);
            PersistentUncaughtExceptionHandler.recordException(e);
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
    private void downloadResources(Survey survey) {
        Survey hydratedSurvey = loadSurvey(survey);
        if (hydratedSurvey != null) {
            // collect files in a set just in case the same binary is
            // used in multiple questions we only need to download once
            Set<String> resources = new HashSet<String>();
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

    private void downloadResources(final String sid, final Set<String> resources) {
        databaseAdaptor.markSurveyHelpDownloaded(sid, false);
        try {
            for (String resource : resources) {
                Log.i(TAG, "Downloading resource: " + resource);
                // Handle both absolute URL (media help files) and S3 object IDs (survey resources)
                // Naive check to determine whether or not this is an absolute filename
                if (resource.startsWith("http")) {
                    final String filename = new File(resource).getName();
                    final File surveyDir = new File(FileUtil.getFilesDir(FileType.FORMS), sid);
                    if (!surveyDir.exists()) {
                        surveyDir.mkdir();
                    }
                    HttpUtil.httpGet(resource, new File(surveyDir, filename));
                } else {
                    // resource is just a filename
                    final String filename = resource + ConstantUtil.ARCHIVE_SUFFIX;
                    final String objectKey = ConstantUtil.S3_SURVEYS_DIR + filename;
                    final File resDir = FileUtil.getFilesDir(FileType.RES);
                    final File file = new File(resDir, filename);
                    S3Api s3 = new S3Api(SurveyDownloadService.this);
                    s3.syncFile(objectKey, file);
                    FileUtil.extract(new ZipInputStream(new FileInputStream(file)), resDir);
                    if (!file.delete()) {
                        Log.e(TAG, "Error deleting resource zip file");
                    }
                }
            }
            databaseAdaptor.markSurveyHelpDownloaded(sid, true);
        } catch (Exception e) {
            databaseAdaptor.markSurveyHelpDownloaded(sid, false);
            displayErrorNotification(ConstantUtil.NOTIFICATION_RESOURCE_ERROR,
                    getString(R.string.error_missing_resources));
            Log.e(TAG, "Could not download resources for survey : " + sid, e);
        }
    }

    /**
     * invokes a service call to get the header information for multiple surveys
     */
    private List<Survey> getSurveyHeaders(String serverBase, String[] surveyIds) {
        List<Survey> surveys = new ArrayList<Survey>();
        for (String id : surveyIds) {
            try {
                final String url = serverBase + SURVEY_HEADER_SERVICE_PATH + id + "&" + FlowApi.getDeviceParams();
                String response = HttpUtil.httpGet(url);
                if (response != null) {
                    surveys.addAll(new SurveyMetaParser().parseList(response, true));
                }
            } catch (IllegalArgumentException | IOException e) {
                Log.e(TAG, e.getMessage());
                displayErrorNotification(ConstantUtil.NOTIFICATION_HEADER_ERROR,
                        String.format(getString(R.string.error_form_header), id));
                PersistentUncaughtExceptionHandler.recordException(e);
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
    private List<Survey> checkForSurveys(String serverBase) {
        List<Survey> surveys = new ArrayList<Survey>();
        try {
            final String url = serverBase + SURVEY_LIST_SERVICE_PATH + "&" + FlowApi.getDeviceParams();
            String response = HttpUtil.httpGet(url);
            if (response != null) {
                surveys = new SurveyMetaParser().parseList(response);
            }
        } catch (IllegalArgumentException | IOException e) {
            displayErrorNotification(ConstantUtil.NOTIFICATION_ASSIGNMENT_ERROR,
                    getString(R.string.error_assignment_read));
            Log.e(TAG, e.getMessage());
            PersistentUncaughtExceptionHandler.recordException(e);
        }
        return surveys;
    }

    private void displayErrorNotification(int id, String msg) {
        ViewUtil.fireNotification(getString(R.string.error_form_sync_title), msg, this, id, null);
    }

    private void displayNotification(int synced, int failed, int total) {
        boolean finished = synced + failed >= total;
        int icon = finished ? android.R.drawable.stat_sys_download_done
                : android.R.drawable.stat_sys_download;

        String title = getString(R.string.downloading_forms);
        // Do not show failed if there is none
        String text = failed > 0 ? String.format(getString(R.string.data_sync_all),
                synced, failed)
                : String.format(getString(R.string.data_sync_synced), synced);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setTicker(title);

        builder.setOngoing(!finished);// Ongoing if still syncing the records

        // Progress will only be displayed in Android versions > 4.0
        builder.setProgress(total, synced+failed, false);

        // Dummy intent. Do nothing when clicked
        PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(), 0);
        builder.setContentIntent(intent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(ConstantUtil.NOTIFICATION_FORMS_SYNCED, builder.build());
    }

    /**
     * Dispatch a Broadcast notification to notify of surveys synchronization.
     * This notification will be received in SurveyHomeActivity, in order to
     * refresh its data
     */
    private void sendBroadcastNotification() {
        Intent intentBroadcast = new Intent(getString(R.string.action_surveys_sync));
        sendBroadcast(intentBroadcast);
    }

}
