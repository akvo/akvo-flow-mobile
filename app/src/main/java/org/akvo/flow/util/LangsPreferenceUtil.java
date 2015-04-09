/*
 *  Copyright (C) 2010-2015 Stichting Akvo (Akvo Foundation)
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.akvo.flow.R;
import org.akvo.flow.dao.SurveyDao;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.exception.PersistentUncaughtExceptionHandler;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

/**
 * utility class for manipulating the preference settings that allow for
 * multi-selection of language-specific arrays
 * 
 * @author Mark Westra
 */
public class LangsPreferenceUtil {
    private static final String TAG = "LANGUAGE_SERVICE";

    /**
     * returns an arrayList of language codes that are active.
     */
    public static String[] getSelectedLangCodes(Context context, int[] indexes,
            boolean[] selectedItems, int codeResourceId) {
        ArrayList<String> codes = new ArrayList<String>();
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
     */
    public static String formLangPreferenceString(boolean[] selectedItems,
            int[] langIndexes) {
        StringBuffer newSelection = new StringBuffer();
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
        List<String> langsPresentNameList = new ArrayList<String>();
        List<Boolean> langsSelectedBooleanList = new ArrayList<Boolean>();
        List<Integer> langsSelectedMasterIndexList = new ArrayList<Integer>();

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

    public static String[] determineLanguages(Context context, Survey survey) {
        HashSet<String> langs = new HashSet<String>();
        // find languages in the survey

        try {
            InputStream in = null;
            if (ConstantUtil.RESOURCE_LOCATION.equalsIgnoreCase(survey
                    .getLocation())) {
                // load from resource
                Resources res = context.getResources();
                in = res.openRawResource(res.getIdentifier(
                        survey.getFileName(), ConstantUtil.RAW_RESOURCE,
                        ConstantUtil.RESOURCE_PACKAGE));
            } else {
                // load from file
                File f = new File(FileUtil.getFilesDir(FileUtil.FileType.FORMS), survey.getFileName());
                in = new FileInputStream(f);
            }
            Survey hydratedSurvey = SurveyDao.loadSurvey(survey, in);
            if (hydratedSurvey != null) {
                // add main language to survey object. It is used in the next
                // section to populate the languages
                survey.setLanguage(hydratedSurvey.getLanguage());

                if (hydratedSurvey.getQuestionGroups() != null) {
                    for (int i = 0; i < hydratedSurvey.getQuestionGroups()
                            .size(); i++) {
                        ArrayList<Question> questions = hydratedSurvey
                                .getQuestionGroups().get(i).getQuestions();
                        if (questions != null) {
                            for (int j = 0; j < questions.size(); j++) {
                                if (questions.get(j).getAltTextMap() != null) {
                                    Set<String> list = questions.get(j)
                                            .getAltTextMap().keySet();
                                    langs.addAll(list);
                                }
                            }
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not parse survey survey file", e);
            PersistentUncaughtExceptionHandler.recordException(e);
        }
        // put everything in an array, with the default language in the first
        // position.
        int i = 0;
        String[] langsArray = new String[langs.size() + 1];
        if (survey.getLanguage() != null) {
            langsArray[0] = survey.getLanguage();
            i++;
        }

        if (langs != null && langs.size() > 0) {
            Iterator<String> itr = langs.iterator();
            while (itr.hasNext()) {
                langsArray[i] = itr.next();
                i++;
            }
        }
        return langsArray;
    }
    
}
