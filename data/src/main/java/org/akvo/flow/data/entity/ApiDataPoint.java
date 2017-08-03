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

package org.akvo.flow.data.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ApiDataPoint {

    private String displayName;
    private String id;

    @SerializedName("lat")
    private Double latitude;

    @SerializedName("lon")
    private Double longitude;

    private long surveyGroupId;

    @SerializedName("lastUpdateDateTime")
    private long lastModified;

    private List<ApiSurveyInstance> surveyInstances;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public long getSurveyGroupId() {
        return surveyGroupId;
    }

    public void setSurveyGroupId(long surveyGroupId) {
        this.surveyGroupId = surveyGroupId;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public List<ApiSurveyInstance> getSurveyInstances() {
        return surveyInstances;
    }

    public void setSurveyInstances(List<ApiSurveyInstance> surveyInstances) {
        this.surveyInstances = surveyInstances;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ApiDataPoint that = (ApiDataPoint) o;
        return id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
