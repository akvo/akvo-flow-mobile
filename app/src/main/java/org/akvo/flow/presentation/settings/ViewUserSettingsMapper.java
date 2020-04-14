/*
 * Copyright (C) 2017,2019 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.domain.entity.UserSettings;

import javax.inject.Inject;

import androidx.annotation.Nullable;

public class ViewUserSettingsMapper {

    public static final String DEFAULT_LANGUAGE = "en";
    public static final int INVALID_LANGUAGE = -1;

    @Inject
    public ViewUserSettingsMapper() {
    }

    public ViewUserSettings transform(@Nullable UserSettings userSettings) {
        if (userSettings == null) {
            return new ViewUserSettings(false, false, 0, "");
        }
        return new ViewUserSettings(userSettings.isScreenOn(), userSettings.isDataEnabled(),
                userSettings.getImageSize(), userSettings.getIdentifier());
    }
}
