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

package org.akvo.flow.data.datasource;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Pair;

import com.squareup.sqlbrite2.BriteDatabase;

import org.akvo.flow.data.entity.ApiDataPoint;
import org.akvo.flow.data.entity.ApiFormHeader;
import org.akvo.flow.data.entity.ApiQuestionAnswer;
import org.akvo.flow.data.entity.ApiSurveyInstance;
import org.akvo.flow.data.entity.SurveyInstanceIdMapper;
import org.akvo.flow.data.entity.form.Form;
import org.akvo.flow.data.entity.form.FormLanguagesMapper;
import org.akvo.flow.data.entity.form.FormMetadataMapper;
import org.akvo.flow.data.util.FlowFileBrowser;
import org.akvo.flow.database.Constants;
import org.akvo.flow.database.RecordColumns;
import org.akvo.flow.database.ResponseColumns;
import org.akvo.flow.database.SurveyColumns;
import org.akvo.flow.database.SurveyGroupColumns;
import org.akvo.flow.database.SurveyInstanceColumns;
import org.akvo.flow.database.SurveyInstanceStatus;
import org.akvo.flow.database.TransmissionStatus;
import org.akvo.flow.database.britedb.BriteSurveyDbAdapter;
import org.akvo.flow.domain.entity.User;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class DatabaseDataSource {

    private static final String DEFAULTS_SURVEY_LANGUAGE = "en";
    private static final String DEFAULT_SURVEY_TYPE = "survey";
    private static final String DEFAULT_SURVEY_LOCATION = "sdcard";

    private final BriteSurveyDbAdapter briteSurveyDbAdapter;
    private final SurveyInstanceIdMapper surveyInstanceIdMapper;
    private final FormMetadataMapper formMetadataMapper;
    private final FormLanguagesMapper formLanguagesMapper;

    @Inject
    public DatabaseDataSource(BriteDatabase db, SurveyInstanceIdMapper surveyInstanceIdMapper,
            FormMetadataMapper formMetadataMapper,
            FormLanguagesMapper formLanguagesMapper) {
        this.briteSurveyDbAdapter = new BriteSurveyDbAdapter(db);
        this.surveyInstanceIdMapper = surveyInstanceIdMapper;
        this.formMetadataMapper = formMetadataMapper;
        this.formLanguagesMapper = formLanguagesMapper;
    }

    public Observable<Cursor> getSurveys() {
        return briteSurveyDbAdapter.getSurveys();
    }

    public Observable<Boolean> deleteSurvey(long surveyId) {
        return briteSurveyDbAdapter.deleteSurveyAndGroup(surveyId);
    }

    public Observable<Cursor> getDataPoints(@NonNull Long surveyGroupId, @Nullable Double latitude,
            @Nullable Double longitude, @Nullable Integer orderBy) {
        if (isRequestFiltered(orderBy)) {
            return briteSurveyDbAdapter
                    .getFilteredDataPoints(surveyGroupId, latitude, longitude, orderBy);
        } else {
            return briteSurveyDbAdapter.getDataPoints(surveyGroupId);
        }
    }

    public Single<Cursor> getDataPoint(String dataPointId) {
        return Single.just(briteSurveyDbAdapter.getDataPoint(dataPointId));
    }

    public Completable syncDataPoints(List<ApiDataPoint> apiDataPoints, final long surveyId) {
        if (apiDataPoints == null) {
            return Completable.complete();
        }
        BriteDatabase.Transaction transaction = briteSurveyDbAdapter.beginTransaction();
        try {
            briteSurveyDbAdapter.deleteSubmittedRecordsForSurvey(surveyId);
            for (ApiDataPoint dataPoint : apiDataPoints) {
                final String id = dataPoint.getId();
                ContentValues values = new ContentValues();
                values.put(RecordColumns.RECORD_ID, id);
                values.put(RecordColumns.SURVEY_GROUP_ID, dataPoint.getSurveyGroupId());
                values.put(RecordColumns.NAME, dataPoint.getDisplayName());
                values.put(RecordColumns.LATITUDE, dataPoint.getLatitude());
                values.put(RecordColumns.LONGITUDE, dataPoint.getLongitude());

                syncSurveyInstances(dataPoint.getSurveyInstances(), id);

                briteSurveyDbAdapter.updateRecord(id, values, dataPoint.getLastModified());
            }
            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
        return Completable.complete();
    }

    private boolean isRequestFiltered(@Nullable Integer orderBy) {
        return orderBy != null && (orderBy == Constants.ORDER_BY_DISTANCE ||
                orderBy == Constants.ORDER_BY_DATE ||
                orderBy == Constants.ORDER_BY_STATUS ||
                orderBy == Constants.ORDER_BY_NAME);
    }

    private void syncSurveyInstances(List<ApiSurveyInstance> surveyInstances, String dataPointId) {
        for (ApiSurveyInstance surveyInstance : surveyInstances) {

            ContentValues values = new ContentValues();
            values.put(SurveyInstanceColumns.SURVEY_ID, surveyInstance.getSurveyId());
            values.put(SurveyInstanceColumns.SUBMITTED_DATE, surveyInstance.getCollectionDate());
            values.put(SurveyInstanceColumns.RECORD_ID, dataPointId);
            values.put(SurveyInstanceColumns.STATUS, SurveyInstanceStatus.DOWNLOADED);
            values.put(SurveyInstanceColumns.SYNC_DATE, System.currentTimeMillis());
            values.put(SurveyInstanceColumns.SUBMITTER, surveyInstance.getSubmitter());

            long id = briteSurveyDbAdapter.syncSurveyInstance(values, surveyInstance.getUuid());

            syncResponses(surveyInstance.getQasList(), id);
        }
        briteSurveyDbAdapter.deleteEmptyRecords();
    }

    private void syncResponses(List<ApiQuestionAnswer> responses, long surveyInstanceId) {
        deleteResponses(responses, surveyInstanceId);
        insertResponses(responses, surveyInstanceId);
    }

    private void deleteResponses(List<ApiQuestionAnswer> responses, long surveyInstanceId) {
        for (ApiQuestionAnswer response : responses) {
            briteSurveyDbAdapter.deleteResponses(surveyInstanceId, response.getQuestionId());
        }
    }

    private void insertResponses(List<ApiQuestionAnswer> responses, long surveyInstanceId) {
        for (ApiQuestionAnswer response : responses) {

            ContentValues values = new ContentValues();
            values.put(ResponseColumns.ANSWER, response.getAnswer());
            values.put(ResponseColumns.TYPE, response.getType());
            values.put(ResponseColumns.QUESTION_ID, response.getQuestionId());
            /*
             * true by default when parsing api response
             * Not sure what this fields is used for ??
             */
            values.put(ResponseColumns.INCLUDE, true);
            values.put(ResponseColumns.SURVEY_INSTANCE_ID, surveyInstanceId);

            briteSurveyDbAdapter.syncResponse(surveyInstanceId, values, response.getQuestionId());
        }
    }

    public Observable<Cursor> getUsers() {
        return briteSurveyDbAdapter.getUsers();
    }

    public Observable<Cursor> getUser(Long userId) {
        return Observable.just(briteSurveyDbAdapter.getUser(userId));
    }

    public Observable<Boolean> editUser(User user) {
        briteSurveyDbAdapter.updateUser(user.getId(), user.getName());
        return Observable.just(true);
    }

    public Observable<Boolean> deleteUser(User user) {
        briteSurveyDbAdapter.deleteUser(user.getId());
        return Observable.just(true);
    }

    public Observable<Long> createUser(String userName) {
        return Observable.just(briteSurveyDbAdapter.createUser(userName));
    }

    public Observable<Boolean> clearCollectedData() {
        briteSurveyDbAdapter.clearCollectedData();
        return Observable.just(true);
    }

    public Observable<Boolean> clearAllData() {
        briteSurveyDbAdapter.clearAllData();
        return Observable.just(true);
    }

    public Observable<Boolean> unSyncedTransmissionsExist() {
        return Observable.just(briteSurveyDbAdapter.unSyncedTransmissionsExist());
    }

    public Observable<Cursor> getAllTransmissions() {
        return Observable.just(briteSurveyDbAdapter.getAllTransmissions());
    }

    public Observable<Cursor> getFormIds(String surveyId) {
        return Observable.just(briteSurveyDbAdapter.getFormIds(surveyId));
    }

    public Observable<Cursor> getFormIds() {
        return Observable.just(briteSurveyDbAdapter.getFormIds());
    }

    public Observable<Boolean> updateFailedTransmissionsSurveyInstances(
            @Nullable final Set<String> filenames) {
        if (filenames == null || filenames.isEmpty()) {
            return Observable.just(true);
        }
        return Observable.fromIterable(filenames)
                .concatMap(new Function<String, Observable<Long>>() {
                    @Override
                    public Observable<Long> apply(String filename) {
                        return Observable
                                .just(briteSurveyDbAdapter.getTransmissionForFileName(filename))
                                .map(new Function<Cursor, Long>() {
                                    @Override
                                    public Long apply(Cursor cursor) {
                                        return surveyInstanceIdMapper.getSurveyInstanceIds(cursor);
                                    }
                                })
                                .filter(new Predicate<Long>() {
                                    @Override
                                    public boolean test(Long aLong) {
                                        return aLong != -1L;
                                    }
                                });
                    }
                })
                .toList()
                .toObservable()
                .concatMap(new Function<List<Long>, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(List<Long> instanceIds) {
                        return updateFailedSubmissions(new HashSet<>(instanceIds));
                    }
                });
    }

    public Observable<Set<String>> setDeletedForms(@Nullable Set<String> deletedFormIds) {
        if (deletedFormIds == null || deletedFormIds.isEmpty()) {
            return Observable.just(Collections.<String>emptySet());
        }
        briteSurveyDbAdapter.setFormsDeleted(deletedFormIds);
        return Observable.just(deletedFormIds);
    }

    public Observable<Cursor> getUnSyncedTransmissions(String formId) {
        return Observable.just(briteSurveyDbAdapter.getUnSyncedTransmissions(formId));
    }

    public Observable<Cursor> getUnSyncedTransmissions() {
        return Observable.just(briteSurveyDbAdapter.getUnSyncedTransmissions());
    }

    public void setFileTransmissionSucceeded(Long id) {
        briteSurveyDbAdapter.updateTransmissionStatus(id, TransmissionStatus.SYNCED);
    }

    public void setFileTransmissionFailed(Long id) {
        briteSurveyDbAdapter.updateTransmissionStatus(id, TransmissionStatus.FAILED);
    }

    public void setFileTransmissionFormDeleted(long id) {
        briteSurveyDbAdapter.updateTransmissionStatus(id, TransmissionStatus.FORM_DELETED);
    }

    public Observable<Boolean> updateFailedSubmissions(Set<Long> failedSubmissions) {
        BriteDatabase.Transaction transaction = briteSurveyDbAdapter.beginTransaction();
        try {
            for (long submission : failedSubmissions) {
                briteSurveyDbAdapter
                        .updateSurveyInstanceStatus(submission, SurveyInstanceStatus.SUBMITTED);
            }
            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
        return Observable.just(true);
    }

    public Observable<Boolean> updateSuccessfulSubmissions(Set<Long> successfulSubmissions) {
        BriteDatabase.Transaction transaction = briteSurveyDbAdapter.beginTransaction();
        try {
            for (long submission : successfulSubmissions) {
                briteSurveyDbAdapter
                        .updateSurveyInstanceStatus(submission, SurveyInstanceStatus.UPLOADED);
            }
            transaction.markSuccessful();
        } finally {
            transaction.end();
        }
        return Observable.just(true);
    }

    public Single<Cursor> getSubmittedInstances() {
        return Single.just(briteSurveyDbAdapter
                .getSurveyInstancesByStatus(SurveyInstanceStatus.SUBMITTED));
    }

    public Completable setInstanceStatusToRequested(long id) {
        briteSurveyDbAdapter.updateSurveyInstanceStatus(id, SurveyInstanceStatus.SUBMIT_REQUESTED);
        return Completable.complete();
    }

    public Single<Cursor> getPendingSurveyInstances() {
        return Single.just(briteSurveyDbAdapter
                .getSurveyInstancesByStatus(SurveyInstanceStatus.SUBMIT_REQUESTED));
    }

    public Completable setInstanceStatusToSubmitted(long id) {
        briteSurveyDbAdapter.updateSurveyInstanceStatus(id, SurveyInstanceStatus.SUBMITTED);
        return Completable.complete();
    }

    public Single<Cursor> getResponses(Long surveyInstanceId) {
        return Single.just(briteSurveyDbAdapter.getResponses(surveyInstanceId));
    }

    public Completable createTransmissions(final Long instanceId, final String formId,
            Set<String> filenames) {
        if (filenames == null || filenames.isEmpty()) {
            return Completable.complete();
        }
        briteSurveyDbAdapter.createTransmissions(instanceId, formId, filenames);
        return Completable.complete();
    }

    public Observable<Boolean> installTestForm() {
        briteSurveyDbAdapter.installTestForm();
        return Observable.just(true);
    }

    public Observable<Boolean> insertSurveyGroup(ApiFormHeader apiFormHeader) {
        ContentValues values = new ContentValues();
        values.put(SurveyGroupColumns.SURVEY_GROUP_ID, apiFormHeader.getGroupId());
        values.put(SurveyGroupColumns.NAME, apiFormHeader.getGroupName());
        values.put(SurveyGroupColumns.REGISTER_SURVEY_ID, apiFormHeader.getRegistrationSurveyId());
        values.put(SurveyGroupColumns.MONITORED, apiFormHeader.isMonitored() ? 1 : 0);
        briteSurveyDbAdapter.addSurveyGroup(values);
        return Observable.just(true);
    }

    public Observable<Boolean> formNeedsUpdate(ApiFormHeader apiFormHeader) {
        final boolean surveyUpToDate = briteSurveyDbAdapter
                .isSurveyUpToDate(apiFormHeader.getId(), apiFormHeader.getVersion());
        return Observable.just(!surveyUpToDate);
    }

    public Observable<Boolean> insertSurvey(ApiFormHeader formHeader,
            boolean cascadeResourcesDownloaded, Form form) {
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(SurveyColumns.SURVEY_ID, formHeader.getId());
        String versionValue = form.getVersion() != null && !"0.0".equals(form.getVersion()) ?
                form.getVersion() :
                formHeader.getVersion();
        updatedValues.put(SurveyColumns.VERSION, versionValue);
        updatedValues.put(SurveyColumns.TYPE, DEFAULT_SURVEY_TYPE);
        updatedValues.put(SurveyColumns.LOCATION, DEFAULT_SURVEY_LOCATION);
        updatedValues.put(SurveyColumns.FILENAME, formHeader.getId() + FlowFileBrowser.XML_SUFFIX);
        updatedValues.put(SurveyColumns.NAME, formHeader.getName());
        updatedValues.put(SurveyColumns.LANGUAGE, getFormLanguage(formHeader));
        updatedValues.put(SurveyColumns.SURVEY_GROUP_ID, formHeader.getGroupId());
        updatedValues.put(SurveyColumns.HELP_DOWNLOADED, cascadeResourcesDownloaded ? 1 : 0);
        briteSurveyDbAdapter.updateSurvey(updatedValues, formHeader.getId());
        return Observable.just(true);
    }

    public Observable<Boolean> deleteAllForms() {
        briteSurveyDbAdapter.deleteAllSurveys();
        return Observable.just(true);
    }

    @NonNull
    private String getFormLanguage(ApiFormHeader formHeader) {
        final String language = formHeader != null ? formHeader.getLanguage() : "";
        return TextUtils.isEmpty(language) ? DEFAULTS_SURVEY_LANGUAGE : language.toLowerCase();
    }

    public Observable<Boolean> saveMissingFiles(Set<String> missingFiles) {
        if (missingFiles == null || missingFiles.isEmpty()) {
            return Observable.just(true);
        }
        briteSurveyDbAdapter.updateFailedTransmissions(missingFiles);
        return Observable.just(true);
    }

    public Single<Pair<Boolean, String>> getFormMetaData(String formId) {
        return briteSurveyDbAdapter.getFormMetaData(formId).map(formMetadataMapper::mapForm);
    }

    public Single<Long> fetchSurveyInstance(String formId, String dataPointId, String formVersion, long userId,
            String userName) {
        return briteSurveyDbAdapter.fetchOrCreateFormInstance(formId, dataPointId, formVersion, userId, userName);
    }

    @NotNull
    public Single<Set<String>> getSavedLanguages(long surveyId) {
        return briteSurveyDbAdapter.getSavedLanguages(surveyId)
                .map(formLanguagesMapper::transform);
    }

    @NotNull
    public Completable saveLanguages(long surveyId, @NotNull Set<String> languages) {
        briteSurveyDbAdapter.saveLanguagePreferences(surveyId, languages);
        return Completable.complete();
    }
}
