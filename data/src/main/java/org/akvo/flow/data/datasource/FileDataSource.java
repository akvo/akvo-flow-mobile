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

import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.akvo.flow.data.entity.MovedFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;

@Singleton
public class FileDataSource {

    private final FileHelper fileHelper;
    private final FolderBrowser folderBrowser;

    @Inject
    public FileDataSource(FileHelper fileHelper, FolderBrowser folderBrowser) {
        this.fileHelper = fileHelper;
        this.folderBrowser = folderBrowser;
    }

    public Observable<List<MovedFile>> moveZipFiles() {
        return moveFiles(FolderBrowser.DIR_DATA);
    }

    public Observable<List<MovedFile>> moveMediaFiles() {
        return moveFiles(FolderBrowser.DIR_MEDIA);
    }

    public Observable<Boolean> copyMediaFile(String originFilePath, String destinationFilePath) {
        File originalFile = new File(originFilePath);
        if (fileHelper.copyFile(originalFile, new File(destinationFilePath)) == null) {
            return Observable.error(new Exception("Error copying video file"));
        } else {
            //noinspection ResultOfMethodCallIgnored
            originalFile.delete();
        }
        return Observable.just(true);
    }

    private Observable<List<MovedFile>> moveFiles(String folderName) {
        File publicFolder = folderBrowser.getPublicFolder(folderName);
        List<MovedFile> movedFiles = new ArrayList<>();
        if (publicFolder.exists()) {
            File[] files = publicFolder.listFiles();
            movedFiles = copyFiles(files, folderName);
            if (files.length == movedFiles.size()) {
                //noinspection ResultOfMethodCallIgnored
                publicFolder.delete();
            }
        }
        return Observable.just(movedFiles);
    }

    private List<MovedFile> copyFiles(@Nullable File[] files, String folderName) {
        List<MovedFile> movedFiles = new ArrayList<>();
        if (files != null) {
            File folder = getPrivateFolder(folderName);
            for (File f : files) {
                String destinationPath = fileHelper.copyFileToFolder(f, folder);
                if (!TextUtils.isEmpty(destinationPath)) {
                    movedFiles.add(new MovedFile(f.getPath(), destinationPath));
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
        }
        return movedFiles;
    }

    private File getPrivateFolder(String folderName) {
        File folder = folderBrowser.getInternalFolder(folderName);
        if (!folder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
        }
        return folder;
    }

    public Observable<Boolean> copyPrivateData() {
        //TODO: error handling will be added in separate issue
        copyPrivateFileToPublic(FolderBrowser.DIR_DATA, FolderBrowser.DIR_PUBLISHED_DATA);
        copyPrivateFileToPublic(FolderBrowser.DIR_MEDIA, FolderBrowser.DIR_PUBLISHED_MEDIA);
        return Observable.just(true);
    }

    private void copyPrivateFileToPublic(String privateFolderName, String publicFolderName) {
        File destinationDataFolder = folderBrowser.getPublicFolder(publicFolderName);
        if (!destinationDataFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            destinationDataFolder.mkdirs();
        }
        File dataFolder = getPrivateFolder(privateFolderName);
        if (dataFolder.exists()) {
            File[] files = dataFolder.listFiles();
            if (files != null) {
                for (File f : files) {
                    fileHelper.copyFileToFolder(f, destinationDataFolder);
                }
            }
        }
    }

    public Observable<Boolean> removePublicFiles() {
        deleteFilesInPublicFolder(FolderBrowser.DIR_PUBLISHED_DATA);
        deleteFilesInPublicFolder(FolderBrowser.DIR_PUBLISHED_MEDIA);
        return Observable.just(true);
    }

    private void deleteFilesInPublicFolder(String folderName) {
        File dataFolder = folderBrowser.getPublicFolder(folderName);
        File[] files = dataFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    public void deleteAllUserFiles() {
        List<File> foldersToDelete = folderBrowser.findAllPossibleFolders(FolderBrowser.DIR_FORMS);
        foldersToDelete.addAll(folderBrowser.findAllPossibleFolders(FolderBrowser.DIR_RES));
        File inboxFolder = folderBrowser.getPublicFolder(FolderBrowser.DIR_INBOX);
        if (inboxFolder.exists()) {
            foldersToDelete.add(inboxFolder);
        }
        for (File file : foldersToDelete) {
            fileHelper.deleteFilesInDirectory(file, true);
        }
        deleteResponsesFiles();
    }

    public void deleteResponsesFiles() {
        List<File> foldersToDelete = folderBrowser.findAllPossibleFolders(FolderBrowser.DIR_DATA);
        foldersToDelete.addAll(folderBrowser.findAllPossibleFolders(FolderBrowser.DIR_MEDIA));
        foldersToDelete.addAll(folderBrowser.findAllPossibleFolders(FolderBrowser.DIR_TMP));
        File exportedFolder = folderBrowser.getPublicFolder(FolderBrowser.DIR_PUBLISHED);
        if (exportedFolder.exists()) {
            foldersToDelete.add(exportedFolder);
        }
        for (File file : foldersToDelete) {
            fileHelper.deleteFilesInDirectory(file, true);
        }
    }
}
