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

package org.akvo.flow.data.repository;

import android.database.Cursor;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.entity.ApiDataPoint;
import org.akvo.flow.data.entity.ApiLocaleResult;
import org.akvo.flow.data.entity.ApiSurveyInstance;
import org.akvo.flow.data.entity.DataPointMapper;
import org.akvo.flow.data.entity.SurveyMapper;
import org.akvo.flow.data.entity.SyncedTimeMapper;
import org.akvo.flow.data.entity.UserMapper;
import org.akvo.flow.data.net.FlowRestApi;
import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.entity.Survey;
import org.akvo.flow.domain.entity.User;
import org.akvo.flow.domain.exception.AssignmentRequiredException;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.reactivestreams.Publisher;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import retrofit2.HttpException;

public class SurveyDataRepository implements SurveyRepository {

    private final DataSourceFactory dataSourceFactory;
    private final DataPointMapper dataPointMapper;
    private final SyncedTimeMapper syncedTimeMapper;
    private final FlowRestApi restApi;
    private final SurveyMapper surveyMapper;
    private final UserMapper userMapper;

    @Inject
    public SurveyDataRepository(DataSourceFactory dataSourceFactory,
            DataPointMapper dataPointMapper, SyncedTimeMapper syncedTimeMapper, FlowRestApi restApi,
            SurveyMapper surveyMapper, UserMapper userMapper) {
        this.dataSourceFactory = dataSourceFactory;
        this.dataPointMapper = dataPointMapper;
        this.syncedTimeMapper = syncedTimeMapper;
        this.restApi = restApi;
        this.surveyMapper = surveyMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Observable<List<Survey>> getSurveys() {
        return dataSourceFactory.getDataBaseDataSource().getSurveys()
                .map(new Function<Cursor, List<Survey>>() {
                    @Override
                    public List<Survey> apply(Cursor cursor) {
                        return surveyMapper.getSurveys(cursor);
                    }
                });
    }

    @Override
    public Observable<List<DataPoint>> getDataPoints(Long surveyGroupId,
            Double latitude, Double longitude, Integer orderBy) {
        return dataSourceFactory.getDataBaseDataSource()
                .getDataPoints(surveyGroupId, latitude, longitude, orderBy).concatMap(
                        new Function<Cursor, Observable<List<DataPoint>>>() {
                            @Override
                            public Observable<List<DataPoint>> apply(Cursor cursor) {
                                return Observable.just(dataPointMapper.getDataPoints(cursor));
                            }
                        });
    }

    @Override
    public Flowable<Integer> syncRemoteDataPoints(final long surveyGroupId) {
        return syncDataPoints(surveyGroupId)
                .onErrorResumeNext(new Function<Throwable, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Throwable throwable) {
                        if (isErrorForbidden(throwable)) {
                            return Flowable.error(new AssignmentRequiredException(
                                    "Dashboard Assignment missing"));
                        } else {
                            return Flowable.error(throwable);
                        }
                    }
                });
    }

    private boolean isErrorForbidden(Throwable throwable) {
        return throwable instanceof HttpException
                && ((HttpException) throwable).code() == HttpURLConnection.HTTP_FORBIDDEN;
    }

    private String getSyncedTime(long surveyGroupId) {
        Cursor syncedTime = dataSourceFactory.getDataBaseDataSource().getSyncedTime(surveyGroupId);
        return syncedTimeMapper.getTime(syncedTime);
    }

    private Flowable<Integer> syncDataPoints(final long surveyGroupId) {
        final State state = new State(getSyncedTime(surveyGroupId));
        return loadNewDataPoints(surveyGroupId, state)
                .doOnNext(new Consumer<State>() {
                    @Override
                    public void accept(@NonNull State state) throws Exception {
                        List<ApiDataPoint> lastBatch = state.getLastBatch();
                        dataSourceFactory.getDataBaseDataSource().syncDataPoints(lastBatch);
                    }
                })
                .repeatWhen(new Function<Flowable<Object>, Publisher<?>>() {
                    @Override
                    public Publisher<?> apply(@NonNull Flowable<Object> flowable) throws Exception {
                        return flowable.delay(15, TimeUnit.SECONDS);
                    }
                })
                .takeUntil(new Predicate<State>() {
                    @Override
                    public boolean test(State state) throws Exception {
                        return state.getLastBatch().isEmpty();
                    }
                })
                .filter(new Predicate<State>() {
                    @Override
                    public boolean test(State state) throws Exception {
                        return state.getLastBatch().isEmpty();
                    }
                })
                .map(new Function<State, Integer>() {
                    @Override
                    public Integer apply(State state) {
                        return state.retrievedItems;
                    }
                });
    }

    private Flowable<State> loadNewDataPoints(final long surveyGroupId,
            final State state) {
        return Flowable.defer(new Callable<Flowable<ApiLocaleResult>>() {
            @Override
            public Flowable<ApiLocaleResult> call() throws Exception {
                return restApi.loadNewDataPoints(surveyGroupId,
                        state.getTimestamp());
            }
        }).map(new Function<ApiLocaleResult, List<ApiDataPoint>>() {
            @Override
            public List<ApiDataPoint> apply(@NonNull ApiLocaleResult apiLocaleResult)
                    throws Exception {
                if (apiLocaleResult == null || apiLocaleResult.getDataPoints() == null) {
                    return Collections.emptyList();
                }
                List<ApiDataPoint> dataPoints = apiLocaleResult.getDataPoints();
                dataPoints.removeAll(state.getLastBatch()); //remove duplicates
                return dataPoints;
            }
        }).concatMap(new Function<List<ApiDataPoint>, Flowable<State>>() {
            @Override
            public Flowable<State> apply(@NonNull List<ApiDataPoint> dataPoints)
                    throws Exception {
                return filterDataPoints(dataPoints, state);
            }
        });
    }

    private Flowable<State> filterDataPoints(@NonNull List<ApiDataPoint> dataPoints,
            final State state) {
        return Flowable.fromIterable(dataPoints)
                .filter(new Predicate<ApiDataPoint>() {
                    @Override
                    public boolean test(@NonNull ApiDataPoint apiDataPoint) throws Exception {
                        List<ApiSurveyInstance> instances = apiDataPoint.getSurveyInstances();
                        return instances != null && !instances.isEmpty();
                    }
                })
                .toList()
                .flatMap(new Function<List<ApiDataPoint>, SingleSource<State>>() {
                    @Override
                    public SingleSource<State> apply(@NonNull List<ApiDataPoint> points)
                            throws Exception {
                        state.update(points);
                        return Single.just(state);
                    }
                })
                .toFlowable();
    }

    public class State {

        @NonNull
        private final List<ApiDataPoint> lastBatch;

        private String timestamp;
        private int retrievedItems = 0;

        State(String timestamp) {
            this.timestamp = timestamp;
            this.lastBatch = new ArrayList<>();
        }

        void update(List<ApiDataPoint> dataPoints) {
            if (dataPoints == null) {
                return;
            }
            lastBatch.clear();
            lastBatch.addAll(dataPoints);
            retrievedItems += dataPoints.size();
            int size = lastBatch.size();
            if (size != 0) {
                ApiDataPoint lastDataPoint = lastBatch.get(size - 1);
                timestamp = String.valueOf(lastDataPoint.getLastModified());
            }
        }

        String getTimestamp() {
            return timestamp;
        }

        List<ApiDataPoint> getLastBatch() {
            return lastBatch;
        }
    }

    @Override
    public Observable<Boolean> deleteSurvey(long surveyToDeleteId) {
        return dataSourceFactory.getDataBaseDataSource().deleteSurvey(surveyToDeleteId);
    }

    @Override
    public Observable<List<User>> getUsers() {
        return dataSourceFactory.getDataBaseDataSource().getUsers()
                .map(new Function<Cursor, List<User>>() {
                    @Override
                    public List<User> apply(Cursor cursor) {
                        return userMapper.mapUsers(cursor);
                    }
                });
    }

    @Override
    public Observable<Boolean> editUser(User user) {
        return dataSourceFactory.getDataBaseDataSource().editUser(user);
    }

    @Override
    public Observable<Boolean> deleteUser(User user) {
        return dataSourceFactory.getDataBaseDataSource().deleteUser(user);
    }

    @Override
    public Observable<Long> createUser(String userName) {
        return dataSourceFactory.getDataBaseDataSource().createUser(userName);
    }

    @Override
    public Observable<User> getUser(Long userId) {
        return dataSourceFactory.getDataBaseDataSource().getUser(userId)
                .map(new Function<Cursor, User>() {
                    @Override
                    public User apply(Cursor cursor) {
                        return userMapper.mapUser(cursor);
                    }
                });
    }
}
