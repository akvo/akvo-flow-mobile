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

package org.akvo.flow.presentation.form.view.languages

import android.app.AlertDialog
import android.app.Dialog
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
import org.akvo.flow.util.ConstantUtil
import javax.inject.Inject

class LanguagesDialogFragment : DialogFragment(), LanguagesView {

    @Inject
    lateinit var presenter: LanguagesPresenter

    lateinit var formId: String

    companion object {

        const val TAG = "LanguagesDialogFragment"

        @JvmStatic
        fun newInstance(formId: String): LanguagesDialogFragment {
            return LanguagesDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ConstantUtil.FORM_ID_EXTRA, formId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeInjector()
        formId = arguments!!.getString(ConstantUtil.FORM_ID_EXTRA, "")
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
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        listView.adapter = languageAdapter
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                languageAdapter.updateSelected(position)
            }
        return AlertDialog.Builder(activity)
            .setTitle(R.string.surveylanglabel)
            .setView(listView)
            .setPositiveButton(R.string.okbutton) { dialog, _ ->
                dialog?.dismiss()
                useSelectedLanguages(listView.adapter as LanguageAdapter)
            }.create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.loadLanguages(formId)
    }

    private fun useSelectedLanguages(languageAdapter: LanguageAdapter) {
        val selectedLanguages =
            languageAdapter.selectedLanguages
        if (selectedLanguages != null && selectedLanguages.size > 0) {
             presenter.saveLanguages(selectedLanguages)
        } else {
            displayError()
        }
    }

    private fun displayError() {
        activity?.let {
            LanguagesErrorDialog.newInstance(formId)
                .show(it.supportFragmentManager, LanguagesErrorDialog.TAG)
        }
    }

//    private fun saveLanguages(selectedLanguages: Set<String>) {
//        surveyLanguagesDataSource.saveLanguagePreferences(
//            mSurveyGroup.getId(),
//            selectedLanguages
//        )
//        loadLanguages()
//        mAdapter.notifyOptionsChanged()
//    }
}