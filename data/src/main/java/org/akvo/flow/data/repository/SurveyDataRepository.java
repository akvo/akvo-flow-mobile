/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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
import android.util.Pair;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.datasource.DatabaseDataSource;
import org.akvo.flow.data.entity.ApiDataPoint;
import org.akvo.flow.data.entity.ApiLocaleResult;
import org.akvo.flow.data.entity.ApiSurveyInstance;
import org.akvo.flow.data.entity.DataPointMapper;
import org.akvo.flow.data.entity.FormInstanceMapper;
import org.akvo.flow.data.entity.FormInstanceMetadataMapper;
import org.akvo.flow.data.entity.S3File;
import org.akvo.flow.data.entity.SurveyMapper;
import org.akvo.flow.data.entity.SyncedTimeMapper;
import org.akvo.flow.data.entity.Transmission;
import org.akvo.flow.data.entity.TransmissionFilenameMapper;
import org.akvo.flow.data.entity.TransmissionMapper;
import org.akvo.flow.data.entity.UploadError;
import org.akvo.flow.data.entity.UploadFormDeletedError;
import org.akvo.flow.data.entity.UploadResult;
import org.akvo.flow.data.entity.UploadSuccess;
import org.akvo.flow.data.entity.UserMapper;
import org.akvo.flow.data.entity.form.FormIdMapper;
import org.akvo.flow.data.net.RestApi;
import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.entity.FormInstanceMetadata;
import org.akvo.flow.domain.entity.InstanceIdUuid;
import org.akvo.flow.domain.entity.Survey;
import org.akvo.flow.domain.entity.User;
import org.akvo.flow.domain.exception.AssignmentRequiredException;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

public class SurveyDataRepository implements SurveyRepository {

    private final DataSourceFactory dataSourceFactory;
    private final DataPointMapper dataPointMapper;
    private final SyncedTimeMapper syncedTimeMapper;
    private final RestApi restApi;
    private final SurveyMapper surveyMapper;
    private final UserMapper userMapper;
    private final TransmissionFilenameMapper transmissionFileMapper;
    private final FormIdMapper formIdMapper;
    private final TransmissionMapper transmissionMapper;
    private final FormInstanceMapper formInstanceMapper;
    private final FormInstanceMetadataMapper formInstanceMetadataMapper;

    //TODO: this needs to be split, too many methods and params
    @Inject
    public SurveyDataRepository(DataSourceFactory dataSourceFactory,
            DataPointMapper dataPointMapper, SyncedTimeMapper syncedTimeMapper, RestApi restApi,
            SurveyMapper surveyMapper, UserMapper userMapper,
            TransmissionFilenameMapper transmissionFilenameMapper,
            TransmissionMapper transmissionMapper, FormInstanceMapper formInstanceMapper,
            FormIdMapper formIdMapper, FormInstanceMetadataMapper formInstanceMetadataMapper) {
        this.dataSourceFactory = dataSourceFactory;
        this.dataPointMapper = dataPointMapper;
        this.syncedTimeMapper = syncedTimeMapper;
        this.restApi = restApi;
        this.surveyMapper = surveyMapper;
        this.userMapper = userMapper;
        this.transmissionFileMapper = transmissionFilenameMapper;
        this.formIdMapper = formIdMapper;
        this.transmissionMapper = transmissionMapper;
        this.formInstanceMapper = formInstanceMapper;
        this.formInstanceMetadataMapper = formInstanceMetadataMapper;
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
    public Single<DataPoint> getDataPoint(final String datapointId) {
        return dataSourceFactory.getDataBaseDataSource()
                .getDataPoint(datapointId)
                .flatMap(new Function<Cursor, Single<DataPoint>>() {
                    @Override
                    public Single<DataPoint> apply(Cursor cursor) {
                        DataPoint dataPoint = dataPointMapper.mapOneDataPoint(cursor);
                        if (dataPoint == null) {
                            return Single.error(new Exception(
                                    "Datapoint with id: " + datapointId + " not found"));
                        }
                        return Single.just(dataPoint);
                    }
                });
    }

    @Override
    public Flowable<Integer> downloadDataPoints(final long surveyGroupId) {
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
        return downloadDataPoints(surveyGroupId, state)
                .doOnNext(new Consumer<State>() {
                    @Override
                    public void accept(@NonNull State state) {
                        List<ApiDataPoint> lastBatch = state.getLastBatch();
                        dataSourceFactory.getDataBaseDataSource().syncDataPoints(lastBatch);
                    }
                })
                .repeatWhen(new Function<Flowable<Object>, Publisher<?>>() {
                    @Override
                    public Publisher<?> apply(@NonNull Flowable<Object> flowable) {
                        return flowable.delay(15, TimeUnit.SECONDS);
                    }
                })
                .takeUntil(new Predicate<State>() {
                    @Override
                    public boolean test(State state) {
                        return state.getLastBatch().isEmpty();
                    }
                })
                .filter(new Predicate<State>() {
                    @Override
                    public boolean test(State state) {
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

    private Flowable<State> downloadDataPoints(final long surveyGroupId,
            final State state) {
        return Flowable.defer(new Callable<Flowable<ApiLocaleResult>>() {
            @Override
            public Flowable<ApiLocaleResult> call() {
                return restApi.downloadDataPoints(surveyGroupId,
                        state.getTimestamp());
            }
        }).map(new Function<ApiLocaleResult, List<ApiDataPoint>>() {
            @Override
            public List<ApiDataPoint> apply(@NonNull ApiLocaleResult apiLocaleResult) {
                if (apiLocaleResult == null || apiLocaleResult.getDataPoints() == null) {
                    return Collections.emptyList();
                }
                List<ApiDataPoint> dataPoints = apiLocaleResult.getDataPoints();
                dataPoints.removeAll(state.getLastBatch()); //remove duplicates
                return dataPoints;
            }
        }).concatMap(new Function<List<ApiDataPoint>, Flowable<State>>() {
            @Override
            public Flowable<State> apply(@NonNull List<ApiDataPoint> dataPoints) {
                return filterDataPoints(dataPoints, state);
            }
        });
    }

    private Flowable<State> filterDataPoints(@NonNull List<ApiDataPoint> dataPoints,
            final State state) {
        return Flowable.fromIterable(dataPoints)
                .filter(new Predicate<ApiDataPoint>() {
                    @Override
                    public boolean test(@NonNull ApiDataPoint apiDataPoint) {
                        List<ApiSurveyInstance> instances = apiDataPoint.getSurveyInstances();
                        return instances != null && !instances.isEmpty();
                    }
                })
                .toList()
                .flatMap(new Function<List<ApiDataPoint>, SingleSource<State>>() {
                    @Override
                    public SingleSource<State> apply(@NonNull List<ApiDataPoint> points) {
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

    @Override
    public Observable<Boolean> clearResponses() {
        return dataSourceFactory.getDataBaseDataSource().clearCollectedData();
    }

    @Override
    public Observable<Boolean> clearAllData() {
        return dataSourceFactory.getDataBaseDataSource().clearAllData();
    }

    @Override
    public Observable<Boolean> unSyncedTransmissionsExist() {
        return dataSourceFactory.getDataBaseDataSource().unSyncedTransmissionsExist();
    }

    @Override
    public Observable<List<String>> getAllTransmissionFileNames() {
        return dataSourceFactory.getDataBaseDataSource().getAllTransmissions()
                .map(new Function<Cursor, List<String>>() {
                    @Override
                    public List<String> apply(Cursor cursor) {
                        return transmissionFileMapper.mapToFileNameList(cursor);
                    }
                });
    }

    @Override
    public Observable<List<String>> getFormIds(String surveyId) {
        return dataSourceFactory.getDataBaseDataSource().getFormIds(surveyId)
                .map(new Function<Cursor, List<String>>() {
                    @Override
                    public List<String> apply(Cursor cursor) {
                        return formIdMapper.mapToFormId(cursor);
                    }
                });
    }

    @Override
    public Observable<List<String>> getFormIds() {
        return dataSourceFactory.getDataBaseDataSource().getFormIds()
                .map(new Function<Cursor, List<String>>() {
                    @Override
                    public List<String> apply(Cursor cursor) {
                        return formIdMapper.mapToFormId(cursor);
                    }
                });
    }

    @Override
    @NotNull
    public Single<Pair<Boolean, String>> getFormMeta(@NotNull String formId) {
        return dataSourceFactory.getDataBaseDataSource().getFormMetaData(formId);
    }

    @Override
    @NotNull
    public Single<Long> fetchSurveyInstance(@NotNull String formId, @NotNull String datapointId,
            @NotNull String formVersion, long userId, @Nullable String userName) {
        return dataSourceFactory.getDataBaseDataSource()
                .fetchSurveyInstance(formId, datapointId, formVersion, userId, userName);
    }

    @Override
    public Observable<Set<String>> processTransmissions(final String deviceId,
            @NonNull String surveyId) {
        return getSurveyTransmissions(surveyId)
                .concatMap(new Function<List<Transmission>, Observable<Set<String>>>() {
                    @Override
                    public Observable<Set<String>> apply(List<Transmission> transmissions) {
                        return syncTransmissions(transmissions, deviceId);
                    }
                });
    }

    @Override
    public Observable<Set<String>> processTransmissions(final String deviceId) {
        return getAllTransmissions()
                .concatMap(new Function<List<Transmission>, Observable<Set<String>>>() {
                    @Override
                    public Observable<Set<String>> apply(List<Transmission> transmissions) {
                        return syncTransmissions(transmissions, deviceId);
                    }
                });
    }

    @VisibleForTesting
    Observable<List<Transmission>> getSurveyTransmissions(@NonNull String surveyId) {
        return getFormIds(surveyId)
                .flatMap(new Function<List<String>, Observable<List<Transmission>>>() {
                    @Override
                    public Observable<List<Transmission>> apply(List<String> formIds) {
                        return Observable.fromIterable(formIds)
                                .flatMap(new Function<String, Observable<List<Transmission>>>() {
                                    @Override
                                    public Observable<List<Transmission>> apply(String formId) {
                                        return getFormTransmissions(formId);
                                    }
                                })
                                .flatMapIterable(
                                        new Function<List<Transmission>, List<Transmission>>() {
                                            @Override
                                            public List<Transmission> apply(
                                                    List<Transmission> transmissions) {
                                                return transmissions;
                                            }
                                        })
                                .toList()
                                .toObservable();
                    }
                });
    }

    private Observable<List<Transmission>> getFormTransmissions(String formId) {
        return dataSourceFactory.getDataBaseDataSource()
                .getUnSyncedTransmissions(formId)
                .map(new Function<Cursor, List<Transmission>>() {
                    @Override
                    public List<Transmission> apply(Cursor cursor) {
                        return transmissionMapper.transform(cursor);
                    }
                });
    }

    private Observable<List<Transmission>> getAllTransmissions() {
        return dataSourceFactory.getDataBaseDataSource().getUnSyncedTransmissions()
                .map(new Function<Cursor, List<Transmission>>() {
                    @Override
                    public List<Transmission> apply(Cursor cursor) {
                        return transmissionMapper.transform(cursor);
                    }
                });
    }

    @Override
    public Single<List<InstanceIdUuid>> getSubmittedInstances() {
        return dataSourceFactory.getDataBaseDataSource().getSubmittedInstances()
                .map(new Function<Cursor, List<InstanceIdUuid>>() {
                    @Override
                    public List<InstanceIdUuid> apply(Cursor cursor) {
                        return formInstanceMapper.getInstanceIdUuids(cursor);
                    }
                });
    }

    @Override
    public Completable setInstanceStatusToRequested(long id) {
        return dataSourceFactory.getDataBaseDataSource().setInstanceStatusToRequested(id);
    }

    @Override
    public Single<List<Long>> getPendingSurveyInstances() {
        return dataSourceFactory.getDataBaseDataSource().getPendingSurveyInstances()
                .map(new Function<Cursor, List<Long>>() {
                    @Override
                    public List<Long> apply(Cursor cursor) {
                        return formInstanceMapper.getInstanceIds(cursor);
                    }
                });
    }

    @Override
    public Single<FormInstanceMetadata> getFormInstanceData(final Long instanceId,
            final String deviceId) {
        return dataSourceFactory.getDataBaseDataSource().getResponses(instanceId)
                .map(new Function<Cursor, FormInstanceMetadata>() {
                    @Override
                    public FormInstanceMetadata apply(Cursor cursor) {
                        return formInstanceMetadataMapper.transform(cursor, deviceId);
                    }
                })
                .flatMap(new Function<FormInstanceMetadata, Single<FormInstanceMetadata>>() {
                    @Override
                    public Single<FormInstanceMetadata> apply(
                            FormInstanceMetadata formInstanceMetadata) {
                        if (!formInstanceMetadata.isValid()) {
                            return Single
                                    .error(new Exception("Invalid form instance: " + instanceId));
                        } else {
                            return Single.just(formInstanceMetadata);
                        }
                    }
                });
    }

    @Override
    public Completable createTransmissions(final Long instanceId, final String formId,
            Set<String> fileNames) {
        final DatabaseDataSource dataBaseDataSource = dataSourceFactory.getDataBaseDataSource();
        return dataBaseDataSource
                .createTransmissions(instanceId, formId, fileNames)
                .andThen(dataBaseDataSource.setInstanceStatusToSubmitted(instanceId));
    }

    private Observable<Set<String>> updateSurveyInstance(List<UploadResult> list) {
        DatabaseDataSource dataBaseDataSource = dataSourceFactory.getDataBaseDataSource();
        Set<Long> failedTransmissions = new HashSet<>();
        Set<Long> successFullTransmissions = new HashSet<>();
        final Set<String> deletedFormsTransmissions = new HashSet<>();
        for (UploadResult result : list) {
            if (result instanceof UploadSuccess) {
                successFullTransmissions.add(result.getSurveyInstanceId());
            } else {
                failedTransmissions.add(result.getSurveyInstanceId());
                if (result instanceof UploadFormDeletedError) {
                    deletedFormsTransmissions.add(((UploadFormDeletedError) result).getFormId());
                }
            }
        }
        successFullTransmissions.removeAll(failedTransmissions);
        return Observable.zip(dataBaseDataSource.updateFailedSubmissions(failedTransmissions),
                dataBaseDataSource.updateSuccessfulSubmissions(successFullTransmissions),
                new BiFunction<Boolean, Boolean, Set<String>>() {
                    @Override
                    public Set<String> apply(Boolean ignored, Boolean ignored2) {
                        return deletedFormsTransmissions;
                    }
                });
    }

    private Observable<Set<String>> syncTransmissions(List<Transmission> transmissions,
            final String deviceId) {
        return Observable.fromIterable(transmissions)
                .concatMap(new Function<Transmission, Observable<UploadResult>>() {
                    @Override
                    public Observable<UploadResult> apply(final Transmission transmission) {
                        return syncTransmission(transmission, deviceId);
                    }
                })
                .toList().toObservable()
                .concatMap(new Function<List<UploadResult>, Observable<Set<String>>>() {
                    @Override
                    public Observable<Set<String>> apply(List<UploadResult> list) {
                        return updateSurveyInstance(list);
                    }
                });
    }

    @VisibleForTesting
    Observable<UploadResult> syncTransmission(final Transmission transmission,
            final String deviceId) {
        final DatabaseDataSource dataBaseDataSource = dataSourceFactory.getDataBaseDataSource();
        final long transmissionId = transmission.getId();
        final long surveyInstanceId = transmission.getRespondentId();
        final String formId = transmission.getFormId();
        return restApi.uploadFile(transmission)
                .concatMap(new Function<Response<ResponseBody>, Observable<?>>() {
                    @Override
                    public Observable<?> apply(Response ignored) {
                        S3File s3File = transmission.getS3File();
                        return restApi.notifyFileAvailable(s3File.getAction(),
                                transmission.getFormId(), s3File.getFile().getName(), deviceId);
                    }
                })
                .doOnNext(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) {
                        dataBaseDataSource.setFileTransmissionSucceeded(transmissionId);
                    }
                })
                .map(new Function<Object, UploadResult>() {
                    @Override
                    public UploadResult apply(Object ignored) {
                        return new UploadSuccess(surveyInstanceId);
                    }
                })
                .onErrorReturn(new Function<Throwable, UploadResult>() {
                    @Override
                    public UploadResult apply(Throwable throwable) {
                        boolean formNotFound = throwable instanceof HttpException && (
                                ((HttpException) throwable).code() == 404);
                        if (formNotFound) {
                            dataBaseDataSource.setFileTransmissionFormDeleted(transmissionId);
                            return new UploadFormDeletedError(surveyInstanceId, formId);
                        } else {
                            Timber.e(throwable);
                            dataBaseDataSource.setFileTransmissionFailed(transmissionId);
                            return new UploadError(surveyInstanceId);
                        }
                    }
                });
    }
}
