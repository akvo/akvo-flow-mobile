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

public class DataPoint {

    private final String id;
    private final String name;
    private final long lastModified;
    private final long surveyGroupId;
    private final Double latitude;
    private final Double longitude;
    private final int status;

    public DataPoint(String id, String name, long lastModified, long surveyGroupId, Double latitude,
            Double longitude, int status) {
        this.id = id;
        this.name = name;
        this.lastModified = lastModified;
        this.surveyGroupId = surveyGroupId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getSurveyGroupId() {
        return surveyGroupId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public int getStatus() {
        return status;
    }
}
