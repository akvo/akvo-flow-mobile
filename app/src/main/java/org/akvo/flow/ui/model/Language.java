/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.model;

import org.akvo.flow.ui.adapter.LanguageAdapter;

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
     * Used by the {@link LanguageAdapter} to display text to
     * the user
     * @return
     */
    @Override
    public String toString() {
        return language;
    }
}
