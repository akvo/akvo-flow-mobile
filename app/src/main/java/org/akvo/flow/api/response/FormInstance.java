package org.akvo.flow.api.response;

import java.util.List;

public class FormInstance {
    private String formId;
    private String formInstanceId;
    private String dataPointId;
    private String deviceId;
    private String username;
    private String email;
    private long submissionDate;
    private long duration;
    private List<Response> responses;

    public List<Response> getResponses() {
        return responses;
    }

    public void setResponses(List<Response> responses) {
        this.responses = responses;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getFormInstanceId() {
        return formInstanceId;
    }

    public void setFormInstanceId(String formInstanceId) {
        this.formInstanceId = formInstanceId;
    }

    public String getDataPointId() {
        return dataPointId;
    }

    public void setDataPointId(String dataPointId) {
        this.dataPointId = dataPointId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(long submissionDate) {
        this.submissionDate = submissionDate;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
