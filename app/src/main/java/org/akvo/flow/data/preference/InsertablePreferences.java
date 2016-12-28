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
