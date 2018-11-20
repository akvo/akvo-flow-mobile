/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain.interactor;

import org.akvo.flow.domain.entity.ApkData;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.ApkRepository;
import org.akvo.flow.domain.repository.UserRepository;
import org.akvo.flow.domain.util.VersionHelper;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;

public class SaveApkData extends UseCase {

    public static final String KEY_APK_DATA = "apk_data";
    private final ApkRepository apkRepository;
    private final UserRepository userRepository;
    private final VersionHelper versionHelper;

    @Inject
    protected SaveApkData(ThreadExecutor threadExecutor, PostExecutionThread postExecutionThread,
            ApkRepository apkRepository, UserRepository userRepository,
            VersionHelper versionHelper) {
        super(threadExecutor, postExecutionThread);
        this.apkRepository = apkRepository;
        this.userRepository = userRepository;
        this.versionHelper = versionHelper;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(final Map<String, T> parameters) {
        if (parameters == null || !parameters.containsKey(KEY_APK_DATA)) {
            throw new IllegalArgumentException("Missing params");
        }

        final ApkData apkData = (ApkData) parameters.get(KEY_APK_DATA);
        return apkRepository.getApkDataPreference()
                .concatMap(new Function<ApkData, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(ApkData savedApkData) {
                        if (savedApkData == null || versionHelper
                                .isNewerVersion(savedApkData.getVersion(), apkData.getVersion())) {
                            return Observable.zip(apkRepository
                                            .saveApkDataPreference((ApkData) parameters.get(KEY_APK_DATA)),
                                    userRepository.clearAppUpdateNotified(),
                                    new BiFunction<Boolean, Boolean, Boolean>() {
                                        @Override
                                        public Boolean apply(Boolean first, Boolean second) {
                                            if (first == null || second == null) {
                                                return false;
                                            }
                                            return first && second;
                                        }
                                    });
                        }
                        return Observable.just(false);
                    }
                });
    }
}
