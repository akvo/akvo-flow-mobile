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

package org.akvo.flow.domain.interactor.setup;

import android.text.TextUtils;

import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.repository.SetupRepository;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function7;

public class SaveSetup extends UseCase {

    public static final String PARAM_SETUP = "setup";

    private final SetupRepository setupRepository;

    @Inject
    protected SaveSetup(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread,
            SetupRepository setupRepository) {
        super(threadExecutor, postExecutionThread);
        this.setupRepository = setupRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        if (parameters == null || parameters.get(PARAM_SETUP) == null) {
            return Observable.error(new IllegalArgumentException("Missing setup params"));
        }
        SetUpParams params = (SetUpParams) parameters.get(PARAM_SETUP);
        return saveSetupConfig(params);
    }

    private Observable<Boolean> saveSetupConfig(SetUpParams params) {
        return Observable
                .zip(saveApiKey(params.getApiKey()), saveAwsAccessKey(params.getAwsAccessKeyId()),
                        saveAwsBucket(params.getAwsBucket()),
                        saveAwsSecretKey(params.getAwsSecretKey()),
                        saveInstanceUrl(params.getInstanceUrl()),
                        saveServerBase(params.getServerBase()),
                        saveSigningKey(params.getSigningKey()),
                        new Function7<Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean,
                                Boolean>() {
                            @Override
                            public Boolean apply(Boolean aBoolean, Boolean aBoolean2,
                                    Boolean aBoolean3, Boolean aBoolean4, Boolean aBoolean5,
                                    Boolean aBoolean6, Boolean aBoolean7) {
                                return true;
                            }
                        });
    }

    private Observable<Boolean> saveApiKey(final String apiKey) {
        return setupRepository.getApiKey()
                .flatMap(new Function<String, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(String savedApiKey) {
                        if (TextUtils.isEmpty(savedApiKey)) {
                            return setupRepository.saveApiKey(apiKey);
                        }
                        return Observable.just(true);
                    }
                });
    }

    private Observable<Boolean> saveAwsAccessKey(final String awsAccessKeyId) {
        return setupRepository.getAwsAccessKey()
                .flatMap(new Function<String, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(String savedAwsAccessKeyId) {
                        if (TextUtils.isEmpty(savedAwsAccessKeyId)) {
                            return setupRepository.saveAwsAccessKey(awsAccessKeyId);
                        }
                        return Observable.just(true);
                    }
                });
    }

    private Observable<Boolean> saveAwsBucket(final String awsBucket) {
        return setupRepository.getAwsBucket()
                .flatMap(new Function<String, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(String savedAwsBucket) {
                        if (TextUtils.isEmpty(savedAwsBucket)) {
                            return setupRepository.saveAwsBucket(awsBucket);
                        }
                        return Observable.just(true);
                    }
                });
    }

    private Observable<Boolean> saveAwsSecretKey(final String awsSecretKey) {
        return setupRepository.getAwsSecretKey()
                .flatMap(new Function<String, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(String savedAwsSecretKey) {
                        if (TextUtils.isEmpty(savedAwsSecretKey)) {
                            return setupRepository.saveAwsSecretKey(awsSecretKey);
                        }
                        return Observable.just(true);
                    }
                });
    }

    private Observable<Boolean> saveInstanceUrl(final String instanceUrl) {
        return setupRepository.getInstanceUrl()
                .flatMap(new Function<String, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(String savedInstanceUrl) {
                        if (TextUtils.isEmpty(savedInstanceUrl)) {
                            return setupRepository.saveInstanceUrl(instanceUrl);
                        }
                        return Observable.just(true);
                    }
                });
    }

    private Observable<Boolean> saveServerBase(final String serverBase) {
        return setupRepository.getServerBase()
                .flatMap(new Function<String, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(String savedServerBase) {
                        if (TextUtils.isEmpty(savedServerBase)) {
                            return setupRepository.saveServerBase(serverBase);
                        }
                        return Observable.just(true);
                    }
                });
    }

    private Observable<Boolean> saveSigningKey(final String signingKey) {
        return setupRepository.getSigningKey()
                .flatMap(new Function<String, ObservableSource<Boolean>>() {
                    @Override
                    public ObservableSource<Boolean> apply(String savedSigningKey) {
                        if (TextUtils.isEmpty(savedSigningKey)) {
                            return setupRepository.saveSigningKey(signingKey);
                        }
                        return Observable.just(true);
                    }
                });
    }

}
