/*
 *  Copyright (C) 2010-2018 Stichting Akvo (Akvo Foundation)
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
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Base64;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.database.ResponseColumns;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.database.UserColumns;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.MakeDataPrivate;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.response.FormInstance;
import org.akvo.flow.domain.response.Response;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.GsonMapper;
import org.akvo.flow.util.MediaFileHelper;
import org.akvo.flow.util.NotificationHelper;
import org.akvo.flow.util.StringUtil;
import org.akvo.flow.util.files.ZipFileBrowser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

/**
 * Handle survey export and sync in a background thread. The export process takes
 * no arguments, and will try to zip all the survey instances with a SUBMIT_REQUESTED status
 * but with no EXPORT_DATE (export hasn't happened yet). Ideally, and if the service has been
 * triggered by a survey submission, only one survey instance will be exported. However, if for
 * whatever reason, a previous export attempt has failed, a new export will be tried on each
 * execution of the service, until the zip file finally gets exported. A possible scenario for
 * this is the submission of a survey when the external storage is not available, postponing the
 * export until it gets ready.
 * After the export of the zip files, the sync will be run, attempting to upload all the non synced
 * files to the datastore.
 *
 * @author Christopher Fagiani
 */
public class DataSyncService extends IntentService {

    private static final String TAG = "DataSyncService";
    private static final String DELIMITER = "\t";
    private static final String SPACE = "\u0020"; // safe from source whitespace reformatting

    private static final String SIGNING_ALGORITHM = "HmacSHA1";

    private static final String SURVEY_DATA_FILE_JSON = "data.json";
    private static final String SIG_FILE_NAME = ".sig";

    private static final String UTF_8_CHARSET = "UTF-8";

    @Inject
    SurveyDbDataSource mDatabase;

    @Inject
    Prefs preferences;

    @Inject
    ZipFileBrowser zipFileBrowser;

    @Inject
    MediaFileHelper mediaFileHelper;

    @Inject
    MakeDataPrivate makeDataPrivate;

    @Named("uploadSync")
    @Inject
    UseCase upload;

    @Named("allowedToConnectSync")
    @Inject
    UseCase allowedToConnect;

    @Named("checkDeviceNotificationSync")
    @Inject
    UseCase checkDeviceNotification;

    public DataSyncService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FlowApp application = (FlowApp) getApplicationContext();
        application.getApplicationComponent().inject(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        makeDataPrivate.dispose();
        upload.dispose();
        allowedToConnect.dispose();
        checkDeviceNotification.dispose();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        makeDataPrivate.dispose();
        makeDataPrivate.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onComplete() {
                exportAndSync();
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                exportAndSync();
            }
        }, null);
    }

    private void exportAndSync() {
        try {
            mDatabase.open();
            exportSurveys();
            allowedToConnect.execute(new DefaultObserver<Boolean>() {
                @Override
                public void onNext(Boolean connectAllowed) {
                    if (connectAllowed) {
                        checkDeviceNotification();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    Timber.e(e);
                }
            }, null);
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        } finally {
            if (mDatabase != null) {
                mDatabase.close();
            }
        }
    }

    // ================================================================= //
    // ============================ EXPORT ============================= //
    // ================================================================= //

    /**
     * Create zip files, if necessary
     */
    private void exportSurveys() {
        // First off, ensure surveys marked as 'exported' are indeed found in the external storage.
        // Missing surveys will be set to 'submitted', so the next step re-creates these files too.
        checkExportedFiles();

        for (long id : getUnexportedSurveys()) {
            try {
                exportSurvey(id);
             //if the zip creation fails for one survey, let it still attempt to create the others
            } catch (Exception e) {
                Timber.e(e, "Error creating zip file for %d", id);
            }
        }
    }

    private void exportSurvey(long id) {
        ZipFileData zipFileData = formZip(id);

        if (zipFileData != null) {
            // Create new entries in the transmission queue
            mDatabase.createTransmission(id, zipFileData.formId, zipFileData.filename);
            updateSurveyStatus(id, SurveyInstanceStatus.SUBMITTED);

            for (String image : zipFileData.imagePaths) {
                mDatabase.createTransmission(id, zipFileData.formId, image);
            }
        }
    }

    private void checkExportedFiles() {
        Cursor cursor = mDatabase.getSurveyInstancesByStatus(SurveyInstanceStatus.SUBMITTED);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    long id = cursor
                            .getLong(cursor.getColumnIndexOrThrow(SurveyInstanceColumns._ID));
                    String uuid = cursor
                            .getString(cursor.getColumnIndexOrThrow(SurveyInstanceColumns.UUID));
                    if (!zipFileBrowser.getSurveyInstanceFile(uuid).exists()) {
                        Timber.d("Exported file for survey %s not found. It's status " +
                                "will be set to 'submitted', and will be reprocessed", uuid);
                        updateSurveyStatus(id, SurveyInstanceStatus.SUBMIT_REQUESTED);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }

    @NonNull
    private long[] getUnexportedSurveys() {
        long[] surveyInstanceIds = new long[0];// Avoid null cases
        Cursor cursor = mDatabase.getSurveyInstancesByStatus(SurveyInstanceStatus.SUBMIT_REQUESTED);
        if (cursor != null) {
            surveyInstanceIds = new long[cursor.getCount()];
            if (cursor.moveToFirst()) {
                do {
                    surveyInstanceIds[cursor.getPosition()] =
                            cursor.getLong(cursor.getColumnIndexOrThrow(SurveyInstanceColumns._ID));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return surveyInstanceIds;
    }

    private ZipFileData formZip(long surveyInstanceId) {
        try {
            ZipFileData zipFileData = new ZipFileData();
            // Process form instance data and collect image filenames
            Set<String> imagePaths = new HashSet<>();
            FormInstance formInstance = processFormInstance(surveyInstanceId,
                    imagePaths);
            zipFileData.imagePaths.addAll(imagePaths);
            // Serialize form instance as JSON
            zipFileData.data = new GsonMapper().write(formInstance, FormInstance.class);
            zipFileData.uuid = formInstance.getUUID();
            zipFileData.formId = formInstance.getFormId();
            if (TextUtils.isEmpty(zipFileData.formId)) {
                NullPointerException exception = new NullPointerException(" formId is null");
                Timber.e(exception);
            }
            Survey survey = mDatabase.getSurvey(zipFileData.formId);
            if (survey == null) {
                NullPointerException exception = new NullPointerException("survey is null");
                Timber.e(exception);
                //form name is only used for notification so it is ok if empty
                zipFileData.formName = "";
            } else {
                zipFileData.formName = survey.getName();
            }

            // The filename will match the Survey Instance UUID
            File zipFile = zipFileBrowser.getSurveyInstanceFile(zipFileData.uuid);

            // Write the data into the zip file
            String fileName = zipFile.getName();
            zipFileData.filename = fileName;
            Timber.i("Creating zip file: " + fileName);
            FileOutputStream fout = new FileOutputStream(zipFile);
            CheckedOutputStream checkedOutStream = new CheckedOutputStream(fout, new Adler32());
            ZipOutputStream zos = new ZipOutputStream(checkedOutStream);

            writeTextToZip(zos, zipFileData.data, SURVEY_DATA_FILE_JSON);
            String signingKeyString = BuildConfig.SIGNING_KEY;
            if (!StringUtil.isNullOrEmpty(signingKeyString)) {
                MessageDigest sha1Digest = MessageDigest.getInstance("SHA1");
                byte[] digest = sha1Digest.digest(zipFileData.data.getBytes(UTF_8_CHARSET));
                SecretKeySpec signingKey = new SecretKeySpec(
                        signingKeyString.getBytes(UTF_8_CHARSET),
                        SIGNING_ALGORITHM);
                Mac mac = Mac.getInstance(SIGNING_ALGORITHM);
                mac.init(signingKey);
                byte[] hmac = mac.doFinal(digest);
                String encodedHmac = Base64.encodeToString(hmac, Base64.DEFAULT);
                writeTextToZip(zos, encodedHmac, SIG_FILE_NAME);
            }

            final String checksum = "" + checkedOutStream.getChecksum().getValue();
            zos.close();
            Timber.i("Closed zip output stream for file: " + fileName + ". Checksum: " + checksum);
            return zipFileData;
        } catch (@NonNull IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            Timber.e(e, e.getMessage());
            return null;
        }
    }

    /**
     * Writes the contents of text to a zip entry within the Zip file behind zos
     * named fileName
     */
    private void writeTextToZip(@NonNull ZipOutputStream zos, @NonNull String text, String fileName)
            throws IOException {
        Timber.i("Writing zip entry");
        zos.putNextEntry(new ZipEntry(fileName));
        byte[] allBytes = text.getBytes(UTF_8_CHARSET);
        zos.write(allBytes, 0, allBytes.length);
        zos.closeEntry();
        Timber.i("Entry Complete");
    }

    /**
     * Iterate over the survey data returned from the database and populate the
     * ZipFileData information, setting the UUID, Survey ID, image paths, and String data.
     */
    @NonNull
    private FormInstance processFormInstance(long surveyInstanceId,
            @NonNull Set<String> imagePaths) {
        FormInstance formInstance = new FormInstance();
        List<Response> responses = new ArrayList<>();
        Cursor data = mDatabase.getResponsesData(surveyInstanceId);

        if (data != null && data.moveToFirst()) {
            String deviceIdentifier = preferences
                    .getString(Prefs.KEY_DEVICE_IDENTIFIER, Prefs.DEFAULT_VALUE_DEVICE_IDENTIFIER);
            deviceIdentifier = cleanVal(deviceIdentifier);
            // evaluate indices once, outside the loop
            int survey_fk_col = data.getColumnIndexOrThrow(SurveyInstanceColumns.SURVEY_ID);
            int question_fk_col = data.getColumnIndexOrThrow(ResponseColumns.QUESTION_ID);
            int answer_type_col = data.getColumnIndexOrThrow(ResponseColumns.TYPE);
            int answer_col = data.getColumnIndexOrThrow(ResponseColumns.ANSWER);
            int filename_col = data.getColumnIndexOrThrow(ResponseColumns.FILENAME);
            int iterationColumn = data.getColumnIndexOrThrow(ResponseColumns.ITERATION);
            int disp_name_col = data.getColumnIndexOrThrow(UserColumns.NAME);
            int email_col = data.getColumnIndexOrThrow(UserColumns.EMAIL);
            int submitted_date_col = data
                    .getColumnIndexOrThrow(SurveyInstanceColumns.SUBMITTED_DATE);
            int uuid_col = data.getColumnIndexOrThrow(SurveyInstanceColumns.UUID);
            int duration_col = data.getColumnIndexOrThrow(SurveyInstanceColumns.DURATION);
            int localeId_col = data.getColumnIndexOrThrow(SurveyInstanceColumns.RECORD_ID);
            // Note: No need to query the surveyInstanceId, we already have that value

            do {
                // Sanitize answer value. No newlines or tabs!
                String value = data.getString(answer_col);
                if (value != null) {
                    value = value.replace("\n", SPACE);
                    value = value.replace(DELIMITER, SPACE);
                    value = value.trim();
                }
                // never send empty answers
                if (value == null || value.length() == 0) {
                    continue;
                }
                final long submitted_date = data.getLong(submitted_date_col);
                final long surveyal_time = (data.getLong(duration_col)) / 1000;

                if (formInstance.getUUID() == null) {
                    formInstance.setUUID(data.getString(uuid_col));
                    formInstance.setFormId(data.getString(survey_fk_col));
                    formInstance.setDataPointId(data.getString(localeId_col));
                    formInstance.setDeviceId(deviceIdentifier);
                    formInstance.setSubmissionDate(submitted_date);
                    formInstance.setDuration(surveyal_time);
                    formInstance.setUsername(cleanVal(data.getString(disp_name_col)));
                    formInstance.setEmail(cleanVal(data.getString(email_col)));
                }

                // If the response has any file attached, enqueue it to the image list
                String filePath = data.getString(filename_col);
                String filename = getFilenameFromPath(filePath);

                if (!TextUtils.isEmpty(filename)) {
                    imagePaths.add(filename);
                }

                String type = data.getString(answer_type_col);

                String rawQuestionId = data.getString(question_fk_col);
                int iteration = data.getInt(iterationColumn);
                String[] tokens = rawQuestionId.split("\\|", -1);
                if (tokens.length == 2) {
                    // This is a compound ID from a repeatable question
                    rawQuestionId = tokens[0];
                    iteration = Integer.parseInt(tokens[1]);
                }
                iteration = Math.max(iteration, 0);
                Response response = new Response();
                response.setQuestionId(rawQuestionId);
                response.setAnswerType(type);
                response.setValue(value);
                response.setIteration(iteration);
                responses.add(response);
            } while (data.moveToNext());

            formInstance.setResponses(responses);
        }
        if (data != null) {
            data.close();
        }

        return formInstance;
    }

    @Nullable
    private String getFilenameFromPath(@Nullable String filePath) {
        String filename = null;
        if (!TextUtils.isEmpty(filePath) && filePath.contains(File.separator)
                && filePath.contains(".")) {
            filename = filePath.substring(filePath.lastIndexOf(File.separator) + 1);

        }
        return filename;
    }

    // replace troublesome chars in user-provided values
    // replaceAll() compiles a Pattern, and so is inefficient inside a loop
    @Nullable
    private String cleanVal(@Nullable String val) {
        if (val != null) {
            if (val.contains(DELIMITER)) {
                val = val.replace(DELIMITER, SPACE);
            }
            if (val.contains(",")) {
                val = val.replace(",", SPACE);
            }
            if (val.contains("\n")) {
                val = val.replace("\n", SPACE);
            }
        }
        return val;
    }

    private void checkDeviceNotification() {
        checkDeviceNotification.dispose();
        checkDeviceNotification.execute(new DefaultObserver<List<String>>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                syncFiles();
            }

            @Override
            public void onNext(List<String> deletedFiles) {
                for (String formId : deletedFiles) {
                    displayFormDeletedNotification(formId);
                }
                syncFiles();
            }
        }, null);

    }

    private void syncFiles() {
        upload.dispose();
        upload.execute(new DefaultObserver<Set<String>>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                broadcastDataPointStatusChange();
            }

            @Override
            public void onNext(Set<String> errorForms) {
                for (String formId : errorForms) {
                    displayErrorNotification(formId);
                }
                broadcastDataPointStatusChange();
            }

        }, null);
    }

    private void updateSurveyStatus(long surveyInstanceId, int status) {
        mDatabase.updateSurveyInstanceStatus(surveyInstanceId, status);
        broadcastDataPointStatusChange();
    }

    private void broadcastDataPointStatusChange() {
        Intent intentBroadcast = new Intent(ConstantUtil.ACTION_DATA_SYNC);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentBroadcast);
    }

    private void displayErrorNotification(String formId) {
        NotificationHelper.displayErrorNotification(getString(R.string.sync_error_title, formId),
                getString(R.string.sync_error_message), this, formId(formId));
    }

    private void displayFormDeletedNotification(String formId) {
        final int notificationId = formId(formId);

        String text = String.format(getString(R.string.data_sync_error_form_deleted_text), formId);
        String title = getString(R.string.data_sync_error_form_deleted_title);

        NotificationHelper.displayNonOnGoingErrorNotification(this, notificationId, text, title);
    }

    /**
     * Coerce a form id into its numeric format
     */
    private static int formId(String id) {
        try {
            return Integer.valueOf(id);
        } catch (NumberFormatException e) {
            Timber.e(id + " is not a valid form id");
            return 0;
        }
    }

    /**
     * Helper class to wrap zip file's meta-data
     */
    public static class ZipFileData {

        @Nullable
        String uuid = null;

        @Nullable
        String formId = null;

        @Nullable
        String formName = null;

        @Nullable
        String filename = null;

        @Nullable
        String data = null;

        final List<String> imagePaths = new ArrayList<>();
    }
}
