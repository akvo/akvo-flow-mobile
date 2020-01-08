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

package org.akvo.flow.presentation.datapoints.list.entity;

public class ListDataPoint {

    public static final double INVALID_COORDINATE = -1;

    private final String displayName;
    private final int status;
    private final String id;
    private final double latitude;
    private final double longitude;
    private final long lastModified;

    public ListDataPoint(String displayName, int status, String id, double latitude,
            double longitude, long lastModified) {
        this.displayName = displayName;
        this.status = status;
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.lastModified = lastModified;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getStatus() {
        return status;
    }

    public String getId() {
        return id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getLastModified() {
        return lastModified;
    }

    public boolean isLocationValid() {
        return getLatitude() != INVALID_COORDINATE && getLongitude() != INVALID_COORDINATE;
    }
}
