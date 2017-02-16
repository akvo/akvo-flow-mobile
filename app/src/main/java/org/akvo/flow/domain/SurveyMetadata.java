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

package org.akvo.flow.domain;

public class SurveyMetadata {

    private String id;
    private String app;
    private String name;
    private SurveyGroup surveyGroup;
    private double version;

    public SurveyMetadata() {
    }

    public String getId() {
        return id;
    }

    public String getApp() {
        return app;
    }

    public String getName() {
        return name;
    }

    public SurveyGroup getSurveyGroup() {
        return surveyGroup;
    }

    public double getVersion() {
        return version;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSurveyGroup(SurveyGroup surveyGroup) {
        this.surveyGroup = surveyGroup;
    }

    public void setVersion(double version) {
        this.version = version;
    }
}
