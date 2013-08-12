/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpStatus;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.exception.PersistentUncaughtExceptionHandler;
import com.gallatinsystems.survey.device.util.Base64;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.FileUtil;
import com.gallatinsystems.survey.device.util.HttpUtil;
import com.gallatinsystems.survey.device.util.MultipartStream;
import com.gallatinsystems.survey.device.util.PropertyUtil;
import com.gallatinsystems.survey.device.util.StatusUtil;
import com.gallatinsystems.survey.device.util.StringUtil;
import com.gallatinsystems.survey.device.util.ViewUtil;

/**
 * this activity will extract all submitted, unsent data from the database and
 * will form a zip file containing the data and corresponding image files (if
 * any). It will upload the data to the server and then will delete the data on
 * the device.
 * 
 * this activity can either export the zip file or export and send the zip file
 * (if it is invoked with the SEND type set under the TYPE_KEY in the extras
 * bundle)
 * 
 * 
 * @author Christopher Fagiani
 * 
 */
public class DataSyncService extends Service {

	private static final String TAG = "DATA_SYNC_SERVICE";
	private static final String NOTHING = "NADA";
	private static final String DELIMITER = "\t";
	private static final String SPACE = "\u0020"; //safe from source whitespace reformatting

	private static final String SIGNING_KEY_PROP = "signingKey";
	private static final String SIGNING_ALGORITHM = "HmacSHA1";

	private static final int COMPLETE_ID = 1;
	private static final int ID_NONE = -1;

	private static final boolean INCLUDE_IMAGES_IN_ZIP = false;

	private static final String NOTIFICATION_PATH = "/processor?action=submit&fileName=";
	private static final String NOTIFICATION_PN_PARAM = "&phoneNumber=";
	private static final String CHECKSUM_PARAM = "&checksum=";
	private static final String IMEI_PARAM = "&imei=";
	private static final String DATA_CONTENT_TYPE = "application/zip";
	private static final String S3_DATA_FILE_PATH = "devicezip";
	private static final String IMAGE_CONTENT_TYPE = "image/jpeg";
	private static final String S3_IMAGE_FILE_PATH = "images";

	private static final int BUF_SIZE = 2048;

	private static final NumberFormat PCT_FORMAT = NumberFormat.getPercentInstance();

	private SurveyDbAdapter databaseAdaptor;
	private static final String TEMP_FILE_NAME = "wfp"; //used to have an extra slash. Semantically harmless, but made the DB lookup fail
	private static final String ZIP_IMAGE_DIR = "images/";
	private static final String SURVEY_DATA_FILE = "data.txt";
	private static final String SIG_FILE_NAME = ".sig";
	private static final String REGION_DATA_FILE = "regions.txt";
	private Thread thread;
	private static Semaphore lock = new Semaphore(1);
	private static int counter = 0;
	private PropertyUtil props;
	
	private boolean debugFailMedia = false;

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
					Bundle extras = intent.getExtras();
					String type = extras != null ? extras
							.getString(ConstantUtil.OP_TYPE_KEY)
							: ConstantUtil.SEND;
					boolean forceFlag = extras != null ? extras.getBoolean(
							ConstantUtil.FORCE_KEY, false) : false;
					runSync(type, forceFlag);
				}
			}
		});
		thread.start();
		return Service.START_STICKY;
	}
	
	private boolean unsentData() {
		return databaseAdaptor.unsentDataCount() > 0;
	}
	
	private boolean unexportedData() {
		return databaseAdaptor.unexportedDataCount() > 0;
	}

	/**
	 * sets the uncaught exception handler for this thread so we can report
	 * exceptions to the server
	 */
	public void onCreate() {
		super.onCreate();
		Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler
				.getInstance());
		props = new PropertyUtil(getResources());
	}

	/**
	 * executes the data export/sync operation (based on the type passed in). If
	 * the type is SYNC the system will make sure that it has a data connection
	 * and that the user preferences don't prohibit using that connection
	 * (preventing upload of data over the cellular data network, for instance).
	 * 
	 * @param type
	 *            - either SYNC or EXPORT
	 * 
	 * @param forceFlag
	 *            - true to send regardless of history
	 */
	private void runSync(String type, boolean forceFlag) {
		try {
			lock.acquire();
			counter++;

			databaseAdaptor = new SurveyDbAdapter(this);
			databaseAdaptor.open();
			
			final String serverBase = getServerBase();
			final int uploadIndex = getUploadIndex();

			if (isAbleToRun(type, uploadIndex) && unsentData()) {
				final String fileName = createFileName(ConstantUtil.EXPORT.equals(type));
				final String destName = getDestName(fileName);
				
				// Create the zip file and get its meta-data
				ZipFileData zipFileData = formZip(fileName);
				
				if (zipFileData.hasData()) {
					if (ConstantUtil.SEND.equals(type)) {
						if (!INCLUDE_IMAGES_IN_ZIP) {
							sendImages(zipFileData.imagePaths);
						}
						sendData(zipFileData, fileName, destName, serverBase, uploadIndex);
					} else {
						fireNotification(ConstantUtil.EXPORT, destName);
					}
				} else if (forceFlag) {
					fireNotification(NOTHING, null);
				}

			} else if (unexportedData()) {
				// if we can't run the export, write the data as a zip
				String fileName = createFileName(false);
				ZipFileData zipFileData = formZip(fileName, true);
				if (zipFileData.hasRespondentIDs()) {
					databaseAdaptor.markDataAsExported(zipFileData.respondentIDs);
				}
			}
		} catch (InterruptedException e) {
			Log.e(TAG, "Data sync interrupted", e);
			PersistentUncaughtExceptionHandler.recordException(e);
		} finally {
			if (databaseAdaptor != null) {
				databaseAdaptor.close();
			}
			lock.release();
		}
		
		counter--;
		if (counter == 0) {
			stopSelf();
		}
	}
	
	private void sendData(ZipFileData zipFileData, String fileName, String destName,
			String serverBase, int uploadIndex) {
		databaseAdaptor.updateTransmissionHistory(zipFileData.respondentIDs,
				fileName, ConstantUtil.IN_PROGRESS_STATUS);
		boolean isOk = sendFile(fileName, S3_DATA_FILE_PATH,
				props.getProperty(ConstantUtil.DATA_S3_POLICY),
				props.getProperty(ConstantUtil.DATA_S3_SIG),
				DATA_CONTENT_TYPE);
		if (isOk) {
			//Notify GAE back-end that data is available
			if (sendProcessingNotification(serverBase, destName, zipFileData.checksum)) {
				//Mark everything completed
				databaseAdaptor.markDataAsSent(zipFileData.respondentIDs, String.valueOf(true));
				// update the transmission history records too
				databaseAdaptor.updateTransmissionHistory(zipFileData.respondentIDs,
						fileName, ConstantUtil.COMPLETE_STATUS);
				
				databaseAdaptor.updatePlotStatus(zipFileData.regionIDs, ConstantUtil.SENT_STATUS);
				fireNotification(ConstantUtil.SEND, destName);
			} else {
				Log.e(TAG, "Could not update send status of data in the database. It will be resent on next execution of the service");
				//Notification failed, update transmission history
				// TODO: this could be a different "failed" status
				databaseAdaptor.updateTransmissionHistory(zipFileData.respondentIDs,
						fileName, ConstantUtil.FAILED_STATUS);
			}
		} else {
			// S3 upload failed, update transmission history
			databaseAdaptor.updateTransmissionHistory(zipFileData.respondentIDs,
					fileName, ConstantUtil.FAILED_STATUS);
		}
	}
	
	private boolean uploadImage(String image) {
		return sendFile(image, 
				S3_IMAGE_FILE_PATH,
				props.getProperty(ConstantUtil.IMAGE_S3_POLICY),
				props.getProperty(ConstantUtil.IMAGE_S3_SIG),
				IMAGE_CONTENT_TYPE);
	}
	
	private void sendImages(Map<String, List<String>> imagePaths) {
		for (Entry<String, List<String>> paths : imagePaths.entrySet()) {
			final String respondentID = paths.getKey();
			
			for (String image : paths.getValue()) {
				databaseAdaptor.createTransmissionHistory(Long.valueOf(respondentID),
						image, ConstantUtil.IN_PROGRESS_STATUS);
				
				if (uploadImage(image) || !debugFailMedia) {// TODO: Get rid of this flag
					databaseAdaptor.updateTransmissionHistory(
							Long.valueOf(respondentID),
							image, ConstantUtil.COMPLETE_STATUS);
				} else {
					databaseAdaptor.updateTransmissionHistory(
							Long.valueOf(respondentID),
							image, ConstantUtil.FAILED_STATUS);
				}
			}
		}
	}
	
	private String getServerBase() {
		final String serverBase = databaseAdaptor.findPreference(ConstantUtil.SERVER_SETTING_KEY);
		if (!TextUtils.isEmpty(serverBase)) {
			return getResources().getStringArray(R.array.servers)[Integer.parseInt(serverBase)];
		}
		
		return props.getProperty(ConstantUtil.SERVER_BASE);
	}
	
	private int getUploadIndex() {
		final String uploadOption = databaseAdaptor.findPreference(
				ConstantUtil.CELL_UPLOAD_SETTING_KEY);
		if (!TextUtils.isEmpty(uploadOption)) {
			return Integer.parseInt(uploadOption);
		}
		
		return ID_NONE;
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
	 * sends a message to the service with the file name that was just uploaded
	 * so it can start processing the file
	 * 
	 * @param fileName
	 * @return
	 */
	private boolean sendProcessingNotification(String serverBase,
			String fileName, String checksum) {
		boolean success = false;
		try {
			HttpUtil.httpGet(serverBase + NOTIFICATION_PATH + fileName
					+ NOTIFICATION_PN_PARAM + StatusUtil.getPhoneNumber(this)
					+ IMEI_PARAM + StatusUtil.getImei(this)
					+ CHECKSUM_PARAM + checksum);
			success = true;
		} catch (Exception e) {
			Log.e(TAG, "Could not send processing call", e);
		}
		return success;
	}

	
	/**
	 * displays a notification in the system status bar indicating the
	 * completion of the export/save operation
	 * 
	 * TODO: the notifications may not have room to display the entire error message.
	 * Clicking them should trigger display of the full error by the Survey app
	 * 
	 * @param type
	 */
	private void fireNotification(String type, String extraText) {
		CharSequence tickerText = null;
		if (ConstantUtil.SEND.equals(type)) {
			tickerText = getResources().getText(R.string.uploadcomplete);
		} else if (ConstantUtil.EXPORT.equals(type)) {
			tickerText = getResources().getText(R.string.exportcomplete);
		} else if (ConstantUtil.PROGRESS.equals(type)) {
			tickerText = getResources().getText(R.string.uploadprogress);
		} else if (ConstantUtil.FILE_COMPLETE.equals(type)) {
			tickerText = getResources().getText(R.string.filecomplete);
		} else if (ConstantUtil.ERROR.equals(type)) {
			tickerText = getResources().getText(R.string.uploaderror);
		} else {
			tickerText = getResources().getText(R.string.nothingtoexport);//This default is unclear to user
		}
		ViewUtil.fireNotification(tickerText.toString(),
				extraText != null ? extraText : "",
				this,
				COMPLETE_ID,
				null);
	}

	private ZipFileData formZip(String fileName) {
		return formZip(fileName, false);
	}

	/**
	 * create a zip file containing all the submitted data and images
	 * Upload or include images unless dataOnly is set
	 * 
	 * @return three sets of strings
	 *   [0] respondent ids
	 *   [1] region ids
	 *   [2] zip checksum
	 */
	private ZipFileData formZip(String fileName, boolean unexportedOnly) {
		ZipFileData zipFileData = new ZipFileData();
		StringBuilder surveyBuf = new StringBuilder();
		StringBuilder regionBuf = new StringBuilder();
		try {
			// extract survey data
			processSurveyData(surveyBuf, zipFileData.imagePaths, zipFileData.respondentIDs,
					unexportedOnly);

			// extract region data
			processRegionData(regionBuf, zipFileData.regionIDs);

			// now write the data
			if (zipFileData.hasData()) {
				File zipFile = new File(fileName);
				fileName = zipFile.getAbsolutePath();//Will normalize filename. 
				Log.i(TAG, "Creating zip file: " + fileName);
				FileOutputStream fout = new FileOutputStream(zipFile);
				CheckedOutputStream checkedOutStream = new CheckedOutputStream(
						fout, new Adler32());
				ZipOutputStream zos = new ZipOutputStream(checkedOutStream);

				// write the survey data
				if (zipFileData.hasRespondentIDs()) {
					writeTextToZip(zos, surveyBuf.toString(), SURVEY_DATA_FILE);
					String signingKeyString = props.getProperty(SIGNING_KEY_PROP);
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
					// create "queued" status records for the zip file
					//TODO: this does not always work!
					for (String id : zipFileData.respondentIDs) {
						databaseAdaptor.createTransmissionHistory(Long.valueOf(id),
								fileName, null);
					}
				}
				
				// write region data
				if (zipFileData.hasRegionIDs()) {
					writeTextToZip(zos, regionBuf.toString(), REGION_DATA_FILE);
				}

				if (INCLUDE_IMAGES_IN_ZIP) {
					byte[] buffer = new byte[BUF_SIZE];
					
					for (Entry<String, List<String>> paths : zipFileData.imagePaths.entrySet()) {
						final String respondentID = paths.getKey();
						
						for (String image : paths.getValue()) {
							try {
								BufferedInputStream bin = new BufferedInputStream(
										new FileInputStream(image));
								String name = ZIP_IMAGE_DIR;
								if (image.contains("/")) {
									name = name + image.substring(image.lastIndexOf("/") + 1);
								} else {
									name = name + image;
								}
								zos.putNextEntry(new ZipEntry(name));
								int bytesRead = bin.read(buffer);
								while (bytesRead > 0) {
									zos.write(buffer, 0, bytesRead);
									bytesRead = bin.read(buffer);
								}
								bin.close();
								zos.closeEntry();
							} catch (Exception e) {
								Log.e(TAG, "Could not add image " + image + " to zip: "
										+ e.getMessage());
							}
						}
					}
				}
				zipFileData.checksum = "" + checkedOutStream.getChecksum().getValue();
				zos.close();
				Log.i(TAG, "Closed zip output stream for file: " + fileName
						+ ". Checksum: " + zipFileData.checksum);
			}
		} catch (Exception e) {
			Log.e(TAG, "Could not save zip: " + e.getMessage(), e);
			// since not being able to write files is a big deal, record this
			// exception so it can be sent to the server
			PersistentUncaughtExceptionHandler.recordException(e);
			fileName = null;
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
	 * @throws IOException
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
	 * iterate over the plot data returned from the database and populate the
	 * string builder and collections passed in with the requisite information.
	 * 
	 * @param buf
	 *            - IN param. After execution this will contain the data to be
	 *            sent
	 * 
	 * @param plotIds
	 *            - IN param. After execution this will contain the ids of the
	 *            plots
	 */
	private void processRegionData(StringBuilder buf, Set<String> plotIds) {
		Cursor data = null;
		try {
			data = databaseAdaptor.listCompletePlotPoints();
			if (data != null && data.isFirst()) {
				do {
					String plotId = data
							.getString(data
									.getColumnIndexOrThrow(SurveyDbAdapter.PLOT_FK_COL));
					String plotPointId = data.getString(data
							.getColumnIndexOrThrow(SurveyDbAdapter.PK_ID_COL));
					if (plotPointId == null) {
						continue;
					}
					buf.append(plotId).append(",");
					plotIds.add(plotId);
					buf.append(plotPointId).append(",");
					buf.append(data.getString(data
							.getColumnIndexOrThrow(SurveyDbAdapter.DISP_NAME_COL)));
					buf.append(",")
							.append(data.getString(data
									.getColumnIndexOrThrow(SurveyDbAdapter.LAT_COL)));
					buf.append(",")
							.append(data.getString(data
									.getColumnIndexOrThrow(SurveyDbAdapter.LON_COL)));
					buf.append(",")
							.append(data.getString(data
									.getColumnIndexOrThrow(SurveyDbAdapter.ELEVATION_COL)));
					buf.append(",")
							.append(data.getString(data
									.getColumnIndexOrThrow(SurveyDbAdapter.CREATED_DATE_COL)));
					buf.append("\n");
				} while (data.moveToNext());
			}
		} catch (Exception e) {
			Log.e(TAG, "Could not extract survey data from db", e);
			PersistentUncaughtExceptionHandler.recordException(e);
		} finally {
			if (data != null) {
				data.close();
			}
		}
	}

	/**
	 * iterate over the survey data returned from the database and populate the
	 * string builder and collections passed in with the requisite information.
	 * 
	 * @param buf
	 *            - IN param. After execution this will contain the data to be
	 *            sent
	 * @param imagePaths
	 *            - IN param. After execution this will contain the list of
	 *            photo paths to send
	 * @param respondentIds
	 *            - IN param. After execution this will contain the ids of the
	 *            respondents
	 */
	private void processSurveyData(StringBuilder buf, Map<String, List<String>> imagePaths,
			Set<String> respondentIds, boolean unexportedOnly) {
		Cursor data = null;
		try {
			if (unexportedOnly) {
				data = databaseAdaptor.fetchUnexportedData();
			} else {
				data = databaseAdaptor.fetchUnsentData();
			}
			if (data != null && data.isFirst()) {
				Log.i(TAG, "There is data to send. Forming contents");
				String deviceIdentifier = databaseAdaptor
						.findPreference(ConstantUtil.DEVICE_IDENT_KEY);
				if (deviceIdentifier == null) {
					deviceIdentifier = "unset";
				} else {
					deviceIdentifier = cleanVal(deviceIdentifier);
				}
				//evaluate indices once, outside the loop
				int survey_fk_col = data.getColumnIndexOrThrow(SurveyDbAdapter.SURVEY_FK_COL);
				int pk_id_col = data.getColumnIndexOrThrow(SurveyDbAdapter.PK_ID_COL);
				int question_fk_col = data.getColumnIndexOrThrow(SurveyDbAdapter.QUESTION_FK_COL);
				int answer_type_col = data.getColumnIndexOrThrow(SurveyDbAdapter.ANSWER_TYPE_COL);
				int answer_col = data.getColumnIndexOrThrow(SurveyDbAdapter.ANSWER_COL);
				int disp_name_col = data.getColumnIndexOrThrow(SurveyDbAdapter.DISP_NAME_COL);
				int email_col = data.getColumnIndexOrThrow(SurveyDbAdapter.EMAIL_COL);
				int submitted_date_col = data.getColumnIndexOrThrow(SurveyDbAdapter.SUBMITTED_DATE_COL);
				int scored_val_col = data.getColumnIndexOrThrow(SurveyDbAdapter.SCORED_VAL_COL);
				int strength_col = data.getColumnIndexOrThrow(SurveyDbAdapter.STRENGTH_COL);
				int uuid_col = data.getColumnIndexOrThrow(SurveyDbAdapter.UUID_COL);
				
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

					buf.append(data.getString(survey_fk_col));
					String respId = data.getString(pk_id_col);
					buf.append(DELIMITER).append(respId);
					buf.append(DELIMITER).append(data.getString(question_fk_col));
					String type = data.getString(answer_type_col);
					buf.append(DELIMITER).append(type);
					buf.append(DELIMITER).append(value);
					buf.append(DELIMITER).append(cleanVal(data.getString(disp_name_col)));
					buf.append(DELIMITER).append(cleanVal(data.getString(email_col)));
					buf.append(DELIMITER).append(data.getString(submitted_date_col));
					buf.append(DELIMITER).append(deviceIdentifier);
					buf.append(DELIMITER).append(neverNull(data.getString(scored_val_col)));
					buf.append(DELIMITER).append(neverNull(data.getString(strength_col)));
					buf.append(DELIMITER).append(data.getString(uuid_col));
					buf.append("\n");

					if (ConstantUtil.IMAGE_RESPONSE_TYPE.equals(type)
							|| ConstantUtil.VIDEO_RESPONSE_TYPE.equals(type)) {
						List<String> paths = imagePaths.get(respId);
						if (paths == null) {
							paths = new ArrayList<String>();
							imagePaths.put(respId, paths);
						}
						paths.add(value);
					}
					respondentIds.add(respId);
				} while (data.moveToNext());
			}
		} catch (Exception e) {
			Log.e(TAG, "Could not extract survey data from db", e);
			PersistentUncaughtExceptionHandler.recordException(e);
		} finally {
			if (data != null) {
				data.close();
			}
		}
	}

	//replace troublesome chars in user-provided values
	//replaceAll() compiles a Pattern, and so is inefficient inside a loop
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
		} else return "";
	}

	/**
	 * sends the zip file containing data/images to the server via an http
	 * upload
	 * 
	 * @param fileAbsolutePath
	 */
	private boolean sendFile(String fileAbsolutePath, String dir,
			String policy, String sig, String contentType) {

		try {
			String fileName = fileAbsolutePath;
			if (fileName.contains(File.separator)) {
				fileName = fileName.substring(fileName
						.lastIndexOf(File.separator)); //TODO: Why show separator?
			}
			final String fileNameForNotification = fileName;
			fireNotification(ConstantUtil.PROGRESS, fileName);
			
			// Generate checksum, to be compared against response's ETag
			final String checksum = FileUtil.getMD5Checksum(fileAbsolutePath);

			MultipartStream stream = new MultipartStream(new URL(
					props.getProperty(ConstantUtil.DATA_UPLOAD_URL)));

			stream.addFormField("key", dir + "/${filename}");
			stream.addFormField("AWSAccessKeyId",
					props.getProperty(ConstantUtil.S3_ID));
			stream.addFormField("acl", "public-read");
			stream.addFormField("success_action_redirect",
					"http://www.gallatinsystems.com/SuccessUpload.html");
			stream.addFormField("policy", policy);
			stream.addFormField("signature", sig);
			stream.addFormField("Content-Type", contentType);
			stream.addFile("file", fileAbsolutePath, null);
			int code = stream
					.execute(new MultipartStream.MultipartStreamStatusListner() {
						@Override
						public void uploadProgress(long bytesSent,
								long totalBytes) {
							double percentComplete = 0.0d;
							if (bytesSent > 0 && totalBytes > 0) {
								percentComplete = ((double) bytesSent)
										/ ((double) totalBytes);
							}
							if (percentComplete > 1.0d) {
								percentComplete = 1.0d;
							}
							fireNotification(ConstantUtil.PROGRESS,
									PCT_FORMAT.format(percentComplete) + " - "
											+ fileNameForNotification);
						}
					});
				
			/*
			 * Determine if an upload to Amazon S3 is successful.
			 * The response headers should contain a redirection to a URL,
			 * in which query, a parameter called "etag" will be the md5 checksum 
			 * of the uploaded file.
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
				fireNotification(ConstantUtil.FILE_COMPLETE,
						fileNameForNotification);
			} else {
				Log.e(TAG, "Server returned a bad checksum after upload: " + checksum);
				fireNotification(ConstantUtil.ERROR,
						getString(R.string.uploaderror) + " "
								+ fileNameForNotification);
				return false;
			}
		} catch (Exception e) {
			Log.e(TAG, "Could not send upload " + e.getMessage(), e);

			PersistentUncaughtExceptionHandler.recordException(e);
			return false;
		}
		return true;
	}

	/**
	 * constructs a filename for the data file
	 * 
	 * @return
	 */
	private String createFileName(boolean isAll) {
		//TODO move zip file extension to constantutil
		String fileName = TEMP_FILE_NAME + System.nanoTime() + ".zip"; 
		String dir = FileUtil.getStorageDirectory(ConstantUtil.SURVEYAL_DIR,
				fileName,
				props.getBoolean(ConstantUtil.USE_INTERNAL_STORAGE));
		FileUtil.findOrCreateDir(dir);		
		if(isAll){
			fileName = fileName.replace(".zip", "-all.zip");
		}		
		return dir + File.separator + fileName;
	}

	/**
	 * this method checks if the service can perform the requested operation. If
	 * the operation type is SEND and there is no connectivity, this will return
	 * false, otherwise it will return true
	 * 
	 * @param type
	 * @return
	 */
	private boolean isAbleToRun(String type, int uploadModeIndex) {
		boolean ok = false;
		// since a null type is treated like send, check !export
		if (!ConstantUtil.EXPORT.equals(type)) {
			if (uploadModeIndex > ID_NONE
					&& ConstantUtil.UPLOAD_NEVER_IDX == uploadModeIndex) {
				ok = StatusUtil.hasDataConnection(this, true);
			} else {
				ok = StatusUtil.hasDataConnection(this, false);
			}
		} else {
			// if we're exporting, we don't need to check the network
			ok = true;
		}
		return ok;
	}
	
	/**
	 * Helper class to wrap zip file's meta-data
	 *
	 */
	class ZipFileData {
		Set<String> respondentIDs = new HashSet<String>();
		Set<String> regionIDs = new HashSet<String>();
		String checksum = null;
		Map<String, List<String>> imagePaths = new HashMap<String, List<String>>();
		
		boolean hasData() {
			return (hasRespondentIDs() || hasRegionIDs());
		}
		
		boolean hasRespondentIDs() {
			return !respondentIDs.isEmpty();
		}
		
		boolean hasRegionIDs() {
			return !regionIDs.isEmpty();
		}
		
	}
	
}
