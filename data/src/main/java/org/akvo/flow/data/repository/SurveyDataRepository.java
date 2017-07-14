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
import org.akvo.flow.data.entity.SurveyMapper;
import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.entity.Survey;
import org.akvo.flow.domain.repository.SurveyRepository;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

public class SurveyDataRepository implements SurveyRepository {

    private final DataSourceFactory dataSourceFactory;
    private final SurveyMapper surveyMapper;

    @Inject
    public SurveyDataRepository(DataSourceFactory dataSourceFactory, SurveyMapper surveyMapper) {
        this.dataSourceFactory = dataSourceFactory;
        this.surveyMapper = surveyMapper;
    }

    @Override
    public Observable<List<Survey>> getSurveys() {
        return dataSourceFactory.getDataBaseDataSource().getSurveys().map(
                new Func1<Cursor, List<Survey>>() {
                    @Override
                    public List<Survey> call(Cursor cursor) {
                        return surveyMapper.getSurveys(cursor);
                    }
                });
    }

    @Override
    public Observable<Boolean> deleteSurvey(long surveyToDeleteId) {
        return dataSourceFactory.getDataBaseDataSource().deleteSurvey(surveyToDeleteId);
    }

    @Override
    public Observable<List<DataPoint>> getDataPoints(Long surveyGroupId, Double latitude,
            Double longitude, Integer orderBy) {
        return null;
    }

    @Override
    public Observable<Integer> syncRemoteDataPoints(long surveyGroupId) {
        return null;
    }
}