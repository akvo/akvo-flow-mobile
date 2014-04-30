package org.akvo.flow.ui.view;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
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

    public QuestionGroupTab(Context context, QuestionGroup group,  SurveyListener surveyListener,
            QuestionInteractionListener questionListener) {
        super(context);
        mQuestionGroup = group;
        mSurveyListener = surveyListener;
        mQuestionListener = questionListener;
        mQuestionViews = new ArrayList<QuestionView>();
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
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        // We set the ScrollView focusable in order to catch the focus when scrolling.
        // This will prevent weird behaviors when errors are present in focused
        // QuestionViews (scroll gets stuck at that position)
        requestFocus();
        super.onScrollChanged(l, t, oldl, oldt);
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
     *
     * @return
     */
    public List<Question> checkInvalidQuestions() {
        List<Question> missingQuestions = new ArrayList<Question>();
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
        for (QuestionView q : mQuestionViews) {
            // Notify the View so it can release any system resource (i.e. Location updates)
            q.releaseResources();
        }
    }

}
