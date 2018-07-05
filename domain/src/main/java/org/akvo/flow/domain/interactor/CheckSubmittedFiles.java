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

package org.akvo.flow.domain.interactor;

import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import org.akvo.flow.domain.entity.InstanceIdUuid;
import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.FileRepository;
import org.akvo.flow.domain.repository.SurveyRepository;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import timber.log.Timber;

public class CheckSubmittedFiles extends UseCase {

    private final SurveyRepository surveyRepository;
    private final FileRepository fileRepository;

    @Inject
    public CheckSubmittedFiles(@Nullable ThreadExecutor threadExecutor,
            @Nullable PostExecutionThread postExecutionThread,
            SurveyRepository surveyRepository, FileRepository fileRepository) {
        super(threadExecutor, postExecutionThread);
        this.surveyRepository = surveyRepository;
        this.fileRepository = fileRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        return surveyRepository.getSubmittedInstances()
                .flatMap(new Function<List<InstanceIdUuid>, Observable<List<Boolean>>>() {
                    @Override
                    public Observable<List<Boolean>> apply(List<InstanceIdUuid> instanceIdUuids) {
                        return checkExistingFiles(instanceIdUuids);
                    }
                });
    }

    private Observable<List<Boolean>> checkExistingFiles(List<InstanceIdUuid> instanceIdUuids) {
        return Observable.fromIterable(instanceIdUuids)
                .flatMap(new Function<InstanceIdUuid, Observable<Boolean>>() {
                            @Override
                            public Observable<Boolean> apply(InstanceIdUuid instanceIdUuid) {
                                return updateMissingFileInstances(instanceIdUuid);
                            }
                        })
                .toList().toObservable();
    }

    private Observable<Boolean> updateMissingFileInstances(final InstanceIdUuid instanceIdUuid) {
        return fileRepository.getZipFile(instanceIdUuid.getUuid())
                .map(new Function<File, Pair<InstanceIdUuid, File>>() {
                    @Override
                    public Pair<InstanceIdUuid, File> apply(File file) {
                        return new Pair<>(instanceIdUuid, file);
                    }
                })
                .filter(new Predicate<Pair<InstanceIdUuid, File>>() {
                    @Override
                    public boolean test(Pair<InstanceIdUuid, File> pair) {
                        return !pair.second.exists();
                    }
                })
                .flatMap(new Function<Pair<InstanceIdUuid, File>, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(Pair<InstanceIdUuid, File> idUuidPair) {
                        long surveyInstanceId = idUuidPair.first.getId();
                        Timber.d("Exported file for survey %s not found. It's status " +
                                        "will be set to 'submitted', and will be reprocessed",
                                surveyInstanceId);
                        return surveyRepository.setInstanceStatusToRequested(surveyInstanceId);
                    }
                });
    }
}
