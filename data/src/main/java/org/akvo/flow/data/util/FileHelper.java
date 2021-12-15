/*
 * Copyright (C) 2018-2019 Stichting Akvo (Akvo Foundation)
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
 *
 */

package org.akvo.flow.data.util;

import android.text.TextUtils;
import android.util.Base64;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class FileHelper {

    private static final int BUFFER_SIZE = 2048;

    @Inject
    public FileHelper() {
    }

    /**
     * Compute MD5 checksum of the given file
     */
    @Nullable
    private byte[] getMD5Checksum(File file) {
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

    @NonNull
    public String getMd5Base64(File file) {
        byte[] md5Checksum = getMD5Checksum(file);
        if (md5Checksum != null) {
            return Base64.encodeToString(md5Checksum, Base64.NO_WRAP);
        } else {
            return "";
        }
    }

    @NonNull
    public String hexMd5(File file) {
        byte[] rawHash = getMD5Checksum(file);
        if (rawHash != null) {
            StringBuilder builder = new StringBuilder();
            for (byte b : rawHash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        }
        return "";
    }

    public String copyFileToFolder(File originalFile, File destinationFolder) {
        File file = new File(destinationFolder, originalFile.getName());
        return copyFile(originalFile, file);
    }

    /**
     * Copies a file from originalFile to destinationFile
     *
     * @return the destination file path if copy succeeded, null otherwise
     */
    public String copyFile(File originalFile, File destinationFile) {
        String destinationPath = null;
        try {
            destinationPath = saveStreamToFile(new FileInputStream(originalFile), destinationFile);
        } catch (FileNotFoundException e) {
            Timber.e(e);
        }
        return destinationPath;
    }

    @Nullable
    public String saveByteArrayToFile(byte[] bytes, File destinationFile) {
        if (bytes != null) {
            try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
                fos.write(bytes);
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        return destinationFile.getAbsolutePath();
    }

    @Nullable
    public String saveStreamToFile(InputStream inputStream, File destinationFile) {
        copyStream(inputStream, destinationFile);
        close(inputStream);
        return destinationFile.getAbsolutePath();
    }

    public void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                //Ignored
            }
        }
    }

    /**
     * deletes all files in the directory (recursively) AND then deletes the
     * directory itself if the "deleteFlag" is true
     */
    @SuppressWarnings({ "ResultOfMethodCallIgnored" })
    public void deleteFilesInDirectory(File folder, boolean deleteFolder) {
        if (folder != null && folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
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
            if (deleteFolder) {
                folder.delete();
            }
        }
    }

    @Nullable
    public String getFilenameFromPath(@Nullable String filePath) {
        String filename = null;
        if (!TextUtils.isEmpty(filePath) && filePath.contains(File.separator)
                && filePath.contains(".")) {
            filename = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
        }
        return filename;
    }

    public void deleteFile(File zipFolder, String zipFileName) {
        File zipFile = new File(zipFolder, zipFileName);
        if (zipFile.exists()) {
            zipFile.delete();
        }
    }

    public void writeZipFile(File zipFolder, String zipFileName, String formInstanceData)
            throws IOException {
        File zipFile = new File(zipFolder, zipFileName);
        Timber.d("Writing zip to file %s", zipFile.getName());
        FileOutputStream fout = new FileOutputStream(zipFile);
        CheckedOutputStream checkedOutStream = new CheckedOutputStream(fout, new Adler32());
        ZipOutputStream zos = new ZipOutputStream(checkedOutStream);
        zos.putNextEntry(new ZipEntry(Constants.SURVEY_DATA_FILE_JSON));
        byte[] allBytes = formInstanceData.getBytes(Constants.UTF_8_CHARSET);
        zos.write(allBytes, 0, allBytes.length);
        zos.closeEntry();
        zos.close();
        fout.close();
    }

    public void extractOnlineArchive(ResponseBody responseBody, File targetFolder) {
        InputStream inputStream = responseBody.byteStream();
        extractZipContent(inputStream, targetFolder);
        close(inputStream);
    }

    public void saveRemoteFile(ResponseBody responseBody, File filePath) {
        InputStream inputStream = responseBody.byteStream();
        saveStreamToFile(inputStream, filePath);
    }

    public boolean validFile(File file) {
        return file.exists() && validZipFile(file);
    }

    private boolean validZipFile(File file) {
        try {
            return new ZipFile(file).size() > 0;
        } catch (IOException e) {
            Timber.e(e);
            return false;
        }
    }

    private void copyStream(InputStream inputStream, File destinationFile) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } catch (IOException e) {
            Timber.e(e);
        } finally {
            close(out);
        }
    }

    private void extractZipContent(InputStream input, File destinationFolder) {
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(input);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null && !entry.isDirectory()) {
                File f = new File(destinationFolder, entry.getName());
                copyStream(zis, f);
                zis.closeEntry();
            }
        } catch (IOException e) {
            Timber.e(e);
        } finally {
            close(zis);
        }
    }
}
