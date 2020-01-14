/*
 * Copyright (C) 2020 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.data.repository;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.entity.ApiLocaleResult;
import org.akvo.flow.data.net.RestApi;
import org.akvo.flow.domain.exception.AssignmentRequiredException;
import org.akvo.flow.domain.repository.DataPointRepository;

import java.net.HttpURLConnection;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.functions.Function;
import retrofit2.HttpException;

public class DataPointDataRepository implements DataPointRepository {

    private final DataSourceFactory dataSourceFactory;
    private final RestApi restApi;

    @Inject
    public DataPointDataRepository(DataSourceFactory dataSourceFactory, RestApi restApi) {
        this.dataSourceFactory = dataSourceFactory;
        this.restApi = restApi;
    }

    @Override
    public Single<Integer> downloadDataPoints(final long surveyGroupId) {
        return syncDataPoints(surveyGroupId)
                .onErrorResumeNext((Function<Throwable, Single<Integer>>) throwable -> {
                    if (isErrorForbidden(throwable)) {
                        return Single.error(new AssignmentRequiredException(
                                "Dashboard Assignment missing"));
                    } else {
                        return Single.error(throwable);
                    }
                });
    }

    private boolean isErrorForbidden(Throwable throwable) {
        return throwable instanceof HttpException
                && ((HttpException) throwable).code() == HttpURLConnection.HTTP_FORBIDDEN;
    }

    private Single<Integer> syncDataPoints(final long surveyGroupId) {
        return restApi.downloadDataPoints(surveyGroupId)
                .flatMap((Function<ApiLocaleResult, Single<Integer>>) apiLocaleResult
                        -> syncDataPoints(apiLocaleResult, surveyGroupId));
    }

    private Single<Integer> syncDataPoints(ApiLocaleResult apiLocaleResult,
            final long surveyGroupId) {
        return dataSourceFactory.getDataBaseDataSource()
                .syncDataPoints(apiLocaleResult.getDataPoints(), surveyGroupId)
                .andThen(Single.just(apiLocaleResult.getDataPoints().size()));
    }
}
