package org.akvo.flow.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.akvo.flow.R;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.ui.view.BarcodeQuestionView;
import org.akvo.flow.ui.view.DateQuestionView;
import org.akvo.flow.ui.view.FreetextQuestionView;
import org.akvo.flow.ui.view.GeoQuestionView;
import org.akvo.flow.ui.view.MediaQuestionView;
import org.akvo.flow.ui.view.OptionQuestionView;
import org.akvo.flow.ui.view.QuestionHeaderView;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.util.ConstantUtil;

import java.util.List;

public class QuestionGroupFragment extends Fragment {
    private static final String ARG_POSITION = "position";

    private int mPosition;
    private String mSurveyId;
    private long mSurveyInstanceId;
    private QuestionGroup mQuestionGroup;

    private OnFragmentInteractionListener mListener;

    public static QuestionGroupFragment newInstance(int position) {
        QuestionGroupFragment fragment = new QuestionGroupFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }
    public QuestionGroupFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null) {
            throw new IllegalArgumentException("Arguments not supplied");
        }
        mPosition = getArguments().getInt(ARG_POSITION);
        mSurveyId = mListener.getSurveyId();
        mSurveyInstanceId = mListener.getSurveyInstanceId();
        mQuestionGroup = mListener.getQuestionGroup(mPosition);
    }

    @Override
    public void onResume() {
        super.onResume();
        handleDependencies();
    }

    /**
     * handleDependencies manages all the dependencies for this Group.
     * Dependecies may happen across different Question Groups, thus
     * we need to communicate with the Activity, which contains all the
     * responses for the Survey.
     */
    private void handleDependencies() {
        // TODO: TODO
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.i("QuestionGroupFragment", "onCreateView()");
        //View v = inflater.inflate(R.layout.fragment_question_group, container, false);

        final Context context = getActivity();
        final String language = mListener.getDefaultLang();
        final String[] languages = mListener.getLanguages();
        final boolean readOnly = false;

        // TODO: Inflate view from layout file
        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        List<Question> questions = mQuestionGroup.getQuestions();

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
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
            ll.addView(questionView);
            if (i < questions.size() - 1) {
                View ruler = new View(context);
                ruler.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 2));
                ruler.setBackgroundColor(0xFFFFFFFF);
                ll.addView(ruler);
            }
        }

        scrollView.addView(ll);
        return scrollView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        public QuestionGroup getQuestionGroup(int position);
        public String getSurveyId();
        public long getSurveyInstanceId();
        public String getDefaultLang();
        public String[] getLanguages();
    }

}
