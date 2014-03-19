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

package org.akvo.flow.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import org.akvo.flow.R;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.exception.PersistentUncaughtExceptionHandler;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.HttpUtil;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.PropertyUtil;
import org.akvo.flow.util.StatusUtil;
import org.akvo.flow.util.ViewUtil;

/**
 * this background service will check the rest api for a new version of the APK.
 * If found, it will attempt to download automatically and then fire a
 * notification prompting the user to install
 * 
 * @author Christopher Fagiani
 */
public class ApkUpdateService extends Service {
    private static final String TAG = "APK_UPDATE_SERVICE";

    private static final String APK_VERSION_SERVICE_PATH = "/deviceapprest?action=getLatestVersion&deviceType=androidPhone&appCode=fieldSurvey";

    private PropertyUtil props;
    private Thread thread;
    private SurveyDbAdapter databaseAdaptor;

    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * life cycle method for the service. This is called by the system when the
     * service is started
     */
    public int onStartCommand(final Intent intent, int flags, final int startid) {
        thread = new Thread(new Runnable() {
            public void run() {
                checkAndDownload(startid);
            }
        });
        thread.start();
        return Service.START_REDELIVER_INTENT;
    }

    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler
                .getInstance());
        props = new PropertyUtil(getResources());
    }

    private void checkAndDownload(int startId) {
        String serverBase = null;
        try {
            databaseAdaptor = new SurveyDbAdapter(this);
            databaseAdaptor.open();
            serverBase = databaseAdaptor.getPreference(ConstantUtil.SERVER_SETTING_KEY);
        } finally {
            databaseAdaptor.close();
            databaseAdaptor = null;
        }
        if (StatusUtil.hasDataConnection(this)) {
            if (serverBase != null && serverBase.trim().length() > 0) {
                serverBase = getResources().getStringArray(R.array.servers)[Integer
                        .parseInt(serverBase)];
            } else {
                serverBase = props.getProperty(ConstantUtil.SERVER_BASE);
            }
            try {
                String response = HttpUtil.httpGet(serverBase
                        + APK_VERSION_SERVICE_PATH);

                if (response != null) {
                    JSONObject json = new JSONObject(response);
                    if (json != null) {
                        String ver = json.getString("version");
                        if (ver != null) {
                            String installedVer = PlatformUtil.getVersionName(this);
                            if (installedVer != null) {
                                if (ver.toLowerCase()
                                        .trim()
                                        .compareTo(
                                                installedVer.toLowerCase()
                                                        .trim()) > 0) {
                                    // there is a newer version so we need to
                                    // download
                                    downloadApk(json.getString("fileName"), ver);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Could not call apk version service", e);
            }
        }
        stopSelf(startId);
    }

    /**
     * downloads the apk file and stores it on the file system
     * 
     * @param remoteFile
     * @param surveyId
     */
    private void downloadApk(String fileName, String version) {
        try {
            String remoteFile = props.getProperty(ConstantUtil.DATA_UPLOAD_URL)
                    + ConstantUtil.REMOTE_APK_DIR + fileName;

            String localPath = FileUtil.getPathForFile(fileName,
                    ConstantUtil.APK_DIR + version + "/",
                    props.getBoolean(ConstantUtil.USE_INTERNAL_STORAGE));
            File localFile = new File(localPath);
            if (!localFile.exists()) {
                FileOutputStream out = null;
                try {
                    out = FileUtil
                            .getFileOutputStream(
                                    fileName,
                                    ConstantUtil.APK_DIR + version + "/",
                                    props.getBoolean(ConstantUtil.USE_INTERNAL_STORAGE),
                                    this);
                } catch (FileNotFoundException e1) {
                    Log.e(TAG, "Could not write apk file", e1);
                    PersistentUncaughtExceptionHandler.recordException(e1);
                }
                HttpUtil.httpDownload(remoteFile, out);
                if (out != null) {
                    try {
                        out.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }

            }
            fireNotification(localPath);
        } catch (Exception e) {
            Log.e(TAG, "Could not download apk file", e);
        }
    }

    /**
     * sends a notification indicating that an apk is ready for install
     * 
     * @param count
     */
    private void fireNotification(String filePath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(filePath)),
                "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Resources res = getResources();
        ViewUtil.fireNotification(res.getString(R.string.updateavail),
                res.getString(R.string.clicktoinstall), this, 0, null, intent,
                true);
    }
    
}
