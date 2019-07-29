/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.offlinemaps.domain.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class MapInfo implements Parcelable {

    public static int ZOOM_MAX = 2;

    private final double latitude;
    private final double longitude;
    private final double zoom;

    public MapInfo(double latitude, double longitude, double zoom) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.zoom = zoom;
    }

    protected MapInfo(Parcel in) {
        latitude = in.readDouble();
        longitude = in.readDouble();
        zoom = in.readDouble();
    }

    public static final Creator<MapInfo> CREATOR = new Creator<MapInfo>() {
        @Override
        public MapInfo createFromParcel(Parcel in) {
            return new MapInfo(in);
        }

        @Override
        public MapInfo[] newArray(int size) {
            return new MapInfo[size];
        }
    };

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getZoom() {
        return zoom;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeDouble(zoom);
    }
}
