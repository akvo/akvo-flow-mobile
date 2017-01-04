/*
 *  Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.akvo.flow.R;
import org.akvo.flow.api.FlowApi;
import org.akvo.flow.api.S3Api;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.dao.SurveyDbAdapter.ResponseColumns;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceColumns;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceStatus;
import org.akvo.flow.dao.SurveyDbAdapter.TransmissionStatus;
import org.akvo.flow.dao.SurveyDbAdapter.UserColumns;
import org.akvo.flow.domain.FileTransmission;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.response.FormInstance;
import org.akvo.flow.domain.response.Response;
import org.akvo.flow.exception.PersistentUncaughtExceptionHandler;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FileUtil.FileType;
import org.akvo.flow.util.NotificationHelper;
import org.akvo.flow.util.PropertyUtil;
import org.akvo.flow.util.StatusUtil;
import org.akvo.flow.util.StringUtil;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

/**
 * Handle survey export and sync in a background thread. The export process takes
 * no arguments, and will try to zip all the survey instances with a SUBMITTED status
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

    private static final String TAG = "SyncService";
    private static final String DELIMITER = "\t";
    private static final String SPACE = "\u0020"; // safe from source whitespace reformatting

    private static final String SIGNING_KEY_PROP = "signingKey";
    private static final String SIGNING_ALGORITHM = "HmacSHA1";

    private static final String SURVEY_DATA_FILE_JSON = "data.json";
    private static final String SIG_FILE_NAME = ".sig";

    private static final String DATA_CONTENT_TYPE = "application/zip";
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";
    private static final String PNG_CONTENT_TYPE = "image/png";
    private static final String VIDEO_CONTENT_TYPE = "video/mp4";

    private static final String ACTION_SUBMIT = "submit";
    private static final String ACTION_IMAGE = "image";

    // Reuse notification for data export
    private static final int NOTIFICATION_DATA_EXPORT = 1;

    /**
     * Number of retries to upload a file to S3
     */
    private static final int FILE_UPLOAD_RETRIES = 2;

    private PropertyUtil mProps;
    private SurveyDbAdapter mDatabase;
    private static final String UTF_8_CHARSET = "UTF-8";

    public DataSyncService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            mProps = new PropertyUtil(getResources());
            mDatabase = new SurveyDbAdapter(this);
            mDatabase.open();

            exportSurveys();// Create zip files, if necessary

            if (StatusUtil.hasDataConnection(this)) {
                syncFiles();// Sync everything
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            PersistentUncaughtExceptionHandler.recordException(e);
        } finally {
            if (mDatabase != null) {
                mDatabase.close();
            }
        }
    }

    // ================================================================= //
    // ============================ EXPORT ============================= //
    // ================================================================= //

    private void exportSurveys() {
        // First off, ensure surveys marked as 'exported' are indeed found in the external storage.
        // Missing surveys will be set to 'submitted', so the next step re-creates these files too.
        checkExportedFiles();

        for (long id : getUnexportedSurveys()) {
            ZipFileData zipFileData = formZip(id);
            if (zipFileData != null) {
                displayNotification(getString(R.string.exportcomplete), zipFileData.formName);

                // Create new entries in the transmission queue
                mDatabase.createTransmission(id, zipFileData.formId, zipFileData.filename);
                updateSurveyStatus(id, SurveyInstanceStatus.EXPORTED);

                for (String image : zipFileData.imagePaths) {
                    mDatabase.createTransmission(id, zipFileData.formId, image);
                }
            }
        }
    }

    @NonNull
    private File getSurveyInstanceFile(String uuid) {
        return new File(FileUtil.getFilesDir(FileType.DATA), uuid + ConstantUtil.ARCHIVE_SUFFIX);
    }

    private void checkExportedFiles() {
        Cursor cursor = mDatabase.getSurveyInstancesByStatus(SurveyInstanceStatus.EXPORTED);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    long id = cursor
                            .getLong(cursor.getColumnIndexOrThrow(SurveyInstanceColumns._ID));
                    String uuid = cursor
                            .getString(cursor.getColumnIndexOrThrow(SurveyInstanceColumns.UUID));
                    if (!getSurveyInstanceFile(uuid).exists()) {
                        Log.d(TAG, "Exported file for survey " + uuid + " not found. It's status " +
                                "will be set to 'submitted', and will be reprocessed");
                        updateSurveyStatus(id, SurveyInstanceStatus.SUBMITTED);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }

    @NonNull
    private long[] getUnexportedSurveys() {
        long[] surveyInstanceIds = new long[0];// Avoid null cases
        Cursor cursor = mDatabase.getSurveyInstancesByStatus(SurveyInstanceStatus.SUBMITTED);
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
            FormInstance formInstance = processFormInstance(surveyInstanceId,
                    zipFileData.imagePaths);

            if (formInstance == null) {
                return null;
            }

            // Serialize form instance as JSON
            zipFileData.data = new ObjectMapper().writeValueAsString(formInstance);
            zipFileData.uuid = formInstance.getUUID();
            zipFileData.formId = String.valueOf(formInstance.getFormId());
            zipFileData.formName = mDatabase.getSurvey(zipFileData.formId).getName();

            File zipFile = getSurveyInstanceFile(
                    zipFileData.uuid);// The filename will match the Survey Instance UUID

            // Write the data into the zip file
            String fileName = zipFile.getAbsolutePath();// Will normalize filename.
            zipFileData.filename = fileName;
            Log.i(TAG, "Creating zip file: " + fileName);
            FileOutputStream fout = new FileOutputStream(zipFile);
            CheckedOutputStream checkedOutStream = new CheckedOutputStream(fout, new Adler32());
            ZipOutputStream zos = new ZipOutputStream(checkedOutStream);

            writeTextToZip(zos, zipFileData.data, SURVEY_DATA_FILE_JSON);
            String signingKeyString = mProps.getProperty(SIGNING_KEY_PROP);
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
            Log.i(TAG,
                    "Closed zip output stream for file: " + fileName + ". Checksum: " + checksum);
            return zipFileData;
        } catch (@NonNull IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            PersistentUncaughtExceptionHandler.recordException(e);
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    /**
     * Writes the contents of text to a zip entry within the Zip file behind zos
     * named fileName
     */
    private void writeTextToZip(@NonNull ZipOutputStream zos, @NonNull String text, String fileName)
            throws IOException {
        Log.i(TAG, "Writing zip entry");
        zos.putNextEntry(new ZipEntry(fileName));
        byte[] allBytes = text.getBytes(UTF_8_CHARSET);
        zos.write(allBytes, 0, allBytes.length);
        zos.closeEntry();
        Log.i(TAG, "Entry Complete");
    }

    /**
     * Iterate over the survey data returned from the database and populate the
     * ZipFileData information, setting the UUID, Survey ID, image paths, and String data.
     */
    @NonNull
    private FormInstance processFormInstance(long surveyInstanceId,
            @NonNull List<String> imagePaths) {
        FormInstance formInstance = new FormInstance();
        List<Response> responses = new ArrayList<>();
        Cursor data = mDatabase.getResponsesData(surveyInstanceId);

        if (data != null && data.moveToFirst()) {
            String deviceIdentifier = mDatabase.getPreference(ConstantUtil.DEVICE_IDENT_KEY);
            if (deviceIdentifier == null) {
                deviceIdentifier = "unset";
            } else {
                deviceIdentifier = cleanVal(deviceIdentifier);
            }
            // evaluate indices once, outside the loop
            int survey_fk_col = data.getColumnIndexOrThrow(SurveyInstanceColumns.SURVEY_ID);
            int question_fk_col = data.getColumnIndexOrThrow(ResponseColumns.QUESTION_ID);
            int answer_type_col = data.getColumnIndexOrThrow(ResponseColumns.TYPE);
            int answer_col = data.getColumnIndexOrThrow(ResponseColumns.ANSWER);
            int filename_col = data.getColumnIndexOrThrow(ResponseColumns.FILENAME);
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
                    formInstance.setFormId(
                            data.getLong(survey_fk_col));// FormInstance uses a number for this attr
                    formInstance.setDataPointId(data.getString(localeId_col));
                    formInstance.setDeviceId(deviceIdentifier);
                    formInstance.setSubmissionDate(submitted_date);
                    formInstance.setDuration(surveyal_time);
                    formInstance.setUsername(cleanVal(data.getString(disp_name_col)));
                    formInstance.setEmail(cleanVal(data.getString(email_col)));
                }

                // If the response has any file attached, enqueue it to the image list
                String filename = data.getString(filename_col);
                if (!TextUtils.isEmpty(filename)) {
                    imagePaths.add(filename);
                }

                // Ensure backwards compatibility. Old image responses may contain filenames
                String type = data.getString(answer_type_col);
                if (ConstantUtil.IMAGE_RESPONSE_TYPE.equals(type)
                        || ConstantUtil.VIDEO_RESPONSE_TYPE.equals(type)) {
                    if (!TextUtils.isEmpty(value) && new File(value).exists()) {
                        imagePaths.add(value);
                    }
                }

                int iteration = 0;
                String qid = data.getString(question_fk_col);
                String[] tokens = qid.split("\\|", -1);
                if (tokens.length == 2) {
                    // This is a compound ID from a repeatable question
                    qid = tokens[0];
                    iteration = Integer.parseInt(tokens[1]);
                }

                Response response = new Response();
                response.setQuestionId(qid);
                response.setAnswerType(type);
                response.setValue(value);
                response.setIteration(iteration);
                responses.add(response);
            } while (data.moveToNext());

            formInstance.setResponses(responses);
            data.close();
        }

        return formInstance;
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
        final String serverBase = StatusUtil.getServerBase(this);
        // Check notifications for this device. This will update the status of the transmissions
        // if necessary, or mark form as deleted.
        checkDeviceNotifications(serverBase);

        List<FileTransmission> transmissions = mDatabase.getUnsyncedTransmissions();

        if (transmissions.isEmpty()) {
            return;
        }

        Set<Long> syncedSurveys = new HashSet<>();// Successful transmissions
        Set<Long> unsyncedSurveys = new HashSet<>();// Unsuccessful transmissions

        final int totalFiles = transmissions.size();
        displayProgressNotification(0, totalFiles);

        for (int i = 0; i < totalFiles; i++) {
            FileTransmission transmission = transmissions.get(i);
            final long surveyInstanceId = transmission.getRespondentId();
            if (syncFile(transmission.getFileName(), transmission.getFormId(), serverBase)) {
                syncedSurveys.add(surveyInstanceId);
            } else {
                unsyncedSurveys.add(surveyInstanceId);
            }
            displayProgressNotification(i, totalFiles);// Progress is the % of files handled so far
        }

        // Retain successful survey instances, to mark them as SYNCED
        syncedSurveys.removeAll(unsyncedSurveys);

        displaySyncedNotification(syncedSurveys.size(), unsyncedSurveys.size());

        for (long surveyInstanceId : syncedSurveys) {
            updateSurveyStatus(surveyInstanceId, SurveyInstanceStatus.SYNCED);
        }

        // Ensure the unsynced ones are just EXPORTED
        for (long surveyInstanceId : unsyncedSurveys) {
            updateSurveyStatus(surveyInstanceId, SurveyInstanceStatus.EXPORTED);
        }
    }

    private boolean syncFile(@NonNull String filename, @NonNull String formId,
            @NonNull String serverBase) {
        if (TextUtils.isEmpty(filename) || filename.lastIndexOf(".") < 0) {
            return false;
        }

        String contentType, dir, action;
        boolean isPublic;
        String ext = filename.substring(filename.lastIndexOf("."));
        contentType = contentType(ext);
        switch (ext) {
            case ConstantUtil.JPG_SUFFIX:
            case ConstantUtil.PNG_SUFFIX:
            case ConstantUtil.VIDEO_SUFFIX:
                dir = ConstantUtil.S3_IMAGE_DIR;
                action = ACTION_IMAGE;
                isPublic = true;// Images/Videos have a public read policy
                break;
            case ConstantUtil.ARCHIVE_SUFFIX:
                dir = ConstantUtil.S3_DATA_DIR;
                action = ACTION_SUBMIT;
                isPublic = false;
                break;
            default:
                return false;
        }

        // Temporarily set the status to 'IN PROGRESS'. Transmission status should
        // *always* be updated with the outcome of the upload operation.
        mDatabase.updateTransmissionHistory(filename, TransmissionStatus.IN_PROGRESS);

        int status = TransmissionStatus.FAILED;
        boolean synced = false;

        if (sendFile(filename, dir, contentType, isPublic, FILE_UPLOAD_RETRIES)) {
            FlowApi api = new FlowApi();
            switch (api.sendProcessingNotification(serverBase, formId, action,
                    getDestName(filename))) {
                case HttpStatus.SC_OK:
                    status = TransmissionStatus.SYNCED;// Mark everything completed
                    synced = true;
                    break;
                case HttpStatus.SC_NOT_FOUND:
                    // This form has been deleted in the dashboard, thus we cannot sync it
                    displayErrorNotification(formId);
                    status = TransmissionStatus.FORM_DELETED;
                    break;
                default:// Any error code
                    break;
            }
        }

        mDatabase.updateTransmissionHistory(filename, status);
        return synced;
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
            S3Api s3Api = new S3Api(this);
            ok = s3Api.put(objectKey, file, contentType, isPublic);
            if (!ok && retries > 0) {
                // If we have not expired all the retry attempts, try again.
                ok = sendFile(fileAbsolutePath, dir, contentType, isPublic, --retries);
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not send file: " + fileAbsolutePath + ". " + e.getMessage(), e);
            PersistentUncaughtExceptionHandler.recordException(e);
        }

        return ok;
    }

    /**
     * Request missing files (images) in the datastore.
     * The server will provide us with a list of missing images,
     * so we can accordingly update their status in the database.
     * This will help us fixing the Issue #55
     * Steps:
     * 1- Request the list of files to the server
     * 2- Update the status of those files in the local database
     */
    private void checkDeviceNotifications(@NonNull String serverBase) {
        FlowApi flowApi = new FlowApi();
        try {
            String[] surveyIds = mDatabase.getSurveyIds();
            JSONObject jResponse = flowApi.getDeviceNotification(serverBase, surveyIds);

            if (jResponse != null) {
                List<String> files = parseFiles(jResponse.optJSONArray("missingFiles"));
                files.addAll(parseFiles(jResponse.optJSONArray("missingUnknown")));

                // Handle missing files. If an unknown file exists in the filesystem
                // it will be marked as failed in the transmission history, so it can
                // be handled and retried in the next sync attempt.
                for (String filename : files) {
                    if (new File(filename).exists()) {
                        setFileTransmissionFailed(filename);
                    }
                }

                JSONArray jForms = jResponse.optJSONArray("deletedForms");
                if (jForms != null) {
                    for (int i = 0; i < jForms.length(); i++) {
                        String id = jForms.getString(i);
                        Survey s = mDatabase.getSurvey(id);
                        if (s != null) {
                            displayFormDeletedNotification(id, s.getName());
                        }
                        mDatabase.deleteSurvey(id);
                    }
                }
            } else {
                Log.e(TAG, "Could not retrieve missing files");
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not retrieve missing files", e);
        }
    }

    /**
     * Given a json array, return the list of contained filenames,
     * formatting the path to match the structure of the sdcard's files.
     */
    @NonNull
    private List<String> parseFiles(@Nullable JSONArray jFiles) throws JSONException {
        List<String> files = new ArrayList<>();
        if (jFiles != null) {
            for (int i = 0; i < jFiles.length(); i++) {
                // Build the sdcard path for each image
                String filename = jFiles.getString(i);
                File file = new File(FileUtil.getFilesDir(FileType.MEDIA), filename);
                files.add(file.getAbsolutePath());
            }
        }
        return files;
    }

    private void setFileTransmissionFailed(String filename) {
        int rows = mDatabase.updateTransmissionHistory(filename, TransmissionStatus.FAILED);
        if (rows == 0) {
            // Use a dummy "-1" as survey_instance_id, as the database needs that attribute
            mDatabase.createTransmission(-1, null, filename, TransmissionStatus.FAILED);
        }
    }

    @NonNull
    private static String getDestName(@NonNull String filename) {
        if (filename.contains("/")) {
            return filename.substring(filename.lastIndexOf("/") + 1);
        } else if (filename.contains("\\")) {
            filename = filename.substring(filename.lastIndexOf("\\") + 1);
        }

        return filename;
    }

    private void updateSurveyStatus(long surveyInstanceId, int status) {
        // First off, update the status
        mDatabase.updateSurveyStatus(surveyInstanceId, status);

        // Dispatch a Broadcast notification to notify of survey instances status change
        Intent intentBroadcast = new Intent(ConstantUtil.ACTION_DATA_SYNC);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentBroadcast);
    }

    private void displayNotification(String title, String text) {
        NotificationHelper.displayNotification(title, text, this,
                (int) (long) DataSyncService.NOTIFICATION_DATA_EXPORT);
    }

    private void displayErrorNotification(String formId) {
        NotificationHelper.displayErrorNotification(getString(R.string.sync_error_title, formId),
                getString(R.string.sync_error_message), this, formId(formId));
    }

    /**
     * Display a notification showing the up-to-date status of the sync
     *
     * @param synced number of handled transmissions so far (either successful or not)
     * @param total  number of transmissions in the batch
     */
    private void displayProgressNotification(int synced, int total) {
        String title = getString(R.string.data_sync_title);
        String text = getString(R.string.data_sync_text);
        NotificationHelper.displayProgressNotification(this, synced, total, title, text,
                ConstantUtil.NOTIFICATION_DATA_SYNC);
    }

    /**
     * Display a notification showing the final status of the sync
     *
     * @param syncedForms number of successful transmissions
     * @param failedForms number of failed transmissions
     */
    private void displaySyncedNotification(int syncedForms, int failedForms) {
        // Do not show failed if there is none
        String text = failedForms > 0 ?
                String.format(getString(R.string.data_sync_all), syncedForms, failedForms)
                :
                String.format(getString(R.string.data_sync_synced), syncedForms);

        String title = getString(R.string.data_sync_title);
        NotificationHelper.displayNonOngoingNotificationWithProgress(this, text, title,
                ConstantUtil.NOTIFICATION_DATA_SYNC);
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
            Log.e(TAG, id + " is not a valid form id");
            return 0;
        }
    }

    /**
     * Helper class to wrap zip file's meta-data
     */
    class ZipFileData {

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
