/*
 *  Copyright (C) 2010-2019 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.domain;

import androidx.annotation.Nullable;

import org.akvo.flow.serialization.response.value.CascadeValue;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.utils.entity.OptionValue;

public class QuestionResponse {

    public static final int NO_ITERATION = -1;
    private static final int ONE_ITERATION = 0;

    private final String value;
    private final String type;
    private final Long id;
    private final Long surveyInstanceId;
    private final String questionId;
    private final String filename;
    private final boolean includeFlag;
    private final int iteration;

    private QuestionResponse(String value, String type, Long id, Long surveyInstanceId,
            String questionId, String filename, boolean includeFlag, int iteration) {
        this.value = value;
        this.type = type;
        this.id = id;
        this.surveyInstanceId = surveyInstanceId;
        this.questionId = questionId;
        this.filename = filename;
        this.includeFlag = includeFlag;
        this.iteration = iteration;
    }

    public boolean getIncludeFlag() {
        return includeFlag;
    }

    public boolean isValid() {
        // We have to check that it has a value that isn't just a blank
        if (value == null || value.trim().length() == 0) {
            return false;
        }
        // now check that, if it's a geo question, we have something specified
        if (ConstantUtil.GEO_RESPONSE_TYPE.equals(type)) {
            String[] tokens = value.split("\\|", -1);
            if (tokens.length >= 2) {
                // at least the first 2 tokens must be numeric
                for (int i = 0; i < 2; i++) {
                    String token = tokens[i];
                    try {
                        if (token.trim().length() > 0) {
                            Double.parseDouble(token);
                        } else {
                            return false;
                        }
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public String getQuestionId() {
        return questionId;
    }

    public Long getSurveyInstanceId() {
        return surveyInstanceId;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public boolean hasValue() {
        boolean hasVal = false;
        if (value != null && value.trim().length() > 0) {
            hasVal = true;
        }
        return hasVal;
    }

    /**
     * Build a human-readable representation of the response.
     * Based on the response type, this means handling and parsing each value in a different way.
     */
    public String getDatapointNameValue() {
        if (type == null || value == null) {
            return "";
        }

        String name;
        switch (type) {
            case ConstantUtil.CASCADE_RESPONSE_TYPE:
                name = CascadeValue.getDatapointName(value);
                break;
            case ConstantUtil.OPTION_RESPONSE_TYPE:
                name = OptionValue.getDatapointName(value);
                break;
            default:
                name = value;
                break;
        }

        name = name.replaceAll("\\s+", " ");// Trim line breaks, multiple spaces, etc
        name = name.replaceAll("\\s*\\|\\s*", " - ");// Replace pipes with hyphens

        return name.trim();
    }

    public int getIteration() {
        return iteration;
    }

    /**
     * The key is usually the questionId except for questions that belong to a repeatable group
     * whose key is composed by questionId|iteration
     */
    public String getResponseKey() {
        String responseKey = getQuestionId();
        if (getIteration() > ONE_ITERATION) {
            responseKey = responseKey + "|" + getIteration();
        }
        return responseKey;
    }

    public boolean isAnswerToRepeatableGroup() {
        return iteration != NO_ITERATION;
    }

    public static class QuestionResponseBuilder {

        private String value;
        private String type;
        private Long id;
        private Long surveyInstanceId;
        private String questionId;
        private String filename;
        private boolean includeFlag = true;
        private int iteration = NO_ITERATION;

        public QuestionResponseBuilder setValue(String value) {
            this.value = value;
            return this;
        }

        public QuestionResponseBuilder setType(String type) {
            this.type = type;
            return this;
        }

        public QuestionResponseBuilder setId(Long id) {
            this.id = id;
            return this;
        }

        public QuestionResponseBuilder setSurveyInstanceId(Long surveyInstanceId) {
            this.surveyInstanceId = surveyInstanceId;
            return this;
        }

        public QuestionResponseBuilder setQuestionId(String questionId) {
            this.questionId = questionId;
            return this;
        }

        public QuestionResponseBuilder setFilename(String filename) {
            this.filename = filename;
            return this;
        }

        public QuestionResponseBuilder setIncludeFlag(boolean includeFlag) {
            this.includeFlag = includeFlag;
            return this;
        }

        public QuestionResponseBuilder setIteration(int iteration) {
            this.iteration = iteration;
            return this;
        }

        public QuestionResponse createQuestionResponse() {
            return new QuestionResponse(value, type, id, surveyInstanceId, questionId, filename,
                    includeFlag, iteration);
        }

        @Nullable
        public QuestionResponse createFromQuestionResponse(
                @Nullable QuestionResponse questionResponse,
                long id) {
            if (questionResponse == null) {
                return null;
            }
            return setId(id)
                    .setValue(questionResponse.getValue())
                    .setIteration(questionResponse.getIteration())
                    .setFilename(questionResponse.getFilename())
                    .setIncludeFlag(questionResponse.getIncludeFlag())
                    .setQuestionId(questionResponse.getQuestionId())
                    .setSurveyInstanceId(questionResponse.getSurveyInstanceId())
                    .setType(questionResponse.getType())
                    .createQuestionResponse();
        }

        @Nullable
        public QuestionResponse createFromQuestionResponse(
                @Nullable QuestionResponse questionResponse,
                boolean includeFlag) {
            if (questionResponse == null) {
                return null;
            }
            return setId(questionResponse.getId())
                    .setValue(questionResponse.getValue())
                    .setIteration(questionResponse.getIteration())
                    .setFilename(questionResponse.getFilename())
                    .setIncludeFlag(includeFlag)
                    .setQuestionId(questionResponse.getQuestionId())
                    .setSurveyInstanceId(questionResponse.getSurveyInstanceId())
                    .setType(questionResponse.getType())
                    .createQuestionResponse();
        }

        /**
         * Return new response object using ids from the existing response
         */
        public QuestionResponse createFromQuestionResponse(@Nullable QuestionResponse oldResponse,
                @Nullable QuestionResponse newResponse) {
            if (oldResponse == null && newResponse == null) {
                return null;
            } else if (oldResponse == null) {
                return newResponse;
            } else if (newResponse == null) {
                return null;
            } else {
                return setId(oldResponse.getId())
                        .setValue(newResponse.getValue())
                        .setIteration(oldResponse.getIteration())
                        .setFilename(newResponse.getFilename())
                        .setIncludeFlag(oldResponse.getIncludeFlag())
                        .setQuestionId(oldResponse.getQuestionId())
                        .setSurveyInstanceId(oldResponse.getSurveyInstanceId())
                        .setType(newResponse.getType())
                        .createQuestionResponse();
            }
        }

        public QuestionResponse createFromQuestionResponse(@Nullable QuestionResponse newResponse) {
            if (newResponse != null) {
                return setValue(newResponse.getValue())
                        .setIteration(newResponse.getIteration())
                        .setFilename(newResponse.getFilename())
                        .setIncludeFlag(newResponse.getIncludeFlag())
                        .setQuestionId(newResponse.getQuestionId())
                        .setSurveyInstanceId(newResponse.getSurveyInstanceId())
                        .setType(newResponse.getType())
                        .createQuestionResponse();
            }
            return null;
        }
    }
}
