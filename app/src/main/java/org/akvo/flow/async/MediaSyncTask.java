/*
 *  Copyright (C) 2015-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.async;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.akvo.flow.api.S3Api;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.util.ConnectivityStateManager;
import org.akvo.flow.util.ConstantUtil;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * Download media files (images, videos) from synced forms.
 */
public class MediaSyncTask extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = MediaSyncTask.class.getSimpleName();

    public interface DownloadListener {
        void onResourceDownload(boolean done);
    }

    private final WeakReference<DownloadListener> mListener;
    private final Context mContext;
    private final File mFile;
    private final ConnectivityStateManager connectivityStateManager;
    private final Prefs prefs;

    /**
     * Download a media file. Provided file must be already updated to use the local filesystem path.
     */
    public MediaSyncTask(Context context, File file, DownloadListener listener) {
        this.mContext = context.getApplicationContext();
        this.mListener = new WeakReference<>(listener);
        this.mFile = file;
        this.connectivityStateManager = new ConnectivityStateManager(mContext);
        this.prefs = new Prefs(mContext);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        if (!connectivityStateManager.isConnectionAvailable(
                prefs.getBoolean(Prefs.KEY_CELL_UPLOAD, Prefs.DEFAULT_VALUE_CELL_UPLOAD))) {
            Log.d(TAG, "No internet connection available. Can't perform the requested operation");
            return false;
        }

        try {
            // Download resource and return success status
            S3Api s3 = new S3Api(mContext);
            s3.get(ConstantUtil.S3_IMAGE_DIR + mFile.getName(), mFile);
            return true;
        } catch (IOException e) {
            Timber.e(e.getMessage());
            if (mFile.exists()) {
                mFile.delete();
            }
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        final DownloadListener listener = mListener.get();
        if (listener != null) {
            listener.onResourceDownload(Boolean.TRUE.equals(result));
        }
    }

}
