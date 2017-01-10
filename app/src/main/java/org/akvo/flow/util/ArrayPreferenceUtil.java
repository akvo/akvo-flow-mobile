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

import android.content.Context;
import android.content.res.Resources;

import java.util.StringTokenizer;

/**
 * utility class for manipulating the preference settings that allow for
 * multi-selection of language-specific arrays
 * 
 * @author Christopher Fagiani
 */
public class ArrayPreferenceUtil {

    /**
     * loads the array from the applications resources and initializes the
     * selection array based on the value of the selectionString passed in
     * 
     * @param context
     * @param selection
     * @return
     */
    public static ArrayPreferenceData loadArray(Context context,
            String selection, int resourceId) {
        Resources res = context.getResources();
        String[] stringArray = res.getStringArray(resourceId);
        boolean[] selectedItems = new boolean[stringArray.length];
        for (int i = 0; i < selectedItems.length; i++) {
            selectedItems[i] = false;
        }
        if (selection != null) {
            StringTokenizer strTok = new StringTokenizer(selection, ",");
            while (strTok.hasMoreTokens()) {
                selectedItems[Integer.parseInt(strTok.nextToken())] = true;
            }
        }
        return new ArrayPreferenceData(stringArray, selectedItems);
    }

}
