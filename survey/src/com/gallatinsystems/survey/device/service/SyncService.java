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

package com.gallatinsystems.survey.device.service;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter.SurveyInstanceColumns;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter.ResponseColumns;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter.UserColumns;
import com.gallatinsystems.survey.device.exception.PersistentUncaughtExceptionHandler;
import com.gallatinsystems.survey.device.util.Base64;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.FileUtil;
import com.gallatinsystems.survey.device.util.PropertyUtil;
import com.gallatinsystems.survey.device.util.StringUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
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
 */
public class SyncService extends IntentService {
    private static final String TAG = "SyncService";
    private static final String DELIMITER = "\t";
    private static final String SPACE = "\u0020"; // safe from source whitespace reformatting

    private static final String SIGNING_KEY_PROP = "signingKey";
    private static final String SIGNING_ALGORITHM = "HmacSHA1";

    /**
     * Used to have an extra  slash. Semantically  harmless, but made  the DB lookup fail
     */
    private static final String TEMP_FILE_NAME = "wfp";
    private static final String SURVEY_DATA_FILE = "data.txt";
    private static final String SIG_FILE_NAME = ".sig";

    private PropertyUtil mProps;
    private SurveyDbAdapter mDatabase;

    public SyncService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler
                .getInstance());

        mProps = new PropertyUtil(getResources());
        mDatabase = new SurveyDbAdapter(this);
        mDatabase.open();

        exportSurveys();

        mDatabase.close();
    }

    private void exportSurveys() {
        int[] surveyInstanceIds = new int[0];// Avoid null cases
        Cursor cursor = mDatabase.getUnexportedSurveyInstances();
        if (cursor != null) {
            surveyInstanceIds = new int[cursor.getCount()];
            if (cursor.moveToFirst()) {
                do {
                    surveyInstanceIds[cursor.getPosition()] = cursor.getInt(
                            cursor.getColumnIndexOrThrow(SurveyInstanceColumns._ID));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        for (int id : surveyInstanceIds) {
            ZipFileData zipFileData = formZip(id);
            if (zipFileData != null) {
                // Create new entries in the transmission queue
                // TODO: Add zip file entry

                for (String image : zipFileData.imagePaths) {
                    // TODO: Add image entries
                }
            }
        }
    }

    private ZipFileData formZip(long surveyInstanceId) {
        String fileName = createFileName();
        ZipFileData zipFileData = new ZipFileData();
        StringBuilder surveyBuf = new StringBuilder();

        // Hold the responses in the StringBuilder
        processSurveyData(surveyInstanceId, surveyBuf, zipFileData.imagePaths);

        // Write the data into the zip file
        try {
            File zipFile = new File(fileName);
            fileName = zipFile.getAbsolutePath();// Will normalize filename.
            zipFileData.filename = fileName;
            Log.i(TAG, "Creating zip file: " + fileName);
            FileOutputStream fout = new FileOutputStream(zipFile);
            CheckedOutputStream checkedOutStream = new CheckedOutputStream(fout, new Adler32());
            ZipOutputStream zos = new ZipOutputStream(checkedOutStream);

            writeTextToZip(zos, surveyBuf.toString(), SURVEY_DATA_FILE);
            String signingKeyString = mProps.getProperty(SIGNING_KEY_PROP);
            if (!StringUtil.isNullOrEmpty(signingKeyString)) {
                MessageDigest sha1Digest = MessageDigest
                        .getInstance("SHA1");
                byte[] digest = sha1Digest.digest(surveyBuf.toString()
                        .getBytes("UTF-8"));
                SecretKeySpec signingKey = new SecretKeySpec(
                        signingKeyString.getBytes("UTF-8"),
                        SIGNING_ALGORITHM);
                Mac mac = Mac.getInstance(SIGNING_ALGORITHM);
                mac.init(signingKey);
                byte[] hmac = mac.doFinal(digest);
                String encodedHmac = Base64.encodeBytes(hmac);
                writeTextToZip(zos, encodedHmac, SIG_FILE_NAME);
            }

            zipFileData.checksum = "" + checkedOutStream.getChecksum().getValue();
            zos.close();
            Log.i(TAG, "Closed zip output stream for file: " + fileName
                    + ". Checksum: " + zipFileData.checksum);
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
        Cursor data = mDatabase.getResponses(surveyInstanceId);

        if (data != null) {
            if (data.moveToFirst()) {
                Log.i(TAG, "There is data to send. Forming contents");
                String deviceIdentifier = mDatabase.findPreference(ConstantUtil.DEVICE_IDENT_KEY);
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
                int survey_start_col = data.getColumnIndexOrThrow(SurveyInstanceColumns.START_DATE);
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
                    final long started_date = data.getLong(survey_start_col);
                    final long surveyal_time = (submitted_date - started_date) / 1000;

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

    /**
     * constructs a filename for the data file
     * 
     * @return
     */
    private String createFileName() {
        // TODO move zip file extension to constantutil
        String fileName = TEMP_FILE_NAME + System.nanoTime() + ".zip";
        String dir = FileUtil.getStorageDirectory(ConstantUtil.SURVEYAL_DIR, fileName, false);
        FileUtil.findOrCreateDir(dir);

        return dir + File.separator + fileName;
    }

    /**
     * Helper class to wrap zip file's meta-data.<br>
     * It will contain:
     * <ul>
     * <li>Respondent IDs</li>
     * <li>Region IDs</li>
     * <li>File's checksum</li>
     * <li>Image Paths</li>
     * </ul>
     */
    class ZipFileData {
        String filename = null;
        String checksum = null;
        List<String> imagePaths = new ArrayList<String>();
    }

}
