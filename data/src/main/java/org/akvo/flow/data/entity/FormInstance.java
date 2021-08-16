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

import java.util.ArrayList;
import java.util.List;

public class FormInstance {

    private final String uuid;
    private final String dataPointId;
    private final String deviceId;
    private final String username;
    private final String email;
    private final String formId;
    private final long submissionDate;
    private final long duration;
    private final double formVersion;
    private final List<Response> responses = new ArrayList<>();

    public FormInstance(String uuid, String dataPointId, String deviceId, String username,
            String email, String formId, long submissionDate, long duration, double formVersion) {
        this.uuid = uuid;
        this.dataPointId = dataPointId;
        this.deviceId = deviceId;
        this.username = username;
        this.email = email;
        this.formId = formId;
        this.submissionDate = submissionDate;
        this.duration = duration;
        this.formVersion = formVersion;
    }

    public List<Response> getResponses() {
        return responses;
    }

    public String getUsername() {
        return username;
    }

    public String getFormId() {
        return formId;
    }

    public String getUUID() {
        return uuid;
    }

    public String getDataPointId() {
        return dataPointId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getEmail() {
        return email;
    }

    public long getSubmissionDate() {
        return submissionDate;
    }

    public long getDuration() {
        return duration;
    }

    public double getFormVersion() {
        return formVersion;
    }
}
