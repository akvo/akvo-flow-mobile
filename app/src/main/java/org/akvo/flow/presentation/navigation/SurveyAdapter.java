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

package org.akvo.flow.presentation.navigation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.akvo.flow.R;

import java.util.ArrayList;
import java.util.List;

class SurveyAdapter extends RecyclerView.Adapter<SurveyAdapter.SurveyViewHolder> {

    private final List<ViewSurvey> surveyList;
    private final int selectedTextColor;
    private final int textColor;
    private long selectedSurveyId;

    SurveyAdapter(Context context) {
        this.surveyList = new ArrayList<>();
        this.selectedTextColor = ContextCompat.getColor(context, R.color.orange_main);
        this.textColor = ContextCompat.getColor(context, R.color.black_main);
    }

    @NonNull
    @Override
    public SurveyAdapter.SurveyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.navigation_item, parent, false);
        return new SurveyViewHolder(view, selectedTextColor, textColor);
    }

    public void setSurveys(@Nullable List<ViewSurvey> surveys, long selectedSurveyId) {
        this.selectedSurveyId = selectedSurveyId;
        surveyList.clear();
        if (surveys != null && !surveys.isEmpty()) {
            surveyList.addAll(surveys);
        }
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(SurveyAdapter.SurveyViewHolder holder, int position) {
        ViewSurvey viewSurvey = surveyList.get(position);
        holder.setViews(viewSurvey, selectedSurveyId == viewSurvey.getId());
    }

    @Override
    public int getItemCount() {
        return surveyList.size();
    }

    public ViewSurvey getItem(int position) {
        return surveyList.get(position);
    }

    void updateSelected(long surveyId) {
        selectedSurveyId = surveyId;
        notifyDataSetChanged();
    }

    static class SurveyViewHolder extends RecyclerView.ViewHolder {

        private final TextView surveyTv;
        private final int selectedTextColor;
        private final int textColor;

        SurveyViewHolder(View view, int selectedTextColor, int textColor) {
            super(view);
            this.surveyTv = view.findViewById(R.id.item_text_view);
            this.selectedTextColor = selectedTextColor;
            this.textColor = textColor;
        }

        void setViews(ViewSurvey navigationItem, boolean isSelected) {
            surveyTv.setText(navigationItem.getName());
            surveyTv.setTextColor(isSelected ? selectedTextColor : textColor);
        }
    }
}
