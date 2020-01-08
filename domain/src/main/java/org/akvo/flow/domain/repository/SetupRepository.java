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

package org.akvo.flow.domain.repository;

import io.reactivex.Observable;

public interface SetupRepository {

    Observable<String> getApiKey();

    Observable<Boolean> saveApiKey(String apiKey);

    Observable<String> getAwsAccessKey();

    Observable<Boolean> saveAwsAccessKey(String awsAccessKey);

    Observable<String> getAwsBucket();

    Observable<Boolean> saveAwsBucket(String awsBucket);

    Observable<String> getAwsSecretKey();

    Observable<Boolean> saveAwsSecretKey(String awsSecretKey);

    Observable<String> getInstanceUrl();

    Observable<Boolean> saveInstanceUrl(String instanceUrl);

    Observable<String> getServerBase();

    Observable<Boolean> saveServerBase(String serverBase);

    Observable<String> getSigningKey();

    Observable<Boolean> saveSigningKey(String signingKey);

}
