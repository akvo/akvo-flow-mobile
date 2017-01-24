/*
 *  Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.akvo.flow.R;
import org.akvo.flow.data.dao.SurveyDao;
import org.akvo.flow.domain.AltText;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.Survey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

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

    /**
     * Find all the languages available for a given survey
     * @param context
     * @param survey
     * @return an array with at least {'en'}, other languages are appended if available
     */
    @NonNull
    public static String[] determineLanguages(@NonNull Context context, @NonNull Survey survey) {
        /**
         * Represents the survey languages we receive from the server
         * Set of strings with language codes (ej: 'en')
         *
         */
        Set<String> availableSurveyLanguages = new LinkedHashSet<>();

        try {
            InputStream in;
            if (ConstantUtil.RESOURCE_LOCATION.equalsIgnoreCase(survey.getLocation())) {
                in = loadFromRawResource(context, survey);
            } else {
                in = loadFromFile(survey);
            }
            Survey hydratedSurvey = SurveyDao.loadSurvey(survey, in);

            appendAllLanguages(survey, availableSurveyLanguages, hydratedSurvey);
        } catch (FileNotFoundException e) {
            Timber.e(e, "Could not parse survey file");
        }
        return availableSurveyLanguages.toArray(new String[availableSurveyLanguages.size()]);
    }

    private static void appendAllLanguages(@NonNull Survey survey,
            @NonNull Set<String> availableSurveyLanguages, @Nullable Survey hydratedSurvey) {
        if (hydratedSurvey != null) {
            // add main language to survey object. It is used in the next
            // section to populate the languages
            String surveyMainLanguage = hydratedSurvey.getLanguage();
            survey.setLanguage(surveyMainLanguage);
            if (!TextUtils.isEmpty(surveyMainLanguage)) {
                //start by inserting the default language in the beginning
                availableSurveyLanguages.add(surveyMainLanguage);
            }
            appendQuestionGroupLanguages(availableSurveyLanguages,
                    hydratedSurvey.getQuestionGroups());
        }
    }

    private static void appendQuestionGroupLanguages(Set<String> availableSurveyLanguages,
            List<QuestionGroup> questionGroups) {
        if (questionGroups != null) {
            int size = questionGroups.size();
            for (int i = 0; i < size; i++) {
                ArrayList<Question> questions = questionGroups.get(i).getQuestions();
                if (questions != null) {
                    for (Question question : questions) {
                        Map<String, AltText> questionAltTextMap = question.getAltTextMap();
                        if (questionAltTextMap != null) {
                            availableSurveyLanguages.addAll(questionAltTextMap.keySet());
                        }
                    }
                }
            }
        }
    }

    @NonNull
    private static InputStream loadFromFile(Survey survey) throws FileNotFoundException {
        File f = new File(FileUtil.getFilesDir(FileUtil.FileType.FORMS), survey.getFileName());
        return new FileInputStream(f);
    }

    @NonNull
    private static InputStream loadFromRawResource(Context context, Survey survey) {
        Resources res = context.getResources();
        return res.openRawResource(
                res.getIdentifier(survey.getFileName(), ConstantUtil.RAW_RESOURCE,
                        ConstantUtil.RESOURCE_PACKAGE));
    }

}
