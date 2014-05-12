package org.akvo.flow.ui.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

import org.akvo.flow.R;
import org.akvo.flow.async.loader.SurveyInstanceLoader;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.dao.SurveyDbAdapter.SurveyInstanceStatus;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.ui.adapter.ResponseListAdapter;
import org.akvo.flow.util.PlatformUtil;

/**
 * Dialog presented to the user when non-submitted Responses exist for
 * a particular Survey. The Dialog will present a List with those responses,
 * allowing the user to resume any of those ongoing Surveys, and an additional
 * 'Start New Survey' option, to start a whole new Response from scratch.
 */
public class ResponsesDialogFragment extends DialogFragment implements OnItemClickListener,
            LoaderManager.LoaderCallbacks<Cursor> {
    private static final String EXTRA_SURVEY_GROUP = "survey_group";
    private static final String EXTRA_RECORD       = "record";
    private static final String EXTRA_SURVEY       = "survey";

    private SurveyGroup mSurveyGroup;
    private String mSurveyId;
    private String mRecordId;// null if non-monitored group

    private ResponseListAdapter mAdapter;
    private ResponsesDialogListener mListener;
    private SurveyDbAdapter mDatabase;

    public interface ResponsesDialogListener {
        public void onResponseClick(String surveyId, String recordId, long surveyInstanceId);
        public void onNewResponse(String surveyId);
    }

    public static ResponsesDialogFragment instantiate(SurveyGroup surveyGroup, String surveyId,
            String recordId) {
        ResponsesDialogFragment fragment = new ResponsesDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_SURVEY_GROUP, surveyGroup);
        args.putString(EXTRA_RECORD, recordId);
        args.putString(EXTRA_SURVEY, surveyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurveyGroup = (SurveyGroup) getArguments().getSerializable(EXTRA_SURVEY_GROUP);
        mRecordId = getArguments().getString(EXTRA_RECORD);
        mSurveyId = getArguments().getString(EXTRA_SURVEY);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = (ResponsesDialogListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ResponsesDialogListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mDatabase == null) {
            mDatabase = new SurveyDbAdapter(getActivity());
            mDatabase.open();
        }

        // Load Responses Cursor
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ContextThemeWrapper context = new ContextThemeWrapper(getActivity(), R.style.Flow_Dialog);

        mAdapter = new ResponseListAdapter(context);

        ListView listView = new ListView(context);
        listView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button button = new Button(context);
        button.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                ListView.LayoutParams.WRAP_CONTENT));
        button.setGravity(Gravity.CENTER);
        button.setText(R.string.start_new_response);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onNewResponse(mSurveyId);
                dismiss();
            }
        });

        listView.addFooterView(button);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);

        getDialog().setTitle(R.string.choose_response);

        return listView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String recordId = (String)view.getTag(ResponseListAdapter.RECORD_KEY);
        long surveyInstanceId = (Long)view.getTag(ResponseListAdapter.RESP_ID_KEY);
        mListener.onResponseClick(mSurveyId, recordId, surveyInstanceId);
        dismiss();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SurveyInstanceLoader(getActivity(), mDatabase, mSurveyGroup.getId(), mRecordId,
                mSurveyId, SurveyInstanceStatus.SAVED);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

}
