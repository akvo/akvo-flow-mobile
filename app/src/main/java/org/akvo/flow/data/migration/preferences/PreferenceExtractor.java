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

package org.akvo.flow.data.migration.preferences;

import android.database.sqlite.SQLiteDatabase;

import org.akvo.flow.database.PreferenceHandler;

import static org.akvo.flow.database.Constants.CELL_UPLOAD_SETTING_KEY;
import static org.akvo.flow.database.Constants.DEVICE_IDENT_KEY;
import static org.akvo.flow.database.Constants.MAX_IMG_SIZE;
import static org.akvo.flow.database.Constants.SCREEN_ON_KEY;

public class PreferenceExtractor {

    private final PreferenceHandler preferenceHandler = new PreferenceHandler();

    public PreferenceExtractor() {
    }

    public MigratablePreferences retrievePreferences(SQLiteDatabase db) {
        String deviceIdentifier = preferenceHandler
                .findPreference(db, DEVICE_IDENT_KEY);
        String cellularData = preferenceHandler
                .findPreference(db, CELL_UPLOAD_SETTING_KEY);
        String screenOn = preferenceHandler.findPreference(db, SCREEN_ON_KEY);
        String imageSize = preferenceHandler.findPreference(db, MAX_IMG_SIZE);
        return new MigratablePreferences(deviceIdentifier, cellularData, screenOn, imageSize);
    }
}
