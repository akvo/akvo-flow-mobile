/*
 *  Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
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
