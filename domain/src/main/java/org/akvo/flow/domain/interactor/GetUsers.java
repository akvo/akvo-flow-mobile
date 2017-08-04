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

import android.support.v4.util.Pair;

import org.akvo.flow.domain.entity.User;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

public class GetUsers extends UseCase {

    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;

    @Inject
    protected GetUsers(ThreadExecutor threadExecutor, PostExecutionThread postExecutionThread,
            UserRepository userRepository, SurveyRepository surveyRepository) {
        super(threadExecutor, postExecutionThread);
        this.userRepository = userRepository;
        this.surveyRepository = surveyRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        return surveyRepository.getUsers()
                .concatMap(new Func1<List<User>, Observable<Pair<User, List<User>>>>() {
                    @Override
                    public Observable<Pair<User, List<User>>> call(final List<User> users) {
                        return userRepository.getSelectedUser()
                                .map(new Func1<Long, Pair<User, List<User>>>() {
                                    @Override
                                    public Pair<User, List<User>> call(final Long selectedUserId) {
                                        User currentUser = null;
                                        for (User u : users) {
                                            if (selectedUserId == u.getId()) {
                                                currentUser = u;
                                                break;
                                            }
                                        }
                                        if (currentUser != null) {
                                            users.remove(currentUser);
                                        }
                                        return new Pair<>(currentUser, users);
                                    }
                                });
                    }
                });
    }
}
