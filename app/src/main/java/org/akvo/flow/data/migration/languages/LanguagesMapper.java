/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.migration.languages;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.akvo.flow.R;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class LanguagesMapper {

    public Set<String> transform(@NonNull Context context, @NonNull String languagesString) {
        if (TextUtils.isEmpty(languagesString)) {
            return Collections.emptySet();
        }
        Resources res = context.getResources();
        String[] stringArray = res.getStringArray(R.array.alllanguagecodes);
        StringTokenizer strTok = new StringTokenizer(languagesString, ",");
        Set<String> languages = new LinkedHashSet<>(strTok.countTokens());
        while (strTok.hasMoreTokens()) {
            languages.add(stringArray[Integer.parseInt(strTok.nextToken())]);
        }
        return languages;
    }
}
