/*
 *  Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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
import android.database.SQLException;
import android.os.AsyncTask;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.data.migration.FlowMigrationListener;
import org.akvo.flow.data.migration.languages.MigrationLanguageMapper;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.database.SurveyDbAdapter;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FileUtil.FileType;

import java.lang.ref.WeakReference;

import timber.log.Timber;

public class ClearDataAsyncTask extends AsyncTask<Boolean, Void, Boolean> {

    /**
     * Use a WeakReference to avoid Context leaks
     */
    private WeakReference<Context> mWeakContext;

    private SurveyDbAdapter mDatabase;

    public ClearDataAsyncTask(Context context) {
        mWeakContext = new WeakReference<>(context);
        // Use the Application Context to be held by the Database
        // This will allow the current Activity to be GC if it's finished
        Context applicationContext = context.getApplicationContext();
        mDatabase = new SurveyDbAdapter(applicationContext,
                new FlowMigrationListener(new Prefs(applicationContext),
                        new MigrationLanguageMapper(applicationContext)));
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
            Timber.e(e.getMessage());
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
            FileUtil.deleteFilesInDirectory(FileUtil.getFilesDir(FileType.FORMS), false);
            // Delete stacktrace files
            FileUtil.deleteFilesInDirectory(FileUtil.getFilesDir(FileType.STACKTRACE), false);
            // Delete bootstraps
            FileUtil.deleteFilesInDirectory(FileUtil.getFilesDir(FileType.INBOX), false);
        }

        // Delete exported zip/image files
        FileUtil.deleteFilesInDirectory(FileUtil.getFilesDir(FileType.DATA), true);
        FileUtil.deleteFilesInDirectory(FileUtil.getFilesDir(FileType.MEDIA), true);
    }
}
