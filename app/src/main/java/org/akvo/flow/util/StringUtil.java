/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
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

import android.text.TextUtils;

/**
 * simple string convenience functions
 * 
 * @author Christopher Fagiani
 */
public class StringUtil {

    /**
     * checks a string to see if it's null or has no non-whitespace characters
     * 
     * @param s
     * @return
     */
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

    // copy a string transforming all control chars
    // (like newline and tab) into spaces
    public static String ControlToSPace(String val) {
        String result = "";
        for (int i = 0; i < val.length(); i++) {
            if (val.charAt(i) < '\u0020')
                result = result + '\u0020';
            else
                result = result + val.charAt(i);
        }

        return result;
    }

    // copy a string transforming all control chars
    // (like newline and tab) and comma into spaces
    public static String ControlCommaToSPace(String val) {
        String result = "";
        for (int i = 0; i < val.length(); i++) {
            if (val.charAt(i) < '\u0020' || val.charAt(i) == ',')
                result = result + '\u0020';
            else
                result = result + val.charAt(i);
        }

        return result;
    }

    public static boolean isValid(String value) {
        return !TextUtils.isEmpty(value) && !value.equalsIgnoreCase("null");
    }
}
