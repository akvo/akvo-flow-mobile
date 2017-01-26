/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.akvo.flow.data.preference.Prefs;

public class PreferenceMapper {

    @Nullable
    public InsertablePreferences transform(@Nullable MigratablePreferences migratablePreferences) {
        if (migratablePreferences == null) {
            return null;
        }

        String deviceIdentifier = migratablePreferences.getDeviceIdentifier();
        boolean cellularUpload = Prefs.DEFAULT_VALUE_CELL_UPLOAD;
        if (!TextUtils.isEmpty(migratablePreferences.getCellularDataUpload())) {
            cellularUpload = "true".equals(migratablePreferences.getCellularDataUpload());
        }
        boolean screenOn = Prefs.DEFAULT_VALUE_SCREEN_ON;
        if (!TextUtils.isEmpty(migratablePreferences.getScreenOn())) {
            screenOn = "true".equals(migratablePreferences.getScreenOn());
        }
        int imgSize = Prefs.DEFAULT_VALUE_IMAGE_SIZE;
        if (!TextUtils.isEmpty(migratablePreferences.getImageSize())) {
            imgSize = Integer.parseInt(migratablePreferences.getImageSize());
        }
        return new InsertablePreferences(deviceIdentifier, cellularUpload, screenOn, imgSize);
    }
}
