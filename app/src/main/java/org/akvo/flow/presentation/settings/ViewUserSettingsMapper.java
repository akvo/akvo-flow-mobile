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

package org.akvo.flow.presentation.settings;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.akvo.flow.domain.entity.UserSettings;

import java.util.List;

import javax.inject.Inject;

public class ViewUserSettingsMapper {

    @Inject
    public ViewUserSettingsMapper() {
    }

    public ViewUserSettings transform(@Nullable UserSettings userSettings,
            @NonNull List<String> languages) {
        if (userSettings == null) {
            int englishPosition = getLanguagePosition(languages, "en");
            return new ViewUserSettings(false, false, englishPosition, 0, "");
        }
        return new ViewUserSettings(userSettings.isScreenOn(), userSettings.isDataEnabled(),
                getLanguagePosition(languages, userSettings.getLanguage()),
                userSettings.getImageSize(), userSettings.getIdentifier());
    }

    private int getLanguagePosition(@NonNull List<String> languages, String language) {
        int languagePosition = 0;
        for (int i = 0; i < languages.size(); i++) {
            if (language.equals(languages.get(i))) {
                languagePosition = i;
                break;
            }
        }
        return languagePosition;
    }
}
