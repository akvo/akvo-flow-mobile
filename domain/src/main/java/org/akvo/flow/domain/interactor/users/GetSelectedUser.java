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

package org.akvo.flow.domain.interactor.users;

import org.akvo.flow.domain.entity.User;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.akvo.flow.domain.repository.UserRepository;
import org.akvo.flow.domain.util.Constants;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class GetSelectedUser extends UseCase {

    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;

    @Inject
    protected GetSelectedUser(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread,
            UserRepository userRepository,
            SurveyRepository surveyRepository) {
        super(threadExecutor, postExecutionThread);
        this.userRepository = userRepository;
        this.surveyRepository = surveyRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        return userRepository.getSelectedUser()
                .concatMap(new Function<Long, Observable<User>>() {
                    @Override
                    public Observable<User> apply(Long userId) {
                        if (Constants.INVALID_USER_ID.equals(userId)) {
                            return Observable.just(new User(Constants.INVALID_USER_ID, null));
                        } else {
                            return surveyRepository.getUser(userId);
                        }
                    }
                });
    }
}
