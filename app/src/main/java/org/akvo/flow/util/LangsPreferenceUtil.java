/*
 *  Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

import org.akvo.flow.R;

import java.util.ArrayList;
import java.util.List;

/**
 * utility class for manipulating the preference settings that allow for
 * multi-selection of language-specific arrays
 * 
 * @author Mark Westra
 */
public class LangsPreferenceUtil {

    /**
     * returns an arrayList of language codes that are active.
     * 
     * @return
     */
    public static String[] getSelectedLangCodes(Context context, int[] indexes,
            boolean[] selectedItems, int codeResourceId) {
        ArrayList<String> codes = new ArrayList<>();
        Resources res = context.getResources();
        String[] allCodes = res.getStringArray(codeResourceId);
        for (int i = 0; i < indexes.length; i++) {
            if (selectedItems[i]) {
                codes.add(allCodes[indexes[i]]);
            }
        }
        return codes.toArray(new String[codes.size()]);
    }

    /**
     * forms a comma-delimited string of array index values used to persist the
     * selected items to the db.
     * 
     * @return
     */
    public static String formLangPreferenceString(boolean[] selectedItems,
            int[] langIndexes) {
        StringBuilder newSelection = new StringBuilder();
        boolean isFirst = true;
        for (int i = 0; i < selectedItems.length; i++) {
            if (selectedItems[i]) {
                if (!isFirst) {
                    newSelection.append(",");
                } else {
                    isFirst = false;
                }
                newSelection.append(langIndexes[i]);
            }
        }
        return newSelection.toString();
    }

    public static LangsPreferenceData createLangPrefData(Context context,
            String val, String langsPresentIndexes) {

        ArrayPreferenceData allLanguagesPresent = ArrayPreferenceUtil
                .loadArray(context, langsPresentIndexes, R.array.alllanguages);
        ArrayPreferenceData allLanguagesSelected = ArrayPreferenceUtil
                .loadArray(context, val, R.array.alllanguages);

        String[] allLanguagesPresentNameArray = allLanguagesPresent.getItems();
        boolean[] allLanguagesPresentBooleanArray = allLanguagesPresent
                .getSelectedItems();
        boolean[] allLanguagesSelectedBooleanArray = allLanguagesSelected
                .getSelectedItems();

        // create a new list of only active languages
        List<String> langsPresentNameList = new ArrayList<>();
        List<Boolean> langsSelectedBooleanList = new ArrayList<>();
        List<Integer> langsSelectedMasterIndexList = new ArrayList<>();

        for (int i = 0; i < allLanguagesPresentNameArray.length; i++) {
            if (allLanguagesPresentBooleanArray[i]) {
                langsPresentNameList.add(allLanguagesPresentNameArray[i]);
                langsSelectedBooleanList
                        .add(allLanguagesSelectedBooleanArray[i]);
                langsSelectedMasterIndexList.add(i);
            }
        }
        // put this in a LangsPreferenceData object and return it
        return new LangsPreferenceData(
                langsPresentNameList.toArray(new String[langsPresentNameList
                        .size()]),
                ArrayUtil.toPrimitiveBooleanArray(langsSelectedBooleanList),
                ArrayUtil.toPrimitiveIntArray(langsSelectedMasterIndexList));

    }

}
