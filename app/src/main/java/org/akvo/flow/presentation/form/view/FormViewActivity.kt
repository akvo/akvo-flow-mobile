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

package org.akvo.flow.presentation.form.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.injector.component.DaggerViewComponent
import org.akvo.flow.presentation.form.languages.Language
import org.akvo.flow.presentation.form.languages.LanguagesDialogFragment
import org.akvo.flow.presentation.form.view.ui.main.QuestionGroupsPagerAdapter
import org.akvo.flow.ui.Navigator
import org.akvo.flow.uicomponents.BackActivity
import org.akvo.flow.uicomponents.SnackBarManager
import org.akvo.flow.util.ConstantUtil
import java.util.ArrayList
import javax.inject.Inject

class FormViewActivity : BackActivity(), IFormView,
    LanguagesDialogFragment.LanguagesSelectionListener {

    private lateinit var sectionsPagerAdapter: QuestionGroupsPagerAdapter
    private lateinit var surveyGroup: SurveyGroup
    private lateinit var datapointId: String
    private lateinit var formId: String
    private var formInstanceId: Long = 0L

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var snackBarManager: SnackBarManager

    @Inject
    lateinit var presenter: FormViewPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_view)
        initializeInjector()
        setupToolBar()
        presenter.view = this
        sectionsPagerAdapter = QuestionGroupsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
    }

    private fun initializeInjector() {
        val viewComponent =
            DaggerViewComponent.builder()
                .applicationComponent((application as FlowApp).applicationComponent)
                .build()
        viewComponent.inject(this)
    }

    override fun onResume() {
        super.onResume()
        surveyGroup = intent.getSerializableExtra(ConstantUtil.SURVEY_EXTRA) as SurveyGroup
        formId = intent.getStringExtra(ConstantUtil.FORM_ID_EXTRA)
        datapointId = intent.getStringExtra(ConstantUtil.DATA_POINT_ID_EXTRA)
        formInstanceId = intent.getLongExtra(ConstantUtil.RESPONDENT_ID_EXTRA, 0)
        presenter.loadForm(formId, formInstanceId, surveyGroup, datapointId)
        sectionsPagerAdapter.groupTitles = mutableListOf()
        sectionsPagerAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.destroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.view_form_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit_lang -> {
                presenter.loadLanguages(surveyGroup.id, formId)
                return true
            }
            R.id.view_map -> {
                navigator.navigateToMapActivity(this, datapointId)
                return true
            }
            R.id.transmission -> {
                navigator.navigateToTransmissionActivity(this, formInstanceId)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun displayError(languages: List<Language>) {
        snackBarManager.displaySnackBarWithAction(
            findViewById(R.id.form_view_root),
            R.string.langmandatorytext,
            R.string.okbutton,
            { displayLanguages(languages) },
            this
        )
    }

    override fun onLanguagesSaved() {
        //TODO("not implemented")
        //notify to reload formUI?
        //add listener
    }

    override fun onLanguagesSavedError() {
        snackBarManager.displaySnackBar(
            findViewById(R.id.form_view_root),
            R.string.error_saving_languages
        )
    }

    override fun displayLanguages(languages: List<Language>) {
        LanguagesDialogFragment.newInstance(languages as ArrayList<Language>)
            .show(supportFragmentManager, LanguagesDialogFragment.TAG)
    }

    override fun showLanguagesError() {
        snackBarManager.displaySnackBar(
            findViewById(R.id.form_view_root),
            R.string.languages_load_error
        )
    }

    override fun useSelectedLanguages(
        selectedLanguages: MutableSet<String>,
        availableLanguages: List<Language>
    ) {
        if (selectedLanguages.isNotEmpty()) {
            presenter.saveLanguages(selectedLanguages, surveyGroup.id)
        } else {
            displayError(availableLanguages)
        }
    }
}
