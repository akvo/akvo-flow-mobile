/*
 * Copyright (C) 2018,2019 Stichting Akvo (Akvo Foundation)
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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.appcompat.app.AppCompatActivity;

import javax.inject.Inject;

public class AppPermissionsHelper {

    private final Context context;
    private final StoragePermissionsHelper storagePermissionsHelper;

    @Inject
    public AppPermissionsHelper(Context context,
            StoragePermissionsHelper storagePermissionsHelper) {
        this.context = context;
        this.storagePermissionsHelper = storagePermissionsHelper;
    }

    public boolean allPermissionsGranted(String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && permissions.length == grantResults.length) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean userPressedDoNotShowAgain(AppCompatActivity activity) {
        return storagePermissionsHelper.userPressedDoNotShowAgain(activity) ||
                userPressedDoNotShowAgainForPhoneState(activity);
    }

    private boolean userPressedDoNotShowAgainForPhoneState(AppCompatActivity activity) {
        return !isPhoneStateAllowed() &&
               !ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.READ_PHONE_STATE);
    }

    public boolean isStorageAllowed() {
        return storagePermissionsHelper.isStorageAllowed();
    }

    public boolean isPhoneStateAllowed() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_PHONE_STATE) == PermissionChecker.PERMISSION_GRANTED;
    }
}
