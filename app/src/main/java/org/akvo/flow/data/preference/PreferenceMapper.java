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

import android.support.annotation.Nullable;
import android.text.TextUtils;

public class PreferenceMapper {

    @Nullable
    public InsertablePreferences transform(@Nullable MigratablePreferences migratablePreferences) {
        if (migratablePreferences == null) {
            return null;
        }

        String deviceIdentifier = migratablePreferences.getDeviceIdentifier();
        boolean cellularUpload = Prefs.DEFAULT_CELLULAR_DATA_UPLOAD;
        if (!TextUtils.isEmpty(migratablePreferences.getCellularDataUpload())) {
            cellularUpload = "true".equals(migratablePreferences.getCellularDataUpload());
        }
        boolean screenOn = Prefs.DEFAULT_SCREEN_ON;
        if (!TextUtils.isEmpty(migratablePreferences.getScreenOn())) {
            screenOn = "true".equals(migratablePreferences.getScreenOn());
        }
        int imgSize = Prefs.DEFAULT_IMAGE_SIZE;
        if (!TextUtils.isEmpty(migratablePreferences.getImageSize())) {
            imgSize = Integer.parseInt(migratablePreferences.getImageSize());
        }
        return new InsertablePreferences(deviceIdentifier, cellularUpload, screenOn, imgSize);
    }
}
