/*
 * Copyright (C) 2017,2019 Stichting Akvo (Akvo Foundation)
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

import android.text.TextUtils;

import androidx.annotation.NonNull;

import javax.inject.Inject;

public class SurveyIdGenerator {

    @Inject
    public SurveyIdGenerator() {
    }

    /**
     * Get the survey id from the survey xml folder
     * The structure can be either surveyId/ or surveyId/folder/
     *
     * @param entryName all the folder path expluding the actial fileName
     * @return
     */
    @NonNull
    public String getSurveyIdFromFilePath(@NonNull String entryName) {
        int fileSeparatorPosition = entryName.lastIndexOf("/");
        String folderPath = fileSeparatorPosition <= 0 ?
                "" : entryName.substring(0, fileSeparatorPosition);
        String folders[] = folderPath == null || folderPath.isEmpty()? null : folderPath.split("/");
        if (folders == null || folders.length == 0) {
            //missing folder
            return "";
        } else {
            int lastItemIndex = folders.length - 1;
            for (int i = lastItemIndex; i >= 0; i--) {
                String folderName = folders[i];
                if (TextUtils.isDigitsOnly(folderName)) {
                    return folderName;
                }
            }
            //if not found just return the lowest subfolder name
            return folders[lastItemIndex];
        }
    }
}
