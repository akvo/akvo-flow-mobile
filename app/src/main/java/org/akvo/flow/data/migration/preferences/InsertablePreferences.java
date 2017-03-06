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

/**
 * Preferences ready to be inserted to SharedPreference
 */
public class InsertablePreferences {

    @Nullable
    private final String deviceIdentifier;
    private final boolean cellularDataEnabled;
    private final boolean screenOn;
    private final int imageSize;

    public InsertablePreferences(@Nullable String deviceIdentifier, boolean cellularDataEnabled,
            boolean screenOn, int imageSize) {
        this.deviceIdentifier = deviceIdentifier;
        this.cellularDataEnabled = cellularDataEnabled;
        this.screenOn = screenOn;
        this.imageSize = imageSize;
    }

    @Nullable
    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public boolean isCellularDataEnabled() {
        return cellularDataEnabled;
    }

    public boolean isScreenOn() {
        return screenOn;
    }

    public int getImageSize() {
        return imageSize;
    }
}
