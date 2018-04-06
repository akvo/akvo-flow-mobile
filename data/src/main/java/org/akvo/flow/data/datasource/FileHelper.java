/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.datasource;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;

import timber.log.Timber;

class FileHelper {

    @Inject
    FileHelper() {
    }

    String copyFileToFolder(File originalFile, File destinationFolder) throws IOException {
        File file = new File(destinationFolder, originalFile.getName());
        return copyFile(originalFile, file);
    }

    String copyFile(File originalFile, File destinationFile) throws IOException {
        String destinationPath = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(originalFile);
            out = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            destinationPath = destinationFile.getAbsolutePath();
        } catch (FileNotFoundException e) {
            Timber.e(e);
        } finally {
            close(in);
            close(out);
        }
        return destinationPath;
    }

    void close(Closeable closeable) {
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
    @SuppressWarnings({ "unchecked", "ResultOfMethodCallIgnored" })
    void deleteFilesInDirectory(File folder, boolean deleteFolder) {
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
}
