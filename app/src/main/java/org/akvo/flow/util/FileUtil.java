/*
 * Copyright (C) 2021 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.util;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import timber.log.Timber;

/**
 * utility for manipulating files
 *
 * @author Christopher Fagiani
 */
public class FileUtil {

    // Directories stored in the External Storage root (i.e. /sdcard/akvoflow/data)
    private static final String DIR_INBOX = "akvoflow/inbox"; // Bootstrap files

    private static final int BUFFER_SIZE = 2048;

    public enum FileType {
        INBOX
    }

    /**
     * Get the appropriate files directory for the given FileType. The directory may or may
     * not be in the app-specific External Storage. The caller cannot assume anything about
     * the location.
     *
     * @param type FileType to determine the type of resource attempting to use.
     * @return File representing the root directory for the given FileType.
     */
    public static File getFilesDir(FileType type) {
        String path = null;
        switch (type) {
            case INBOX:
                path = getExternalStoragePath() + File.separator + DIR_INBOX;
                break;
        }
        File dir = new File(path);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Returns app specific folder on the external storage
     * External Storage may not be available
     */
    @Nullable
    public static String getAppExternalStoragePath(Context context) {
        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir != null) {
            return externalFilesDir.getAbsolutePath();
        }
        return null;
    }

    @NonNull
    public static String getExternalStoragePath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    /**
     * reads data from an InputStream into a string.
     */
    public static String readText(InputStream is) throws IOException {
        ByteArrayOutputStream out = null;
        try {
            out = read(is);
            return out.toString();
        } finally {
            close(out);
        }
    }

    /**
     * reads data from an InputStream into a string.
     */
    public static String readTextWithoutClosing(InputStream is) throws IOException {
        ByteArrayOutputStream out = read(is);
        return out.toString();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int size;
        while ((size = in.read(buffer, 0, buffer.length)) != -1) {
            out.write(buffer, 0, size);
        }
    }

    /**
     * reads the contents of an InputStream into a ByteArrayOutputStream.
     */
    private static ByteArrayOutputStream read(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(is, out);
        return out;
    }

    /**
     * deletes all files in the directory (recursively) AND then deletes the
     * directory itself if the "deleteFlag" is true
     */
    @SuppressWarnings({ "unchecked", "ResultOfMethodCallIgnored" })
    public static void deleteFilesInDirectory(File dir, boolean deleteDir) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete();
                    } else {
                        // recursively delete
                        deleteFilesInDirectory(file, true);
                    }
                }
            }
            // now delete the directory itself
            if (deleteDir) {
                dir.delete();
            }
        }
    }

    /**
     * Compute MD5 checksum of the given file
     */
    private static byte[] getMD5Checksum(File file) {
        InputStream in = null;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            in = new BufferedInputStream(new FileInputStream(file));

            byte[] buffer = new byte[BUFFER_SIZE];

            int read;
            while ((read = in.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }

            return md.digest();
        } catch (NoSuchAlgorithmException | IOException e) {
            Timber.e(e.getMessage());
        } finally {
            close(in);
        }

        return null;
    }

    private static String hexMd5(byte[] rawHash) {
        if (rawHash != null) {
            StringBuilder builder = new StringBuilder();
            for (byte b : rawHash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        }
        return null;
    }

    public static String hexMd5(File file) {
        return hexMd5(getMD5Checksum(file));
    }

    /**
     * Helper function to close a Closeable instance
     */
    public static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            Timber.e(e.getMessage());
        }
    }

}
