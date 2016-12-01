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

package org.akvo.flow.data.util;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * utility for manipulating files
 * 
 * @author Christopher Fagiani
 */
public class FileUtil {
    private static final String TAG = FileUtil.class.getSimpleName();

    // Directories stored in the External Storage root (i.e. /sdcard/akvoflow/data)
    private static final String DIR_DATA = "akvoflow/data/files"; // form responses zip files
    private static final String DIR_MEDIA = "akvoflow/data/media"; // form responses media files
    private static final String DIR_INBOX = "akvoflow/inbox"; // Bootstrap files

    // Directories stored in the app specific External Storage (i.e. /sdcard/Android/data/org.akvo.flow/files/forms)
    private static final String DIR_FORMS = "forms"; // Form definitions
    private static final String DIR_STACKTRACE = "stacktrace"; // Crash reports
    private static final String DIR_TMP = "tmp"; // Temporary files
    private static final String DIR_APK = "apk"; // App upgrades
    private static final String DIR_RES = "res"; // Survey resources (i.e. cascading DB)

    private static final int BUFFER_SIZE = 2048;

    public enum FileType {DATA, MEDIA, INBOX, FORMS, STACKTRACE, TMP, APK, RES}

    /**
     * Get the root of the files storage directory, depending on the resource being app internal
     * (not concerning the user) or not (users might need to pull the resource from the storage).
     * @param internal true for app specific resources, false otherwise
     * @param context
     * @return The root directory for this kind of resources
     */
    private static String getFilesStorageDir(boolean internal, Context context) {
        if (internal) {
            return context.getExternalFilesDir(null).getAbsolutePath();
        }
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    /**
     * writes the contents string to the file indicated by filePath
     */
    public static void writeStringToFile(String contents,
            FileOutputStream filePath) throws IOException {
        if (contents != null) {
            BufferedOutputStream bw = new BufferedOutputStream(filePath);
            bw.write(contents.getBytes("UTF-8"));
            bw.flush();
            bw.close();
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int size;
        while ((size = in.read(buffer, 0, buffer.length)) != -1) {
            out.write(buffer, 0, size);
        }
    }

    /**
     * Get the appropriate files directory for the given FileType. The directory may or may
     * not be in the app-specific External Storage. The caller cannot assume anything about
     * the location.
     * @param type FileType to determine the type of resource attempting to use.
     * @return File representing the root directory for the given FileType.
     */
    public static File getFilesDir(FileType type, Context context) {
        String path = null;
        switch (type) {
            case DATA:
                path = getFilesStorageDir(false, context) + File.separator + DIR_DATA;
                break;
            case MEDIA:
                path = getFilesStorageDir(false, context) + File.separator + DIR_MEDIA;
                break;
            case INBOX:
                path = getFilesStorageDir(false, context) + File.separator + DIR_INBOX;
                break;
            case FORMS:
                path = getFilesStorageDir(true, context) + File.separator + DIR_FORMS;
                break;
            case STACKTRACE:
                path = getFilesStorageDir(true, context) + File.separator + DIR_STACKTRACE;
                break;
            case TMP:
                path = getFilesStorageDir(true, context) + File.separator + DIR_TMP;
                break;
            case APK:
                path = getFilesStorageDir(true, context) + File.separator + DIR_APK;
                break;
            case RES:
                path = getFilesStorageDir(true, context) + File.separator + DIR_RES;
                break;
        }
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

}
