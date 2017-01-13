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

import java.util.HashMap;

/**
 * simple data structure for representing question options.
 * 
 * @author Christopher Fagiani
 */
public class Option {
    private String text;
    private String code;
    private boolean isOther;
    private HashMap<String, AltText> altTextMap = new HashMap<>();

    public Option() {
    }

    /**
     * Copy constructor
     */
    public Option(Option option) {
        this.text = option.getText();
        this.code = option.getCode();
        this.isOther = option.isOther();
        for (AltText altText : option.altTextMap.values()) {
            addAltText(new AltText(altText));// Deep-copy AltText map
        }
    }

    public void addAltText(AltText altText) {
        altTextMap.put(altText.getLanguage(), altText);
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setIsOther(boolean isOther) {
        this.isOther = isOther;
    }

    public boolean isOther() {
        return isOther;
    }

    @Override
    public boolean equals(Object option) {
        if (option == null || !(option instanceof Option)) {
            return false;
        }

        return text != null && text.equals(((Option) option).getText());
    }

}
