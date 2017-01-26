/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.util.ConstantUtil;

import timber.log.Timber;

/**
 * rule used to translate a free text response into some other type of value.
 * 
 * @author Christopher Fagiani
 */
public class ScoringRule {

    private String type;
    private String min;
    private String max;
    private String value;
    private String text;

    public ScoringRule(String type, String min, String max, String text,
            String val) {
        this.type = type;
        this.min = min;
        this.max = max;
        this.value = val;
        this.text = text;
        if (type == null) {
            this.type = ConstantUtil.NUMERIC_SCORING;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    /**
     * attempts to translate the response passed in into a scored value. If the
     * response does not fall within the min/max range designated on this rule,
     * this method will return null. rules are considered satisfied if min <=
     * value <= max or, if textmatch, if text matches response
     * 
     * @param response
     * @return
     */
    public String scoreResponse(String response) {
        String score = null;
        if (response != null) {
            if (ConstantUtil.NUMERIC_SCORING.equalsIgnoreCase(type)) {
                try {
                    Double responseNum = Double.parseDouble(response.trim());
                    if (min == null || responseNum >= Double.parseDouble(min)) {
                        if (max == null
                                || responseNum <= Double.parseDouble(max)) {
                            score = value;
                        }
                    }
                } catch (NumberFormatException e) {
                    Timber.e(e, "Can't perform numeric scoring");
                }
            } else if (ConstantUtil.TEXT_MATCH_SCORING.equalsIgnoreCase(type)) {
                if (response.equals(text)) {
                    score = value;
                }
            }
        }
        return score;
    }
}
