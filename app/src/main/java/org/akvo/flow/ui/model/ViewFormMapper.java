/*
 * Copyright (C) 2010-2017,2019 Stichting Akvo (Akvo Foundation)
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.akvo.flow.data.loader.models.FormInfo;
import org.akvo.flow.domain.SurveyGroup;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ViewFormMapper {

    @NonNull
    public List<ViewForm> transform(@Nullable List<FormInfo> forms, SurveyGroup mSurveyGroup,
            String deletedString) {
        int capacity = forms == null ? 0 : forms.size();
        List<ViewForm> viewForms = new ArrayList<>(capacity);
        if (forms == null) {
            return viewForms;
        }
        for (FormInfo formInfo : forms) {
            if (formInfo != null) {
                viewForms.add(transform(formInfo, mSurveyGroup, deletedString));
            }
        }
        return viewForms;
    }

    @VisibleForTesting
    ViewForm transform(@NonNull FormInfo formInfo, SurveyGroup mSurveyGroup,
            String deletedString) {
        String surveyExtraInfo = getExtraInfo(formInfo, deletedString);
        String time = getTime(formInfo);
        boolean enabledStatus = getEnabledStatus(formInfo, mSurveyGroup);
        return new ViewForm(formInfo.getId(), formInfo.getName(), surveyExtraInfo, time,
                enabledStatus);
    }

    @Nullable
    private String getTime(@NonNull FormInfo formInfo) {
        if (formInfo.getLastSubmission() != null && !formInfo.isRegistrationForm()) {
            return new PrettyTime().format(new Date(formInfo.getLastSubmission()));
        }
        return null;
    }

    @NonNull
    private String getExtraInfo(@NonNull FormInfo formInfo, String deletedString) {
        String surveyExtraInfo;
        StringBuilder surveyExtraInfoBuilder = new StringBuilder(20);
        String version = formInfo.getVersion();
        if (!TextUtils.isEmpty(version)) {
            surveyExtraInfoBuilder.append(" v").append(version);
        }
        if (formInfo.isDeleted()) {
            surveyExtraInfoBuilder.append(" - ").append(deletedString);
        }
        surveyExtraInfo = surveyExtraInfoBuilder.toString();
        return surveyExtraInfo;
    }

    private boolean getEnabledStatus(@NonNull FormInfo formInfo, SurveyGroup mSurveyGroup) {
        return !formInfo.isDeleted() && isFormEnabled(mSurveyGroup, formInfo);
    }

    private boolean isFormEnabled(SurveyGroup surveyGroup, FormInfo formInfo) {
        if (surveyGroup.isMonitored() && !formInfo.isRegistrationForm()) {
            return formInfo.isSubmittedDataPoint();
        } else {
            return !formInfo.hasBeenSubmitted();
        }
    }
}
