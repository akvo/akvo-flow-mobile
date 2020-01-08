/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util;

import androidx.annotation.Nullable;
import android.text.TextUtils;

import javax.inject.Inject;

import timber.log.Timber;

public class LocationValidator {

    private static final double INVALID_COORDINATE = -999D;
    private static final double MINIMUM_LATITUDE = -90.0D;
    private static final double MAXIMUM_LATITUDE = 90.0D;
    private static final double MINIMUM_LONGITUDE = -180.0D;
    private static final double MAXIMUM_LONGITUDE = 180.0D;
    private static final double REASONABLE_MINIMUM_ELEVATION_IN_METERS = -15000;
    private static final double REASONABLE_MAXIMUM_ELEVATION_IN_METERS = 15000;
    private static final double INVALID_ELEVATION_IN_METERS = 16000;

    @Inject
    public LocationValidator() {
    }

    public boolean validCoordinates(@Nullable String lat, @Nullable String lon) {
        return isValidLatitude(lat) && isValidLongitude(lon);
    }

    public boolean isValidLongitude(@Nullable String lon) {
        double longitude = parseCoordinate(lon);
        return MINIMUM_LONGITUDE <= longitude && longitude < MAXIMUM_LONGITUDE;
    }

    public boolean isValidLatitude(@Nullable String lat) {
        double latitude = parseCoordinate(lat);
        return MINIMUM_LATITUDE <= latitude && latitude < MAXIMUM_LATITUDE;
    }

    private double parseCoordinate(@Nullable String doubleAsString) {
        if (!TextUtils.isEmpty(doubleAsString)) {
            try {
                return Double.parseDouble(doubleAsString);
            } catch (NumberFormatException e) {
                Timber.w(e);
            }
        }
        return INVALID_COORDINATE;
    }

    public boolean isValidElevation(String elevationString) {
        double elevation = parseElevation(elevationString);
        return REASONABLE_MINIMUM_ELEVATION_IN_METERS < elevation
                && elevation < REASONABLE_MAXIMUM_ELEVATION_IN_METERS;
    }

    private double parseElevation(@Nullable String elevation) {
        if (!TextUtils.isEmpty(elevation)) {
            try {
                return Double.parseDouble(elevation);
            } catch (NumberFormatException e) {
                Timber.w(e);
            }
        }
        return INVALID_ELEVATION_IN_METERS;
    }
}
