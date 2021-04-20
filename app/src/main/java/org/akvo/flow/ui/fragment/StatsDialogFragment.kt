/*
 *  Copyright (C) 2014-2017,2019 Stichting Akvo (Akvo Foundation)
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
import android.app.Dialog
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.data.loader.StatsLoader
import org.akvo.flow.data.loader.models.Stats
import org.akvo.flow.injector.component.ApplicationComponent
import org.akvo.flow.injector.component.DaggerViewComponent
import org.akvo.flow.util.ConstantUtil
import timber.log.Timber
import javax.inject.Inject

class StatsDialogFragment : DialogFragment(), LoaderManager.LoaderCallbacks<Stats?> {
    private var mSurveyGroupId: Long = 0

    private lateinit var mTotalView: TextView
    private lateinit var mWeekView: TextView
    private lateinit var mDayView: TextView

    @Inject
    lateinit var databaseHelper: SQLiteOpenHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSurveyGroupId = arguments!!.getLong(ConstantUtil.SURVEY_GROUP_ID_EXTRA)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
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
        LoaderManager.getInstance(this).restartLoader(0, null, this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(activity)
        val v = inflater.inflate(R.layout.stats_fragment, null)
        mTotalView = v.findViewById<View>(R.id.total) as TextView
        mWeekView = v.findViewById<View>(R.id.week) as TextView
        mDayView = v.findViewById<View>(R.id.day) as TextView
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.stats)
        builder.setView(v)
        builder.setPositiveButton(R.string.okbutton) { dialog, _ -> dialog.dismiss() }
        return builder.create()
    }

    // ==================================== //
    // ========= Loader Callbacks ========= //
    // ==================================== //
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Stats?> {
        return StatsLoader(activity, mSurveyGroupId, databaseHelper)
    }

    override fun onLoadFinished(loader: Loader<Stats?>, stats: Stats?) {
        if (stats == null) {
            Timber.w("onFinished() - Loader returned no data")
            return
        }
        mTotalView.text = stats.mTotal.toString()
        mWeekView.text = stats.mThisWeek.toString()
        mDayView.text = stats.mToday.toString()
    }

    override fun onLoaderReset(loader: Loader<Stats?>) {
        // EMPTY
    }

    companion object {
        @JvmStatic
        fun newInstance(surveyGroupId: Long): StatsDialogFragment {
            val f = StatsDialogFragment()
            val args = Bundle()
            args.putLong(ConstantUtil.SURVEY_GROUP_ID_EXTRA, surveyGroupId)
            f.arguments = args
            return f
        }
    }
}
