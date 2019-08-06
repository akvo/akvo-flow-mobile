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

import androidx.core.util.Pair;

import org.akvo.flow.domain.entity.ApkData;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.repository.ApkRepository;
import org.akvo.flow.domain.repository.UserRepository;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;

public class GetApkDataPreferences extends UseCase {

    private final ApkRepository apkRepository;
    private final UserRepository userRepository;

    @Inject
    protected GetApkDataPreferences(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, ApkRepository apkRepository,
            UserRepository userRepository) {
        super(threadExecutor, postExecutionThread);
        this.apkRepository = apkRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        return Observable.zip(apkRepository.getApkDataPreference(),
                userRepository.getLastNotificationTime(),
                new BiFunction<ApkData, Long, Pair<ApkData, Long>>() {
                    @Override
                    public Pair<ApkData, Long> apply(ApkData apkData, Long notificationTime) {
                        return new Pair<>(apkData, notificationTime);
                    }
                });
    }
}
