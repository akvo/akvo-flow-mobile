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

/**
 * Preference as extracted from Database
 */
public class MigratablePreferences {

    private final String deviceIdentifier;
    private final String cellularDataUpload;
    private final String screenOn;
    private final String imageSize;

    public MigratablePreferences(String deviceIdentifier, String cellularDataUpload,
            String screenOn, String imageSize) {
        this.deviceIdentifier = deviceIdentifier;
        this.cellularDataUpload = cellularDataUpload;
        this.screenOn = screenOn;
        this.imageSize = imageSize;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public String getCellularDataUpload() {
        return cellularDataUpload;
    }

    public String getScreenOn() {
        return screenOn;
    }

    public String getImageSize() {
        return imageSize;
    }

}
