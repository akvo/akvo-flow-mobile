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

package org.akvo.flow.data.repository;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.domain.repository.SetupRepository;

import javax.inject.Inject;

import io.reactivex.Observable;

public class SetupDataRepository implements SetupRepository {

    private final DataSourceFactory dataSourceFactory;

    @Inject
    public SetupDataRepository(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public Observable<String> getApiKey() {
        return dataSourceFactory.getSecureSharedPreferencesDataSource().getApiKey();
    }

    @Override
    public Observable<Boolean> saveApiKey(String apiKey) {
        return dataSourceFactory.getSecureSharedPreferencesDataSource().saveApiKey(apiKey);
    }

    @Override
    public Observable<String> getAwsAccessKey() {
        return dataSourceFactory.getSecureSharedPreferencesDataSource().getAwsAccessKey();
    }

    @Override
    public Observable<Boolean> saveAwsAccessKey(String awsAccessKey) {
        return dataSourceFactory.getSecureSharedPreferencesDataSource()
                .saveAwsAccessKey(awsAccessKey);
    }

    @Override
    public Observable<String> getAwsBucket() {
        return dataSourceFactory.getSecureSharedPreferencesDataSource().getAwsBucket();
    }

    @Override
    public Observable<Boolean> saveAwsBucket(String awsBucket) {
        return dataSourceFactory.getSecureSharedPreferencesDataSource().saveAwsBucket(awsBucket);
    }

    @Override
    public Observable<String> getAwsSecretKey() {
        return dataSourceFactory.getSecureSharedPreferencesDataSource().getAwsSecretKey();
    }

    @Override
    public Observable<Boolean> saveAwsSecretKey(String awsSecretKey) {
        return dataSourceFactory.getSecureSharedPreferencesDataSource()
                .saveAwsSecretKey(awsSecretKey);
    }

    @Override
    public Observable<String> getInstanceUrl() {
        return dataSourceFactory.getSecureSharedPreferencesDataSource().getInstanceUrl();
    }

    @Override
    public Observable<Boolean> saveInstanceUrl(String instanceUrl) {
        return dataSourceFactory.getSecureSharedPreferencesDataSource()
                .saveInstanceUrl(instanceUrl);
    }

    @Override
    public Observable<String> getServerBase() {
        return dataSourceFactory.getSecureSharedPreferencesDataSource().getServerBase();
    }

    @Override
    public Observable<Boolean> saveServerBase(String serverBase) {
        return dataSourceFactory.getSecureSharedPreferencesDataSource().saveServerBase(serverBase);
    }

    @Override
    public Observable<String> getSigningKey() {
        return dataSourceFactory.getSecureSharedPreferencesDataSource().getSigningKey();
    }

    @Override
    public Observable<Boolean> saveSigningKey(String signingKey) {
        return dataSourceFactory.getSecureSharedPreferencesDataSource().saveSigningKey(signingKey);
    }
}
