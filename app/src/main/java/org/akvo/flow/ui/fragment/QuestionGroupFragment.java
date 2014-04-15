package org.akvo.flow.ui.fragment;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.ui.view.BarcodeQuestionView;
import org.akvo.flow.ui.view.DateQuestionView;
import org.akvo.flow.ui.view.FreetextQuestionView;
import org.akvo.flow.ui.view.GeoQuestionView;
import org.akvo.flow.ui.view.MediaQuestionView;
import org.akvo.flow.ui.view.OptionQuestionView;
import org.akvo.flow.ui.view.QuestionHeaderView;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.util.ConstantUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestionGroupFragment extends ListView {
    private QuestionGroup mQuestionGroup;

    private QuestionListAdapter mAdapter;
    private OnFragmentInteractionListener mListener;
    private SurveyDbAdapter mDatabase;

    private List<QuestionView> mQuestionViews;
    private Map<String, QuestionResponse> mQuestionResponses;// QuestionId - QuestionResponse

    public QuestionGroupFragment (Context context, OnFragmentInteractionListener listener,
            QuestionGroup group, SurveyDbAdapter database) {
        super(context);
        mQuestionGroup = group;
        mListener = listener;
        mDatabase = database;
        mQuestionViews = new ArrayList<QuestionView>();
        mQuestionResponses = new HashMap<String, QuestionResponse>();

        mAdapter = new QuestionListAdapter(mQuestionGroup.getQuestions());
        mAdapter.load();
        setAdapter(mAdapter);
    }

    public void resetQuestions() {
        for (QuestionView view : mQuestionViews) {
            view.resetQuestion(false);
        }
        mQuestionResponses.clear();
        setSelection(0);
    }

    public void updateQuestionLanguages(String[] langCodes) {
        for (QuestionView view : mQuestionViews) {
            view.updateSelectedLanguages(langCodes);
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
            if (!view.isValid()) {
                missingQuestions.add(view.getQuestion());
            }
        }
        return missingQuestions;
    }

    /**
     * Get the *current* UI responses in this tab. Note that this are not the same as stored
     * responses, as we are not loading the state from the DB, just retrieven the current values.
     * TODO: Cache. We should not loop through the QuestionViews each time the responses are requested.
     */
    public Map<String, QuestionResponse> getResponses() {
        Map<String, QuestionResponse> responses = new HashMap<String, QuestionResponse>();
        for (QuestionView q : mQuestionViews) {
            responses.put(q.getQuestion().getId(), q.getResponse(true));
        }

        return responses;
    }

    public void loadState(Map<String, QuestionResponse> responses) {
        loadState(responses, false);
    }

    public void loadState(Map<String, QuestionResponse> responses, boolean prefill) {
        if (mQuestionResponses == null) {
            mQuestionResponses = new HashMap<String, QuestionResponse>();
        }

        for (QuestionView questionView : mQuestionViews) {
            final String questionId = questionView.getQuestion().getId();
            if (responses.containsKey(questionId)) {
                final QuestionResponse response = responses.get(questionId);

                if (prefill) {
                    // Copying values from old instance; Get rid of its Id
                    // Also, update the SurveyInstance Id, matching the current one
                    response.setId(null);
                    response.setRespondentId(mListener.getSurveyInstanceId());

                    mDatabase.createOrUpdateSurveyResponse(response);
                }

                mQuestionResponses.put(questionId, response);

                // Update the question view to reflect the loaded data
                questionView.rehydrate(response);
            }
        }
    }

    /**
     * updates text size of all questions in this tab
     *
     * @param size
     */
    public void updateTextSize(float size) {
        for (QuestionView qv : mQuestionViews) {
            qv.setTextSize(size);
        }
    }

    /**
     * persists the current question responses in this tab to the database
     *
     * @param respondentId
     */
    public void saveState(Long respondentId) {
        if (mQuestionResponses == null) {
            mQuestionResponses = new HashMap<String, QuestionResponse>();
        }

        for (QuestionView q : mQuestionViews) {
            QuestionResponse curResponse = q.getResponse(true);
            if (curResponse != null && curResponse.hasValue()) {
                curResponse.setRespondentId(respondentId);
                mDatabase.createOrUpdateSurveyResponse(curResponse);
                mQuestionResponses.put(curResponse.getQuestionId(), curResponse);
            } else if (curResponse != null && curResponse.getId() != null
                    && curResponse.getId() > 0) {
                // if we don't have a value BUT there is an ID, we need to
                // remove it since the user blanked out their response
                mDatabase.deleteResponse(respondentId, q.getQuestion().getId());
                mQuestionResponses.remove(curResponse.getQuestionId());
            } else if (curResponse != null) {
                // if we're here, the response is blank but hasn't been
                // saved yet (has no ID) so we can just discard it
                mQuestionResponses.remove(curResponse.getQuestionId());
            }

            // Notify the View so it can release any system resource (i.e.
            // Location updates)
            q.releaseResources();
        }
    }

    class QuestionListAdapter extends BaseAdapter {
        private List<Question> mQuestions;

        public QuestionListAdapter(List<Question> questions) {
            mQuestions = questions;
        }

        /**
         * Pre-load all the QuestionViews in memory. getView() will simply
         * retrieve them from the corresponding position in mQuestionViews.
         */
        public void load() {
            for (Question q : mQuestions) {
                final Context context = getContext();
                final String language = mListener.getDefaultLang();
                final String[] languages = mListener.getLanguages();
                final boolean readOnly = false;

                QuestionView questionView;
                if (ConstantUtil.OPTION_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                    questionView = new OptionQuestionView(context, q,
                            language, languages, readOnly);
                } else if (ConstantUtil.FREE_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                    questionView = new FreetextQuestionView(context, q, language, languages, readOnly);
                } else if (ConstantUtil.PHOTO_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                    questionView = new MediaQuestionView(context, q, ConstantUtil.PHOTO_QUESTION_TYPE,
                            language, languages, readOnly);
                } else if (ConstantUtil.VIDEO_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                    questionView = new MediaQuestionView(context, q, ConstantUtil.VIDEO_QUESTION_TYPE,
                            language, languages, readOnly);
                } else if (ConstantUtil.GEO_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                    questionView = new GeoQuestionView(context, q,language, languages, readOnly);
                } else if (ConstantUtil.SCAN_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                    questionView = new BarcodeQuestionView(context, q, language, languages, readOnly);
                } else if (ConstantUtil.DATE_QUESTION_TYPE.equalsIgnoreCase(q.getType())) {
                    questionView = new DateQuestionView(context, q, language, languages, readOnly);
                } else {
                    questionView = new QuestionHeaderView(context, q, language, languages, readOnly);
                }

                // Store the reference to the View
                mQuestionViews.add(questionView);
            }

        }

        @Override
        public int getCount() {
            return mQuestions.size();
        }

        @Override
        public Object getItem(int position) {
            return mQuestions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mQuestionViews.get(position);
        }

    }

    public interface OnFragmentInteractionListener {
        public String getSurveyId();
        public long getSurveyInstanceId();
        public String getDefaultLang();
        public String[] getLanguages();
        public boolean isReadOnly();
        //public void establishDependencies(QuestionGroup group);
    }

}
