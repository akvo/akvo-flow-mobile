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
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.JSONObject;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
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
import com.gallatinsystems.survey.device.util.ViewUtil;

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
    
    public static final int MODE_CHECK    = 101;// Check latest version
    public static final int MODE_DOWNLOAD = 102;// Download latest version
    public static final int MODE_INSTALL  = 103;// Install latest version

    public ApkUpdateService() {
        super(TAG);
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler
                .getInstance());
        switch (intent.getIntExtra(EXTRA_MODE, MODE_CHECK)) {
            case MODE_CHECK:
                checkUpdates();
                break;
            case MODE_DOWNLOAD:
                String location = intent.getStringExtra(EXTRA_LOCATION);
                String version = intent.getStringExtra(EXTRA_VERSION);
                
                // Create parent directories, and delete files, if necessary
                String localPath = cleanupDownloads(location, version);
                
                if (downloadApk(location, version, localPath)) {
                    displayInstallNotification(localPath, version);
                } else {
                    // Clean up sd-card to ensure no corrupted file is leaked.
                    cleanupDownloads(location, version);
                    displayErrorNotification();
                }
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
    
    /**
     * Check for the latest downloaded version. If old versions are found, delete them.
     * The APK corresponding to the installed version will also be deleted, if found,
     * in order to perform a cleanup after an upgrade.
     * 
     * @return the path and version of a newer APK, if found, null otherwise
     */
    public static String[] checkDownloadedVersions(Context context) {
        final String installedVer = PlatformUtil.getVersionName(context);
        
        String maxVersion = installedVer;// Keep track of newest version available
        String apkPath = null;
        
        File appsLocation = new File(FileUtil.getStorageDirectory(ConstantUtil.APK_DIR, false));
        File[] versions = appsLocation.listFiles();
        if (versions != null) {
            for (File version : versions) {
                File[] apks = version.listFiles();
                if (apks == null) {
                    continue;// Nothing to see here
                }
                
                String versionName = version.getName();
                if (!PlatformUtil.isNewerVersion(maxVersion, versionName)) {
                    // Delete old versions
                    for (File apk : apks) {
                        apk.delete();
                    }
                    version.delete();
                } else if (apks.length > 0){
                    maxVersion = versionName;
                    apkPath = apks[0].getAbsolutePath();// There should only be 1
                }
            }
        }
        
        if (apkPath != null && maxVersion != null) {
            return new String[]{apkPath, maxVersion};
        }
        return null;
    }
    
    /**
     * Check if new FLOW versions are available to install. If a new version is available, 
     * we display a notification, requesting the user to download it.
     */
    private void checkUpdates() {
        if (!isAbleToRun()){
            Log.d(TAG, "No internet connection. Can't perform the requested operation");
            return;
        }
        
        try {
            String response = HttpUtil.httpGet(getServerBase() + APK_VERSION_SERVICE_PATH);
            if (!TextUtils.isEmpty(response)) {
                JSONObject json = new JSONObject(response);
                String ver = json.getString("version");
                if (!TextUtils.isEmpty(ver) && !ver.equalsIgnoreCase("null")) {
                    String location = json.getString("fileName");
                    if (PlatformUtil.isNewerVersion(PlatformUtil.getVersionName(this), ver) 
                            && !TextUtils.isEmpty(location)) {
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
    
    private String cleanupDownloads(String location, String version) {
        String fileName = location.substring(location.lastIndexOf('/') + 1);
        String dir = FileUtil.getStorageDirectory(ConstantUtil.APK_DIR + version, false);
        
        File directory = new File(dir);
        // Empty the directory
        if (directory.exists()) {
            for (File f : directory.listFiles()) {
                f.delete();
            }
        }
        directory.delete();
        directory.mkdirs();
        
        String localPath = dir + "/" + fileName;
        
        return localPath;
    }

    /**
     * Downloads the apk file and stores it on the file system
     * After the download, a new notification will be displayed, requesting
     * the user to 'click to install'
     * 
     * @param remoteFile
     * @param surveyId
     */
    private boolean downloadApk(String location, String version, String localPath) {
        if (!isAbleToRun()) {
            Log.d(TAG, "No internet connection. Can't perform the requested operation");
            return false;
        }
        
        boolean ok = false;
        InputStream in = null;
        OutputStream out = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(location);
            conn = (HttpURLConnection) url.openConnection();

            in = new BufferedInputStream(conn.getInputStream());
            out = new BufferedOutputStream(new FileOutputStream(localPath));
            
            int bytesWritten = 0;
            byte[] b = new byte[IO_BUFFER_SIZE];
            
            final int fileSize = conn.getContentLength();
            Log.d(TAG, "APK size: " + fileSize);
            
            int read;
            while ((read = in.read(b)) != -1) {
                out.write(b, 0, read);
                bytesWritten += read;
                displayDownloadProgress(bytesWritten, fileSize);
            }
            out.flush();

            final int status = conn.getResponseCode();
            
            if (status == HttpStatus.SC_OK) {
                Map<String, List<String>> headers = conn.getHeaderFields();
                String etag = headers != null ? getHeader(headers, "ETag") : null;
                etag = etag != null ? etag.replaceAll("\"", "") : null;// Remove quotes
                final String checksum = FileUtil.hexMd5(new File(localPath));
                
                if (etag != null && etag.equals(checksum)) {
                    ok = true;
                } else {
                    Log.e(TAG, "ETag comparison failed. Remote: " + etag + " Local: " + checksum);
                    ok = false;
                }
            } else {
                Log.e(TAG, "Wrong status code: " + status);
                ok = false;
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
        
        return ok;
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
        // Ensure the version is higher
        if (PlatformUtil.isNewerVersion(PlatformUtil.getVersionName(this), version)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(filePath)),
                    "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
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
                UPGRADE_NOTIFICATION, intent, PendingIntent.FLAG_UPDATE_CURRENT);

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
                UPGRADE_NOTIFICATION, intent, PendingIntent.FLAG_UPDATE_CURRENT);

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
    
    /**
     * In your face!
     * 
     * @param path
     * @param version
     */
    public static void displayInstallDialog(final Context context, final String path, final String version) {
       ViewUtil.showConfirmDialog(R.string.updatedownloaded, R.string.clicktoinstall, context, true, 
               new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(context, ApkUpdateService.class);
                    intent.putExtra(EXTRA_MODE, MODE_INSTALL);
                    intent.putExtra(EXTRA_PATH, path);
                    intent.putExtra(EXTRA_VERSION, version);
                    context.startService(intent);
                }
            }); 
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
    
    private void displayErrorNotification() {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(getString(R.string.network_error))
                .setContentText(getString(R.string.apk_upgrade_error))
                .setTicker(getString(R.string.network_error))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(false);
        
        // Dummy intent. Do nothing when clicked
        PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(), 0);
        mBuilder.setContentIntent(intent);
        
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(UPGRADE_NOTIFICATION, mBuilder.build());
    }
    
    /**
     * Helper function to get a particular header field from the header map
     * @param headers
     * @param key
     * @return header value, if found, false otherwise.
     */
    private String getHeader(Map<String, List<String>> headers, String key) {
        List<String> values = headers.get(key);
        if (values != null && values.size() > 0) {
            return values.get(0);
        }
        return null;
    }

}
