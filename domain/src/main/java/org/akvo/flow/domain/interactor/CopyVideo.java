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

import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.FileRepository;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class CopyVideo extends UseCase {

    public static final String ORIGIN_FILE_NAME_PARAM = "original_file";
    public static final String DESTINATION_FILE_NAME_PARAM = "target_file";

    private final FileRepository fileRepository;

    @Inject
    protected CopyVideo(ThreadExecutor threadExecutor,
            PostExecutionThread postExecutionThread, FileRepository fileRepository) {
        super(threadExecutor, postExecutionThread);
        this.fileRepository = fileRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        if (parameters == null || !parameters.containsKey(ORIGIN_FILE_NAME_PARAM)
                || !parameters.containsKey(DESTINATION_FILE_NAME_PARAM)) {
            return Observable.error(new IllegalArgumentException("Missing params"));
        }

        final String originFilePath = (String) parameters.get(ORIGIN_FILE_NAME_PARAM);
        final String destinationFilePath = (String) parameters.get(DESTINATION_FILE_NAME_PARAM);
        return fileRepository.copyFile(originFilePath, destinationFilePath)
                .concatMap(new Function<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> apply(Boolean aBoolean) {
                        return fileRepository.removeFile(originFilePath);
                    }
                });
    }
}
