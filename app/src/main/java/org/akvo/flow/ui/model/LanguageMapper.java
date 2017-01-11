/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.ui.model;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.akvo.flow.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LanguageMapper {

    private final Context context;

    public LanguageMapper(Context context) {
        this.context = context;
    }

    @NonNull
    public List<Language> transform(@Nullable String[] languageCodesSelected,
            @Nullable Set<String> languageCodesAvailable) {
        if (languageCodesSelected == null || languageCodesAvailable == null) {
            return Collections.EMPTY_LIST;
        }
        List<Language> languages = new ArrayList<>(languageCodesAvailable.size());
        for (String language : languageCodesAvailable) {
            Language transformed = transform(language, languageCodesSelected);
            if (transformed != null) {
                languages.add(transformed);
            }
        }
        return languages;
    }

    @Nullable
    private Language transform(@Nullable String languageCode,
            @NonNull String[] languageCodesSelected) {
        if (TextUtils.isEmpty(languageCode)) {
            return null;
        }
        boolean selected = Arrays.asList(languageCodesSelected).contains(languageCode);
        Resources res = context.getResources();
        String[] languageCodes = res.getStringArray(R.array.alllanguagecodes);
        String[] languages = res.getStringArray(R.array.alllanguages);
        /**
         * This presupposes that both arrays languages and language codes have the same order
         * and contain the same languages
         */
        int languagePosition = Arrays.asList(languageCodes).indexOf(languageCode);
        if (languagePosition < 0 || languagePosition >= languages.length) {
            return null;
        }
        String languageName = languages[languagePosition];
        return new Language(languageCode, languageName, selected);
    }
}
