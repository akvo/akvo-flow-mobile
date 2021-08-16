/*
 *  Copyright (C) 2014-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.activity;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.util.VersionHelper;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.util.files.FileUtil;
import org.akvo.flow.util.files.ApkFileBrowser;
import org.akvo.flow.util.files.FileBrowser;

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
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import timber.log.Timber;

public class AppUpdateActivity extends AppCompatActivity {
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_VERSION = "version";
    public static final String EXTRA_CHECKSUM = "md5Checksum";

    private static final int IO_BUFFER_SIZE = 8192;
    private static final int MAX_PROGRESS_IN_PERCENT = 100;

    @Inject
    ApkFileBrowser apkFileBrowser;

    @Inject
    Navigator navigator;

    private Button mInstallBtn;
    private ProgressBar mProgress;
    private UpdateAsyncTask mTask;

    private String mUrl;
    private String mVersion;
    private String mMd5Checksum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_update_activity);
        initializeInjector();
        mUrl = getIntent().getStringExtra(EXTRA_URL);
        mVersion = getIntent().getStringExtra(EXTRA_VERSION);
        mMd5Checksum = getIntent().getStringExtra(EXTRA_CHECKSUM);

        mInstallBtn = findViewById(R.id.install_btn);
        mProgress = findViewById(R.id.progress);
        mProgress.setMax(MAX_PROGRESS_IN_PERCENT);

        final String newApkFilePath = apkFileBrowser
                .verifyLatestApkFile(getApplicationContext(), mMd5Checksum);
        if (newApkFilePath != null) {
            displayInstallPrompt(newApkFilePath);
        } else {
            displayDownloadPrompt();
        }

        setCancelButton();
    }

    private void setCancelButton() {
        findViewById(R.id.cancel_btn).setOnClickListener(v -> cancel());
    }

    private void displayDownloadPrompt() {
        mInstallBtn.setOnClickListener(v -> {
            mInstallBtn.setEnabled(false);
            mTask = new UpdateAsyncTask(AppUpdateActivity.this, mUrl, mVersion,
                    mMd5Checksum);
            mTask.execute();
        });
    }

    private void displayInstallPrompt(final String newApkFilePath) {
        TextView updateTV = findViewById(R.id.update_text);
        updateTV.setText(R.string.clicktoinstall);
        mInstallBtn.setOnClickListener(
                v -> navigator.installAppUpdate(AppUpdateActivity.this, newApkFilePath));
    }

    private void initializeInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return {@link ApplicationComponent}
     */
    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getApplication()).getApplicationComponent();
    }

    private void cancel() {
        if (isRunning()) {
            mTask.cancel(true); // Stop the update process
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

        @Nullable
        private final String mUrl;
        private final String mVersion;
        private final WeakReference<AppUpdateActivity> activityWeakReference;
        private final ApkFileBrowser apkFileBrowser;
        private String mMd5Checksum;

        UpdateAsyncTask(AppUpdateActivity context, String mUrl, String mVersion,
                String mMd5Checksum) {
            this.mUrl = cleanUrl(mUrl);
            this.mVersion = mVersion;
            this.activityWeakReference = new WeakReference<>(context);
            this.mMd5Checksum = mMd5Checksum;
            this.apkFileBrowser = new ApkFileBrowser(new FileBrowser(), new VersionHelper());
        }

        @Nullable
        private String cleanUrl(String url) {
            if (TextUtils.isEmpty(url)) {
                return null;
            } else {
                String cleanUrl = url.toLowerCase();
                if (cleanUrl.startsWith("http:")) {
                    cleanUrl = cleanUrl.replaceFirst("http:", "https:");
                }
                return cleanUrl;
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            if (TextUtils.isEmpty(mUrl)) {
                return null;
            }
            cleanupDownloads();

            AppUpdateActivity appUpdateActivity = activityWeakReference.get();
            if (appUpdateActivity != null) {
                String apkFileName = mUrl.substring(mUrl.lastIndexOf('/') + 1);
                Context context = appUpdateActivity.getApplicationContext();
                String apkFullPath = apkFileBrowser.getFileName(context, mVersion, apkFileName);
                if (apkFullPath != null && downloadApk(mUrl, apkFullPath) && !isCancelled()) {
                    return apkFullPath;
                }
                cleanupDownloads();
            }
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
            if (percentComplete > MAX_PROGRESS_IN_PERCENT) {
                percentComplete = MAX_PROGRESS_IN_PERCENT;
            }
            Timber.d("onProgressUpdate() - APK update: " + percentComplete + "%");
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
            if (appUpdateActivity != null) {
                if (TextUtils.isEmpty(filename)) {
                    appUpdateActivity.onDownloadError();
                } else {
                    appUpdateActivity.onDownloadSuccess(filename);
                }
            }
        }

        @Override
        protected void onCancelled() {
            Timber.d("onCancelled() - APK update task cancelled");
            notifyProgress(0);
            cleanupDownloads();
        }

        /**
         * Clean up sd-card to ensure no corrupted file remains.
         */
        private void cleanupDownloads() {
            AppUpdateActivity appUpdateActivity = activityWeakReference.get();
            if (appUpdateActivity != null) {
                List<File> foldersToDelete = apkFileBrowser
                        .findAllPossibleFolders(appUpdateActivity.getApplicationContext());
                for (File folder : foldersToDelete) {
                    FileUtil.deleteFilesInDirectory(folder, true);
                }
            }
        }

        /**
         * Downloads the apk file and stores it on the file system
         * After the download, a new notification will be displayed, requesting
         * the user to 'click to installAppUpdate'
         */
        private boolean downloadApk(String location, String localPath) {
            Timber.i("App Update: Downloading new version " + mVersion + " from " + mUrl);

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
                Timber.d("APK size: " + fileSize);

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
                                etag != null ? etag.replaceAll("\"", "") : null;
                    }
                    // Compare the MD5, if found. Otherwise, rely on the 200 status code
                    ok = mMd5Checksum == null || mMd5Checksum.equals(checksum);
                } else {
                    Timber.e("Wrong status code: " + status);
                    ok = false;
                }
            } catch (IOException e) {
                Timber.e(e, e.getMessage());
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
        navigator.installAppUpdate(this, filename);
        finish();
    }

    private void onDownloadError() {
        Toast.makeText(this, R.string.apk_upgrade_error, Toast.LENGTH_SHORT).show();
        mInstallBtn.setText(R.string.action_retry);
        mInstallBtn.setEnabled(true);
    }

    private void updateDownloadProgress(int percentComplete) {
        mProgress.setProgress(percentComplete);
    }
}
