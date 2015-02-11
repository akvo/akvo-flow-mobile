/*
 *  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.async;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.akvo.flow.api.S3Api;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.StatusUtil;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class MediaSyncTask extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = MediaSyncTask.class.getSimpleName();

    public interface DownloadListener {
        public void onResourceDownload(boolean done);
    }

    /**
     * Use a WeakReferences to avoid memory leaks
     */
    private WeakReference<DownloadListener> mListener;
    private Context mContext;
    private File mFile;

    public MediaSyncTask(Context context, File file, DownloadListener listener) {
        mContext = context.getApplicationContext();
        mListener = new WeakReference<>(listener);
        mFile = file;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        // Download resource and return success status
        // Upon error, launch notification?
        if (!StatusUtil.hasDataConnection(mContext)) {
            Log.d(TAG, "No internet connection. Can't perform the requested operation");
            return false;
        }

        try {
            S3Api s3 = new S3Api(mContext);
            s3.get(ConstantUtil.S3_IMAGE_DIR + mFile.getName(), mFile);
            return true;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            // TODO: Clean up potentially corrupted files
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
