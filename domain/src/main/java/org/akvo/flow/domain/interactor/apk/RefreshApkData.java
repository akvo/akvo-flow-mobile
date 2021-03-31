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

package org.akvo.flow.domain.interactor.apk;

import android.os.Build;

import androidx.annotation.Nullable;

import org.akvo.flow.domain.entity.ApkData;
import org.akvo.flow.domain.repository.ApkRepository;
import org.akvo.flow.domain.repository.UserRepository;
import org.akvo.flow.domain.util.VersionHelper;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;

public class RefreshApkData {

    public static final String APP_VERSION_PARAM = "version";

    private final ApkRepository apkRepository;
    private final UserRepository userRepository;
    private final VersionHelper versionHelper;
    private final CompositeDisposable disposables;

    @Inject
    public RefreshApkData(ApkRepository apkRepository, UserRepository userRepository,
            VersionHelper versionHelper) {
        this.apkRepository = apkRepository;
        this.userRepository = userRepository;
        this.versionHelper = versionHelper;
        this.disposables = new CompositeDisposable();
    }

    @SuppressWarnings("unchecked")
    public <T> void execute(DisposableObserver<T> observer, final Map<String, Object> parameters) {
        final Observable<T> observable = buildUseCaseObservable(parameters);
        addDisposable(observable.subscribeWith(observer));
    }

    public void dispose() {
        if (!disposables.isDisposed()) {
            disposables.clear();
        }
    }

    private void addDisposable(Disposable disposable) {
        disposables.add(disposable);
    }

    private <T> Observable buildUseCaseObservable(final Map<String, T> parameters) {
        if (parameters == null || !parameters.containsKey(APP_VERSION_PARAM)) {
            throw new IllegalArgumentException("Missing params");
        }
        return apkRepository.loadApkData(Build.VERSION.SDK_INT + "")
                .concatMap((Function<ApkData, Observable<Boolean>>) apkData -> {
                    final String currentVersionName = (String) parameters
                            .get(APP_VERSION_PARAM);
                    if (shouldAppBeUpdated(apkData, currentVersionName)) {
                        ApkData savedApkData = apkRepository.getApkDataPreference();
                        if (apkDataNeedsSaving(savedApkData, apkData)) {
                            return saveNewApkVersion(apkData);
                        }
                        return Observable.just(false);
                    }
                    return Observable.just(false);
                });
    }

    private boolean shouldAppBeUpdated(@Nullable ApkData data, String currentVersionName) {
        if (data == null) {
            return false;
        }
        String remoteVersionName = data.getVersion();
        return versionHelper.isValid(remoteVersionName)
                && versionHelper.isNewerVersion(currentVersionName, remoteVersionName)
                && versionHelper.isValid(data.getFileUrl());
    }

    private boolean apkDataNeedsSaving(ApkData savedApkData, ApkData newApkData) {
        final boolean apkDataUnset = ApkData.NOT_SET_VALUE.equals(savedApkData);
        return apkDataUnset || versionHelper
                .isNewerVersion(savedApkData.getVersion(), newApkData.getVersion());
    }

    private <T> Observable<Boolean> saveNewApkVersion(ApkData apkData) {
        return Observable.zip(apkRepository.saveApkDataPreference(apkData),
                userRepository.clearAppUpdateNotified(),
                (first, second) -> first && second);
    }
}
