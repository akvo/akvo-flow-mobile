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

    public String cleanVal(@Nullable String val) {
        if (val != null) {
            if (val.contains(DELIMITER)) {
                val = val.replace(DELIMITER, SPACE);
            }
            if (val.contains(",")) {
                val = val.replace(",", SPACE);
            }
            if (val.contains("\n")) {
                val = val.replace("\n", SPACE);
            }
        }
        return val;
    }

    public String sanitizeValue(String value) {
        if (value != null) {
            value = value.replace("\n", SPACE);
            value = value.replace(DELIMITER, SPACE);
            value = value.trim();
        }
        return value;
    }
}