/*
 *  Copyright (C) 2013-2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.Nullable;

import java.util.UUID;

/**
 * Utilities class to provide Android related functionalities
 */
public class PlatformUtil {

    /**
     * TODO: use versionCode to compare versions as versionName field does not have to be X.Y.Z
     * format
     *
     * Check if a given version is newer than the current one.
     * Versions are expected to be formatted in a dot-decimal notation: X.Y.Z,
     * being X, Y, and Z integers, and each number separated by a full stop (dot).
     *
     * @return true if the second version is newer than the first one, false otherwise
     */
    public static boolean isNewerVersion(@Nullable String installedVersion,
            @Nullable String newVersion) {
        if (installedVersion == null || newVersion == null) {
            return false;
        }
        // Ensure the Strings are properly formatted
        final String regex = "^\\d+(\\.\\d+)*$"; // Check dot-decimal notation
        if (!installedVersion.matches(regex) || !newVersion.matches(regex)) {
            return false;
        }

        String[] currentParts = installedVersion.split("\\.");
        String[] newPartsParts = newVersion.split("\\.");
        int length = Math.max(currentParts.length, newPartsParts.length);
        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int newPart = i < newPartsParts.length ? Integer.parseInt(newPartsParts[i]) : 0;

            if (currentPart < newPart) {
                return true; // Newer version
            } else if (newPart < currentPart) {
                return false; // Older version
            }
        }

        return false;
    }

    public static int getResource(Context context, int attr) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[] { attr });
        return a.getResourceId(0, 0);
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String recordUuid() {
        String base32Id = Base32.base32Uuid();
        // Put dashes between the 4-5 and 8-9 positions to increase readability
        return base32Id.substring(0, 4) + "-" + base32Id.substring(4, 8) + "-" + base32Id
                .substring(8);
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
}
