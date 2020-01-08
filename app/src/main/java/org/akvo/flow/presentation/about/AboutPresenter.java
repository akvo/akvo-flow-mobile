/*
 * Copyright (C) 2018,2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.about;

import androidx.annotation.Nullable;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.domain.entity.ApkData;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.apk.GetApkData;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.util.VersionHelper;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.presentation.entity.ViewApkMapper;
import org.akvo.flow.util.StringUtil;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

public class AboutPresenter implements Presenter {

    private final VersionHelper versionHelper;
    private final UseCase getApkData;
    private final ViewApkMapper viewApkMapper;

    private AboutView view;

    @Inject
    public AboutPresenter(VersionHelper versionHelper, @Named("getApkData") UseCase getApkData,
            ViewApkMapper viewApkMapper) {
        this.versionHelper = versionHelper;
        this.getApkData = getApkData;
        this.viewApkMapper = viewApkMapper;
    }

    public void setView(AboutView view) {
        this.view = view;
    }

    @Override
    public void destroy() {
        getApkData.dispose();
    }

    void checkApkVersion() {
        Map<String, Object> params = new HashMap<>(2);
        params.put(GetApkData.APP_VERSION_PARAM, BuildConfig.VERSION_NAME);
        getApkData.execute(new DefaultObserver<ApkData>() {
            @Override
            public void onNext(ApkData apkData) {
                if (shouldAppBeUpdated(apkData)) {
                    view.showNewVersionAvailable(viewApkMapper.transform(apkData));
                } else {
                    view.showNoUpdateAvailable();
                }
            }

            @Override
            public void onError(Throwable e) {
                view.showErrorGettingApkData();
            }
        }, params);

    }

    private boolean shouldAppBeUpdated(@Nullable ApkData data) {
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
