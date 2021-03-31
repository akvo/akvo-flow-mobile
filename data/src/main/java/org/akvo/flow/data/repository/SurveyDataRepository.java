/*
 * Copyright (C) 2017-2020 Stichting Akvo (Akvo Foundation)
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

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import org.akvo.flow.data.datasource.DataSourceFactory;
import org.akvo.flow.data.datasource.DatabaseDataSource;
import org.akvo.flow.data.entity.DataPointMapper;
import org.akvo.flow.data.entity.FormInstanceMapper;
import org.akvo.flow.data.entity.FormInstanceMetadataMapper;
import org.akvo.flow.data.entity.S3File;
import org.akvo.flow.data.entity.SurveyMapper;
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
import org.akvo.flow.data.net.s3.S3RestApi;
import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.entity.FormInstanceMetadata;
import org.akvo.flow.domain.entity.InstanceIdUuid;
import org.akvo.flow.domain.entity.Survey;
import org.akvo.flow.domain.entity.User;
import org.akvo.flow.domain.repository.SurveyRepository;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

public class SurveyDataRepository implements SurveyRepository {

    private final DataSourceFactory dataSourceFactory;
    private final DataPointMapper dataPointMapper;
    private final RestApi restApi;
    private final SurveyMapper surveyMapper;
    private final UserMapper userMapper;
    private final TransmissionFilenameMapper transmissionFileMapper;
    private final FormIdMapper formIdMapper;
    private final TransmissionMapper transmissionMapper;
    private final FormInstanceMapper formInstanceMapper;
    private final FormInstanceMetadataMapper formInstanceMetadataMapper;
    private final S3RestApi s3RestApi;

    //TODO: this needs to be split, too many methods and params
    @Inject
    public SurveyDataRepository(DataSourceFactory dataSourceFactory,
                                DataPointMapper dataPointMapper, RestApi restApi, SurveyMapper surveyMapper,
                                UserMapper userMapper, TransmissionFilenameMapper transmissionFilenameMapper,
                                TransmissionMapper transmissionMapper, FormInstanceMapper formInstanceMapper,
                                FormIdMapper formIdMapper, FormInstanceMetadataMapper formInstanceMetadataMapper,
                                S3RestApi s3RestApi) {
        this.dataSourceFactory = dataSourceFactory;
        this.dataPointMapper = dataPointMapper;
        this.restApi = restApi;
        this.surveyMapper = surveyMapper;
        this.userMapper = userMapper;
        this.transmissionFileMapper = transmissionFilenameMapper;
        this.formIdMapper = formIdMapper;
        this.transmissionMapper = transmissionMapper;
        this.formInstanceMapper = formInstanceMapper;
        this.formInstanceMetadataMapper = formInstanceMetadataMapper;
        this.s3RestApi = s3RestApi;
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
    public User getUser(Long userId) {
        return userMapper.mapUser(dataSourceFactory.getDataBaseDataSource().getUser(userId));
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

    @Nullable
    @Override
    public Completable setSurveyViewed(long surveyId) {
        dataSourceFactory.getDataBaseDataSource().setSurveyViewed(surveyId);
        return Completable.complete();
    }

    @Override
    public Completable cleanDataPoints(Long surveyGroupId) {
        dataSourceFactory.getDataBaseDataSource().cleanDataPoints(surveyGroupId);
        return Completable.complete();
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
        Cursor cursor = dataSourceFactory.getDataBaseDataSource().getResponses(instanceId);
        FormInstanceMetadata formInstanceMetadata = formInstanceMetadataMapper.transform(cursor, deviceId);
        if (!formInstanceMetadata.isValid()) {
            return Single.error(new Exception("Invalid form instance: " + instanceId));
        } else {
            return Single.just(formInstanceMetadata);
        }
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
        return s3RestApi.uploadFile(transmission)
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
