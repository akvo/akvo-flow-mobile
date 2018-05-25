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

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;

public class EditUser extends UseCase {

    public static final String PARAM_USER = "user";

    private final SurveyRepository surveyRepository;

    @Inject
    protected EditUser(ThreadExecutor threadExecutor, PostExecutionThread postExecutionThread,
            SurveyRepository surveyRepository) {
        super(threadExecutor, postExecutionThread);
        this.surveyRepository = surveyRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        if (parameters == null || parameters.get(PARAM_USER) == null) {
            return Observable.error(new IllegalArgumentException("missing user"));
        }
        User user = (User) parameters.get(PARAM_USER);
        return surveyRepository.editUser(user);
    }
}
