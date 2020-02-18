/*
 * Copyright (C) 2020 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.presentation.form.languages

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import androidx.fragment.app.DialogFragment
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.injector.component.ApplicationComponent
import org.akvo.flow.injector.component.DaggerViewComponent
import org.akvo.flow.ui.adapter.LanguageAdapter
import org.akvo.flow.uicomponents.SnackBarManager
import org.akvo.flow.util.ConstantUtil
import javax.inject.Inject

class LanguagesDialogFragment : DialogFragment(), LanguagesView {

    @Inject
    lateinit var presenter: LanguagesPresenter

    @Inject
    lateinit var snackBarManager: SnackBarManager

    private var listener: LanguagesSelectionListener? = null

    //TODO: should probably be formId
    private var surveyId = -1L

    companion object {

        const val TAG = "LanguagesDialogFragment"

        @JvmStatic
        fun newInstance(surveyId: Long): LanguagesDialogFragment {
            return LanguagesDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ConstantUtil.SURVEY_ID_EXTRA, surveyId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeInjector()
        surveyId = arguments!!.getLong(ConstantUtil.SURVEY_ID_EXTRA, -1L)
        presenter.setView(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = activity as LanguagesSelectionListener
        } catch (e: ClassCastException) {
            throw ClassCastException("${activity.toString()} must implement LanguagesSelectionListener")
        }
    }

    private fun initializeInjector() {
        val viewComponent = DaggerViewComponent.builder()
            .applicationComponent(getApplicationComponent())
            .build()
        viewComponent.inject(this)
    }

    private fun getApplicationComponent(): ApplicationComponent? {
        return (activity!!.application as FlowApp).getApplicationComponent()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val languageAdapter = LanguageAdapter(activity, emptyList())
        val listView = LayoutInflater.from(activity)
            .inflate(R.layout.languages_list, null) as ListView
        listView.adapter = languageAdapter
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                languageAdapter.updateSelected(position)
            }
        return AlertDialog.Builder(activity)
            .setTitle(R.string.surveylanglabel)
            .setView(listView)
            .setPositiveButton(R.string.okbutton) { _, _ ->
                val selectedLanguages = (listView.adapter as LanguageAdapter).selectedLanguages
                listener?.useSelectedLanguages(selectedLanguages)
            }.create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.loadLanguages(surveyId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.destroy()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface LanguagesSelectionListener {
        fun useSelectedLanguages(selectedLanguages: MutableSet<String>)
    }
}