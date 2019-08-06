/*
 * Copyright (C) 2018-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain.util;

import androidx.annotation.Nullable;

import javax.inject.Inject;

public class TextValueCleaner {
    static final String TAB = "\t";
    static final String COMA = ",";
    static final String NEWLINE = "\n";
    private static final String SPACE = "\u0020"; // safe from source whitespace reformatting

    @Inject
    public TextValueCleaner() {
    }

    public String cleanVal(@Nullable String value) {
        String cleanValue = value;
        if (cleanValue != null) {
            if (cleanValue.contains(TAB)) {
                cleanValue = cleanValue.replace(TAB, SPACE);
            }
            if (cleanValue.contains(COMA)) {
                cleanValue = cleanValue.replace(COMA, SPACE);
            }
            if (cleanValue.contains(NEWLINE)) {
                cleanValue = cleanValue.replace(NEWLINE, SPACE);
            }
        }
        return cleanValue;
    }

    public String sanitizeValue(@Nullable String value) {
        String cleanValue = value;
        if (cleanValue != null) {
            cleanValue = cleanValue.trim();
            cleanValue = cleanValue.replace(NEWLINE, SPACE);
            cleanValue = cleanValue.replace(TAB, SPACE);
        }
        return cleanValue;
    }
}
