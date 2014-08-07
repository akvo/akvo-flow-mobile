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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import org.akvo.flow.R;
import org.akvo.flow.dao.SurveyDao;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.exception.PersistentUncaughtExceptionHandler;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FileUtil.FileType;
import org.akvo.flow.util.LangsPreferenceUtil;
import org.akvo.flow.util.ViewUtil;

/**
 * Service that will check a well-known location on the device's SD card for a
 * zip file that contains data that should be loaded on the device. The root of
 * the zip file can contain a file called dbinstructions.sql. If it does, the
 * sql statements contained therein will be executed in the order they appear.
 * The zip file can also contain any number of directories which can each
 * contain ONE survey (the survey xml and any help media). The name of the
 * directory must be the surveyID and the name of the survey file will be used
 * for the survey name. The system will iterate through each directory and
 * install the survey and help media contained therein. If the survey is already
 * present on the device, the survey in the ZIP file will overwrite the data
 * already on the device. If there are multiple zip files in the directory, this
 * utility will process them in lexicographical order by file name; Any files
 * with a name starting with . will be skipped (to prevent inadvertent
 * processing of MAC OSX metadata files).
 * 
 * @author Christopher Fagiani
 */
public class BootstrapService extends Service {
    private static final String TAG = "BOOTSTRAP_SERVICE";
    public static boolean isProcessing = false;
    private static Semaphore lock = new Semaphore(1);
    private Thread workerThread;
    private SurveyDbAdapter databaseAdapter;
    private static final Integer NOTIFICATION_ID = new Integer(123);

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    /**
     * life cycle method for the service. This is called by the system when the
     * service is started
     */
    public int onStartCommand(final Intent intent, int flags, int startid) {
        workerThread = new Thread(new Runnable() {
            public void run() {
                checkAndInstall();
                stopSelf();
            }
        });
        workerThread.start();
        return Service.START_STICKY;
    }

    /**
     * Checks the bootstrap directory for unprocessed zip files. If they are
     * found, they're processed one at a time. If an error occurs, all
     * processing stops (subsequent zips won't be processed if there are
     * multiple zips in the directory) just in case data in a later zip depends
     * on the previous one being there.
     */
    private void checkAndInstall() {
        try {
            lock.acquire();
            ArrayList<File> zipFiles = getZipFiles();
            if (zipFiles != null && zipFiles.size() > 0) {
                isProcessing = true;
                String startMessage = getString(R.string.bootstrapstart);
                ViewUtil.fireNotification(startMessage, startMessage, this,
                        NOTIFICATION_ID, android.R.drawable.ic_dialog_info);
                databaseAdapter = new SurveyDbAdapter(this);
                databaseAdapter.open();
                try {
                    for (int i = 0; i < zipFiles.size(); i++) {
                        try {
                            processFile(zipFiles.get(i));
                        } catch (Exception e) {
                            // try to roll back any database changes (if the zip
                            // has a rollback file)
                            rollback(zipFiles.get(i));
                            String newFilename = zipFiles.get(i)
                                    .getAbsolutePath();
                            zipFiles.get(i)
                                    .renameTo(
                                            new File(
                                                    newFilename
                                                            + ConstantUtil.PROCESSED_ERROR_SUFFIX));
                            throw (e);
                        }
                    }
                    String endMessage = getString(R.string.bootstrapcomplete);
                    ViewUtil.fireNotification(endMessage, endMessage, this,
                            NOTIFICATION_ID, android.R.drawable.ic_dialog_info);
                    sendBroadcastNotification();
                } finally {
                    if (databaseAdapter != null) {
                        databaseAdapter.close();
                    }
                }
            }
        } catch (Exception e) {
            String errorMessage = getString(R.string.bootstraperror);
            ViewUtil.fireNotification(errorMessage, errorMessage, this,
                    NOTIFICATION_ID, android.R.drawable.ic_dialog_alert);
            Log.e(TAG, "Bootstrap error", e);
        } finally {
            isProcessing = false;
            lock.release();
        }
    }

    /**
     * looks for the rollback file in the zip and, if it exists, attempts to
     * execute the statements contained therein
     * 
     * @param zipFile
     * @throws Exception
     */
    private void rollback(File zipFile) throws Exception {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry entry = null;
        while ((entry = zis.getNextEntry()) != null) {
            String parts[] = entry.getName().split("/");
            String fileName = parts[parts.length - 1];
            // make sure we're not processing a hidden file
            if (!fileName.startsWith(".")) {
                if (entry
                        .getName()
                        .toLowerCase()
                        .endsWith(
                                ConstantUtil.BOOTSTRAP_ROLLBACK_FILE
                                        .toLowerCase())) {
                    processDbInstructions(FileUtil.readTextFromZip(zis), false);
                }
            }
        }
    }

    /**
     * processes a bootstrap zip file
     * 
     * @param zipFile
     * @throws Exception
     */
    private void processFile(File zipFile) throws Exception {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry entry = null;
        HashSet<String> surveysWithImages = new HashSet<String>();
        while ((entry = zis.getNextEntry()) != null) {
            String parts[] = entry.getName().split("/");
            String fileName = parts[parts.length - 1];
            // make sure we're not processing a hidden file
            if (!fileName.startsWith(".")) {
                if (entry.getName().toLowerCase()
                        .endsWith(ConstantUtil.BOOTSTRAP_DB_FILE.toLowerCase())) {
                    processDbInstructions(FileUtil.readTextFromZip(zis), true);

                } else if (!entry.isDirectory()
                        && !ConstantUtil.BOOTSTRAP_ROLLBACK_FILE
                                .equalsIgnoreCase(fileName)) {
                    String id = parts[parts.length - 2];
                    if (entry.getName().toLowerCase()
                            .endsWith(ConstantUtil.XML_SUFFIX.toLowerCase())) {
                        String surveyName = fileName;
                        if (surveyName.contains(".")) {
                            surveyName = surveyName.substring(0,
                                    surveyName.indexOf("."));
                        }
                        Survey survey = databaseAdapter.getSurvey(id);
                        if (survey == null) {
                            survey = new Survey();
                            survey.setId(id);
                            survey.setName(surveyName);
                            survey.setHelpDownloaded(false);
                            survey.setType(ConstantUtil.SURVEY_TYPE);
                        }
                        survey.setLocation(ConstantUtil.FILE_LOCATION);
                        survey.setFileName(fileName);

                        // in both cases (new survey and existing), we need to update the xml
                        File file = new File(FileUtil.getFilesDir(FileType.FORMS), fileName);
                        FileUtil.extractAndSaveFile(zis, new FileOutputStream(file));
                        // now read the survey XML back into memory to see if
                        // there is a version
                        Survey loadedSurvey = null;
                        try {
                            InputStream in = new FileInputStream(file);
                            loadedSurvey = SurveyDao.loadSurvey(survey, in);
                        } catch (FileNotFoundException e) {
                            Log.e(TAG, "Could not load survey xml file");
                        }

                        if (loadedSurvey == null) {
                            // Something went wrong, we cannot continue with this survey
                            continue;
                        }

                        if (loadedSurvey.getVersion() > 0) {
                            survey.setVersion(loadedSurvey.getVersion());
                        } else {
                            survey.setVersion(1d);
                        }

                        // Process SurveyGroup, and save it to the DB
                        SurveyGroup group = parseSurveyGroup(loadedSurvey);
                        survey.setSurveyGroup(group);
                        databaseAdapter.addSurveyGroup(group);

                        // now save the survey and add the languages
                        databaseAdapter.saveSurvey(survey);
                        String[] langs = LangsPreferenceUtil.determineLanguages(this, survey);
                        databaseAdapter.addLanguages(langs);
                    } else {
                        // if it's not a sql file and its not a survey, it must
                        // be help media
                        File helpDir = new File(FileUtil.getFilesDir(FileType.FORMS), id);
                        if (!helpDir.exists()) {
                            helpDir.mkdir();
                        }
                        File file = new File(helpDir, fileName);
                        FileUtil.extractAndSaveFile(zis, new FileOutputStream(file));
                        // record the fact that this survey had media
                        surveysWithImages.add(id);
                    }
                }
            }
        }
        // update survey record so the system knows not to bother trying to
        // re-download media (since it was in the file)
        for (String sid : surveysWithImages) {
            databaseAdapter.markSurveyHelpDownloaded(sid, true);
        }

        // now rename the zip file so we don't process it again
        String newFilename = zipFile.getAbsolutePath();
        zipFile.renameTo(new File(newFilename
                + ConstantUtil.PROCESSED_OK_SUFFIX));
    }

    /**
     * tokenizes instructions using the newline character as a delimiter and
     * executes each line as a separate SQL command;
     * 
     * @param instructions
     */
    private void processDbInstructions(String instructions, boolean failOnError)
            throws Exception {
        if (instructions != null && instructions.trim().length() > 0) {
            String[] instructionList = instructions.split("\n");
            for (int i = 0; i < instructionList.length; i++) {
                String command = instructionList[i].trim();
                if (!command.endsWith(";")) {
                    command = command + ";";
                }
                try {
                    databaseAdapter.executeSql(command);
                } catch (Exception e) {
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * returns an ordered list of zip files that exist in the device's bootstrap
     * directory
     * 
     * @return
     */
    private ArrayList<File> getZipFiles() {
        ArrayList<File> zipFiles = new ArrayList<File>();
        // zip files can only be loaded on the SD card (not internal storage) so
        // we only need to look there
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File dir = FileUtil.getFilesDir(FileType.INBOX);
            File[] fileList = dir.listFiles();
            if (fileList != null) {
                for (int i = 0; i < fileList.length; i++) {
                    if (fileList[i].isFile()
                            && fileList[i]
                                    .getName()
                                    .toLowerCase()
                                    .endsWith(ConstantUtil.ARCHIVE_SUFFIX.toLowerCase())) {
                        zipFiles.add(fileList[i]);
                    }
                }
            }
            Collections.sort(zipFiles);
        }
        return zipFiles;
    }

    private SurveyGroup parseSurveyGroup(Survey survey) {
        // Temporary hack to support the concept of 'Project', where a non-monitored
        // project (current SurveyGroup) can only hold one survey.
        // See https://github.com/akvo/akvo-flow-mobile/issues/100
        SurveyGroup group = survey.getSurveyGroup();
        if (group != null && group.isMonitored()) {
            return group; // Do nothing. The group remains unmodified
        }

        long id = Long.valueOf(survey.getId());
        return new SurveyGroup(id, survey.getName(), survey.getId(), false);
    }

    /**
     * sets up the uncaught exeption handler for this thread so we can report
     * errors to the server.
     */
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler
                .getInstance());
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
