/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain.util;

import androidx.annotation.Nullable;
import android.text.TextUtils;

import javax.inject.Inject;

public class VersionHelper {

    @Inject
    public VersionHelper() {
    }

    /**
     * Check if a given version is newer than the current one.
     * Versions are expected to be formatted in a dot-decimal notation: X.Y.Z,
     * being X, Y, and Z integers, and each number separated by a full stop (dot).
     *
     * @param installedVersion
     * @param newVersion
     * @return true if the second version is newer than the first one, false otherwise
     */
    public boolean isNewerVersion(@Nullable String installedVersion, @Nullable String newVersion) {
        if (!versionValid(installedVersion) || !versionValid(newVersion)) {
            return false;
        }
        String[] currentParts = installedVersion.split("\\.");
        String[] newPartsParts = newVersion.split("\\.");
        int length = Math.max(currentParts.length, newPartsParts.length);
        for (int i = 0; i < length; i++) {
            int currentPart = safeParseInt(currentParts, i);
            int newPart = safeParseInt(newPartsParts, i);
            if (currentPart < newPart) {
                return true;// Newer version
            } else if (newPart < currentPart) {
                return false;// Older version
            }
        }
        return false;
    }

    private boolean versionValid(@Nullable String version) {
        final String regex = "^\\d+(\\.\\d+)*$";
        return version != null && version.matches(regex);
    }

    private int safeParseInt(String[] currentParts, int position) {
        return position < currentParts.length ? Integer.parseInt(currentParts[position]) : 0;
    }

    public boolean isValid(@Nullable String value) {
        return !TextUtils.isEmpty(value) && !value.equalsIgnoreCase("null");
    }
}
