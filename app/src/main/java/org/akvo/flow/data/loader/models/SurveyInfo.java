/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.data.loader.models;

/**
 * Wrapper for the data displayed in the list
 */
public class SurveyInfo {

    private final String id;
    private final String name;
    private final String version;
    private final Long lastSubmission;
    private final boolean deleted;
    private final boolean isRegistrationSurvey;

    public SurveyInfo(String id, String name, String version, Long lastSubmission,
            boolean deleted, boolean isRegistrationSurvey) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.lastSubmission = lastSubmission;
        this.deleted = deleted;
        this.isRegistrationSurvey = isRegistrationSurvey;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Long getLastSubmission() {
        return lastSubmission;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isRegistrationSurvey() {
        return isRegistrationSurvey;
    }
}
