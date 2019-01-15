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

package org.akvo.flow.data.entity;

public class ApiFormHeader {

    private final String id;
    private final String name;
    private final String language;
    private final String version;
    private final double groupId;
    private final String groupName;
    private final boolean monitored;
    private final String registrationSurveyId;

    public ApiFormHeader(String id, String name, String language, String version, double groupId,
            String groupName, boolean monitored, String registrationSurveyId) {
        this.id = id;
        this.name = name;
        this.language = language;
        this.version = version;
        this.groupId = groupId;
        this.groupName = groupName;
        this.monitored = monitored;
        this.registrationSurveyId = registrationSurveyId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLanguage() {
        return language;
    }

    public String getVersion() {
        return version;
    }

    public double getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public boolean isMonitored() {
        return monitored;
    }

    public String getRegistrationSurveyId() {
        return registrationSurveyId;
    }
}
