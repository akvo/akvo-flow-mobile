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

import android.Manifest;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.appcompat.app.AppCompatActivity;

import javax.inject.Inject;

public class StoragePermissionsHelper {

    private final Context context;

    @Inject
    public StoragePermissionsHelper(Context context) {
        this.context = context;
    }

    public boolean storagePermissionsGranted(String permission, @NonNull int[] grantResults) {
        return grantResults.length > 0
                && Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)
                && grantResults[0] == PermissionChecker.PERMISSION_GRANTED;
    }

    public boolean userPressedDoNotShowAgain(AppCompatActivity activity) {
        return !isStorageAllowed() && !ActivityCompat
                .shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public boolean isStorageAllowed() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED;
    }
}
