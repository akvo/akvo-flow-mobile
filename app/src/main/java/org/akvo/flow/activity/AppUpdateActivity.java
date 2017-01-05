/*
 *  Copyright (C) 2014-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.activity;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.util.ConnectivityStateManager;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FileUtil.FileType;
import org.akvo.flow.util.PlatformUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class AppUpdateActivity extends Activity {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_VERSION = "version";
    public static final String EXTRA_CHECKSUM = "md5Checksum";

    private static final String TAG = AppUpdateActivity.class.getSimpleName();
    private static final int IO_BUFFER_SIZE = 8192;
    private static final int MAX_PROGRESS = 100;

    private Button mInstallBtn;
    private ProgressBar mProgress;
    private UpdateAsyncTask mTask;

    private String mUrl;
    private String mVersion;
    private String mMd5Checksum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.app_update_activity);

        mUrl = getIntent().getStringExtra(EXTRA_URL);
        mVersion = getIntent().getStringExtra(EXTRA_VERSION);
        mMd5Checksum = getIntent().getStringExtra(EXTRA_CHECKSUM);

        mInstallBtn = (Button) findViewById(R.id.install_btn);
        mProgress = (ProgressBar) findViewById(R.id.progress);
        mProgress.setMax(MAX_PROGRESS);// Values will be in percentage

        // If the file is already downloaded, just prompt the install text
        final String filename = checkLocalFile();
        if (filename != null) {
            TextView updateTV = (TextView) findViewById(R.id.update_text);
            updateTV.setText(R.string.clicktoinstall);
            mInstallBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PlatformUtil.installAppUpdate(AppUpdateActivity.this, filename);
                }
            });
        } else {
            mInstallBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mInstallBtn.setEnabled(false);
                    mTask = new UpdateAsyncTask(AppUpdateActivity.this, mUrl, mVersion,
                            mMd5Checksum);
                    mTask.execute();
                }
            });
        }

        Button cancelBtn = (Button) findViewById(R.id.cancel_btn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
            }
        });
    }

    /**
     * Check out previously downloaded files. If the APK update is already downloaded,
     * and the MD5 checksum matches, the file is considered downloaded.
     *
     * @return filename of the already downloaded file, if exists. Null otherwise
     */
    private String checkLocalFile() {
        final String latestVersion = FileUtil.checkDownloadedVersions();
        if (latestVersion != null) {
            if (mMd5Checksum != null) {
                // The file was found, but we need to ensure the checksum matches,
                // to ensure the download succeeded
                File file = new File(latestVersion);
                if (!mMd5Checksum.equals(FileUtil.hexMd5(file))) {
                    file.delete();// Wipe corrupted files
                    return null;
                }
            }
            return latestVersion;
        }
        return null;
    }

    private void cancel() {
        if (isRunning()) {
            mTask.cancel(true);// Stop the update process
        }
        finish();
    }

    @Override
    public void onDestroy() {
        if (isRunning()) {
            mTask.cancel(true);
        }
        super.onDestroy();
    }

    private boolean isRunning() {
        return mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING;
    }

    private static class UpdateAsyncTask extends AsyncTask<Void, Integer, String> {

        private final Prefs prefs;
        private final String mUrl;
        private final String mVersion;
        private final WeakReference<AppUpdateActivity> activityWeakReference;
        private final ConnectivityStateManager connectivityStateManager;
        private String mMd5Checksum;

        public UpdateAsyncTask(AppUpdateActivity context, String mUrl, String mVersion,
                String mMd5Checksum) {
            this.prefs = new Prefs(context);
            this.mUrl = mUrl;
            this.mVersion = mVersion;
            this.activityWeakReference = new WeakReference<>(context);
            this.connectivityStateManager = new ConnectivityStateManager(context);
            this.mMd5Checksum = mMd5Checksum;
        }

        @Override
        protected String doInBackground(Void... params) {
            // Create parent directories, and delete files, if necessary
            String filename = createFile(mUrl, mVersion).getAbsolutePath();

            boolean syncOver3GAllowed = prefs
                    .getBoolean(Prefs.KEY_CELL_UPLOAD, Prefs.DEFAULT_VALUE_CELL_UPLOAD);
            if (!connectivityStateManager.isConnectionAvailable(syncOver3GAllowed)) {
                Log.e(TAG, "No internet connection available. Can't perform the requested operation");
            } else if (downloadApk(mUrl, filename) && !isCancelled()) {
                return filename;
            }
            // Clean up sd-card to ensure no corrupted file is leaked.
            cleanupDownloads(mVersion);
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            int bytesWritten = progress[0];
            int totalBytes = progress[1];
            int percentComplete = 0;

            if (bytesWritten > 0 && totalBytes > 0) {
                percentComplete = (int) ((bytesWritten) / ((float) totalBytes) * 100);
            }
            if (percentComplete > MAX_PROGRESS) {
                percentComplete = MAX_PROGRESS;
            }
            Log.d(TAG, "onProgressUpdate() - APK update: " + percentComplete + "%");
            notifyProgress(percentComplete);
        }

        private void notifyProgress(int percentComplete) {
            AppUpdateActivity appUpdateActivity = activityWeakReference.get();
            if (appUpdateActivity != null) {
                appUpdateActivity.updateDownloadProgress(percentComplete);
            }
        }

        @Override
        protected void onPostExecute(String filename) {
            AppUpdateActivity appUpdateActivity = activityWeakReference.get();
            if (TextUtils.isEmpty(filename)) {
                if (appUpdateActivity != null) {
                    appUpdateActivity.onDownloadError();
                }
                return;
            }

            if (appUpdateActivity != null) {
                appUpdateActivity.onDownloadSuccess(filename);
            }
        }

        @Override
        protected void onCancelled() {
            Log.d(TAG, "onCancelled() - APK update task cancelled");
            notifyProgress(0);
            cleanupDownloads(mVersion);
        }

        private void cleanupDownloads(String version) {
            File directory = new File(FileUtil.getFilesDir(FileType.APK), version);
            FileUtil.deleteFilesInDirectory(directory, true);
        }

        /**
         * Wipe any existing apk file, and create a new File for the new one, according to the
         * given version
         *
         * @param location
         * @param version
         * @return
         */
        private File createFile(String location, String version) {
            cleanupDownloads(version);

            String fileName = location.substring(location.lastIndexOf('/') + 1);
            File directory = new File(FileUtil.getFilesDir(FileType.APK), version);
            if (!directory.exists()) {
                directory.mkdir();
            }

            return new File(directory, fileName);
        }

        /**
         * Downloads the apk file and stores it on the file system
         * After the download, a new notification will be displayed, requesting
         * the user to 'click to installAppUpdate'
         */
        private boolean downloadApk(String location, String localPath) {
            Log.i(TAG, "App Update: Downloading new version " + mVersion + " from " + mUrl);

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
                    if (isCancelled()) {
                        return false; // No need to continue the download
                    }
                    out.write(b, 0, read);
                    bytesWritten += read;
                    publishProgress(bytesWritten, fileSize);
                }
                out.flush();

                final int status = conn.getResponseCode();

                if (status == HttpURLConnection.HTTP_OK) {
                    final String checksum = FileUtil.hexMd5(new File(localPath));
                    if (TextUtils.isEmpty(checksum)) {
                        throw new IOException("Downloaded file is not available");
                    }

                    if (mMd5Checksum == null) {
                        // If we don't have a checksum yet, try to get it form the ETag header
                        String etag = conn.getHeaderField("ETag");
                        mMd5Checksum =
                                etag != null ? etag.replaceAll("\"", "") : null;// Remove quotes
                    }
                    // Compare the MD5, if found. Otherwise, rely on the 200 status code
                    ok = mMd5Checksum == null || mMd5Checksum.equals(checksum);
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
                FileUtil.close(in);
                FileUtil.close(out);
            }

            return ok;
        }
    }

    private void onDownloadSuccess(String filename) {
        PlatformUtil.installAppUpdate(AppUpdateActivity.this, filename);
        finish();
    }

    private void onDownloadError() {
        Toast.makeText(this, R.string.apk_upgrade_error, Toast.LENGTH_SHORT).show();
        mInstallBtn.setText(R.string.retry);
        mInstallBtn.setEnabled(true);
    }

    private void updateDownloadProgress(int percentComplete) {
        mProgress.setProgress(percentComplete);
    }
}
