/*
 *  Copyright (C) 2013-2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.data.loader.SurveyInstanceResponseLoader;
import org.akvo.flow.database.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.ui.Navigator;
import org.akvo.flow.ui.adapter.ResponseListAdapter;
import org.akvo.flow.util.ConstantUtil;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import timber.log.Timber;

import static org.akvo.flow.util.ConstantUtil.DATA_POINT_ID_EXTRA;
import static org.akvo.flow.util.ConstantUtil.READ_ONLY_TAG_KEY;
import static org.akvo.flow.util.ConstantUtil.RESPONDENT_ID_TAG_KEY;
import static org.akvo.flow.util.ConstantUtil.SURVEY_GROUP_EXTRA;
import static org.akvo.flow.util.ConstantUtil.SURVEY_ID_TAG_KEY;

public class ResponseListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

    // Context menu items
    private static final int DELETE_ONE = 0;
    private static final int VIEW_HISTORY = 1;

    private SurveyGroup mSurveyGroup;
    private ResponseListAdapter mAdapter;
    private String recordId;

    @Nullable
    private ResponseListListener responseListListener;

    @Inject
    Navigator navigator;

    public static ResponseListFragment newInstance() {
        return new ResponseListFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if (activity instanceof ResponseListListener) {
            responseListListener = (ResponseListListener) activity;
        } else {
            throw new IllegalArgumentException("activity must implement ResponseListListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Intent intent = getActivity().getIntent();
        mSurveyGroup = (SurveyGroup) intent.getSerializableExtra(SURVEY_GROUP_EXTRA);
        recordId = intent.getStringExtra(DATA_POINT_ID_EXTRA);
        if (mAdapter == null) {
            mAdapter = new ResponseListAdapter(getActivity());
            setListAdapter(mAdapter);
        }
        registerForContextMenu(getListView());
        setHasOptionsMenu(true);
        initializeInjector();
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent())
                .build();
        viewComponent.inject(this);
    }

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return {@link ApplicationComponent}
     */
    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getActivity().getApplication()).getApplicationComponent();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(dataSyncReceiver,
                new IntentFilter(ConstantUtil.ACTION_DATA_SYNC));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(dataSyncReceiver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.responseListListener = null;
    }

    private void refresh() {
        getLoaderManager().restartLoader(0, null, ResponseListFragment.this);
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View view,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.add(0, VIEW_HISTORY, 0, R.string.transmissionhist);

        // Allow deletion only for 'saved' responses
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        View itemView = info.targetView;
        if (!(Boolean) itemView.getTag(READ_ONLY_TAG_KEY)) {
            menu.add(0, DELETE_ONE, 2, R.string.deleteresponse);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();

        // This ID is the _id column in the SQLite db
        long surveyInstanceId = mAdapter.getItemId(info.position);
        switch (item.getItemId()) {
            case DELETE_ONE:
                View itemView = info.targetView;
                showConfirmationDialog(surveyInstanceId, itemView.getTag(SURVEY_ID_TAG_KEY) + "");
                break;
            case VIEW_HISTORY:
                viewSurveyInstanceHistory(surveyInstanceId);
                break;
        }
        return true;
    }

    private void showConfirmationDialog(final long surveyInstanceId, final String surveyId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.deleteonewarning)
                .setCancelable(true)
                .setPositiveButton(R.string.okbutton,
                        (dialog, id) -> deleteSurveyInstance(surveyId, surveyInstanceId))
                .setNegativeButton(R.string.cancelbutton,
                        (dialog, id) -> dialog.cancel());
        builder.show();
    }

    private void deleteSurveyInstance(String surveyId, long surveyInstanceId) {
        Context context = getActivity().getApplicationContext();
        SurveyDbAdapter db = new SurveyDbAdapter(context);
        boolean nameResetNeeded = surveyId != null && surveyId
                .equals(mSurveyGroup.getRegisterSurveyId());
        db.open();
        if (nameResetNeeded) {
            db.clearSurveyedLocaleName(surveyInstanceId);
        }
        db.deleteSurveyInstance(String.valueOf(surveyInstanceId));
        db.close();
        if (nameResetNeeded && responseListListener != null) {
            responseListListener.onDataPointNameDeleted();
        }
        refresh();
    }

    private void viewSurveyInstanceHistory(long surveyInstanceId) {
        navigator.navigateToTransmissionActivity(getActivity(), surveyInstanceId);
    }

    @Override
    public void onListItemClick(@NonNull ListView list, View view, int position, long id) {
        String formId = view.getTag(SURVEY_ID_TAG_KEY).toString();
        Long formInstanceId = (Long) view.getTag(RESPONDENT_ID_TAG_KEY);
        Boolean readOnly = (Boolean) view.getTag(READ_ONLY_TAG_KEY);
        navigator.navigateToFormActivity(getActivity(), recordId, formId,
                formInstanceId, readOnly, mSurveyGroup);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SurveyInstanceResponseLoader(getActivity(), recordId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        //EMPTY
    }

    /**
     * TODO: make a static inner class to avoid memory leaks
     * BroadcastReceiver to notify of data synchronisation. This should be
     * fired from DataFixWorker.
     */
    private final BroadcastReceiver dataSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.i("Survey Instance status has changed. Refreshing UI...");
            refresh();
        }
    };

    public interface ResponseListListener {
        void onDataPointNameDeleted();
    }
}
