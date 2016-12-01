/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.util;

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
    public boolean isNewerVersion(String installedVersion, String newVersion) {
        // Ensure the Strings are properly formatted
        final String regex = "^\\d+(\\.\\d+)*$";// Check dot-decimal notation
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
                return true;// Newer version
            } else if (newPart < currentPart) {
                return false;// Older version
            }
        }

        return false;// Same version
    }
}
