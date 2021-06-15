/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;

import org.akvo.flow.R;
import org.akvo.flow.presentation.form.languages.Language;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LanguageAdapter extends ArrayAdapter<Language> {

    @LayoutRes
    private static final int LAYOUT_RESOURCE_ID = R.layout.language_list_item;

    private final List<Language> languages;
    private final LayoutInflater inflater;

    public LanguageAdapter(Context context, @NonNull List<Language> languages) {
        super(context, LAYOUT_RESOURCE_ID, languages);
        this.languages = languages;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    public Set<String> getSelectedLanguages() {
        Set<String> selectedLanguages = new LinkedHashSet<>(3);
        for (Language language : languages) {
            if (language.isSelected()) {
                selectedLanguages.add(language.getLanguageCode());
            }
        }
        return selectedLanguages;
    }

    @Nullable
    @Override
    public Language getItem(int position) {
        return languages.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final View view;
        final CheckBox checkBox;
        Language language = getItem(position);
        if (convertView == null) {
            view = inflater.inflate(LAYOUT_RESOURCE_ID, parent, false);
        } else {
            view = convertView;
        }
        checkBox = view.findViewById(R.id.language_checkbox);
        checkBox.setText(language.getLanguage());
        checkBox.setChecked(language.isSelected());
        return view;
    }

    public void updateSelected(int position) {
        Language language = getItem(position);
        language.setSelected(!language.isSelected());
        notifyDataSetChanged();
    }

    public void setLanguages(@NotNull List<Language> languages) {
        this.languages.clear();
        this.languages.addAll(languages);
        notifyDataSetChanged();
    }
}
