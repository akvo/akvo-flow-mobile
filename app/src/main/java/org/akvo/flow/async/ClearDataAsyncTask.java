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

package org.akvo.flow.async;

import java.io.File;
import java.lang.ref.WeakReference;

import android.content.Context;
import android.database.SQLException;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.PropertyUtil;

public class ClearDataAsyncTask extends AsyncTask<Boolean, Void, Boolean> {
    private static final String TAG = ClearDataAsyncTask.class.getSimpleName();

    /**
     * Use a WeakReference to avoid Context leaks
     */
    private WeakReference<Context> mWeakContext;

    private SurveyDbAdapter mDatabase;
    private boolean mUseInternalStorage;

    public ClearDataAsyncTask(Context context) {
        mWeakContext = new WeakReference<Context>(context);

        mUseInternalStorage = new PropertyUtil(context.getResources())
                .getBoolean(ConstantUtil.USE_INTERNAL_STORAGE);

        // Use the Application Context to be held by the Database
        // This will allow the current Activity to be GC if it's finished
        mDatabase = new SurveyDbAdapter(context.getApplicationContext());
    }

    @Override
    protected Boolean doInBackground(Boolean... params) {
        final boolean responsesOnly = params[0];

        boolean ok = true;
        try {
            // Internal database
            clearDatabase(responsesOnly);

            // External storage
            clearExternalStorage(responsesOnly);
        } catch (SQLException e) {
            Log.e(TAG, e.getMessage());
            ok = false;
        }

        return ok;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        final int messageId = result != null && result ? R.string.clear_data_success
                : R.string.clear_data_error;

        final Context context = mWeakContext.get();
        if (context != null) {
            Toast.makeText(context, messageId, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Permanently deletes data from the internal database.
     * 
     * @param responsesOnly Flag to specify a partial deletion (user generated
     *            data).
     */
    private void clearDatabase(boolean responsesOnly) throws SQLException {
        try {
            mDatabase.open();

            if (responsesOnly) {
                // Delete only user generated data
                mDatabase.clearCollectedData();
            } else {
                mDatabase.clearAllData();
            }
        } finally {
            if (mDatabase != null) {
                mDatabase.close();
            }
        }
    }

    /**
     * Permanently deletes data from the external storage
     * 
     * @param responsesOnly Flag to specify a partial deletion (user generated
     *            data).
     */
    private void clearExternalStorage(boolean responsesOnly) {
        if (!responsesOnly) {
            // Delete downloaded survey xml/zips
            FileUtil.deleteFilesInDirectory(new File(FileUtil.getStorageDirectory(
                    ConstantUtil.DATA_DIR, mUseInternalStorage)), false);

            // Delete stacktrace files (depending on SD card state,
            // they may be written to both internal and external storage)
            FileUtil.deleteFilesInDirectory(new File(FileUtil.getStorageDirectory(
                    ConstantUtil.STACKTRACE_DIR, false)), false);

            FileUtil.deleteFilesInDirectory(new File(FileUtil.getStorageDirectory(
                    ConstantUtil.STACKTRACE_DIR, true)), false);

            // Delete bootstraps
            FileUtil.deleteFilesInDirectory(new File(FileUtil.getStorageDirectory(
                    ConstantUtil.BOOTSTRAP_DIR, mUseInternalStorage)), false);
        }

        // Delete exported zip/image files
        FileUtil.deleteFilesInDirectory(new File(FileUtil.getStorageDirectory(
                ConstantUtil.SURVEYAL_DIR, mUseInternalStorage)), true);
    }
}
