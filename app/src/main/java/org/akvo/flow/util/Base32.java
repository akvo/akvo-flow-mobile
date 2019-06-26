/*
 *  Copyright (C) 2012-2016,2019 Stichting Akvo (Akvo Foundation)
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

import java.util.UUID;

public class Base32 {

    public Base32() {
    }

    /* Creates a base32 version of a UUID. in the output, it replaces the following letters:
     * l, o, i are replace by w, x, y, to avoid confusion with 1 and 0
     * we don't use the z as it can easily be confused with 2, especially in handwriting.
     * If we can't form the base32 version, we return an empty string.
     */
    String base32Uuid() {
        final String uuid = UUID.randomUUID().toString();
        String strippedUUID = (uuid.substring(0, 13) + uuid.substring(24, 27)).replace("-", "");
        StringBuilder result;
        try {
            long id = Long.parseLong(strippedUUID, 16);
            result = new StringBuilder(Long.toString(id, 32).replace("l", "w").replace("o", "x").replace("i", "y"));
            //if 0 in the beginning were removed, re-add them here, we need 12 chars always
            while (result.length() < 12) {
                result.insert(0, "0");
            }
            return result.toString();
        } catch (NumberFormatException e) {
            // if we can't create the base32 UUID string, return the original uuid.
            return uuid;
        }
    }

}
