/*
 * Copyright (C) 2013-2020 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.presentation.record

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.viewpager.widget.ViewPager
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.domain.SurveyGroup
import org.akvo.flow.injector.component.DaggerViewComponent
import org.akvo.flow.ui.Navigator
import org.akvo.flow.ui.adapter.RecordTabsAdapter
import org.akvo.flow.ui.fragment.FormListFragment.FormListListener
import org.akvo.flow.ui.fragment.ResponseListFragment.ResponseListListener
import org.akvo.flow.uicomponents.BackActivity
import org.akvo.flow.uicomponents.SnackBarManager
import org.akvo.flow.util.ConstantUtil
import javax.inject.Inject

class RecordActivity : BackActivity(), FormListListener, ResponseListListener, RecordView {

    private var surveyGroup: SurveyGroup? = null
    private var dataPointId: String? = null

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var snackBarManager: SnackBarManager

    @Inject
    lateinit var presenter: RecordPresenter

    lateinit var rootLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.record_activity)
        initializeInjector()
        presenter.view = this
        setupToolBar()
        val viewPager = findViewById<ViewPager>(R.id.pager)
        val recordTabsAdapter = RecordTabsAdapter(
            supportFragmentManager,
            resources.getStringArray(R.array.record_tabs)
        )
        viewPager.adapter = recordTabsAdapter
        surveyGroup = intent.getSerializableExtra(ConstantUtil.SURVEY_GROUP_EXTRA) as SurveyGroup
        rootLayout = findViewById(R.id.record_root_layout)
    }

    private fun initializeInjector() {
        val viewComponent =
            DaggerViewComponent.builder()
                .applicationComponent((application as FlowApp).applicationComponent)
                .build()
        viewComponent.inject(this)
    }

    public override fun onResume() {
        super.onResume()
        dataPointId = intent.getStringExtra(ConstantUtil.DATA_POINT_ID_EXTRA)
        dataPointId?.let { datapointId ->
            presenter.loadDataPoint(datapointId)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ConstantUtil.FORM_FILLING_REQUEST && resultCode == Activity.RESULT_OK) {
            snackBarManager.displaySnackBar(rootLayout, R.string.snackbar_submitted, this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.destroy()
    }

    override fun onFormClick(formId: String) {
        dataPointId?.let { datapointId ->
            presenter.onFormClick(formId, datapointId)
        }
    }

    private fun showErrorMessage(@StringRes stringResId: Int) {
        snackBarManager.displaySnackBar(rootLayout,  stringResId, this)
    }

    override fun onDataPointNameDeleted() {
        setTitle(R.string.unknown)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.record_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.view_map) {
            navigator.navigateToMapActivity(this, dataPointId)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun showBootStrapPendingError() {
        showErrorMessage(R.string.pleasewaitforbootstrap)
    }

    override fun showMissingCascadeError() {
        showErrorMessage(R.string.error_missing_cascade)
    }

    override fun navigateToForm(formId: String, formInstanceId: Long) {
        navigator.navigateToFormActivity(
            this,
            dataPointId,
            formId,
            formInstanceId,
            false,
            surveyGroup
        )
    }

    override fun showDataPointTitle(displayName: String) {
        title = displayName
    }

    override fun onDataPointError() {
        showErrorMessage(R.string.error_data_point_not_found)
    }
}
