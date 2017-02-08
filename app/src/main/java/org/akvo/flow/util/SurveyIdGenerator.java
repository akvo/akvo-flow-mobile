/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SurveyIdGenerator {

    public SurveyIdGenerator() {
    }

    /**
     * Get the survey id from the survey xml folder
     * The structure can be either surveyId/survey.xml or surveyId/folder/survey.xml
     *
     * @param parts
     * @return
     */
    @NonNull
    public String getSurveyIdFromFilePath(@Nullable String[] parts) {
        if (parts == null || parts.length <= 1) {
            //no file at all or missing folder
            return "";
        } else {
            //we remove the last piece which is the actual filename
            String[] foldersArray = Arrays.copyOfRange(parts, 0, parts.length - 1);
            List<String> folders = Arrays.asList(foldersArray);
            Collections.reverse(folders);
            //remove the xml filename
            for (String folderName : folders) {
                if (TextUtils.isDigitsOnly(folderName)) {
                    return folderName;
                }
            }
            //if not found just return the lowest subfolder name
            return folders.get(0);
        }
    }
}
