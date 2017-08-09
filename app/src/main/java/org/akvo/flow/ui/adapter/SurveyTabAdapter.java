/*
 *  Copyright (C) 2014-2017 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.ui.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.event.QuestionInteractionListener;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.ui.view.QuestionGroupTab;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.ui.view.SubmitTab;

import java.util.List;

public class SurveyTabAdapter extends RecyclerView.Adapter<SurveyTabAdapter.SurveyViewHolder> {

    private List<QuestionGroup> mQuestionGroups;
    private SurveyListener surveyListener;
    private QuestionInteractionListener questionListener;
    //    private SubmitTab mSubmitTab;

    public SurveyTabAdapter(Context context, SurveyListener surveyListener,
            QuestionInteractionListener questionListener) {
        init(surveyListener, questionListener);
    }

    private void init(SurveyListener surveyListener,
            QuestionInteractionListener questionListener) {
        mQuestionGroups = surveyListener.getQuestionGroups();
        this.surveyListener = surveyListener;
        this.questionListener = questionListener;

    }

    public void notifyOptionsChanged() {
//        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
//            questionGroupTab.notifyOptionsChanged();// Spread the word
//        }
        notifyDataSetChanged();
    }

//    public void reset() {
//        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
//            if (questionGroupTab.isLoaded()) {
//                // Only care about the loaded tabs
//                questionGroupTab.loadState();
//            }
//        }
//    }

    /**
     * Attempt to display a particular question, based on the given ID.
     * If the question is found, the containing tab will be scrolled to the question's position.
     * Upon success the tab position will be returned, -1 otherwise
     */
    public int displayQuestion(String questionId) {
//        for (int i = 0; i < mQuestionGroupTabs.size(); i++) {
//            QuestionGroupTab questionGroupTab = mQuestionGroupTabs.get(i);
//            if (questionGroupTab.displayQuestion(questionId)) {
//                return i;
//            }
//        }
        return -1;
    }

    public void onPause() {
        // Propagate onPause callback
//        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
//            questionGroupTab.onPause();
//        }
    }

    public void onResume() {
        // Propagate onResume callback
//        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
//            questionGroupTab.onResume();
//        }
    }

    public void onDestroy() {
        // Propagate onDestroy callback
//        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
//            questionGroupTab.onDestroy();
//        }
    }

    public void onQuestionComplete(String questionId, Bundle data) {
//        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
//            questionGroupTab.onQuestionComplete(questionId, data);
//        }
    }

    public QuestionView getQuestionView(String questionId) {
        QuestionView questionView = null;
//        for (QuestionGroupTab questionGroupTab : mQuestionGroupTabs) {
//            questionView = questionGroupTab.getQuestionView(questionId);
//            if (questionView != null) {
//                break;
//            }
//        }
        return questionView;
    }

    @Override
    public SurveyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            view = new QuestionGroupTab(parent.getContext(), surveyListener, questionListener);
            return new QuestionViewHolder(view);
        } else {
            view = new SubmitTab(parent.getContext(), surveyListener);
            return new TabViewHolder(view);
        }
    }


    //TODO: use constants
    @Override
    public int getItemViewType(int position) {
        if (position < mQuestionGroups.size()) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public void onBindViewHolder(SurveyViewHolder holder, int position) {
        if (position < mQuestionGroups.size()) {
            ((QuestionViewHolder)holder).loadTab(mQuestionGroups.get(position));
        }

        //submit button no need to load anything
    }


    @Override
    public int getItemCount() {
        if (surveyListener.isReadOnly()) {
            return mQuestionGroups.size();
        }
        return mQuestionGroups.size() + 1; //+1 for submit button
    }

    public static abstract class SurveyViewHolder extends RecyclerView.ViewHolder {

        public SurveyViewHolder(View itemView) {
            super(itemView);
        }
    }

    static class QuestionViewHolder extends SurveyViewHolder {

        private final QuestionGroupTab itemView;

        public QuestionViewHolder(View itemView) {
            super(itemView);
            this.itemView = (QuestionGroupTab) itemView; //TODO: fix this remove cast
        }

        public void loadTab(QuestionGroup questionGroup) {
            itemView.setQuestionGroup(questionGroup);
            itemView.load();
            itemView.loadState();
            itemView.setupDependencies();
            itemView.updateRepeatable();
        }
    }

    static class TabViewHolder extends SurveyViewHolder {

        public TabViewHolder(View itemView) {
            super(itemView);
        }
    }

}
