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
import org.akvo.flow.api.FlowApi;
import org.akvo.flow.api.S3Api;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.database.SurveyDbDataSource;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.database.ResponseColumns;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.database.TransmissionStatus;
import org.akvo.flow.database.UserColumns;
import org.akvo.flow.domain.FileTransmission;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.MakeDataPrivate;
import org.akvo.flow.domain.interactor.ThreadAwareUseCase;
import org.akvo.flow.domain.response.FormInstance;
import org.akvo.flow.domain.response.Response;
import org.akvo.flow.util.ConnectivityStateManager;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.GsonMapper;
import org.akvo.flow.util.MediaFileHelper;
import org.akvo.flow.util.NotificationHelper;
import org.akvo.flow.util.StringUtil;
import org.akvo.flow.util.files.ZipFileBrowser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
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

    private static final String DATA_CONTENT_TYPE = "application/zip";
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";
    private static final String PNG_CONTENT_TYPE = "image/png";
    private static final String VIDEO_CONTENT_TYPE = "video/mp4";

    private static final String ACTION_SUBMIT = "submit";
    private static final String ACTION_IMAGE = "image";

    private static final String UTF_8_CHARSET = "UTF-8";

    /**
     * Number of retries to upload a file to S3
     */
    private static final int FILE_UPLOAD_RETRIES = 2;

    @Inject
    SurveyDbDataSource mDatabase;

    @Inject
    Prefs preferences;

    @Inject
    ConnectivityStateManager connectivityStateManager;

    @Inject
    ZipFileBrowser zipFileBrowser;

    @Inject
    MediaFileHelper mediaFileHelper;

    @Inject
    MakeDataPrivate makeDataPrivate;

    @Named("uploadSync")
    @Inject
    ThreadAwareUseCase uploadSync;

    public DataSyncService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FlowApp application = (FlowApp) getApplicationContext();
        application.getApplicationComponent().inject(this);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        makeDataPrivate.dispose();
        uploadSync.dispose();
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
        });
    }

    private void exportAndSync() {
        try {
            mDatabase.open();
            exportSurveys();

            if (connectivityStateManager.isConnectionAvailable(preferences
                    .getBoolean(Prefs.KEY_CELL_UPLOAD, Prefs.DEFAULT_VALUE_CELL_UPLOAD))) {
                syncFiles();
            }
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

    // ================================================================= //
    // ======================= SYNCHRONISATION ========================= //
    // ================================================================= //

    /**
     * Sync every file (zip file, images, etc) that has a non synced state. This refers to:
     * - Queued transmissions
     * - Failed transmissions
     * Each transmission will be retried up to three times. If the transmission does
     * not succeed in those attempts, it will be marked as failed, and retried in the next sync.
     * Files are uploaded to S3 and the response's ETag is compared against a locally computed
     * MD5 checksum. Only if these fields match the transmission will be considered successful.
     */
    private void syncFiles() {
        // Check notifications for this device. This will update the status of the transmissions
        // if necessary, or mark form as deleted.
        Timber.d("syncFiles");
        uploadSync.dispose();
        uploadSync.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                super.onError(e);
                Timber.e(e);
            }

            @Override
            public void onNext(Boolean aBoolean) {
                super.onNext(aBoolean);
                Timber.d("Success");
            }
        }, null);

//        List<FileTransmission> transmissions = mDatabase.getUnSyncedTransmissions();
//
//        if (transmissions.isEmpty()) {
//            return;
//        }
//
//        Set<Long> syncedSurveys = new HashSet<>();// Successful transmissions
//        Set<Long> unsyncedSurveys = new HashSet<>();// Unsuccessful transmissions
//
//        final int totalFiles = transmissions.size();
//
//        for (int i = 0; i < totalFiles; i++) {
//            FileTransmission transmission = transmissions.get(i);
//            final long surveyInstanceId = transmission.getRespondentId();
//            if (syncFile(transmission)) {
//                syncedSurveys.add(surveyInstanceId);
//            } else {
//                unsyncedSurveys.add(surveyInstanceId);
//            }
//        }
//
//        // Retain successful survey instances, to mark them as UPLOADED
//        syncedSurveys.removeAll(unsyncedSurveys);
//
//        for (long surveyInstanceId : syncedSurveys) {
//            updateSurveyStatus(surveyInstanceId, SurveyInstanceStatus.UPLOADED);
//        }
//
//        // Ensure the unsynced ones are just SUBMITTED
//        for (long surveyInstanceId : unsyncedSurveys) {
//            updateSurveyStatus(surveyInstanceId, SurveyInstanceStatus.SUBMITTED);
//        }
    }

    private boolean syncFile(@NonNull FileTransmission transmission) {
        String filename = transmission.getFileName();
        String formId = transmission.getFormId();
        if (TextUtils.isEmpty(filename) || filename.lastIndexOf(".") < 0) {
            return false;
        }

        String contentType;
        String dir;
        String action;
        boolean isPublic;
        String filePath;
        String ext = filename.substring(filename.lastIndexOf("."));
        contentType = contentType(ext);
        switch (ext) {
            case ConstantUtil.JPG_SUFFIX:
            case ConstantUtil.PNG_SUFFIX:
            case ConstantUtil.VIDEO_SUFFIX:
                dir = ConstantUtil.S3_IMAGE_DIR;
                action = ACTION_IMAGE;
                isPublic = true;// Images/Videos have a public read policy
                filePath = buildMediaFilePath(filename);
                break;
            case ConstantUtil.ARCHIVE_SUFFIX:
                dir = ConstantUtil.S3_DATA_DIR;
                action = ACTION_SUBMIT;
                isPublic = false;
                filePath = buildZipFilePath(filename);
                break;
            default:
                return false;
        }

        // Temporarily set the status to 'IN PROGRESS'. Transmission status should
        // *always* be updated with the outcome of the upload operation.
        mDatabase.updateTransmissionStatus(transmission.getId(), TransmissionStatus.IN_PROGRESS);

        int status = TransmissionStatus.FAILED;
        boolean synced = false;

        if (sendFile(filePath, dir, contentType, isPublic, FILE_UPLOAD_RETRIES)) {
            FlowApi api = new FlowApi(getApplicationContext());
            switch (api.sendProcessingNotification(formId, action, filePath)) {
                case HttpURLConnection.HTTP_OK:
                    status = TransmissionStatus.SYNCED; // Mark everything synced
                    synced = true;
                    break;
                case HttpURLConnection.HTTP_NOT_FOUND:
                    // This form has been deleted in the dashboard, thus we cannot sync it
                    displayErrorNotification(formId);
                    status = TransmissionStatus.FORM_DELETED;
                    break;
                default:// Any error code
                    break;
            }
        }

        mDatabase.updateTransmissionStatus(transmission.getId(), status);
        return synced;
    }

    private String buildZipFilePath(String filename) {
        return zipFileBrowser.getZipFile(filename).getAbsolutePath();
    }

    private String buildMediaFilePath(String filename) {
        return mediaFileHelper.getMediaFile(filename).getAbsolutePath();
    }

    private boolean sendFile(@NonNull String fileAbsolutePath, String dir, String contentType,
            boolean isPublic, int retries) {
        final File file = new File(fileAbsolutePath);
        if (!file.exists()) {
            return false;
        }

        boolean ok = false;
        try {
            String fileName = fileAbsolutePath;
            if (fileName.contains(File.separator)) {
                fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
            }

            final String objectKey = dir + fileName;
            S3Api s3Api = new S3Api();
            ok = s3Api.put(objectKey, file, contentType, isPublic);
            if (!ok && retries > 0) {
                // If we have not expired all the retry attempts, try again.
                ok = sendFile(fileAbsolutePath, dir, contentType, isPublic, --retries);
            }
        } catch (IOException e) {
            Timber.e(e, "Could not send file: " + fileAbsolutePath + ". " + e.getMessage());
        }

        return ok;
    }

    private void updateSurveyStatus(long surveyInstanceId, int status) {
        // First off, update the status
        mDatabase.updateSurveyStatus(surveyInstanceId, status);

        // Dispatch a Broadcast notification to notify of survey instances status change
        Intent intentBroadcast = new Intent(ConstantUtil.ACTION_DATA_SYNC);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentBroadcast);
    }

    private void displayErrorNotification(String formId) {
        NotificationHelper.displayErrorNotification(getString(R.string.sync_error_title, formId),
                getString(R.string.sync_error_message), this, formId(formId));
    }

    private void displayFormDeletedNotification(String id, String name) {
        // Create a unique ID for this form's delete notification
        final int notificationId = formId(id);

        // Do not show failed if there is none
        String text = String.format(getString(R.string.data_sync_error_form_deleted_text), name);
        String title = getString(R.string.data_sync_error_form_deleted_title);

        NotificationHelper.displayNonOnGoingErrorNotification(this, notificationId, text, title);
    }

    private String contentType(@NonNull String ext) {
        switch (ext) {
            case ConstantUtil.PNG_SUFFIX:
                return PNG_CONTENT_TYPE;
            case ConstantUtil.JPG_SUFFIX:
                return JPEG_CONTENT_TYPE;
            case ConstantUtil.VIDEO_SUFFIX:
                return VIDEO_CONTENT_TYPE;
            case ConstantUtil.ARCHIVE_SUFFIX:
                return DATA_CONTENT_TYPE;
            default:
                return null;
        }
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
