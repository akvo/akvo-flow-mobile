/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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

import android.support.annotation.Nullable;

import javax.inject.Inject;

public class TextValueCleaner {
    private static final String DELIMITER = "\t";
    private static final String SPACE = "\u0020"; // safe from source whitespace reformatting

    @Inject
    public TextValueCleaner() {
    }

    public String cleanVal(@Nullable String value) {
        String cleanValue = value;
        if (cleanValue != null) {
            if (cleanValue.contains(DELIMITER)) {
                cleanValue = cleanValue.replace(DELIMITER, SPACE);
            }
            if (cleanValue.contains(",")) {
                cleanValue = cleanValue.replace(",", SPACE);
            }
            if (cleanValue.contains("\n")) {
                cleanValue = cleanValue.replace("\n", SPACE);
            }
        }
        return cleanValue;
    }

    public String sanitizeValue(@Nullable String value) {
        String cleanValue = value;
        if (cleanValue != null) {
            cleanValue = cleanValue.trim();
            cleanValue = cleanValue.replace("\n", SPACE);
            cleanValue = cleanValue.replace(DELIMITER, SPACE);
        }
        return cleanValue;
    }
}