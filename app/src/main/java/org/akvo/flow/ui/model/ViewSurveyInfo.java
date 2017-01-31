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

public class ViewSurveyInfo {

    private final String id;
    private final String surveyName;
    private final String surveyExtraInfo;
    private final String time;
    private final boolean enabled;

    public ViewSurveyInfo(String id, String surveyName, String surveyExtraInfo, String time,
            boolean enabled) {
        this.id = id;
        this.surveyName = surveyName;
        this.surveyExtraInfo = surveyExtraInfo;
        this.time = time;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getSurveyName() {
        return surveyName;
    }

    public String getSurveyExtraInfo() {
        return surveyExtraInfo;
    }

    public String getTime() {
        return time;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
