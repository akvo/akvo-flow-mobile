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

import java.util.HashMap;

/**
 * domain object for help media. If the type == tip, then value is undefined.
 * 
 * @author Christopher Fagiani
 */
public class QuestionHelp {
    private HashMap<String, AltText> altTextMap = new HashMap<String, AltText>();
    private String type;
    private String text;
    private String value;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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

    public HashMap<String, AltText> getAltTextMap() {
        return altTextMap;
    }

    public AltText getAltText(String lang) {
        return altTextMap.get(lang);
    }

    public void addAltText(AltText altText) {
        altTextMap.put(altText.getLanguage(), altText);
    }

    /**
     * checks whether this help object is well formed
     */
    public boolean isValid() {
        if (text == null || text.trim().length() == 0) {
            // if text is null, then value must be populated for this to be
            // valid
            if (value == null || value.trim().length() == 0) {
                return false;
            }
        } else {
            // if text is not null, then it can't be the string "null"
            if ("null".equalsIgnoreCase(text.trim())) {
                return false;
            }
        }

        return true;
    }
}
