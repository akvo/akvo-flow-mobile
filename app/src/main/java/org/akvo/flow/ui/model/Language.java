/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.ui.model;

public class Language {

    private final String languageCode;
    private final String language;
    private boolean selected;

    public Language(String languageCode, String language, boolean selected) {
        this.languageCode = languageCode;
        this.language = language;
        this.selected = selected;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getLanguage() {
        return language;
    }


    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Used by the {@link org.akvo.flow.activity.FormActivity.LanguageAdapter} to display text to
     * the user
     * @return
     */
    @Override
    public String toString() {
        return language;
    }
}
