/*
 *  Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.util;

import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * simple string convenience functions
 * 
 * @author Christopher Fagiani
 */
public class StringUtil {

    public static final char SPACE_CHAR = '\u0020';

    /**
     * checks a string to see if it's null or has no non-whitespace characters
     * 
     * @param s
     * @return
     */
    public static boolean isNullOrEmpty(@Nullable String s) {
        return s == null || s.trim().length() == 0;
    }

    // copy a string transforming all control chars
    // (like newline and tab) into spaces
    public static String controlToSpace(@Nullable String val) {
        String result = "";
        if (val == null) {
            return result;
        }
        for (int i = 0; i < val.length(); i++) {
            if (val.charAt(i) < SPACE_CHAR)
                result = result + SPACE_CHAR;
            else
                result = result + val.charAt(i);
        }

        return result;
    }

    public static boolean isValid(@Nullable String value) {
        return !TextUtils.isEmpty(value) && !value.equalsIgnoreCase("null");
    }
}
