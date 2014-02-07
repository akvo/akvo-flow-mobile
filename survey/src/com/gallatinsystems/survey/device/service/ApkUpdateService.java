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

package com.gallatinsystems.survey.device.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;

import org.json.JSONObject;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.exception.PersistentUncaughtExceptionHandler;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.FileUtil;
import com.gallatinsystems.survey.device.util.HttpUtil;
import com.gallatinsystems.survey.device.util.PlatformUtil;
import com.gallatinsystems.survey.device.util.PropertyUtil;
import com.gallatinsystems.survey.device.util.StatusUtil;

/**
 * This background service will check the rest api for a new version of the APK.
 * If found, it will display a notification, requesting permission to download and
 * install it. After clicking the notification, the app will download and install
 * the new APK.
 * 
 * @author Christopher Fagiani
 */
public class ApkUpdateService extends IntentService {
    private static final String TAG = "APK_UPDATE_SERVICE";

    private static final String APK_VERSION_SERVICE_PATH = "/deviceapprest?action=getLatestVersion&deviceType=androidPhone&appCode=fieldSurvey";
    
    private static final NumberFormat PCT_FORMAT = NumberFormat.getPercentInstance();
    
    private static final int UPGRADE_NOTIFICATION = 101;
    
    private static final int IO_BUFFER_SIZE = 8192;
    
    public static final String EXTRA_MODE     = "mode";
    public static final String EXTRA_LOCATION = "location";
    public static final String EXTRA_VERSION  = "version";
    public static final String EXTRA_PATH     = "path";
    
    public static final int MODE_CHECK    = 0;// Only check the version
    public static final int MODE_DOWNLOAD = 1;// Download latest version
    public static final int MODE_INSTALL  = 2;// Install latest version

    public ApkUpdateService() {
        super(TAG);
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler
                .getInstance());
        if (!isAbleToRun()) {
            Log.d(TAG, "No internet connection. Can't perform the requested operation");
            return;
        }
        
        switch (intent.getIntExtra(EXTRA_MODE, MODE_CHECK)) {
            case MODE_CHECK:
                checkUpgrade();
                break;
            case MODE_DOWNLOAD:
                downloadApk(intent.getStringExtra(EXTRA_LOCATION), intent.getStringExtra(EXTRA_VERSION));
                break;
            case MODE_INSTALL:
                install(intent.getStringExtra(EXTRA_PATH), intent.getStringExtra(EXTRA_VERSION));
                break;
        }
    }

    private String getServerBase() {
        SurveyDbAdapter db = new SurveyDbAdapter(this).open();
        final String serverBase = db.findPreference(ConstantUtil.SERVER_SETTING_KEY);
        db.close();
        if (!TextUtils.isEmpty(serverBase)) {
            return getResources().getStringArray(R.array.servers)[Integer.parseInt(serverBase)];
        }

        PropertyUtil props = new PropertyUtil(getResources());
        return props.getProperty(ConstantUtil.SERVER_BASE);
    }
    
    private void checkUpgrade() {
        try {
            String response = HttpUtil.httpGet(getServerBase() + APK_VERSION_SERVICE_PATH);

            if (!TextUtils.isEmpty(response)) {
                JSONObject json = new JSONObject(response);
                String ver = json.getString("version");
                if (!TextUtils.isEmpty(ver) && !ver.equalsIgnoreCase("null")) {
                    String installedVer = PlatformUtil.getVersionName(this);
                    String location = json.getString("fileName");
                    if (PlatformUtil.isNewerVersion(installedVer, ver) && !TextUtils.isEmpty(location)) {
                        // there is a newer version
                        displayDownloadNotification(location, ver);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not call apk version service", e);
            PersistentUncaughtExceptionHandler.recordException(e);
        }
    }
    
    private int fetchFileSize(String location) {
        int size = -1;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(location);
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.connect();
            
            size = conn.getContentLength();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        
        return size;
    }

    /**
     * Downloads the apk file and stores it on the file system
     * After the download, a new notification will be displayed, requesting
     * the user to 'click to install'
     * 
     * @param remoteFile
     * @param surveyId
     */
    private void downloadApk(String location, String version) {
        InputStream in = null;
        OutputStream out = null;
        HttpURLConnection conn = null;
        
        final int fileSize = fetchFileSize(location);
        Log.d(TAG, "APK size: " + fileSize);
        
        String fileName = location.substring(location.lastIndexOf('/') + 1);
        String dir = FileUtil.getStorageDirectory(ConstantUtil.APK_DIR + version, false);
        new File(dir).mkdirs();
        String localPath = dir + "/" + fileName;
        
        try {
            URL url = new URL(location);
            conn = (HttpURLConnection) url.openConnection();

            in = new BufferedInputStream(conn.getInputStream());
            out = new BufferedOutputStream(new FileOutputStream(localPath));
            
            int bytesWritten = 0;
            byte[] b = new byte[IO_BUFFER_SIZE];
            
            int read;
            while ((read = in.read(b)) != -1) {
                out.write(b, 0, read);
                bytesWritten += read;
                displayDownloadProgress(bytesWritten, fileSize);
            }
            
            out.flush();

            int status = conn.getResponseCode();
            if (status == 200) {
                displayInstallNotification(localPath, version);
            } else {
                Log.e(TAG, "Wrong status code: " + status);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            try {
                out.close();
            } catch (Exception ignored) {}
            try {
                in.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * this method checks if the service can perform the requested operation. If
     * there is no connectivity, this will return false, otherwise it will
     * return true
     * 
     * @param type
     * @return
     */
    private boolean isAbleToRun() {
        return StatusUtil.hasDataConnection(this, false);
    }
    
    private void install(String filePath, String version) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(filePath)),
                "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    
    /**
     * Sends a notification indicating that an APK is ready to download
     * 
     * @param location
     * @param version
     */
    private void displayDownloadNotification(String location, String version) {
        Intent intent = new Intent(this, ApkUpdateService.class);
        intent.putExtra(EXTRA_MODE, MODE_DOWNLOAD);
        intent.putExtra(EXTRA_LOCATION, location);
        intent.putExtra(EXTRA_VERSION, version);
        PendingIntent pendingIntent = PendingIntent.getService(this, 
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.info)
                .setContentTitle(getString(R.string.updateavail))
                .setContentText(getString(R.string.clicktodownload))
                .setTicker(getString(R.string.updateavail))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setOngoing(true);
        
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(UPGRADE_NOTIFICATION, mBuilder.build());
    }
    
    /**
     * Sends a notification indicating that an APK is ready to install
     * 
     * @param path
     * @param version
     */
    private void displayInstallNotification(String path, String version) {
        Intent intent = new Intent(this, ApkUpdateService.class);
        intent.putExtra(EXTRA_MODE, MODE_INSTALL);
        intent.putExtra(EXTRA_PATH, path);
        intent.putExtra(EXTRA_VERSION, version);
        PendingIntent pendingIntent = PendingIntent.getService(this, 
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.info)
                .setContentTitle(getString(R.string.updatedownloaded))
                .setContentText(getString(R.string.clicktoinstall))
                .setTicker(getString(R.string.updatedownloaded))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setOngoing(true);
        
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(UPGRADE_NOTIFICATION, mBuilder.build());
    }
    
    private void displayDownloadProgress(int bytesWritten, int totalBytes) {
        double percentComplete = 0.0d;
        if (bytesWritten > 0 && totalBytes > 0) {
            percentComplete = ((double) bytesWritten) / ((double) totalBytes);
        }
        if (percentComplete > 1.0d) {
            percentComplete = 1.0d;
        }
        
        int icon = percentComplete < 1.0 ? android.R.drawable.stat_sys_download
                : android.R.drawable.stat_sys_download_done;
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(icon)
                .setContentTitle(getString(R.string.downloadingupdate))
                .setContentText(getString(R.string.completed) + PCT_FORMAT.format(percentComplete))
                .setTicker(getString(R.string.downloadingupdate))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true);
        
        // Progress will only be displayed in Android versions > 4.0
        if (percentComplete > 0.0) {
            mBuilder.setProgress(totalBytes, bytesWritten, false);
        } else {
            mBuilder.setProgress(1, 1, true);
        }
        
        // Dummy intent. Do nothing when clicked
        PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(), 0);
        mBuilder.setContentIntent(intent);
        
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(UPGRADE_NOTIFICATION, mBuilder.build());
    }

}
