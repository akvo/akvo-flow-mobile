/*
 *  Copyright (C) 2016-2107 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.event;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import java.util.Timer;
import java.util.TimerTask;

/**
 * TimedLocationListener is a reusable helper class to get GPS locations.
 * If geolocation is unknown after the LOCATION_TIMEOUT_IN_MS milliseconds, the caller will receive a
 * time out event.
 */
public class TimedLocationListener implements LocationListener {

    public static final float ACCURACY_DEFAULT = 20f; // 20 meters
    private static final long LOCATION_TIMEOUT_IN_MS = 1000 * 60; // 1 minute
    private static final float ACCURACY_UNRELIABLE = 0f;

    private final Handler mHandler = new Handler();
    private final Listener mListener;
    private final LocationManager mLocationManager;
    private final boolean mAllowMockLocations;

    private Timer mTimer;
    private boolean mListeningLocation;

    public TimedLocationListener(Context context, Listener listener, boolean allowMockLocations) {
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mListener = listener;
        mListeningLocation = false;
        mAllowMockLocations = allowMockLocations;
    }

    public void start() {
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mListener.onGPSDisabled();
            return;
        }

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        mListeningLocation = true;

        // Ensure no pending tasks are running
        if (mTimer != null) {
            mTimer.cancel();
        }
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Ensure it runs on the UI thread!
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mListeningLocation) {
                            stop();
                            mListener.onTimeout();
                        }
                    }
                });
            }
        }, LOCATION_TIMEOUT_IN_MS);
    }

    public void stop() {
        if (mTimer != null) {
            mTimer.cancel();
        }
        mLocationManager.removeUpdates(this);
        mListeningLocation = false;
    }

    public boolean isListening() {
        return mListeningLocation;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (isValid(location) && mListeningLocation) {
            mListener.onLocationReady(location.getLatitude(), location.getLongitude(),
                    location.getAltitude(), location.getAccuracy());
        }
    }

    public void onProviderDisabled(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider) && mListeningLocation) {
            stop();// Cancel task and ensure state is updated before passing on the event
            mListener.onGPSDisabled();
        }
    }

    public void onProviderEnabled(String provider) {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private boolean isValid(Location location) {
        return isLocationProviderValid(location) && isAccuracyValid(location);
    }

    private boolean isAccuracyValid(Location location) {
        return location.getAccuracy() > ACCURACY_UNRELIABLE;
    }

    private boolean isLocationProviderValid(Location location) {
        return mAllowMockLocations || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
                || !location.isFromMockProvider();
    }

    public interface Listener {

        void onLocationReady(double latitude, double longitude, double altitude, float accuracy);

        void onTimeout();

        void onGPSDisabled();
    }
}
