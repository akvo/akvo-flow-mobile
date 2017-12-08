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

package org.akvo.flow.presentation.navigation;

import android.support.v4.util.Pair;

import org.akvo.flow.domain.entity.Survey;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.DeleteSurvey;
import org.akvo.flow.domain.interactor.SaveSelectedSurvey;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.presentation.Presenter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

public class FlowNavigationPresenter implements Presenter {

    private final UseCase getAllSurveys;
    private final UseCase deleteSurvey;
    private final UseCase saveSelectedSurvey;

    private final SurveyMapper surveyMapper;
    private final SurveyGroupMapper surveyGroupMapper;

    private IFlowNavigationView view;

    @Inject
    public FlowNavigationPresenter(@Named("getAllSurveys") UseCase getAllSurveys,
            SurveyMapper surveyMapper, @Named("deleteSurvey") UseCase deleteSurvey,
            SurveyGroupMapper surveyGroupMapper,
            @Named("saveSelectedSurvey") UseCase saveSelectedSurvey) {
        this.getAllSurveys = getAllSurveys;
        this.surveyMapper = surveyMapper;
        this.deleteSurvey = deleteSurvey;
        this.surveyGroupMapper = surveyGroupMapper;
        this.saveSelectedSurvey = saveSelectedSurvey;
    }

    @Override
    public void destroy() {
        getAllSurveys.dispose();
        deleteSurvey.dispose();
        saveSelectedSurvey.dispose();
    }

    public void setView(IFlowNavigationView view) {
        this.view = view;
    }

    public void load() {
        getAllSurveys.execute(new DefaultObserver<Pair<List<Survey>, Long>>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Error getting all surveys");
            }

            @Override
            public void onNext(Pair<List<Survey>, Long> result) {
                view.display(surveyMapper.transform(result.first), result.second);
            }
        }, null);
    }

    public void onDeleteSurvey(final long surveyGroupId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put(DeleteSurvey.SURVEY_ID_PARAM, surveyGroupId);
        deleteSurvey.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
                load();
            }

            @Override
            public void onNext(Boolean aBoolean) {
                view.notifySurveyDeleted(surveyGroupId);
                load();
            }
        }, params);
    }

    public void onSurveyItemTap(final ViewSurvey viewSurvey) {
        if (viewSurvey != null) {
            Map<String, Object> params = new HashMap<>(2);
            params.put(SaveSelectedSurvey.KEY_SURVEY_GROUP_ID, viewSurvey.getId());
            saveSelectedSurvey.execute(new DefaultObserver<Boolean>() {
                @Override
                public void onError(Throwable e) {
                    Timber.e(e);
                }

                @Override
                public void onNext(Boolean aBoolean) {
                    view.selectSurvey(surveyGroupMapper.transform(viewSurvey));
                }
            }, params);
        }

    }
}
