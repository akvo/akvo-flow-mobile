/*
 *  Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.serialization.response.value.CascadeValue;
import org.akvo.flow.serialization.response.value.OptionValue;
import org.akvo.flow.util.ConstantUtil;

public class QuestionResponse {
    private String value;
    private String type;
    private Long id;
    private Long respondentId;
    private String questionId;
    private String filename;
    private boolean includeFlag;

    public QuestionResponse(String val, String t, String questionId) {
        this.value = val;
        this.type = t;
        this.questionId = questionId;
        this.includeFlag = true;
    }

    public QuestionResponse() {
        id = null;
        type = null;
        value = null;
        respondentId = null;
        questionId = null;
        includeFlag = true;
    }

    public boolean getIncludeFlag() {
        return includeFlag;
    }

    public void setIncludeFlag(boolean includeFlag) {
        this.includeFlag = includeFlag;
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

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public Long getRespondentId() {
        return respondentId;
    }

    public void setRespondentId(Long respondentId) {
        this.respondentId = respondentId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
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

}
