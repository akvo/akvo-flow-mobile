/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.akvo.flow.data.loader.models.SurveyInfo;
import org.akvo.flow.domain.SurveyGroup;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ViewFormMapper {

    @NonNull
    public List<ViewForm> transform(@Nullable List<SurveyInfo> forms, SurveyGroup mSurveyGroup,
            String deletedString) {
        int capacity = forms == null ? 0 : forms.size();
        List<ViewForm> viewForms = new ArrayList<>(capacity);
        if (forms == null) {
            return viewForms;
        }
        for (SurveyInfo surveyInfo : forms) {
            if (surveyInfo != null) {
                viewForms.add(transform(surveyInfo, mSurveyGroup, deletedString));
            }
        }
        return viewForms;
    }

    private ViewForm transform(@NonNull SurveyInfo surveyInfo, SurveyGroup mSurveyGroup,
            String deletedString) {
        String surveyExtraInfo = getExtraInfo(surveyInfo, deletedString);
        String time = getTime(surveyInfo);
        boolean enabledStatus = getEnabledStatus(surveyInfo, mSurveyGroup);
        return new ViewForm(surveyInfo.getId(), surveyInfo.getName(), surveyExtraInfo, time,
                enabledStatus);
    }

    @Nullable
    private String getTime(@NonNull SurveyInfo surveyInfo) {
        if (surveyInfo.getLastSubmission() != null && !surveyInfo.isRegistrationSurvey()) {
            return new PrettyTime().format(new Date(surveyInfo.getLastSubmission()));
        }
        return null;
    }

    @NonNull
    private String getExtraInfo(@NonNull SurveyInfo surveyInfo, String deletedString) {
        String surveyExtraInfo;
        StringBuilder surveyExtraInfoBuilder = new StringBuilder(20);
        String version = surveyInfo.getVersion();
        if (!TextUtils.isEmpty(version)) {
            surveyExtraInfoBuilder.append(" v").append(version);
        }
        if (surveyInfo.isDeleted()) {
            surveyExtraInfoBuilder.append(" - ").append(deletedString);
        }
        surveyExtraInfo = surveyExtraInfoBuilder.toString();
        return surveyExtraInfo;
    }

    private boolean getEnabledStatus(@NonNull SurveyInfo surveyInfo, SurveyGroup mSurveyGroup) {
        return !surveyInfo.isDeleted() && isFormEnabled(mSurveyGroup, surveyInfo);
    }

    private boolean isFormEnabled(SurveyGroup surveyGroup, SurveyInfo surveyInfo) {
        if (surveyGroup.isMonitored() && !surveyInfo.isRegistrationSurvey()) {
            return surveyInfo.isSubmittedDataPoint();
        } else {
            return !surveyInfo.hasBeenSubmitted();
        }
    }
}
