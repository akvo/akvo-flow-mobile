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
package org.akvo.flow.ui.fragment

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ListView
import androidx.fragment.app.ListFragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.data.loader.SurveyInstanceResponseLoader
import org.akvo.flow.database.SurveyDbAdapter
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.injector.component.ApplicationComponent
import org.akvo.flow.injector.component.DaggerViewComponent
import org.akvo.flow.tracking.TrackingHelper
import org.akvo.flow.ui.Navigator
import org.akvo.flow.ui.adapter.ResponseListAdapter
import org.akvo.flow.util.ConstantUtil
import timber.log.Timber
import javax.inject.Inject

class ResponseListFragment : ListFragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private var mSurveyGroup: SurveyGroup? = null
    private var recordId: String? = null
    private var responseListListener: ResponseListListener? = null
    private var trackingHelper: TrackingHelper? = null
    private lateinit var mAdapter: ResponseListAdapter

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var databaseHelper: SQLiteOpenHelper

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        responseListListener = if (activity is ResponseListListener) {
            activity
        } else {
            throw IllegalArgumentException("activity must implement ResponseListListener")
        }
        trackingHelper = TrackingHelper(activity)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val intent = activity?.intent
        mSurveyGroup = intent?.getSerializableExtra(ConstantUtil.SURVEY_GROUP_EXTRA) as SurveyGroup?
        recordId = intent?.getStringExtra(ConstantUtil.DATA_POINT_ID_EXTRA)
        mAdapter = ResponseListAdapter(activity)
        listAdapter = mAdapter
        registerForContextMenu(listView)
        setHasOptionsMenu(true)
        initializeInjector()
    }

    private fun initializeInjector() {
        val viewComponent = DaggerViewComponent.builder()
            .applicationComponent(applicationComponent)
            .build()
        viewComponent.inject(this)
    }

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return [ApplicationComponent]
     */
    private val applicationComponent: ApplicationComponent
        get() = (activity!!.application as FlowApp).getApplicationComponent()

    override fun onResume() {
        super.onResume()
        refresh()
        LocalBroadcastManager.getInstance(activity!!).registerReceiver(dataSyncReceiver,
            IntentFilter(ConstantUtil.ACTION_DATA_SYNC))
        trackingHelper?.logHistoryTabViewed()
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(activity!!).unregisterReceiver(dataSyncReceiver)
    }

    override fun onDetach() {
        super.onDetach()
        responseListListener = null
        trackingHelper = null
    }

    private fun refresh() {
        LoaderManager.getInstance(this).restartLoader(0, null, this@ResponseListFragment)
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu, view, menuInfo)
        menu.add(0, VIEW_HISTORY, 0, R.string.transmissionhist)

        // Allow deletion only for 'saved' responses
        val info = menuInfo as AdapterContextMenuInfo?
        val itemView = info!!.targetView
        if (!(itemView.getTag(ConstantUtil.READ_ONLY_TAG_KEY) as Boolean)) {
            menu.add(0, DELETE_ONE, 2, R.string.deleteresponse)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterContextMenuInfo

        // This ID is the _id column in the SQLite db
        val surveyInstanceId = mAdapter.getItemId(info.position)
        when (item.itemId) {
            DELETE_ONE -> {
                val itemView = info.targetView
                val surveyId = itemView.getTag(ConstantUtil.SURVEY_ID_TAG_KEY).toString()
                showConfirmationDialog(surveyInstanceId, surveyId)
            }
            VIEW_HISTORY -> viewSurveyInstanceHistory(surveyInstanceId)
        }
        return true
    }

    private fun showConfirmationDialog(surveyInstanceId: Long, surveyId: String) {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(R.string.deleteonewarning)
            .setCancelable(true)
            .setPositiveButton(R.string.okbutton) { _: DialogInterface?, _: Int ->
                deleteSurveyInstance(surveyId, surveyInstanceId)
            }
            .setNegativeButton(R.string.cancelbutton)
            { dialog: DialogInterface, _: Int -> dialog.cancel() }
        builder.show()
    }

    private fun deleteSurveyInstance(surveyId: String?, surveyInstanceId: Long) {
        val db = SurveyDbAdapter(databaseHelper)
        val nameResetNeeded = surveyId != null && (surveyId == mSurveyGroup?.registerSurveyId)
        db.open()
        if (nameResetNeeded) {
            db.clearSurveyedLocaleName(surveyInstanceId)
        }
        db.deleteSurveyInstance(surveyInstanceId.toString())
        db.close()
        if (nameResetNeeded) {
            responseListListener?.onDataPointNameDeleted()
        }
        refresh()
    }

    private fun viewSurveyInstanceHistory(surveyInstanceId: Long) {
        navigator.navigateToTransmissionActivity(activity, surveyInstanceId)
    }

    override fun onListItemClick(list: ListView, view: View, position: Int, id: Long) {
        val formId = view.getTag(ConstantUtil.SURVEY_ID_TAG_KEY).toString()
        val formInstanceId = view.getTag(ConstantUtil.RESPONDENT_ID_TAG_KEY) as Long
        val readOnly = view.getTag(ConstantUtil.READ_ONLY_TAG_KEY) as Boolean
        navigator.navigateToFormActivity(activity,
            recordId,
            formId,
            formInstanceId,
            readOnly,
            mSurveyGroup)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return SurveyInstanceResponseLoader(activity, recordId, databaseHelper)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
        mAdapter.changeCursor(cursor)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        //EMPTY
    }

    /**
     * TODO: make a static inner class to avoid memory leaks
     * BroadcastReceiver to notify of data synchronisation. This should be
     * fired from DataFixWorker.
     */
    private val dataSyncReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.i("Survey Instance status has changed. Refreshing UI...")
            refresh()
        }
    }

    interface ResponseListListener {
        fun onDataPointNameDeleted()
    }

    companion object {
        // Context menu items
        private const val DELETE_ONE = 0
        private const val VIEW_HISTORY = 1

        @JvmStatic
        fun newInstance(): ResponseListFragment {
            return ResponseListFragment()
        }
    }
}
