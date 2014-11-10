/*
 *  Copyright (C) 2010-2014 Stichting Akvo (Akvo Foundation)
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.util.Log;

import org.akvo.flow.R;
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
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.StatusUtil;
import org.akvo.flow.util.ViewUtil;

/**
 * this activity will check for new surveys on the device and install as needed
 * 
 * @author Christopher Fagiani
 */
public class SurveyDownloadService extends Service {
    private static final String TAG = "SURVEY_DOWNLOAD_SERVICE";

    public static final String EXTRA_SURVEYS = "surveys";// Intent parameter to specify which surveys need to be updated

    private static final String DEFAULT_TYPE = "Survey";
    private static final int COMPLETE_ID = 2;
    private static final int FAIL_ID = 3;

    private static final String SURVEY_LIST_SERVICE_PATH = "/surveymanager?action=getAvailableSurveysDevice&devicePhoneNumber=";
    private static final String SURVEY_HEADER_SERVICE_PATH = "/surveymanager?action=getSurveyHeader&surveyId=";
    private static final String DEV_ID_PARAM = "&devId=";
    private static final String IMEI_PARAM = "&imei=";
    private static final String VERSION_PARAM = "&ver=";

    private SurveyDbAdapter databaseAdaptor;
    private Thread thread;
    private ThreadPoolExecutor downloadExecutor;
    private static Semaphore lock = new Semaphore(1);

    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * life cycle method for the service. This is called by the system when the
     * service is started
     */
    public int onStartCommand(final Intent intent, int flags, int startid) {
        thread = new Thread(new Runnable() {
            public void run() {
                if (intent != null) {
                    String[] surveyIds = intent.getStringArrayExtra(EXTRA_SURVEYS);
                    checkAndDownload(surveyIds);
                    sendBroadcastNotification();
                }
            }
        });
        thread.start();
        return Service.START_REDELIVER_INTENT;
    }

    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler
                .getInstance());
        downloadExecutor = new ThreadPoolExecutor(1, 3, 5000,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    }

    /**
     * if no surveyIds are passed in, this will check for new surveys and, if
     * there are some new ones, downloads them to the DATA_DIR. If surveyIds are
     * passed in, then those specific surveys will be downloaded. If they're already
     * on the device, the surveys will be replaced with the new ones.
     */
    private void checkAndDownload(String[] surveyIds) {
        if (StatusUtil.hasDataConnection(this)) {
            try {
                lock.acquire();
                databaseAdaptor = new SurveyDbAdapter(this);
                databaseAdaptor.open();
                
                // Load preferences
                final String serverBase = StatusUtil.getServerBase(this);
                final String deviceId = getDeviceId();

                List<Survey> surveys;
                if (surveyIds != null) {
                    surveys = getSurveyHeaders(serverBase, surveyIds, deviceId);
                } else {
                    surveys = checkForSurveys(serverBase, deviceId);
                }

                if (!surveys.isEmpty()) {
                    // First, sync the SurveyGroups
                    syncSurveyGroups(surveys);
                    
                    // if there are surveys for this device, see if we need them
                    surveys = databaseAdaptor.checkSurveyVersions(surveys);
                    int updateCount = 0;
                    for (Survey survey : surveys) {
                        try {
                            downloadSurvey(survey);
                            databaseAdaptor.saveSurvey(survey);
                            String[] langs = LangsPreferenceUtil.determineLanguages(this, survey);
                            databaseAdaptor.addLanguages(langs);
                            downloadHelp(survey);
                            updateCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Error downloading survey: " + survey.getId(), e);
                            fireNotification(getString(R.string.cannotupdate), FAIL_ID);
                            PersistentUncaughtExceptionHandler
                                    .recordException(new TransferException(survey.getId(), null, e));
                        }
                    }
                    if (updateCount > 0) {
                        fireNotification(getString(R.string.surveysupdated), COMPLETE_ID);
                    }
                }

                // now check if any previously downloaded surveys still need
                // don't have their help media pre-cached
                if (StatusUtil.hasDataConnection(this)) {
                    surveys = databaseAdaptor.getSurveyList(SurveyGroup.ID_NONE);
                    for (Survey survey : surveys) {
                        if (!survey.isHelpDownloaded()) {
                            downloadHelp(survey);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Could not update surveys", e);
                fireNotification(getString(R.string.cannotupdate), FAIL_ID);
                PersistentUncaughtExceptionHandler.recordException(e);
            } finally {
                databaseAdaptor.close();
                lock.release();
            }
        }
        try {
            downloadExecutor.shutdown();
            // wait up to 30 minutes to download the media
            downloadExecutor.awaitTermination(1800, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Error while waiting for download executor to terminate", e);
        }
        stopSelf();
    }

    private String getDeviceId() {
        return databaseAdaptor.getPreference(ConstantUtil.DEVICE_IDENT_KEY);
    }
    
    private void syncSurveyGroups(List<Survey> surveys) {
        // First, form the groups
        Map<Long, SurveyGroup> surveyGroups = new HashMap<Long, SurveyGroup>();
        for (Survey survey : surveys) {
            SurveyGroup group = survey.getSurveyGroup();

            // Temporary hack to support the concept of 'Project', where a non-monitored
            // project (current SurveyGroup) can only hold one survey.
            // See https://github.com/akvo/akvo-flow-mobile/issues/100
            if (!group.isMonitored()) {
                // TODO: Use String for SurveyGroup ids
                try {
                    long id = Long.valueOf(survey.getId());
                    group = new SurveyGroup(id, survey.getName(), survey.getId(), false);
                    survey.setSurveyGroup(group);
                } catch (NumberFormatException e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            surveyGroups.put(group.getId(), group);
        }
        
        // Now, add them to the database
        for (SurveyGroup surveyGroup : surveyGroups.values()) {
            databaseAdaptor.addSurveyGroup(surveyGroup);
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

        extract(file, FileUtil.getFilesDir(FileType.FORMS));

        // Compressed file is not needed any more
        if (!file.delete()) {
            Log.e(TAG, "Could not delete survey zip file: " + filename);
        }

        survey.setFileName(survey.getId() + ConstantUtil.XML_SUFFIX);
        survey.setType(DEFAULT_TYPE);
        survey.setLocation(ConstantUtil.FILE_LOCATION);
    }

    /**
     * Extract a zipped file contents into the destination directory
     * @param src File object of the zip file
     * @param dst directory wherein the contents will be extracted
     */
    private void extract(File src, File dst) throws IOException {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(src));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            File f = new File(src, entry.getName());
            FileOutputStream fout = new FileOutputStream(f);
            byte[] buffer = new byte[8192];
            int size;
            while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
                fout.write(buffer, 0, size);
            }
            fout.close();
            zis.closeEntry();
        }
        zis.close();
    }

    /**
     * checks to see if we should pre-cache help media files (based on the
     * property in the settings db) and, if we should, downloads the files
     * 
     * @param survey
     */
    private void downloadHelp(Survey survey) {
        // first, see if we should even bother trying to download
        if (StatusUtil.hasDataConnection(this)) {
            try {
                InputStream in = null;
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
                Survey hydratedSurvey = SurveyDao.loadSurvey(survey, in);
                if (hydratedSurvey != null) {
                    // collect files in a set just in case the same binary is
                    // used in multiple questions
                    // we only need to download once
                    Set<String> fileSet = new HashSet<String>();
                    for (QuestionGroup group : hydratedSurvey.getQuestionGroups()) {
                        for (Question question : group.getQuestions()) {
                            if (!question.getHelpByType(ConstantUtil.VIDEO_HELP_TYPE).isEmpty()) {
                                fileSet.add(question.getHelpByType(ConstantUtil.VIDEO_HELP_TYPE)
                                        .get(0).getValue());
                            }
                            for (QuestionHelp help : question.getHelpByType(ConstantUtil.IMAGE_HELP_TYPE)) {
                                fileSet.add(help.getValue());
                            }
                            // Question src data (i.e. cascading question resources)
                            if (question.getSrc() != null) {
                                fileSet.add(question.getSrc());
                            }
                        }
                    }
                    for (String file : fileSet) {
                        downloadBinary(file, survey.getId());
                    }
                    databaseAdaptor.markSurveyHelpDownloaded(survey.getId(), true);
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Could not parse survey survey file", e);
                PersistentUncaughtExceptionHandler.recordException(e);
            }
        }
    }

    /**
     * Uses the thread pool executor to download the remote file passed in via a
     * background thread. Any zip file will be uncompressed in the survey resources directory
     */
    private void downloadBinary(final String remoteFile, final String surveyId) {
        final String filename = remoteFile.substring(remoteFile.lastIndexOf("/") + 1);
        final File dir = new File(FileUtil.getFilesDir(FileType.FORMS), surveyId);
        if (!dir.exists()) {
            dir.mkdir();
        }
        final File file = new File(dir, filename);
        downloadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpUtil.httpGet(remoteFile, file);
                    if (filename.endsWith(ConstantUtil.ARCHIVE_SUFFIX)) {
                        extract(file, dir);
                        if (!file.delete()) {
                            Log.e(TAG, "Error deleting resource zip file");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Could not download help media file", e);
                }
            }
        });
    }

    /**
     * invokes a service call to get the header information for multiple surveys
     */
    private List<Survey> getSurveyHeaders(String serverBase, String[] surveyIds, String deviceId)
            throws IOException {
        final String deviceIdParam = deviceId != null ?
                DEV_ID_PARAM + URLEncoder.encode(deviceId, "UTF-8")  : "";

        List<Survey> surveys = new ArrayList<Survey>();
        for (String id : surveyIds) {
            try {
                final String url = serverBase + SURVEY_HEADER_SERVICE_PATH + id
                        + "&devicePhoneNumber=" + StatusUtil.getPhoneNumber(this) + deviceIdParam;
                String response = HttpUtil.httpGet(url);
                if (response != null) {
                    surveys.addAll(new SurveyMetaParser().parseList(response, true));
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
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
    private List<Survey> checkForSurveys(String serverBase, String deviceId) throws IOException {
        List<Survey> surveys = new ArrayList<Survey>();
        String phoneNumber = StatusUtil.getPhoneNumber(this);
        if (phoneNumber == null) {
            phoneNumber = "";
        }
        String imei = StatusUtil.getImei(this);
        String version = PlatformUtil.getVersionName(this);
        final String url = serverBase
                + SURVEY_LIST_SERVICE_PATH + URLEncoder.encode(phoneNumber, "UTF-8")
                + IMEI_PARAM + URLEncoder.encode(imei, "UTF-8")
                + VERSION_PARAM + URLEncoder.encode(version, "UTF-8")
                + (deviceId != null ? DEV_ID_PARAM + URLEncoder.encode(deviceId, "UTF-8") : "");
        String response = HttpUtil.httpGet(url);
        if (response != null) {
            surveys = new SurveyMetaParser().parseList(response);
        }
        return surveys;
    }

    /**
     * displays a notification in the system status bar indicating the
     * completion of the download operation
     */
    private void fireNotification(String text, int notificationID) {
        ViewUtil.fireNotification(text, text, this, notificationID, null);
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
