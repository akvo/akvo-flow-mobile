/*
 *  Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.domain;

import java.util.HashMap;

/**
 * Level represents a cascading question level. It just holds the level name (multilingual)
 */
public class Level {
    private String text;
    private HashMap<String, AltText> altTextMap = new HashMap<String, AltText>();

    public void addAltText(AltText altText) {
        altTextMap.put(altText.getLanguage(), altText);
    }

    public HashMap<String, AltText> getAltTextMap() {
        return altTextMap;
    }

    public AltText getAltText(String lang) {
        return altTextMap.get(lang);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
