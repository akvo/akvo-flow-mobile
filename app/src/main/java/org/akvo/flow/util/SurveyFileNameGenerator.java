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

public class SurveyFileNameGenerator {

    public SurveyFileNameGenerator() {
    }

    @NonNull
    public String generateFileName(@NonNull String entryName) {
        if (entryName.isEmpty()) {
            return "";
        }
        int fileSeparatorPosition = entryName.lastIndexOf("/");
        return fileSeparatorPosition < 0 ?
                entryName :
                entryName.substring(fileSeparatorPosition + 1);
    }
}