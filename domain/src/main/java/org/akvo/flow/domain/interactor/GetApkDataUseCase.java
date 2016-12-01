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
import org.akvo.flow.domain.repository.ApkRepository;

import java.util.Map;

import javax.inject.Inject;

import rx.Observable;

public class GetApkDataUseCase extends UseCase {

    private final ApkRepository apkRepository;

    public static final String BASE_URL_KEY = "base_url_key";

    @Inject
    public GetApkDataUseCase(ThreadExecutor threadExecutor, PostExecutionThread postExecutionThread, ApkRepository apkRepository) {
        super(threadExecutor, postExecutionThread);
        this.apkRepository = apkRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {

        //TODO: get base url from another repository
        if (parameters == null || !parameters.containsKey(BASE_URL_KEY)) {
            return Observable.error(new IllegalArgumentException("Base url not provided"));
        }
        //TODO: check if we are allowed to go online using 3G else throw exception
        String baseUrl = (String) parameters.get(BASE_URL_KEY);
        return apkRepository.loadApkData(baseUrl);
    }
}
