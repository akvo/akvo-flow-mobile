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

package org.akvo.flow.event;

import android.Manifest;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.util.ConstantUtil;

public class PermissionAwareLocationListener {

    private final TimedLocationListener locationListener;
    private final Context context;
    private final String questionId;
    private final PermissionListener permissionListener;

    public PermissionAwareLocationListener(Context context, TimedLocationListener.Listener listener,
            boolean allowMockLocation, String questionId, PermissionListener permissionListener) {
        this.context = context;
        this.locationListener = new TimedLocationListener(context, listener, allowMockLocation);
        this.questionId = questionId;
        this.permissionListener = permissionListener;
    }

    public void startLocationIfPossible() {
        if (isLocationPermissionGranted()) {
            startLocation();
        } else {
            requestLocationPermission();
        }
    }

    public void stopLocation() {
        if (locationListener.isListening()) {
            locationListener.stop();
        }
    }

    public void handlePermissionResult(int requestCode, @NonNull String[] permissions,
                                       @NonNull int[] grantResults) {
        if (grantResults.length == 0 || permissions.length == 0) {
            permissionListener.onPermissionNotGranted();
        } else if (requestCode == ConstantUtil.LOCATION_PERMISSION_CODE
                && Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[0])
                && grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
            startLocation();
        } else {
            permissionListener.onPermissionNotGranted();
        }
    }

    private void requestLocationPermission() {
        FormActivity activity = (FormActivity) context;
        activity.requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                ConstantUtil.LOCATION_PERMISSION_CODE, questionId);
    }

    private boolean isLocationPermissionGranted() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED;
    }

    private void startLocation() {
        locationListener.start();
    }

    public boolean isListening() {
        return locationListener.isListening();
    }

    public interface PermissionListener {

        void onPermissionNotGranted();
    }
}
