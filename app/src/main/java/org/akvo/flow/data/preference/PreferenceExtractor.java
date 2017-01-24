/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.preference;

import android.database.sqlite.SQLiteDatabase;

import org.akvo.flow.util.ConstantUtil;

public class PreferenceExtractor {

    public MigratablePreferences create(PreferenceHandler preferenceHandler, SQLiteDatabase db) {
        String deviceIdentifier = preferenceHandler
                .findPreference(db, ConstantUtil.DEVICE_IDENT_KEY);
        String celularData = preferenceHandler
                .findPreference(db, ConstantUtil.CELL_UPLOAD_SETTING_KEY);
        String screenOn = preferenceHandler.findPreference(db, ConstantUtil.SCREEN_ON_KEY);
        String imageSize = preferenceHandler.findPreference(db, ConstantUtil.MAX_IMG_SIZE);
        return new MigratablePreferences(deviceIdentifier, celularData, screenOn, imageSize);
    }
}
