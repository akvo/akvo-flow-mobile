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

public class ViewSurveyInfoMapper {

    public ViewSurveyInfo transform(@NonNull SurveyInfo surveyInfo, SurveyGroup mSurveyGroup,
            boolean mRegistered, String deletedString) {
        String surveyExtraInfo;
        String time = null;
        boolean enabled;
        StringBuilder surveyExtraInfoBuilder = new StringBuilder(20);
        String version = surveyInfo.getVersion();
        if (!TextUtils.isEmpty(version)) {
            surveyExtraInfoBuilder.append(" v").append(version);
        }
        enabled = isSurveyEnabled(surveyInfo, mSurveyGroup, mRegistered);
        if (surveyInfo.isDeleted()) {
            enabled = false;
            surveyExtraInfoBuilder.append(" - ").append(deletedString);
        }
        surveyExtraInfo = surveyExtraInfoBuilder.toString();
        if (surveyInfo.getLastSubmission() != null && !surveyInfo.isRegistrationSurvey()) {
            time = new PrettyTime().format(new Date(surveyInfo.getLastSubmission()));
        }
        return new ViewSurveyInfo(surveyInfo.getId(), surveyInfo.getName(), surveyExtraInfo, time,
                enabled);
    }

    @NonNull
    public List<ViewSurveyInfo> transform(@Nullable List<SurveyInfo> surveyInfos,
            SurveyGroup mSurveyGroup, boolean mRegistered, String deletedString) {
        int capacity = surveyInfos == null ? 0 : surveyInfos.size();
        List<ViewSurveyInfo> viewSurveyInfos = new ArrayList<>(capacity);
        if (surveyInfos == null) {
            return viewSurveyInfos;
        }
        for (SurveyInfo surveyInfo : surveyInfos) {
            if (surveyInfo != null) {
                viewSurveyInfos.add(transform(surveyInfo, mSurveyGroup, mRegistered,
                        deletedString));
            }
        }
        return viewSurveyInfos;
    }

    //TODO: this is a bit confusing
    private boolean isSurveyEnabled(SurveyInfo surveyInfo, SurveyGroup surveyGroup,
            boolean registered) {
        if (surveyGroup.isMonitored()) {
            return surveyInfo.isRegistrationSurvey() != registered;
        }
        return !registered;// Not monitored. Only one response allowed
    }
}
