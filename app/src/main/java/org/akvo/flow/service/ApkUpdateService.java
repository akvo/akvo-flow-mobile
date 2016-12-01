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
*/

package org.akvo.flow.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.entity.ApkData;
import org.akvo.flow.domain.interactor.GetApkDataUseCase;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.entity.ViewApkData;
import org.akvo.flow.presentation.entity.ViewApkMapper;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.util.StatusUtil;
import org.akvo.flow.util.StringUtil;
import org.akvo.flow.util.VersionHelper;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Subscriber;

/**
 * This background service will check the rest api for a new version of the APK.
 * If found, it will display a notification, requesting permission to download and
 * installAppUpdate it. After clicking the notification, the app will download and installAppUpdate
 * the new APK.
 *
 * @author Christopher Fagiani
 */
public class ApkUpdateService extends IntentService {

    private static final String TAG = "APK_UPDATE_SERVICE";

    @Inject
    ApkUpdateHelper apkUpdateHelper;

    @Inject
    Navigator navigator;

    @Inject
    @Named("getApkData")
    UseCase getApkData;

    @Inject
    VersionHelper versionHelper;

    @Inject
    ViewApkMapper mapper;

    public ApkUpdateService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FlowApp application = (FlowApp) getApplicationContext();
        application.getApplicationComponent().inject(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getApkData.unSubscribe();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //TODO: refactor those as well
//        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler.getInstance());
        checkUpdates();

    }

    /**
     * Check if new FLOW versions are available to installAppUpdate. If a new version is available,
     * we display a notification, requesting the user to download it.
     */
    private void checkUpdates() {
        //TODO: check if allowed to access internet
        if (!StatusUtil.hasDataConnection(this)) {
            Log.d(TAG, "No internet connection. Can't perform the requested operation");
            return;
        }

        Map<String, String> params = new HashMap<>(1);
        //TODO: very ugly, have datasource for server base url
        params.put(GetApkDataUseCase.BASE_URL_KEY, StatusUtil.getServerBase(this));
        //TODO: create default subscriber to avoid repeating code
        getApkData.execute(new Subscriber<ApkData>() {
            @Override
            public void onCompleted() {
                //EMPTY
            }

            @Override
            public void onError(Throwable e) {
                //TODO: check exceptions
                Log.e(TAG, "Could not call apk version service", e);
                //PersistentUncaughtExceptionHandler.recordException(e);
                //TODO: refactor this
            }

            @Override
            public void onNext(ApkData apkData) {
                ViewApkData viewApkData = mapper.transform(apkData);
                if (shouldAppBeUpdated(viewApkData)) {
                    navigator.navigateToAppUpdate(ApkUpdateService.this, viewApkData);
                }
            }

        }, params);

    }

    private boolean shouldAppBeUpdated(@Nullable ViewApkData data) {
        if (data == null) {
            return false;
        }
        String remoteVersionName = data.getVersion();
        String currentVersionName = BuildConfig.VERSION_NAME;
        return StringUtil.isValid(remoteVersionName)
                && versionHelper.isNewerVersion(currentVersionName, remoteVersionName)
                && StringUtil.isValid(data.getFileUrl());
    }
}
