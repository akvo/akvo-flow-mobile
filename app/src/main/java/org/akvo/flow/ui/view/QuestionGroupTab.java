/*
 *  Copyright (C) 2014-2015 Stichting Akvo (Akvo Foundation)
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

import android.animation.LayoutTransition;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.akvo.flow.R;
import org.akvo.flow.domain.Dependency;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionListener;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public class QuestionGroupTab extends LinearLayout implements RepetitionHeader.OnDeleteListener {
    private QuestionGroup mQuestionGroup;
    private QuestionInteractionListener mQuestionListener;
    private SurveyListener mSurveyListener;

    private Map<String, QuestionView> mQuestionViews;
    private final Set<String> mQuestions;// Map group's questions for a quick look-up
    private LinearLayout mContainer;
    private boolean mLoaded;

    private int mIterations;
    private LayoutInflater mInflater;
    private TextView mRepetitionsText;

    private Map<Integer, RepetitionHeader> mHeaders;
    private List<Integer> mRepetitions;

    public QuestionGroupTab(Context context, QuestionGroup group,  SurveyListener surveyListener,
            QuestionInteractionListener questionListener) {
        super(context);
        mQuestionGroup = group;
        mSurveyListener = surveyListener;
        mQuestionListener = questionListener;
        mQuestionViews = new HashMap<>();
        mHeaders = new HashMap<>();
        mRepetitions = new ArrayList<>();
        mLoaded = false;
        mIterations = 0;// Repeatable group's instances so far
        mInflater = LayoutInflater.from(context);
        mQuestions = new HashSet<>();
        for (Question q : mQuestionGroup.getQuestions()) {
            mQuestions.add(q.getId());
        }
        init();
    }

    private void init() {
        // Load question group view and set it as ScrollView's child
        // FIXME: Would it make more sense to initialize this attrs in the XML file?
        setOrientation(VERTICAL);
        inflate(getContext(), R.layout.question_group_tab, this);
        mContainer = (LinearLayout)findViewById(R.id.question_list);
        mRepetitionsText = (TextView)findViewById(R.id.repeat_header);

        // Animate view additions/removals if possible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mContainer.setLayoutTransition(new LayoutTransition());
        }

        findViewById(R.id.next_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSurveyListener.nextTab();
            }
        });

        if (mQuestionGroup.isRepeatable()) {
            findViewById(R.id.repeat_header).setVisibility(VISIBLE);
            View repeatBtn = findViewById(R.id.repeat_btn);
            repeatBtn.setVisibility(VISIBLE);
            repeatBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadGroup();
                    setupDependencies();
                }
            });
        }

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
        loadGroup();
    }

    public void notifyOptionsChanged() {
        for (QuestionView qv : mQuestionViews.values()) {
            qv.notifyOptionsChanged();
        }
    }

    public void onQuestionComplete(String questionId, Bundle data) {
        QuestionView qv = mQuestionViews.get(questionId);
        if (qv != null) {
            qv.questionComplete(data);
        }
    }

    /**
     * Checks to make sure the mandatory questions in this tab have a response
     */
    public List<Question> checkInvalidQuestions() {
        List<Question> missingQuestions = new ArrayList<Question>();
        for (QuestionView qv : mQuestionViews.values()) {
            qv.checkMandatory();
            if (!qv.isValid() && qv.areDependenciesSatisfied()) {
                // Only considered invalid if the dependencies are fulfilled
                missingQuestions.add(qv.getQuestion());
            }
        }
        return missingQuestions;
    }

    public void loadState() {
        for (QuestionView qv : mQuestionViews.values()) {
            qv.resetQuestion(false);// Clean start
        }

        // If the group is repeatable, delete multiple iterations
        if (mQuestionGroup.isRepeatable()) {
            mContainer.removeAllViews();
            mQuestionViews.clear();
            mIterations = 0;

            // Load existing iterations. If no iteration is available, show one by default.
            loadRepetitions();
            //int iterCount = getIterationCount();
            int iterCount = mRepetitions.size();
            if (iterCount == 0) {
                // Leave one iteration by default
                iterCount = 1;
            }
            for (int i=0; i<iterCount; i++) {
                loadGroup();
            }
        }

        Map<String, QuestionResponse> responses = mSurveyListener.getResponses();
        for (QuestionView qv : mQuestionViews.values()) {
            final String questionId = qv.getQuestion().getId();
            if (responses.containsKey(questionId)) {
                // Update the question view to reflect the loaded data
                qv.rehydrate(responses.get(questionId));
            }
        }
    }

    public QuestionView getQuestionView(String questionId) {
        return mQuestionViews.get(questionId);
    }

    public void onPause() {
        // Propagate onPause callback
        for (QuestionView qv : mQuestionViews.values()) {
            qv.onPause();
        }
    }

    public void onResume() {
        // Propagate onResume callback
        for (QuestionView qv : mQuestionViews.values()) {
            qv.onResume();
        }
    }

    public boolean isLoaded() {
        return mLoaded;
    }

    private void loadGroup() {
        mIterations++;
        // Get existing ID, or create a new one (incremental).
        if (mRepetitions.size() < mIterations) {
            if (mRepetitions.isEmpty()) {
                mRepetitions.add(0);
            } else {
                mRepetitions.add(mRepetitions.get(mRepetitions.size() - 1) + 1);
            }
        }
        final int repetitionId = mRepetitions.get(mIterations-1);

        if (mQuestionGroup.isRepeatable()) {
            mRepetitionsText.setText("Repetitions: " + mIterations);// FIXME: Externalize string

            RepetitionHeader header = new RepetitionHeader(getContext(), mQuestionGroup.getHeading(),
                    repetitionId, mIterations, this);
            mHeaders.put(repetitionId, header);
            mContainer.addView(header);
        }

        final Context context = getContext();
        for (Question q : mQuestionGroup.getQuestions()) {

            if (mQuestionGroup.isRepeatable()) {
                q = Question.copy(q, q.getId() + "|" + repetitionId);// compound id. (qid|repetition)
            }

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

            mQuestionViews.put(q.getId(), questionView);// Store the reference to the View

            // Add divider (within the View)
            mInflater.inflate(R.layout.divider, questionView);
            mContainer.addView(questionView);
        }
    }

    @Override
    public void onDeleteRepetition(int repetitionID) {
        for (String qid : mQuestions) {
            qid += "|" + repetitionID;
            QuestionView qv = mQuestionViews.get(qid);
            if (qv != null) {
                qv.onPause();
                mQuestionViews.remove(qid);
                mContainer.removeView(qv);
            }
            mSurveyListener.deleteResponse(qid);
        }
        // Adjust header indexes
        for (int id : mRepetitions) {
            if (id > repetitionID) {
                mHeaders.get(id).setIndex(id-1);
            }
        }

        // Remove repetition header
        View header = mHeaders.remove(repetitionID);
        if (header != null) {
            mContainer.removeView(header);
        }

        mRepetitions.remove(Integer.valueOf(repetitionID));
        mIterations--;

        Toast.makeText(getContext(), "Rep " + repetitionID + " deleted", Toast.LENGTH_SHORT).show();
    }

    private void loadRepetitions() {
        Set<Integer> reps = new HashSet<>();
        for (QuestionResponse qr : mSurveyListener.getResponses().values()) {
            String[] qid = qr.getQuestionId().split("\\|", -1);
            if (qid.length == 2 && mQuestions.contains(qid[0])) {
                reps.add(Integer.valueOf(qid[1]));
            }
        }

        mRepetitions = new ArrayList<>(reps);
        Collections.sort(mRepetitions);
    }

    /**
     * getIterationCount calculates the number of iterations in the ongoing form for the current question group.
     */
    private int getIterationCount() {
        int iterations = 0;

        for (QuestionResponse qr : mSurveyListener.getResponses().values()) {
            String[] qid = qr.getQuestionId().split("\\|", -1);
            if (qid.length == 2 && mQuestions.contains(qid[0])) {
                int iteration = Integer.parseInt(qid[1]);
                iterations = Math.max(iterations, iteration);
            }
        }

        return iterations;
    }

    public void setupDependencies() {
        for (QuestionView qv : mQuestionViews.values()) {
            setupDependencies(qv);
        }
    }

    private void setupDependencies(QuestionView qv) {
        final List<Dependency> dependencies = qv.getQuestion().getDependencies();
        if (dependencies == null) {
            return;// No dependencies for this question
        }

        for (Dependency dependency : dependencies) {
            QuestionView parentQ;
            String parentQId = dependency.getQuestion();
            if (mQuestionGroup.isRepeatable() && mQuestions.contains(parentQId)) {
                // Internal dependencies need to compound the inner question ID (questionId|iteration)
                parentQId += "|" + getIteration(qv.getQuestion().getId());
                dependency.setQuestion(parentQId);
                parentQ = getQuestionView(parentQId);// Local search
            } else {
                parentQ = mSurveyListener.getQuestionView(parentQId);// Global search
            }

            if (parentQ != null && qv != parentQ) {
                parentQ.addQuestionInteractionListener(qv);
                qv.checkDependencies();
            }
        }
    }

    private int getIteration(String questionId) {
        String[] qid = questionId.split("\\|", -1);
        if (qid.length == 2) {
            return Integer.parseInt(qid[1]);
        }
        return -1;
    }

}
