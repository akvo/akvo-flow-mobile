/*
 * Copyright (C) 2018-2019 Stichting Akvo (Akvo Foundation)
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

import android.location.LocationListener;
import android.os.Bundle;
import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

public class WeakLocationListener implements LocationListener {

    private final WeakReference<LocationListener> locationListenerRef;

    public WeakLocationListener(@NonNull LocationListener locationListener) {
        locationListenerRef = new WeakReference<>(locationListener);
    }

    @Override
    public void onLocationChanged(android.location.Location location) {
        LocationListener locationListener = locationListenerRef.get();
        if (locationListener == null) {
            return;
        }
        locationListener.onLocationChanged(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        LocationListener locationListener = locationListenerRef.get();
        if (locationListener == null) {
            return;
        }
        locationListener.onStatusChanged(provider, status, extras);
    }

    @Override
    public void onProviderEnabled(String provider) {
        LocationListener locationListener = locationListenerRef.get();
        if (locationListener == null) {
            return;
        }
        locationListener.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        LocationListener locationListener = locationListenerRef.get();
        if (locationListener == null) {
            return;
        }
        locationListener.onProviderDisabled(provider);
    }
}
