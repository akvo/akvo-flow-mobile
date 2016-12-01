/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.data.datasource.exception;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.akvo.flow.data.util.Constants;
import org.akvo.flow.data.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.inject.Inject;

import rx.Observable;

public class ExceptionFileDataSource {

    private static final String TAG = ExceptionFileDataSource.class.getSimpleName();

    private final Context context;

    @Inject
    public ExceptionFileDataSource(Context context) {
        this.context = context;
    }

    public Observable<Boolean> save(@Nullable Throwable throwable) {
        if (throwable == null) {
            return Observable.just(false);
        }
        recordException(throwable);
        return Observable.just(true);
    }

    /**
     * saves the exception to the filesystem. this can be used to save otherwise
     * handled exceptions so they can be reported to the server.
     *
     * @param exception
     */
    public void recordException(Throwable exception) {
        if (!ignoreException(exception)) {
            // save the error
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            exception.printStackTrace(printWriter);
            if (exception.getMessage() != null) {
                printWriter.print("\n" + exception.getMessage());
            }

            String filename = Constants.STACKTRACE_FILENAME + System.currentTimeMillis()
                    + Constants.STACKTRACE_SUFFIX;
            File file = new File(FileUtil.getFilesDir(FileUtil.FileType.STACKTRACE, context), filename);
            FileOutputStream out;
            try {
                out = new FileOutputStream(file);
                FileUtil.writeStringToFile(result.toString(), out);
            } catch (IOException e) {
                Log.e(TAG, "Couldn't save trace file", e);
            } finally {
                try {
                    result.close();
                } catch (IOException e) {
                    Log.w(TAG, "Can't close print writer object", e);
                }
            }
        }
    }

    /**
     * checks against a white-list of exceptions we ignore (mainly communication
     * errors that can arise if we're offline).
     *
     * @param exception
     * @return
     */
    private static boolean ignoreException(Throwable exception) {
        if (exception instanceof UnknownHostException) {
            return true;
        } else if (exception instanceof SocketException) {
            return true;
        } else if (exception instanceof IllegalStateException) {
            if (exception.getMessage() != null
                    && exception
                    .getMessage()
                    .toLowerCase()
                    .contains("sqlitedatabase created and never closed")) {
                return true;
            }
        }
        return false;
    }

}
