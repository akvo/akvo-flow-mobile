/*
 *  Copyright (C) 2013-2017,2019-2020 Stichting Akvo (Akvo Foundation)
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

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.ListFragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.data.loader.FormInfoLoader
import org.akvo.flow.data.loader.models.FormInfo
import org.akvo.flow.utils.entity.SurveyGroup
import org.akvo.flow.injector.component.ApplicationComponent
import org.akvo.flow.injector.component.DaggerViewComponent
import org.akvo.flow.ui.model.ViewForm
import org.akvo.flow.ui.model.ViewFormMapper
import org.akvo.flow.util.ConstantUtil
import java.util.ArrayList
import javax.inject.Inject

class FormListFragment : ListFragment(), LoaderManager.LoaderCallbacks<List<FormInfo>>,
    AdapterView.OnItemClickListener {

    private lateinit var mSurveyGroup: SurveyGroup
    private lateinit var mAdapter: SurveyAdapter
    private lateinit var recordId: String

    private var listener: FormListListener? = null
    private val mapper = ViewFormMapper()

    @Inject
    lateinit var databaseHelper: SQLiteOpenHelper

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        listener = try {
            context as FormListListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement FormListListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val intent = activity!!.intent
        mSurveyGroup = intent.getSerializableExtra(ConstantUtil.SURVEY_EXTRA) as SurveyGroup
        recordId = intent.getStringExtra(ConstantUtil.DATA_POINT_ID_EXTRA) as String
        setHasOptionsMenu(true)
        mAdapter = SurveyAdapter(activity)
        listAdapter = mAdapter
        listView.onItemClickListener = this
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

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        val surveyId = mAdapter.getItem(position)!!.id
        listener?.onFormClick(surveyId)
    }

    internal class SurveyAdapter(context: Context?) : ArrayAdapter<ViewForm?>(context!!,
        LAYOUT_RES,
        ArrayList()) {
        private val versionTextSize: Int = context!!.resources
            .getDimensionPixelSize(R.dimen.survey_version_text_size)
        private val titleTextSize: Int = context!!.resources
            .getDimensionPixelSize(R.dimen.survey_title_text_size)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var listItem = convertView
            val formViewHolder: FormViewHolder
            if (listItem == null) {
                listItem = LayoutInflater.from(context).inflate(LAYOUT_RES, null)
                formViewHolder = FormViewHolder(listItem)
                listItem.tag = formViewHolder
            } else {
                formViewHolder = listItem.tag as FormViewHolder
            }
            val viewForm = getItem(position)
            formViewHolder.updateViews(viewForm, versionTextSize, titleTextSize)
            if (viewForm!!.isEnabled) {
                listItem!!.isClickable = false
                listItem.alpha = 1f
            } else {
                listItem!!.isClickable = true
                listItem.alpha = 0.5f
            }
            return listItem
        }

        fun addAll(forms: List<ViewForm?>) {
            for (s in forms) {
                add(s)
            }
        }

        companion object {
            private const val LAYOUT_RES = R.layout.survey_item
        }

    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<FormInfo>> {
        return FormInfoLoader(activity, recordId, mSurveyGroup, databaseHelper)
    }

    override fun onLoadFinished(loader: Loader<List<FormInfo>>, data: List<FormInfo>) {
        mAdapter.clear()
        val forms = mapper.transform(data, mSurveyGroup, getString(R.string.form_deleted))
        mAdapter.addAll(forms)
    }

    override fun onLoaderReset(loader: Loader<List<FormInfo>>) {
        //EMPTY
    }

    interface FormListListener {
        fun onFormClick(formId: String)
    }

    internal class FormViewHolder(private val view: View?) {
        private val formNameView: TextView = view!!.findViewById(R.id.survey_name_tv)
        private val lastSubmissionTitle: TextView = view!!.findViewById(R.id.date_label)
        private val lastSubmissionView: TextView = view!!.findViewById(R.id.date)

        fun updateViews(surveyInfo: ViewForm?, versionTextSize: Int, titleTextSize: Int) {
            val versionSpannable = getSpannableString(versionTextSize, surveyInfo!!.surveyExtraInfo)
            val titleSpannable = getSpannableString(titleTextSize, surveyInfo.surveyName)
            formNameView.text = TextUtils.concat(titleSpannable, versionSpannable)
            view!!.isEnabled = surveyInfo.isEnabled
            formNameView.isEnabled = surveyInfo.isEnabled
            if (surveyInfo.time != null) {
                lastSubmissionView.text = surveyInfo.time
                lastSubmissionTitle.visibility = View.VISIBLE
                lastSubmissionView.visibility = View.VISIBLE
            } else {
                lastSubmissionTitle.visibility = View.GONE
                lastSubmissionView.visibility = View.GONE
            }
        }

        private fun getSpannableString(textSize: Int, string: String): SpannableString {
            val spannable = SpannableString(string)
            spannable.setSpan(AbsoluteSizeSpan(textSize),
                0,
                string.length,
                Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            return spannable
        }

    }

    companion object {
        fun newInstance(): FormListFragment {
            return FormListFragment()
        }
    }
}
