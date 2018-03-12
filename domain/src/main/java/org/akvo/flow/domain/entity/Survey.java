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

package org.akvo.flow.domain.entity;

public class Survey {
    private final long id;
    private final String name;
    private final boolean isMonitored;
    private final String registrationSurveyId;

    public Survey(long id, String name, boolean isMonitored, String registrationSurveyId) {
        this.id = id;
        this.name = name;
        this.isMonitored = isMonitored;
        this.registrationSurveyId = registrationSurveyId;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isMonitored() {
        return isMonitored;
    }

    public String getRegistrationSurveyId() {
        return registrationSurveyId;
    }
}
