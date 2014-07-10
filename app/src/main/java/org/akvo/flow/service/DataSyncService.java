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

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import org.akvo.flow.R;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.dao.SurveyDbAdapter.ResponseColumns;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceColumns;
import org.akvo.flow.dao.SurveyDbAdapter.UserColumns;
import org.akvo.flow.dao.SurveyDbAdapter.TransmissionStatus;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceStatus;
import org.akvo.flow.domain.FileTransmission;
import org.akvo.flow.exception.PersistentUncaughtExceptionHandler;
import org.akvo.flow.util.Base64;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FileUtil.FileType;
import org.akvo.flow.util.HttpUtil;
import org.akvo.flow.util.MultipartStream;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.PropertyUtil;
import org.akvo.flow.util.StatusUtil;
import org.akvo.flow.util.StringUtil;
import org.akvo.flow.util.ViewUtil;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
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
 *
 * Handle survey export and sync in a background thread. The export process takes
 * no arguments, and will try to zip all the survey instances with a SUBMITTED status
 * but with no EXPORT_DATE (export hasn't happened yet). Ideally, and if the service has been
 * triggered by a survey submission, only one survey instance will be exported. However, if for
 * whatever reason, a previous export attempt has failed, a new export will be tried on each
 * execution of the service, until the zip file finally gets exported. A possible scenario for
 * this is the submission of a survey when the external storage is not available, postponing the
 * export until it gets ready.
 *
 * After the export of the zip files, the sync will be run, attempting to upload all the non synced
 * files to the datastore.
 *
 * @author Christopher Fagiani
 *
 */
public class DataSyncService extends IntentService {
    private static final String TAG = "SyncService";
    private static final String DELIMITER = "\t";
    private static final String SPACE = "\u0020"; // safe from source whitespace reformatting

    private static final String SIGNING_KEY_PROP = "signingKey";
    private static final String SIGNING_ALGORITHM = "HmacSHA1";

    private static final String SURVEY_DATA_FILE = "data.txt";
    private static final String SIG_FILE_NAME = ".sig";

    // Sync constants
    private static final String DEVICE_NOTIFICATION_PATH = "/devicenotification?";
    private static final String NOTIFICATION_PATH = "/processor?action=";
    private static final String FILENAME_PARAM = "&fileName=";
    private static final String NOTIFICATION_PN_PARAM = "&phoneNumber=";
    private static final String CHECKSUM_PARAM = "&checksum=";
    private static final String IMEI_PARAM = "&imei=";
    private static final String DEVICE_ID_PARAM = "&devId=";
    private static final String VERSION_PARAM = "&ver=";

    private static final String DATA_CONTENT_TYPE = "application/zip";
    private static final String IMAGE_CONTENT_TYPE = "image/jpeg";
    private static final String VIDEO_CONTENT_TYPE = "video/mp4";
    private static final String S3_DATA_FILE_PATH = "devicezip";
    private static final String S3_IMAGE_FILE_PATH = "images";

    private static final String ACTION_SUBMIT = "submit";
    private static final String ACTION_IMAGE = "image";

    private static final String UTF8 = "UTF-8";

    /**
     * Number of retries to upload a file to S3
     */
    private static final int FILE_UPLOAD_RETRIES = 2;

    private PropertyUtil mProps;
    private SurveyDbAdapter mDatabase;

    public DataSyncService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler.getInstance());

        mProps = new PropertyUtil(getResources());
        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();

        exportSurveys();// Create zip files, if necessary

        if (StatusUtil.hasDataConnection(this)) {
            syncFiles();// Sync everything
        }

        mDatabase.close();
    }

    // ================================================================= //
    // ============================ EXPORT ============================= //
    // ================================================================= //

    private void exportSurveys() {
        long[] surveyInstanceIds = new long[0];// Avoid null cases
        Cursor cursor = mDatabase.getUnexportedSurveyInstances();
        if (cursor != null) {
            surveyInstanceIds = new long[cursor.getCount()];
            if (cursor.moveToFirst()) {
                do {
                    surveyInstanceIds[cursor.getPosition()] = cursor.getLong(
                            cursor.getColumnIndexOrThrow(SurveyInstanceColumns._ID));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        for (long id : surveyInstanceIds) {
            ZipFileData zipFileData = formZip(id);
            if (zipFileData != null) {
                displayExportNotification(getDestName(zipFileData.filename));

                // Create new entries in the transmission queue
                mDatabase.createTransmission(id, zipFileData.filename);
                updateSurveyStatus(id, SurveyInstanceStatus.EXPORTED);

                for (String image : zipFileData.imagePaths) {
                    mDatabase.createTransmission(id, image);
                }
            }
        }
    }

    private ZipFileData formZip(long surveyInstanceId) {
        File zipFile = new File(FileUtil.getFilesDir(FileType.DATA),
                System.nanoTime() + ConstantUtil.ARCHIVE_SUFFIX);
        ZipFileData zipFileData = new ZipFileData();
        StringBuilder surveyBuf = new StringBuilder();

        // Hold the responses in the StringBuilder
        processSurveyData(surveyInstanceId, surveyBuf, zipFileData.imagePaths);

        // Write the data into the zip file
        try {
            String fileName = zipFile.getAbsolutePath();// Will normalize filename.
            zipFileData.filename = fileName;
            Log.i(TAG, "Creating zip file: " + fileName);
            FileOutputStream fout = new FileOutputStream(zipFile);
            CheckedOutputStream checkedOutStream = new CheckedOutputStream(fout, new Adler32());
            ZipOutputStream zos = new ZipOutputStream(checkedOutStream);

            writeTextToZip(zos, surveyBuf.toString(), SURVEY_DATA_FILE);
            String signingKeyString = mProps.getProperty(SIGNING_KEY_PROP);
            if (!StringUtil.isNullOrEmpty(signingKeyString)) {
                MessageDigest sha1Digest = MessageDigest.getInstance("SHA1");
                byte[] digest = sha1Digest.digest(surveyBuf.toString().getBytes("UTF-8"));
                SecretKeySpec signingKey = new SecretKeySpec(signingKeyString.getBytes("UTF-8"),
                        SIGNING_ALGORITHM);
                Mac mac = Mac.getInstance(SIGNING_ALGORITHM);
                mac.init(signingKey);
                byte[] hmac = mac.doFinal(digest);
                String encodedHmac = Base64.encodeBytes(hmac);
                writeTextToZip(zos, encodedHmac, SIG_FILE_NAME);
            }

            final String checksum = "" + checkedOutStream.getChecksum().getValue();
            zos.close();
            Log.i(TAG, "Closed zip output stream for file: " + fileName + ". Checksum: " + checksum);
        } catch (IOException e) {
            PersistentUncaughtExceptionHandler.recordException(e);
            Log.e(TAG, e.getMessage());
            zipFileData = null;
        } catch (NoSuchAlgorithmException e) {
            PersistentUncaughtExceptionHandler.recordException(e);
            Log.e(TAG, e.getMessage());
            zipFileData = null;
        } catch (InvalidKeyException e) {
            PersistentUncaughtExceptionHandler.recordException(e);
            Log.e(TAG, e.getMessage());
            zipFileData = null;
        }

        return zipFileData;
    }

    /**
     * writes the contents of text to a zip entry within the Zip file behind zos
     * named fileName
     *
     * @param zos
     * @param text
     * @param fileName
     * @throws java.io.IOException
     */
    private void writeTextToZip(ZipOutputStream zos, String text,
            String fileName) throws IOException {
        Log.i(TAG, "Writing zip entry");
        zos.putNextEntry(new ZipEntry(fileName));
        byte[] allBytes = text.getBytes("UTF-8");
        zos.write(allBytes, 0, allBytes.length);
        zos.closeEntry();
        Log.i(TAG, "Entry Complete");
    }

    /**
     * iterate over the survey data returned from the database and populate the
     * string builder and collections passed in with the requisite information.
     *
     * @param surveyInstanceId - Survey Instance Id
     * @param buf - IN param. After execution this will contain the data to be sent
     * @param imagePaths - IN param. After execution this will contain the list of photo paths to send
     */
    private void processSurveyData(long surveyInstanceId, StringBuilder buf, List<String> imagePaths) {
        Cursor data = mDatabase.getResponsesData(surveyInstanceId);

        if (data != null) {
            if (data.moveToFirst()) {
                Log.i(TAG, "There is data to send. Forming contents");
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
                int disp_name_col = data.getColumnIndexOrThrow(UserColumns.NAME);
                int email_col = data.getColumnIndexOrThrow(UserColumns.EMAIL);
                int submitted_date_col = data.getColumnIndexOrThrow(SurveyInstanceColumns.SUBMITTED_DATE);
                int scored_val_col = data.getColumnIndexOrThrow(ResponseColumns.SCORED_VAL);
                int strength_col = data.getColumnIndexOrThrow(ResponseColumns.STRENGTH);
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

                    buf.append(data.getString(survey_fk_col));
                    buf.append(DELIMITER).append(String.valueOf(surveyInstanceId));
                    buf.append(DELIMITER).append(data.getString(question_fk_col));
                    String type = data.getString(answer_type_col);
                    buf.append(DELIMITER).append(type);
                    buf.append(DELIMITER).append(value);
                    buf.append(DELIMITER).append(cleanVal(data.getString(disp_name_col)));
                    buf.append(DELIMITER).append(cleanVal(data.getString(email_col)));
                    buf.append(DELIMITER).append(submitted_date);
                    buf.append(DELIMITER).append(deviceIdentifier);
                    buf.append(DELIMITER).append(neverNull(data.getString(scored_val_col)));
                    buf.append(DELIMITER).append(neverNull(data.getString(strength_col)));
                    buf.append(DELIMITER).append(data.getString(uuid_col));
                    buf.append(DELIMITER).append(surveyal_time);
                    buf.append(DELIMITER).append(data.getString(localeId_col));
                    buf.append("\n");

                    if (ConstantUtil.IMAGE_RESPONSE_TYPE.equals(type)
                            || ConstantUtil.VIDEO_RESPONSE_TYPE.equals(type)) {
                        imagePaths.add(value);
                    }
                } while (data.moveToNext());
            }

            data.close();
        }
    }

    // replace troublesome chars in user-provided values
    // replaceAll() compiles a Pattern, and so is inefficient inside a loop
    private String cleanVal(String val) {
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

    private String neverNull(String val) {
        if (val != null) {
            return val;
        } else
            return "";
    }

    // ================================================================= //
    // ======================= SYNCHRONISATION ========================= //
    // ================================================================= //

    /**
     * Sync every file (zip file, images, etc) that has a non synced state. This refers to:
     * - Queued transmissions
     * - Failed transmissions
     *
     * Each transmission will be retried up to three times. If the transmission does
     * not succeed in those attempts, it will be marked as failed, and retried in the next sync.
     * Files are uploaded to S3 and the response's ETag is compared against a locally computed
     * MD5 checksum. Only if these fields match the transmission will be considered successful.
     */
    private void syncFiles() {
        final String serverBase = StatusUtil.getServerBase(this);
        // Sync missing files. This will update the status of the transmissions if necessary
        checkMissingFiles(serverBase);

        List<FileTransmission> transmissions = mDatabase.getUnsyncedTransmissions();

        if (transmissions.isEmpty()) {
            return;
        }

        Set<Long> syncedSurveys = new HashSet<Long>();// Successful transmissions
        Set<Long> unsyncedSurveys = new HashSet<Long>();// Unsuccessful transmissions

        int synced = 0, failed = 0, total = transmissions.size();
        displaySyncNotification(synced, failed, total);

        for (FileTransmission transmission : transmissions) {
            final long surveyInstanceId = transmission.getRespondentId();
            if (syncFile(transmission.getFileName(), transmission.getStatus(), serverBase)) {
                syncedSurveys.add(surveyInstanceId);
                synced++;
            } else {
                unsyncedSurveys.add(surveyInstanceId);
                failed++;
            }
            displaySyncNotification(synced, failed, total);
        }

        // Retain successful survey instances, to mark them as SYNCED
        syncedSurveys.removeAll(unsyncedSurveys);

        for (long surveyInstanceId : syncedSurveys) {
            updateSurveyStatus(surveyInstanceId, SurveyInstanceStatus.SYNCED);
        }

        // Ensure the unsynced ones are just EXPORTED
        for (long surveyInstanceId : unsyncedSurveys) {
            updateSurveyStatus(surveyInstanceId, SurveyInstanceStatus.EXPORTED);
        }
    }

    private boolean syncFile(String filename, int status, String serverBase) {
        String contentType, dir, policy, signature, action;
        if (filename.endsWith(ConstantUtil.IMAGE_SUFFIX) || filename.endsWith(ConstantUtil.VIDEO_SUFFIX)) {
            contentType = filename.endsWith(ConstantUtil.IMAGE_SUFFIX) ? IMAGE_CONTENT_TYPE
                    : VIDEO_CONTENT_TYPE;
            dir = S3_IMAGE_FILE_PATH;
            policy = mProps.getProperty(ConstantUtil.IMAGE_S3_POLICY);
            signature = mProps.getProperty(ConstantUtil.IMAGE_S3_SIG);
            // Only notify server if the previous attempts have failed
            action = TransmissionStatus.FAILED == status ? ACTION_IMAGE : null;
        } else {
            contentType = DATA_CONTENT_TYPE;
            dir = S3_DATA_FILE_PATH;
            policy = mProps.getProperty(ConstantUtil.DATA_S3_POLICY);
            signature = mProps.getProperty(ConstantUtil.DATA_S3_SIG);
            action = ACTION_SUBMIT;
        }

        mDatabase.updateTransmissionHistory(filename, TransmissionStatus.IN_PROGRESS);

        boolean ok = sendFile(filename, dir, policy, signature, contentType, FILE_UPLOAD_RETRIES);
        final String destName = getDestName(filename);

        if (ok && action != null) {
            // If action is not null, notify GAE back-end that data is available
            ok = sendProcessingNotification(serverBase, action, destName, null);// TODO: checksum
        }

        // Update database and display notification
        // TODO: Ensure no Exception can be thrown from previous steps, to avoid leaking IN_PROGRESS status
        if (ok) {
            // Mark everything completed
            mDatabase.updateTransmissionHistory(filename, TransmissionStatus.SYNCED);
        } else {
            mDatabase.updateTransmissionHistory(filename, TransmissionStatus.FAILED);
        }

        return ok;
    }

    private boolean sendFile(String fileAbsolutePath, String dir, String policy, String sig,
            String contentType, int retries) {
        try {
            String fileName = fileAbsolutePath;
            if (fileName.contains(File.separator)) {
                fileName = fileName.substring(fileName.lastIndexOf(File.separator)); // TODO: Why show separator?
            }

            // Generate checksum, to be compared against response's ETag
            final String checksum = FileUtil.getMD5Checksum(fileAbsolutePath);

            MultipartStream stream = new MultipartStream(new URL(
                    mProps.getProperty(ConstantUtil.DATA_UPLOAD_URL)));

            stream.addFormField("key", dir + "/${filename}");
            stream.addFormField("AWSAccessKeyId", mProps.getProperty(ConstantUtil.S3_ID));
            stream.addFormField("acl", "public-read");
            stream.addFormField("success_action_redirect", "http://www.gallatinsystems.com/SuccessUpload.html");
            stream.addFormField("policy", policy);
            stream.addFormField("signature", sig);
            stream.addFormField("Content-Type", contentType);
            stream.addFile("file", fileAbsolutePath, null);
            int code = stream.execute(new MultipartStream.MultipartStreamStatusListner() {
                @Override
                public void uploadProgress(long bytesSent, long totalBytes) {
                    double percentComplete = 0.0d;
                    if (bytesSent > 0 && totalBytes > 0) {
                        percentComplete = ((double) bytesSent)
                                / ((double) totalBytes);
                    }
                    if (percentComplete > 1.0d) {
                        percentComplete = 1.0d;
                    }
                    // TODO: Include this progress somehow in the notification. Maybe the percentage should be bytes transmision progress
                    //fireNotification(NotificationType.PROGRESS, PCT_FORMAT.format(percentComplete)
                            //+ " - " + fileNameForNotification);
                }
            });

            /*
            * Determine if an upload to Amazon S3 is successful. The response
            * headers should contain a redirection to a URL, in which query, a
            * parameter called "etag" will be the md5 checksum of the uploaded
            * file.
            */
            String etag = null;
            if (code == HttpStatus.SC_SEE_OTHER) {
                String location = stream.getResponseHeader("Location");
                Uri uri = Uri.parse(location);
                etag = uri.getQueryParameter("etag");
                etag = etag.replaceAll("\"", "");// Remove quotes
                Log.d(TAG, "ETag: " + etag);
            } else {
                Log.e(TAG, "Server returned a bad code after upload: " + code);
            }

            if (etag != null && etag.equals(checksum)) {
                Log.d(TAG, "File uploaded successfully to datastore : " + fileName);
                return true;
            } else {
                Log.e(TAG, "Server returned a bad checksum after upload: " + etag);

                if (retries > 0) {
                    Log.i(TAG, "Retrying upload. Remaining attempts: " + retries);
                    return sendFile(fileAbsolutePath, dir, policy, sig, contentType, --retries);
                }

                Log.e(TAG, "File " + fileName + " upload failed.");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not send upload " + e.getMessage(), e);
            PersistentUncaughtExceptionHandler.recordException(e);
            return false;
        }
    }

    /**
     * Request missing files (images) in the datastore.
     * The server will provide us with a list of missing images,
     * so we can accordingly update their status in the database.
     * This will help us fixing the Issue #55
     *
     * Steps:
     * 1- Request the list of files to the server
     * 2- Update the status of those files in the local database
     */
    private void checkMissingFiles(String serverBase) {
        try {
            String response = getDeviceNotification(serverBase);
            if (!TextUtils.isEmpty(response)) {
                JSONObject jResponse = new JSONObject(response);
                JSONArray jMissingFiles = jResponse.optJSONArray("missingFiles");
                JSONArray jMissingUnknown = jResponse.optJSONArray("missingUnknown");

                // Mark the status of the files as 'Failed'
                for (String filename : parseFiles(jMissingFiles)) {
                    setFileTransmissionFailed(filename);
                }

                // Handle unknown files. If an unknown file exists in the filesystem
                // it will be marked as failed in the transmission history, so it can
                // be handled and retried in the next sync attempt.
                for (String filename : parseFiles(jMissingUnknown)) {
                    if (new File(filename).exists()) {
                        setFileTransmissionFailed(filename);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not retrieve missing files", e);
        }
    }

    /**
     * Given a json array, return the list of contained filenames,
     * formatting the path to match the structure of the sdcard's files.
     * @param jFiles
     * @return
     * @throws JSONException
     */
    private List<String> parseFiles(JSONArray jFiles) throws JSONException {
        List<String> files = new ArrayList<String>();
        if (jFiles != null) {
            for (int i=0; i<jFiles.length(); i++) {
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
            mDatabase.createTransmission(-1, filename, TransmissionStatus.FAILED);
        }
    }

    /**
     * Request the notifications GAE has ready for us, like the list of missing files.
     * @param serverBase
     * @return String body of the HTTP response
     * @throws Exception
     */
    private String getDeviceNotification(String serverBase) throws Exception {
        String phoneNumber = StatusUtil.getPhoneNumber(this);
        String imei = StatusUtil.getImei(this);
        String deviceId = mDatabase.getPreference(ConstantUtil.DEVICE_IDENT_KEY);
        String version = PlatformUtil.getVersionName(this);

        String url = serverBase + DEVICE_NOTIFICATION_PATH
                + (phoneNumber != null ?
                NOTIFICATION_PN_PARAM.substring(1) + URLEncoder.encode(phoneNumber, UTF8)
                : "")
                + (imei != null ?
                IMEI_PARAM + URLEncoder.encode(imei, UTF8)
                : "")
                + (deviceId != null ?
                DEVICE_ID_PARAM + URLEncoder.encode(deviceId, UTF8)
                : "")
                + VERSION_PARAM + URLEncoder.encode(version, UTF8);
        return HttpUtil.httpGet(url);
    }

    /**
     * sends a message to the service with the file name that was just uploaded
     * so it can start processing the file
     *
     * @param fileName
     * @return
     */
    private boolean sendProcessingNotification(String serverBase, String action, String fileName,
            String checksum) {
        boolean success = false;
        String url = serverBase + NOTIFICATION_PATH + action
                + FILENAME_PARAM + fileName
                + NOTIFICATION_PN_PARAM + StatusUtil.getPhoneNumber(this)
                + IMEI_PARAM + StatusUtil.getImei(this)
                + (checksum != null ? CHECKSUM_PARAM + checksum : "");
        try {
            HttpUtil.httpGet(url);
            success = true;
        } catch (Exception e) {
            Log.e(TAG, "GAE sync notification failed for file: " + fileName);
        }
        return success;
    }

    private static String getDestName(String filename) {
        if (filename.contains("/")) {
            return filename.substring(filename.lastIndexOf("/") + 1);
        } else if (filename.contains("\\")) {
            filename = filename.substring(filename.lastIndexOf("\\") + 1);
        }

        return filename;
    }

    /**
     *
     */
    private void updateSurveyStatus(long surveyInstanceId, int status) {
        // First off, update the status
        mDatabase.updateSurveyStatus(surveyInstanceId, status);

        // Dispatch a Broadcast notification to notify of survey instances status change
        Intent intentBroadcast = new Intent(getString(R.string.action_data_sync));
        sendBroadcast(intentBroadcast);
    }

    private void displayExportNotification(String filename) {
        String text = getString(R.string.exportcomplete);
        ViewUtil.fireNotification(text, filename, this, ConstantUtil.NOTIFICATION_DATA_SYNC, null);
    }

    /**
     * Display a notification showing the up-to-date status of the sync
     * @param synced number of successful transmissions
     * @param failed number of failed transmissions
     * @param total number of transmissions in the batch
     */
    private void displaySyncNotification(int synced, int failed, int total) {
        final boolean finished = synced + failed == total;
        int icon = finished ? android.R.drawable.stat_sys_upload_done
                : android.R.drawable.stat_sys_upload;

        // Do not show failed if there is none
        String text = failed > 0 ? String.format(getString(R.string.data_sync_all), synced, failed)
            : String.format(getString(R.string.data_sync_synced), synced);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(icon)
                .setContentTitle(getString(R.string.data_sync_title))
                .setContentText(text)
                .setTicker(text);

        builder.setOngoing(!finished);// Ongoing if still syncing data

        // Progress will only be displayed in Android versions > 4.0
        builder.setProgress(total, synced + failed, false);

        // Dummy intent. Do nothing when clicked
        PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(), 0);
        builder.setContentIntent(intent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(ConstantUtil.NOTIFICATION_DATA_SYNC, builder.build());
    }

    /**
     * Helper class to wrap zip file's meta-data.<br>
     * It will contain:
     * <ul>
     * <li>filename</li>
     * <li>Image Paths</li>
     * </ul>
     */
    class ZipFileData {
        String filename = null;
        List<String> imagePaths = new ArrayList<String>();
    }

}
