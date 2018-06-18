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

import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.SurveyRepository;

import java.util.Map;

import io.reactivex.Observable;

public class UploadDataPoints extends ConfigurableThreadUseCase {

    public static final String KEY_SURVEY_ID = "survey_id";

    private final SurveyRepository surveyRepository;

    protected UploadDataPoints(@Nullable ThreadExecutor threadExecutor,
            @Nullable PostExecutionThread postExecutionThread,
            SurveyRepository surveyRepository) {
        super(threadExecutor, postExecutionThread);
        this.surveyRepository = surveyRepository;
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        String surveyId = parameters != null ? (String) parameters.get(KEY_SURVEY_ID) : null;
        return surveyRepository.getFormIds(surveyId);
    }
}
