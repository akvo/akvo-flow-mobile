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

package org.akvo.flow.data.loader.models;

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
