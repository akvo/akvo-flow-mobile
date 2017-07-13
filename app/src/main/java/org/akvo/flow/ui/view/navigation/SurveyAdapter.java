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

package org.akvo.flow.ui.view.navigation;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.akvo.flow.R;

import java.util.ArrayList;
import java.util.List;

class SurveyAdapter extends RecyclerView.Adapter<SurveyAdapter.SurveyViewHolder> {

    private final List<SurveyItem> surveyList = new ArrayList<>();

    SurveyAdapter() {
        surveyList.add(new SurveyItem("test Survey"));
        surveyList.add(new SurveyItem("test Survey2"));
        surveyList.add(new SurveyItem("test Survey3"));
        surveyList.add(new SurveyItem("test Survey3"));
        surveyList.add(new SurveyItem("test Survey3"));
        surveyList.add(new SurveyItem("test Survey3"));
        surveyList.add(new SurveyItem("test Survey3"));
        surveyList.add(new SurveyItem("test Survey3"));
        surveyList.add(new SurveyItem("test Survey3"));
        surveyList.add(new SurveyItem("test Survey3"));
    }

    @Override
    public SurveyAdapter.SurveyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.navigation_item_survey, parent, false);
        return new SurveyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SurveyAdapter.SurveyViewHolder holder, int position) {
        holder.setViews(surveyList.get(position));
    }

    @Override
    public int getItemCount() {
        return surveyList.size();
    }

    class SurveyViewHolder extends RecyclerView.ViewHolder {

        private TextView surveyTv;

        SurveyViewHolder(View view) {
            super(view);
            this.surveyTv = (TextView) view;
        }

        void setViews(SurveyItem navigationItem) {
            surveyTv.setText(navigationItem.getTitle());
        }
    }
}
