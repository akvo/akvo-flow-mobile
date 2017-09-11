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

import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.UserRepository;
import org.akvo.flow.domain.util.ConnectivityStateManager;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;

public class AllowedToConnect extends UseCase {

    private final UserRepository userRepository;
    private final ConnectivityStateManager connectivityStateManager;

    @Inject
    protected AllowedToConnect(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, UserRepository userRepository,
            ConnectivityStateManager connectivityStateManager) {
        super(threadExecutor, postExecutionThread);
        this.userRepository = userRepository;
        this.connectivityStateManager = connectivityStateManager;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        if (connectivityStateManager.isWifiConnected()) {
            return Observable.just(true);
        } else {
            return userRepository.mobileSyncAllowed();
        }
    }
}
