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

import org.akvo.flow.domain.entity.UserSettings;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.UserRepository;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Function4;

public class GetUserSettings extends UseCase {

    private final UserRepository userRepository;

    @Inject
    protected GetUserSettings(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, UserRepository userRepository) {
        super(threadExecutor, postExecutionThread);
        this.userRepository = userRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        return Observable.zip(userRepository.keepScreenOn(), userRepository.mobileSyncAllowed(),
                userRepository.getImageSize(), userRepository.getDeviceId(),
                new Function4<Boolean, Boolean, Integer, String, UserSettings>() {
                    @Override
                    public UserSettings apply(Boolean screenOn, Boolean mobileSync,
                            Integer imageSize, String deviceId) {
                        return new UserSettings(screenOn, mobileSync, imageSize,
                                deviceId);
                    }
                });
    }
}
