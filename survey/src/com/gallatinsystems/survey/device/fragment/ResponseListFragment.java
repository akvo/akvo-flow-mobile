package com.gallatinsystems.survey.device.fragment;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.activity.TransmissionHistoryActivity;
import com.gallatinsystems.survey.device.async.loader.SurveyInstanceLoader;
import com.gallatinsystems.survey.device.dao.SurveyDbAdapter;
import com.gallatinsystems.survey.device.domain.SurveyGroup;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.ViewUtil;
import com.gallatinsystems.survey.device.view.adapter.SubmittedSurveyReviewCursorAdaptor;

public class ResponseListFragment extends ListFragment implements LoaderCallbacks<Cursor>, 
            OnItemClickListener {
    private static final String TAG = ResponseListFragment.class.getSimpleName();
    // Loader id
    private static final int ID_SURVEY_INSTANCE_LIST = 0;
    
    // Menu items
    private static final int DELETE_ALL = 0;
    private static final int RESEND_ALL = 1;
    
    // Context menu items
    private static final int DELETE_ONE = 0;
    private static final int VIEW_HISTORY = 1;
    private static final int RESEND_ONE = 2;
    
    private static final int UPDATE_INTERVAL_MS = 10000; // every ten seconds
    
    private String mUserId;
    private SurveyGroup mSurveyGroup;
    private String mSurveyedLocaleId;
    private SubmittedSurveyReviewCursorAdaptor mAdapter;
    
    private SurveyDbAdapter mDatabase;
    
    private Handler mHandler = new Handler();
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            refresh();
            mHandler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mHandler.post(mUpdateTimeTask);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mUpdateTimeTask);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabase.close();
    }
    
    public void refresh(SurveyGroup surveyGroup, String surveyedLocaleId) {
        mSurveyGroup = surveyGroup;
        mSurveyedLocaleId = surveyedLocaleId;
        refresh();
    }
    
    private void refresh() {// TODO: Rename these methods
        getLoaderManager().restartLoader(ID_SURVEY_INSTANCE_LIST, null, ResponseListFragment.this);
    }
    
    public static ResponseListFragment instantiate() {
        ResponseListFragment fragment = new ResponseListFragment();
        return fragment;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDatabase = new SurveyDbAdapter(getActivity());
        mDatabase.open();

        if(mAdapter == null) {
            mAdapter = new SubmittedSurveyReviewCursorAdaptor(getActivity());// Cursor Adapter
            setListAdapter(mAdapter);
        }
        registerForContextMenu(getListView());// Same implementation as before
        setHasOptionsMenu(true);
        
        refresh();
    }
    
    public void setUserId(String userId) {
        mUserId = userId;
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(getActivity(), "Not Implemented", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.add(0, DELETE_ONE, 0, R.string.deletesurvey);
        menu.add(0, VIEW_HISTORY, 1, R.string.transmissionhist);
        menu.add(0, RESEND_ONE, 2, R.string.resendone);
    }

    /**
     * Presents the survey options menu when the user presses the menu key
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, DELETE_ALL, 0, R.string.deleteall);
        menu.add(0, RESEND_ALL, 1, R.string.resendall);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        Long surveyInstanceId = mAdapter.getItemId(info.position);// This ID is the _id column in the SQLite db
        switch (item.getItemId()) {
            case DELETE_ONE:
                deleteSurveyInstance(surveyInstanceId);
                break;
            case VIEW_HISTORY:
                viewSurveyInstanceHistory(surveyInstanceId);
                break;
            case RESEND_ONE:
                resendSurveyInstance(surveyInstanceId);
                break;
        }
        return true;
    }
    
    private void deleteSurveyInstance(final long surveyInstanceId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.deleteonewarning)
                .setCancelable(true)
                .setPositiveButton(R.string.okbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int id) {
                                SurveyDbAdapter db = new SurveyDbAdapter(getActivity()).open();
                                db.deleteRespondent(String.valueOf(surveyInstanceId));
                                db.close();
                                refresh();
                            }
                        })
                .setNegativeButton(R.string.cancelbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int id) {
                                dialog.cancel();
                            }
                        });
        builder.show();
    }
    
    private void viewSurveyInstanceHistory(long surveyInstanceId) {
        Intent i = new Intent(getActivity(), TransmissionHistoryActivity.class);
        i.putExtra(ConstantUtil.RESPONDENT_ID_KEY, surveyInstanceId);
        startActivity(i);
    }
    
    private void resendSurveyInstance(final long surveyInstanceId) {
        ViewUtil.showAdminAuthDialog(getActivity(),
                new ViewUtil.AdminAuthDialogListener() {
                    @Override
                    public void onAuthenticated() {
                        SurveyDbAdapter db = new SurveyDbAdapter(getActivity()).open();
                        db.markDataUnsent(surveyInstanceId);
                        db.close();
                        Intent dataIntent = new Intent(
                                ConstantUtil.DATA_AVAILABLE_INTENT);
                        getActivity().sendBroadcast(dataIntent);
                        ViewUtil.showConfirmDialog(
                                R.string.submitcompletetitle,
                                R.string.submitcompletetext,
                                getActivity());
                    }
                });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ID_SURVEY_INSTANCE_LIST:
                final int surveyGroupId = mSurveyGroup != null ? mSurveyGroup.getId() : SurveyGroup.ID_NONE;
                return new SurveyInstanceLoader(getActivity(), mDatabase, surveyGroupId, mSurveyedLocaleId);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case ID_SURVEY_INSTANCE_LIST:
                mAdapter.changeCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
    
}
