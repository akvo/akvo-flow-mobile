/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.domain.interactor;

import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.ExceptionRepository;

import java.util.Map;

import javax.inject.Inject;

import rx.Observable;

public class SaveException extends UseCase {

    public static final String EXCEPTION_PARAM_KEY = "exception_key";

    private final ExceptionRepository exceptionRepository;

    @Inject
    public SaveException(ThreadExecutor threadExecutor, PostExecutionThread postExecutionThread, ExceptionRepository exceptionRepository) {
        super(threadExecutor, postExecutionThread);
        this.exceptionRepository = exceptionRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        if (parameters == null || !parameters.containsKey(EXCEPTION_PARAM_KEY)) {
            return Observable.just(new IllegalArgumentException("Missing exception param"));
        }
        Throwable throwable = (Throwable) parameters.get(EXCEPTION_PARAM_KEY);
        return exceptionRepository.save(throwable);
    }
}
