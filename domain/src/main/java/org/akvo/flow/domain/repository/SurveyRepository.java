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

package org.akvo.flow.domain.repository;

import androidx.annotation.NonNull;

import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.entity.FormInstanceMetadata;
import org.akvo.flow.domain.entity.InstanceIdUuid;
import org.akvo.flow.domain.entity.Survey;
import org.akvo.flow.domain.entity.TransmissionResult;
import org.akvo.flow.domain.entity.User;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface SurveyRepository {

    Observable<List<Survey>> getSurveys();

    Observable<List<DataPoint>> getDataPoints(Long surveyGroupId, Double latitude,
            Double longitude, Integer orderBy);

    Single<DataPoint> getDataPoint(String datapointId);

    Observable<Boolean> deleteSurvey(long surveyToDeleteId);

    Observable<List<User>> getUsers();

    Observable<Boolean> editUser(User user);

    Observable<Boolean> deleteUser(User user);

    Observable<Long> createUser(String userName);

    User getUser(Long userId);

    Observable<Boolean> clearResponses();

    Observable<Boolean> clearAllData();

    Observable<Boolean> unSyncedTransmissionsExist();

    Observable<List<String>> getAllTransmissionFileNames();

    Observable<Set<TransmissionResult>> processTransmissions(String deviceId, @NonNull String surveyId);

    Observable<Set<TransmissionResult>> processTransmissions(String deviceId);

    Single<List<InstanceIdUuid>> getSubmittedInstances();

    Completable setInstanceStatusToRequested(long id);

    Single<List<Long>> getPendingSurveyInstances();

    Single<FormInstanceMetadata> getFormInstanceData(Long instanceId, String deviceId);

    Completable createTransmissions(Long instanceId, String formId, Set<String> fileNames);

    Observable<List<String>> getFormIds(String surveyId);

    Observable<List<String>> getFormIds();

    @Nullable
    Completable setSurveyViewed(long surveyId);

    Completable cleanDataPoints(Long surveyGroupId);
}
