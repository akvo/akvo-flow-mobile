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

import org.akvo.flow.domain.entity.User;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;

import java.util.Map;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

public class DeleteUser extends UseCase {

    public static final String PARAM_USER = "user";

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;

    @Inject
    protected DeleteUser(ThreadExecutor threadExecutor, PostExecutionThread postExecutionThread,
            SurveyRepository surveyRepository, UserRepository userRepository) {
        super(threadExecutor, postExecutionThread);
        this.surveyRepository = surveyRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        if (parameters == null || parameters.get(PARAM_USER) == null) {
            return Observable.error(new IllegalArgumentException("missing user"));
        }
        final User user = (User) parameters.get(PARAM_USER);
        return userRepository.getSelectedUser()
                .concatMap(new Func1<Long, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(Long selectedUserId) {
                        if (selectedUserId == user.getId()) {
                            return userRepository.clearSelectedUser().concatMap(
                                    new Func1<Boolean, Observable<Boolean>>() {
                                        @Override
                                        public Observable<Boolean> call(Boolean aBoolean) {
                                            return deleteUser(user);
                                        }
                                    });
                        } else {
                            return deleteUser(user);
                        }
                    }
                });
    }

    private Observable<Boolean> deleteUser(User user) {
        return surveyRepository.deleteUser(user);
    }
}
