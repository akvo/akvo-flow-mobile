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

package org.akvo.flow.data.datasource.files;

import androidx.annotation.Nullable;
import android.text.TextUtils;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Predicate;
import okhttp3.ResponseBody;
import org.akvo.flow.data.util.Constants;
import org.akvo.flow.data.util.ExternalStorageHelper;
import org.akvo.flow.data.util.FileHelper;
import org.akvo.flow.data.util.FlowFileBrowser;
import timber.log.Timber;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class FileDataSource {

    private final FileHelper fileHelper;
    private final FlowFileBrowser flowFileBrowser;
    private final ExternalStorageHelper externalStorageHelper;

    @Inject
    public FileDataSource(FileHelper fileHelper, FlowFileBrowser flowFileBrowser,
            ExternalStorageHelper externalStorageHelper) {
        this.fileHelper = fileHelper;
        this.flowFileBrowser = flowFileBrowser;
        this.externalStorageHelper = externalStorageHelper;
    }

    public Completable moveZipFiles() {
        moveFiles(FlowFileBrowser.DIR_DATA);
        return Completable.complete();
    }

    public Completable moveMediaFiles() {
        moveFiles(FlowFileBrowser.DIR_MEDIA + FlowFileBrowser.CADDISFLY_OLD_FOLDER);
        moveFiles(FlowFileBrowser.DIR_MEDIA);
        return Completable.complete();
    }

    public Observable<Boolean> copyFile(String originFilePath, String destinationFilePath) {
        File originalFile = new File(originFilePath);
        File destinationFile = new File(destinationFilePath);
        String copiedFilePath = fileHelper.copyFile(originalFile, destinationFile);
        if (copiedFilePath == null) {
            return Observable.error(new Exception("Error copying video file"));
        }
        return Observable.just(true);
    }

    private void moveFiles(String folderName) {
        File publicFolder = flowFileBrowser.getPublicFolder(folderName);
        List<String> movedFiles;
        if (publicFolder != null && publicFolder.exists()) {
            File[] files = publicFolder.listFiles();
            movedFiles = copyFiles(files, folderName);
            if (files.length == movedFiles.size()) {
                //noinspection ResultOfMethodCallIgnored
                publicFolder.delete();
            }
        }
    }

    private List<String> copyFiles(@Nullable File[] files, String folderName) {
        List<String> movedFiles = new ArrayList<>();
        if (files != null) {
            File folder = getPrivateFolder(folderName);
            for (File f : files) {
                String destinationPath;
                if (f.isDirectory() && FlowFileBrowser.DIR_DATA.equals(folderName)){
                    destinationPath = f.getAbsolutePath();
                    fileHelper.deleteFilesInDirectory(f, false);
                } else {
                    destinationPath = fileHelper.copyFileToFolder(f, folder);
                }
                if (!TextUtils.isEmpty(destinationPath)) {
                    movedFiles.add(destinationPath);
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
        }
        return movedFiles;
    }

    private File getPrivateFolder(String folderName) {
        File folder = flowFileBrowser.getInternalFolder(folderName);
        if (!folder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
        }
        return folder;
    }

    public Observable<Boolean> publishFiles(List<String> fileNames) {
        boolean dataCopied = copyPrivateFileToAppExternalFolder(FlowFileBrowser.DIR_DATA,
                FlowFileBrowser.DIR_PUBLISHED_DATA, fileNames);
        boolean mediaCopied = copyPrivateFileToAppExternalFolder(FlowFileBrowser.DIR_MEDIA,
                FlowFileBrowser.DIR_PUBLISHED_MEDIA, fileNames);
        return Observable.just(dataCopied || mediaCopied);
    }

    private boolean copyPrivateFileToAppExternalFolder(String privateFolderName,
            String publicFolderName, List<String> fileNames) {
        boolean filesCopied = false;
        File destinationDataFolder = flowFileBrowser.getAppExternalFolder(publicFolderName);
        if (destinationDataFolder != null && !destinationDataFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            destinationDataFolder.mkdirs();
        }
        File dataFolder = getPrivateFolder(privateFolderName);
        if (dataFolder.exists()) {
            File[] files = dataFolder.listFiles();
            if (files != null) {
                for (File fileToMove : files) {
                    if (fileNames.contains(fileToMove.getName())) {
                        filesCopied = true;
                        fileHelper.copyFileToFolder(fileToMove, destinationDataFolder);
                    }
                }
            }
        }
        return filesCopied;
    }

    public Observable<Boolean> removePublishedFiles() {
        deleteFilesInAppExternalFolder(FlowFileBrowser.DIR_PUBLISHED_DATA);
        deleteFilesInAppExternalFolder(FlowFileBrowser.DIR_PUBLISHED_MEDIA);
        return Observable.just(true);
    }

    private void deleteFilesInAppExternalFolder(String folderName) {
        File dataFolder = flowFileBrowser.getAppExternalFolder(folderName);
        File[] files = dataFolder == null ? null : dataFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    public Observable<Boolean> deleteAllUserFiles() {
        List<File> foldersToDelete = flowFileBrowser
                .findAllPossibleFolders(FlowFileBrowser.DIR_FORMS);
        foldersToDelete.addAll(flowFileBrowser.findAllPossibleFolders(FlowFileBrowser.DIR_RES));
        File inboxFolder = flowFileBrowser.getPublicFolder(FlowFileBrowser.DIR_INBOX);
        if (inboxFolder != null && inboxFolder.exists()) {
            foldersToDelete.add(inboxFolder);
        }
        for (File file : foldersToDelete) {
            fileHelper.deleteFilesInDirectory(file, true);
        }
        deleteResponsesFiles();
        return Observable.just(true);
    }

    public Observable<Boolean> deleteResponsesFiles() {
        List<File> foldersToDelete = flowFileBrowser
                .findAllPossibleFolders(FlowFileBrowser.DIR_DATA);
        foldersToDelete.addAll(flowFileBrowser.findAllPossibleFolders(FlowFileBrowser.DIR_MEDIA));
        foldersToDelete.addAll(flowFileBrowser.findAllPossibleFolders(FlowFileBrowser.DIR_TMP));
        File exportedFolder = flowFileBrowser.getAppExternalFolder(FlowFileBrowser.DIR_PUBLISHED);
        if (exportedFolder != null && exportedFolder.exists()) {
            foldersToDelete.add(exportedFolder);
        }
        for (File file : foldersToDelete) {
            fileHelper.deleteFilesInDirectory(file, true);
        }
        return Observable.just(true);
    }

    public Observable<Long> getAvailableStorage() {
        return Observable.just(externalStorageHelper.getExternalStorageAvailableSpaceInMb());
    }

    public Maybe<File> getIncorrectZipFile(String uuid) {
        return getZipFile(uuid)
                .filter(new Predicate<File>() {
                    @Override
                    public boolean test(File file) {
                        return !fileHelper.validFile(file);
                    }
                });
    }

    public Completable writeDataToZipFile(String zipFileName, String formInstanceData) {
        File folder = flowFileBrowser.getExistingAppInternalFolder(FlowFileBrowser.DIR_DATA);
        try {
            //delete any previous zip file
            fileHelper.deleteFile(folder, zipFileName);
            fileHelper.writeZipFile(folder, zipFileName, formInstanceData);
            return Completable.complete();
        } catch (IOException e) {
            return Completable.error(e);
        }
    }

    public Observable<Boolean> extractRemoteArchive(ResponseBody responseBody, String folderName) {
        File formFolder = flowFileBrowser.getExistingAppInternalFolder(folderName);
        fileHelper.extractOnlineArchive(responseBody, formFolder);
        return Observable.just(true);
    }

    public Observable<InputStream> getFormFile(String id) {
        File formFolder = flowFileBrowser.getExistingAppInternalFolder(FlowFileBrowser.DIR_FORMS);
        InputStream input;
        try {
            input = new FileInputStream(new File(formFolder, id + FlowFileBrowser.XML_SUFFIX));
        } catch (FileNotFoundException e) {
            Timber.e(e);
            return Observable.error(e);
        }
        return Observable.just(input);
    }

    private Single<File> getZipFile(String uuid) {
        String name = uuid + Constants.ARCHIVE_SUFFIX;
        File file = new File(flowFileBrowser.getInternalFolder(FlowFileBrowser.DIR_DATA), name);
        return Single.just(file);
    }
}
