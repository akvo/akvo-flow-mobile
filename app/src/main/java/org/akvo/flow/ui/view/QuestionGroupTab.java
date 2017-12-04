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

package org.akvo.flow.ui.view;

import android.animation.LayoutTransition;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.domain.Dependency;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.event.QuestionInteractionListener;
import org.akvo.flow.event.SurveyListener;
import org.akvo.flow.ui.view.barcode.BarcodeQuestionViewFactory;
import org.akvo.flow.ui.view.geolocation.GeoQuestionView;
import org.akvo.flow.ui.view.option.OptionQuestionFactory;
import org.akvo.flow.ui.view.signature.SignatureQuestionView;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuestionGroupTab extends LinearLayout implements QuestionGroupIterationHeader.OnDeleteListener {

    private final QuestionGroup mQuestionGroup;
    private final QuestionInteractionListener mQuestionListener;
    private final SurveyListener mSurveyListener;

    private final Map<String, QuestionView> mQuestionViews;
    private final Set<String> mQuestions;// Map group's questions for a quick look-up
    private LinearLayout mContainer;
    private ScrollView mScroller;
    private boolean mLoaded;

    private TextView mRepetitionsText;

    private final Map<Integer, QuestionGroupIterationHeader> groupIterationHeaders;
    private final RepeatableGroupIterations groupIterations;

    public QuestionGroupTab(Context context, QuestionGroup group, SurveyListener surveyListener,
            QuestionInteractionListener questionListener) {
        super(context);
        mQuestionGroup = group;
        mSurveyListener = surveyListener;
        mQuestionListener = questionListener;
        mQuestionViews = new HashMap<>();
        groupIterationHeaders = new HashMap<>();
        groupIterations = new RepeatableGroupIterations();
        mLoaded = false;
        mQuestions = new HashSet<>();
        for (Question q : mQuestionGroup.getQuestions()) {
            mQuestions.add(q.getId());
        }
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);
        setFocusable(true);
        setFocusableInTouchMode(true);

        inflate(getContext(), R.layout.question_group_tab, this);
        mScroller = (ScrollView) findViewById(R.id.scroller);
        mContainer = (LinearLayout) findViewById(R.id.question_list);
        mRepetitionsText = (TextView) findViewById(R.id.repeat_header);

        // Animate view additions/removals if possible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mContainer.setLayoutTransition(new LayoutTransition());
        }

        View next = findViewById(R.id.next_btn);
        next.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSurveyListener.nextTab();
            }
        });
        next.setVisibility(mSurveyListener.isReadOnly() ? GONE : VISIBLE);

        if (mQuestionGroup.isRepeatable()) {
            mRepetitionsText.setVisibility(VISIBLE);
            View repeatBtn = findViewById(R.id.repeat_btn);
            repeatBtn.setVisibility(mSurveyListener.isReadOnly() ? GONE : VISIBLE);
            repeatBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadGroup();
                    setupDependencies();
                }
            });
        }
        setTag(mQuestionGroup.getOrder());
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
        List<Question> missingQuestions = new ArrayList<>();
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

        // FIXME: Call loadGroup here, remove original one

        // If the group is repeatable, delete multiple iterations
        if (mQuestionGroup.isRepeatable()) {
            mContainer.removeAllViews();
            mQuestionViews.clear();

            // Load existing iterations. If no iteration is available, show one by default.
            groupIterations.loadIDs(mQuestions, mSurveyListener.getResponses().values());
            int iterCount = Math.max(groupIterations.size(), 1);
            for (int i = 0; i < iterCount; i++) {
                loadGroup(i);
            }
            updateGroupIterationHeaders();
        }

        displayResponses();
    }

    private void updateGroupIterationHeaders() {
        if (groupIterationHeaders.size() > 1) {
            Collection<QuestionGroupIterationHeader> iterationHeaders = groupIterationHeaders
                    .values();
            for (QuestionGroupIterationHeader header : iterationHeaders) {
                header.enableDeleteButton();
            }
        } else if (groupIterationHeaders.size() == 1) {
            QuestionGroupIterationHeader header = groupIterationHeaders.entrySet().iterator().next()
                    .getValue();
            header.disableDeleteButton();
        }
    }

    private void displayResponses() {
        Map<String, QuestionResponse> responses = mSurveyListener.getResponses();
        for (QuestionView qv : mQuestionViews.values()) {
            String questionId = qv.getQuestion().getId();
            if (responses.containsKey(questionId)) {
                qv.rehydrate(responses.get(questionId));
            } else if (qv.getQuestion().isRepeatable() && !TextUtils.isEmpty(questionId)) {
                questionId = questionId.split("\\|")[0];
                if (responses.containsKey(questionId)) {
                    qv.rehydrate(responses.get(questionId));
                }
            }
        }
    }

    public QuestionView getQuestionView(String questionId) {
        return mQuestionViews.get(questionId);
    }

    /**
     * Attempt to display a particular question, based on the given question ID.
     */
    public boolean displayQuestion(String questionId) {
        QuestionView qv = getQuestionView(questionId);
        if (qv != null) {
            mScroller.scrollTo(qv.getLeft(), qv.getTop());
            return true;
        }
        return false;
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

    public void onDestroy() {
        // Propagate onDestroy callback
        for (QuestionView qv : mQuestionViews.values()) {
            qv.onDestroy();
        }
    }

    public boolean isLoaded() {
        return mLoaded;
    }

    private void updateRepetitionsHeader() {
        //TODO: replace string concatenation by replaceable params inside string and/or plurals
        mRepetitionsText
                .setText(getContext().getString(R.string.repetitions) + groupIterations.size());
    }

    private void loadGroup() {
        loadGroup(groupIterations.size());
        if (mQuestionGroup.isRepeatable()) {
            updateGroupIterationHeaders();
        }
    }

    private void loadGroup(int index) {
        final int repetitionId = getRepetitionId(index);
        final int visualIndicator = index + 1;

        if (mQuestionGroup.isRepeatable()) {
            updateRepetitionsHeader();
            QuestionGroupIterationHeader header =
                    new QuestionGroupIterationHeader(getContext(), mQuestionGroup.getHeading(),
                            repetitionId, visualIndicator,
                            mSurveyListener.isReadOnly() ? null : this);
            groupIterationHeaders.put(repetitionId, header);
            mContainer.addView(header);
        }

        final Context context = getContext();
        for (Question q : mQuestionGroup.getQuestions()) {
            if (mQuestionGroup.isRepeatable()) {
                q = Question.copy(q, q.getId() + "|" + repetitionId);
            }

            QuestionView questionView;
            if (ConstantUtil.OPTION_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = OptionQuestionFactory.createOptionQuestion(context, q, mSurveyListener);
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
                questionView = BarcodeQuestionViewFactory
                        .createBarcodeQuestion(context, q, mSurveyListener);
            } else if (ConstantUtil.DATE_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new DateQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.CASCADE_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new CascadeQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.GEOSHAPE_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new GeoshapeQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.SIGNATURE_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new SignatureQuestionView(context, q, mSurveyListener);
            } else if (ConstantUtil.CADDISFLY_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                questionView = new CaddisflyQuestionView(context, q, mSurveyListener);
            } else {
                questionView = new QuestionHeaderView(context, q, mSurveyListener);
            }

            // Add question interaction listener
            questionView.addQuestionInteractionListener(mQuestionListener);

            mQuestionViews.put(q.getId(), questionView);// Store the reference to the View

            // Add divider (within the View)
            inflate(getContext(), R.layout.divider, questionView);
            mContainer.addView(questionView);
        }
    }

    private int getRepetitionId(int index) {
        return groupIterations.size() <= index ?
                groupIterations.next() :
                groupIterations.getRepetitionId(index);
    }

    @Override
    public void onDeleteRepetition(Integer repetitionID) {
        // Delete question views and corresponding responses
        for (String questionId : mQuestions) {
            String qid = questionId + "|" + repetitionID;
            QuestionView qv = mQuestionViews.get(qid);
            if (qv != null) {
                qv.onDestroy();
                mQuestionViews.remove(qid);
                mContainer.removeView(qv);
            }
            mSurveyListener.deleteResponse(questionId, String.valueOf(repetitionID));
        }

        // Rearrange header positions (just the visual indicator).
        for (Integer id : groupIterations) {
            if (id.intValue() == repetitionID.intValue()) {
                View header = groupIterationHeaders.remove(repetitionID);
                if (header != null) {
                    mContainer.removeView(header);
                }
            } else if (id > repetitionID && groupIterationHeaders.containsKey(id)) {
                groupIterationHeaders.get(id).decreasePosition();
            }
        }

        // Remove the ID from the repetitions list.
        groupIterations.remove(repetitionID);
        updateRepetitionsHeader();
        updateGroupIterationHeaders();
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
                parentQId += "|" + parseRepetitionId(qv.getQuestion().getId());
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

    private int parseRepetitionId(String questionId) {
        String[] qid = questionId.split("\\|", -1);
        if (qid.length == 2) {
            return Integer.parseInt(qid[1]);
        }
        return -1;
    }

    static class RepeatableGroupIterations implements Iterable<Integer> {

        List<Integer> mIDs = new ArrayList<>();

        /**
         * For the given form instance, load the list of repetitions IDs.
         * The populated list will contain the IDs of existing repetitions.
         * Although IDs are auto-incremented numeric values, there might be
         * gaps caused by deleted iterations.
         *
         * @param questions
         * @param questionResponses
         */
        void loadIDs(Set<String> questions, Collection<QuestionResponse> questionResponses) {
            Set<Integer> reps = new HashSet<>();
            for (QuestionResponse qr : questionResponses) {
                String qid = qr.getQuestionId();
                if (!TextUtils.isEmpty(qid) && questions.contains(qid) && qr
                        .isAnswerToRepeatableGroup()) {
                    reps.add(qr.getIteration());
                }
            }

            mIDs = new ArrayList<>(reps);
            Collections.sort(mIDs);
        }

        /**
         * Create and return the next repetition's ID.
         */
        int next() {
            int id = 0;
            if (!mIDs.isEmpty()) {
                id = mIDs.get(mIDs.size() - 1) + 1;// Increment last item's ID
            }
            mIDs.add(id);
            return id;
        }

        int getRepetitionId(int index) {
            return mIDs.get(index);
        }

        int size() {
            return mIDs.size();
        }

        void remove(Integer repetitionID) {
            mIDs.remove(repetitionID);
        }

        @Override
        public Iterator<Integer> iterator() {
            return mIDs.iterator();
        }
    }
}
