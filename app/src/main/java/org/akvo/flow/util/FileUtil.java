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

package org.akvo.flow.util;

import android.content.Context;
import android.database.Cursor;
import android.media.ExifInterface;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.app.FlowApp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import timber.log.Timber;

/**
 * utility for manipulating files
 *
 * @author Christopher Fagiani
 */
public class FileUtil {

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
            case DATA:
                path = getFilesStorageDir(false) + File.separator + DIR_DATA;
                break;
            case MEDIA:
                path = getFilesStorageDir(false) + File.separator + DIR_MEDIA;
                break;
            case INBOX:
                path = getFilesStorageDir(false) + File.separator + DIR_INBOX;
                break;
            case FORMS:
                path = getFilesStorageDir(true) + File.separator + DIR_FORMS;
                break;
            case STACKTRACE:
                path = getFilesStorageDir(true) + File.separator + DIR_STACKTRACE;
                break;
            case TMP:
                path = getFilesStorageDir(true) + File.separator + DIR_TMP;
                break;
            case APK:
                path = getFilesStorageDir(true) + File.separator + DIR_APK;
                break;
            case RES:
                path = getFilesStorageDir(true) + File.separator + DIR_RES;
                break;
        }
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Get the root of the files storage directory, depending on the resource being app internal
     * (not concerning the user) or not (users might need to pull the resource from the storage).
     *
     * @param internal true for app specific resources, false otherwise
     * @return The root directory for this kind of resources
     */
    private static String getFilesStorageDir(boolean internal) {
        if (internal) {
            return FlowApp.getApp().getExternalFilesDir(null).getAbsolutePath();
        }
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
     * extract zip file contents into destination folder.
     */
    public static void extract(ZipInputStream zis, File dst) throws IOException {
        ZipEntry entry;
        try {
            while ((entry = zis.getNextEntry()) != null && !entry.isDirectory()) {
                File f = new File(dst, entry.getName());
                FileOutputStream fout = new FileOutputStream(f);
                FileUtil.copy(zis, fout);
                fout.close();
                zis.closeEntry();
            }
        } finally {
            close(zis);
        }
    }

    /**
     * deletes all files in the directory (recursively) AND then deletes the
     * directory itself if the "deleteFlag" is true
     */
    public static void deleteFilesInDirectory(File dir, boolean deleteDir) {
        if (dir != null && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isFile()) {
                        files[i].delete();
                    } else {
                        // recursively delete
                        deleteFilesInDirectory(files[i], true);
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
     * Compute MD5 checksum of the given path's file
     */
    private static byte[] getMD5Checksum(String path) {
        return getMD5Checksum(new File(path));
    }

    /**
     * Compute MD5 checksum of the given file
     */
    public static byte[] getMD5Checksum(File file) {
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

    public static String hexMd5(byte[] rawHash) {
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
     * Compare to images to determine if their content is the same. To state
     * that the two of them are the same, the datetime contained in their exif
     * metadata will be compared. If the exif does not contain a datetime, the
     * MD5 checksum of the images will be compared.
     *
     * @param image1 Absolute path to the first image
     * @param image2 Absolute path to the second image
     * @return true if their datetime is the same, false otherwise
     */
    private static boolean compareImages(String image1, String image2) {
        boolean equals = false;
        try {
            ExifInterface exif1 = new ExifInterface(image1);
            ExifInterface exif2 = new ExifInterface(image2);

            final String datetime1 = exif1.getAttribute(ExifInterface.TAG_DATETIME);
            final String datetime2 = exif2.getAttribute(ExifInterface.TAG_DATETIME);

            if (!TextUtils.isEmpty(datetime1) && !TextUtils.isEmpty(datetime1)) {
                equals = datetime1.equals(datetime2);
            } else {
                Timber.d("Datetime is null or empty. The MD5 checksum will be compared");
                equals = compareFilesChecksum(image1, image2);
            }
        } catch (IOException e) {
            Timber.e(e.getMessage());
        }

        return equals;
    }

    /**
     * Compare to files to determine if their content is the same. To state that
     * the two of them are the same, the MD5 checksum will be compared. Note
     * that if any of the files does not exist, or if its checksum cannot be
     * computed, false will be returned.
     *
     * @param path1 Absolute path to the first file
     * @param path2 Absolute path to the second file
     * @return true if their MD5 checksum is the same, false otherwise.
     */
    private static boolean compareFilesChecksum(String path1, String path2) {
        final byte[] checksum1 = getMD5Checksum(path1);
        final byte[] checksum2 = getMD5Checksum(path2);

        return Arrays.equals(checksum1, checksum2);
    }

    /**
     * Some manufacturers will duplicate the image saving a copy in the DCIM
     * folder. This method will try to spot those situations and remove the
     * duplicated image.
     *
     * @param context  Context
     * @param filepath The absolute path to the original image
     */
    public static void cleanDCIM(Context context, String filepath) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] {
                        MediaStore.Images.ImageColumns.DATA,
                        MediaStore.Images.ImageColumns.DATE_TAKEN
                },
                null,
                null,
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC"
        );

        if (cursor.moveToFirst()) {
            final String lastImagePath = cursor.getString(cursor
                    .getColumnIndex(MediaStore.Images.ImageColumns.DATA));

            if ((!filepath.equals(lastImagePath))
                    && (FileUtil.compareImages(filepath, lastImagePath))) {
                final int result = context.getContentResolver().delete(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Images.ImageColumns.DATA + " = ?",
                        new String[] {
                                lastImagePath
                        });

                if (result == 1) {
                    Timber.i("Duplicated file successfully removed: " + lastImagePath);
                } else {
                    Timber.e("Error removing duplicated image:" + lastImagePath);
                }
            }
        }

        cursor.close();
    }

    /**
     * Check for the latest downloaded version. If old versions are found, delete them.
     * The APK corresponding to the installed version will also be deleted, if found,
     * in order to perform a cleanup after an upgrade.
     *
     * @return the path and version of a newer APK, if found, null otherwise
     */
    public static String checkDownloadedVersions() {

        String maxVersion = BuildConfig.VERSION_NAME;// Keep track of newest version available
        String apkPath = null;

        File appsLocation = getFilesDir(FileType.APK);
        File[] versions = appsLocation.listFiles();
        if (versions != null) {
            for (File version : versions) {
                File[] apks = version.listFiles();
                if (apks == null) {
                    continue;// Nothing to see here
                }

                String versionName = version.getName();
                if (!PlatformUtil.isNewerVersion(maxVersion, versionName)) {
                    // Delete old versions
                    for (File apk : apks) {
                        apk.delete();
                    }
                    version.delete();
                } else if (apks.length > 0) {
                    maxVersion = versionName;
                    apkPath = apks[0].getAbsolutePath();// There should only be 1
                }
            }
        }

        if (apkPath != null && maxVersion != null) {
            return apkPath;
        }
        return null;
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
