/*
 *  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */
package org.akvo.flow.ui.view;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionListener;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestionGroupTab extends ScrollView {
    private QuestionGroup mQuestionGroup;
    private QuestionInteractionListener mQuestionListener;
    private SurveyListener mSurveyListener;

    private List<QuestionView> mQuestionViews;
    private LinearLayout mContainer;
    private boolean mLoaded;

    public QuestionGroupTab(Context context, QuestionGroup group,  SurveyListener surveyListener,
            QuestionInteractionListener questionListener) {
        super(context);
        mQuestionGroup = group;
        mSurveyListener = surveyListener;
        mQuestionListener = questionListener;
        mQuestionViews = new ArrayList<>();
        mLoaded = false;
        init();
    }

    private void init() {
        // Instantiate LinearLayout container and set it as ScrollView's child
        mContainer = new LinearLayout(getContext());
        mContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        mContainer.setOrientation(LinearLayout.VERTICAL);
        addView(mContainer);

        setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    /**
     * Pre-load all the QuestionViews in memory. getView() will simply
     * retrieve them from the corresponding position in mQuestionViews.
     */
    public void load() {
        mLoaded = true;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        final Context context = getContext();
        for (Question q : mQuestionGroup.getQuestions()) {
            QuestionView questionView;
            if (ConstantUtil.OPTION_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new OptionQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.FREE_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new FreetextQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.PHOTO_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new MediaQuestionView(context, q, mSurveyListener,
                        ConstantUtil.PHOTO_QUESTION_TYPE);
            } else if (ConstantUtil.VIDEO_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new MediaQuestionView(context, q, mSurveyListener,
                        ConstantUtil.VIDEO_QUESTION_TYPE);
            } else if (ConstantUtil.GEO_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new GeoQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.SCAN_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new BarcodeQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.DATE_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new DateQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.CASCADE_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new CascadeQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.GEOSHAPE_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new GeoshapeQuestionView(context, q, mSurveyListener);
            } else {
                questionView = new QuestionHeaderView(context, q, mSurveyListener);
            }

            // Add question interaction listener
            questionView.addQuestionInteractionListener(mQuestionListener);

            mQuestionViews.add(questionView);// Store the reference to the View

            // Add divider (within the View)
            inflater.inflate(R.layout.divider, questionView);
            mContainer.addView(questionView);
        }

        if (!mSurveyListener.isReadOnly()) {
            Button next = new Button(context);
            next.setText(R.string.nextbutton);
            next.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            next.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSurveyListener.nextTab();
                }
            });
            mContainer.addView(next);
        }
    }

    public void notifyOptionsChanged() {
        for (QuestionView view : mQuestionViews) {
            view.notifyOptionsChanged();
        }
    }

    public void onQuestionComplete(String questionId, Bundle data) {
        for (QuestionView view : mQuestionViews) {
            if (questionId.equals(view.getQuestion().getId())) {
                // TODO: Optimize this lookup (Map)
                view.questionComplete(data);
            }
        }
    }

    /**
     * checks to make sure the mandatory questions in this tab have a response
     */
    public List<Question> checkInvalidQuestions() {
        List<Question> missingQuestions = new ArrayList<>();
        for (QuestionView view : mQuestionViews) {
            view.checkMandatory();
            if (!view.isValid() && view.areDependenciesSatisfied()) {
                // Only considered invalid if the dependencies are fulfilled
                missingQuestions.add(view.getQuestion());
            }
        }
        return missingQuestions;
    }

    public void loadState() {
        Map<String, QuestionResponse> responses = mSurveyListener.getResponses();
        for (QuestionView questionView : mQuestionViews) {
            questionView.resetQuestion(false);// Clean start
            final String questionId = questionView.getQuestion().getId();
            if (responses.containsKey(questionId)) {
                final QuestionResponse response = responses.get(questionId);
                // Update the question view to reflect the loaded data
                questionView.rehydrate(response);
            }
        }
    }

    public QuestionView getQuestionView(String questionId) {
        for (QuestionView questionView : mQuestionViews) {
            if (questionId.equals(questionView.getQuestion().getId())) {
                return questionView;
            }
        }
        return null;
    }

    public void onPause() {
        // Propagate onPause callback
        for (QuestionView q : mQuestionViews) {
            q.onPause();
        }
    }

    public void onResume() {
        // Propagate onResume callback
        for (QuestionView q : mQuestionViews) {
            q.onResume();
        }
    }

    public boolean isLoaded() {
        return mLoaded;
    }

}
