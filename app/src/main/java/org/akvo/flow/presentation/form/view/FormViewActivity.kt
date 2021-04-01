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
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.injector.component.DaggerViewComponent
import org.akvo.flow.presentation.form.languages.Language
import org.akvo.flow.presentation.form.languages.LanguagesDialogFragment
import org.akvo.flow.presentation.form.view.entity.ViewForm
import org.akvo.flow.presentation.form.view.groups.GeoShapeListener
import org.akvo.flow.presentation.form.view.groups.MediaQuestionListener
import org.akvo.flow.presentation.form.view.groups.QuestionGroupFragment
import org.akvo.flow.presentation.form.view.groups.QuestionGroupsPagerAdapter
import org.akvo.flow.ui.Navigator
import org.akvo.flow.uicomponents.BackActivity
import org.akvo.flow.uicomponents.SnackBarManager
import org.akvo.flow.util.ConstantUtil
import java.io.File
import java.util.ArrayList
import javax.inject.Inject

class FormViewActivity : BackActivity(), IFormView,
    LanguagesDialogFragment.LanguagesSelectionListener, MediaQuestionListener, GeoShapeListener {

    private lateinit var sectionsPagerAdapter: QuestionGroupsPagerAdapter
    private lateinit var surveyGroup: SurveyGroup
    private lateinit var datapointId: String
    private lateinit var formId: String
    private lateinit var viewPager: ViewPager
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
        sectionsPagerAdapter =
            QuestionGroupsPagerAdapter(
                supportFragmentManager
            )
        viewPager = findViewById(R.id.pager)
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
        formId = intent.getStringExtra(ConstantUtil.FORM_ID_EXTRA)!!
        datapointId = intent.getStringExtra(ConstantUtil.DATA_POINT_ID_EXTRA)!!
        formInstanceId = intent.getLongExtra(ConstantUtil.RESPONDENT_ID_EXTRA, 0)
        presenter.loadForm(formId, formInstanceId, surveyGroup, datapointId)
        sectionsPagerAdapter.groups = mutableListOf()
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
        showError(R.string.error_saving_languages)
    }

    override fun displayLanguages(languages: List<Language>) {
        LanguagesDialogFragment.newInstance(languages as ArrayList<Language>)
            .show(supportFragmentManager, LanguagesDialogFragment.TAG)
    }

    override fun showLanguagesError() {
        showError(R.string.languages_load_error)
    }

    override fun displayForm(viewForm: ViewForm) {
        sectionsPagerAdapter.groups = viewForm.groups
        sectionsPagerAdapter.notifyDataSetChanged()

        supportActionBar?.let { supportActionBar ->
            supportActionBar.title = viewForm.title
            supportActionBar.subtitle = "v ${viewForm.version}"
        }
    }

    override fun showErrorLoadingForm() {
        Toast.makeText(applicationContext, R.string.error_loading_form, Toast.LENGTH_SHORT).show()
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

    override fun downloadMedia(filename: String, index: Int) {
        val groupFragment =
            supportFragmentManager.fragments[viewPager.currentItem] as QuestionGroupFragment
        groupFragment.downloadMedia(filename, index)
    }

    override fun viewVideo(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            val fileUri = FileProvider
                .getUriForFile(this, ConstantUtil.FILE_PROVIDER_AUTHORITY, file)
            navigator.navigateToVideoView(this, fileUri)
        } else {
            showError(R.string.error_video_preview)
        }
    }

    private fun showError(@StringRes errorString: Int) {
        snackBarManager.displaySnackBar(findViewById(R.id.form_view_root), errorString)
    }

    override fun viewImage(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            navigator.navigateToLargeImage(this, filePath)
        } else {
            showError(R.string.error_img_preview)
        }
    }

    override fun viewShape(geoJson: String) {
        val data = Bundle()
        data.putString(ConstantUtil.GEOSHAPE_RESULT, geoJson)
        navigator.navigateToViewGeoShapeActivity(this, data)
    }
}
