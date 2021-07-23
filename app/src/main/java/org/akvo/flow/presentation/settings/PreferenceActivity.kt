/*
 * Copyright (C) 2017-2021 Stichting Akvo (Akvo Foundation)
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
 *
 */
package org.akvo.flow.presentation.settings

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.appbar.AppBarLayout
import org.akvo.flow.BuildConfig
import org.akvo.flow.R
import org.akvo.flow.app.FlowApp
import org.akvo.flow.injector.component.ApplicationComponent
import org.akvo.flow.injector.component.DaggerViewComponent
import org.akvo.flow.presentation.settings.DeleteAllWarningDialog.DeleteAllListener
import org.akvo.flow.presentation.settings.DeleteResponsesWarningDialog.DeleteResponsesListener
import org.akvo.flow.presentation.settings.DownloadFormDialog.DownloadFormListener
import org.akvo.flow.presentation.settings.ReloadFormsConfirmationDialog.ReloadFormsListener
import org.akvo.flow.service.DataFixWorker
import org.akvo.flow.tracking.TrackingHelper
import org.akvo.flow.ui.Navigator
import org.akvo.flow.uicomponents.BackActivity
import org.akvo.flow.uicomponents.SnackBarManager
import javax.inject.Inject
import kotlin.math.abs

class PreferenceActivity : BackActivity(), PreferenceView, DeleteResponsesListener,
    DeleteAllListener, DownloadFormListener, ReloadFormsListener {

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var presenter: PreferencePresenter

    @Inject
    lateinit var snackBarManager: SnackBarManager

    private lateinit var trackingHelper: TrackingHelper

    private lateinit var appBarLayout: AppBarLayout
    private lateinit var toolbarShadow: View
    private lateinit var deviceIdentifierTv: TextView
    private lateinit var instanceNameTv: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var screenOnSc: SwitchCompat
    private lateinit var enableDataSc: SwitchCompat
    private lateinit var imageSizeSp: Spinner

    private var listenersEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)
        setTitle(R.string.settings)
        initializeInjector()
        setupToolBar()
        initViews()
        setUpToolBarAnimationListener()
        updateProgressDrawable()
        presenter.setView(this)
        trackingHelper = TrackingHelper(this)
        presenter.loadPreferences()
    }

    private fun initViews() {
        appBarLayout = findViewById(R.id.appbar_layout)
        toolbarShadow = findViewById(R.id.toolbar_shadow)
        deviceIdentifierTv = findViewById(R.id.preference_identifier_value)
        instanceNameTv = findViewById(R.id.preference_instance_value)
        progressBar = findViewById(R.id.progress)
        screenOnSc = findViewById(R.id.switch_screen_on)
        enableDataSc = findViewById(R.id.switch_enable_data)
        imageSizeSp = findViewById(R.id.preference_image_size)
        findViewById<View>(R.id.send_data_points).setOnClickListener { onDataPointSendTap() }
        findViewById<View>(R.id.preference_delete_collected_data_title).setOnClickListener { onDeleteCollectedDataTap() }
        findViewById<View>(R.id.preference_delete_collected_data_subtitle).setOnClickListener { onDeleteCollectedDataTap() }
        findViewById<View>(R.id.preference_delete_everything_title,).setOnClickListener { onDeleteAllTap() }
        findViewById<View>(R.id.preference_delete_everything_subtitle).setOnClickListener { onDeleteAllTap() }
        findViewById<View>(R.id.preference_gps_fixes).setOnClickListener { onGpsFixesTap() }
        findViewById<View>(R.id.preference_storage).setOnClickListener { onCheckSdCardStateOptionTap() }
        findViewById<View>(R.id.preference_download_form_title).setOnClickListener { onDownloadFormOptionTap() }
        findViewById<View>(R.id.preference_download_form_subtitle).setOnClickListener { onDownloadFormOptionTap() }
        findViewById<View>(R.id.preference_reload_forms_title).setOnClickListener { onReloadAllFormsOptionTap() }
        findViewById<View>(R.id.preference_reload_forms_subtitle).setOnClickListener { onReloadAllFormsOptionTap() }
        enableDataSc.setOnCheckedChangeListener { _, isChecked -> onDataCheckChanged(isChecked) }
        screenOnSc.setOnCheckedChangeListener { _, isChecked -> onScreenOnCheckChanged(isChecked) }
        imageSizeSp.onItemSelectedListener = object: OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                onImageSizeSelected(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //EMPTY
            }
        }
    }

    private fun setUpToolBarAnimationListener() {
        appBarLayout.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                when {
                    abs(verticalOffset) == appBarLayout.totalScrollRange -> {
                        onToolBarCollapsed()
                    }
                    verticalOffset == 0 -> {
                        onToolbarExpanded()
                    }
                    else -> {
                        onToolbarMove()
                    }
                }
            }

            private fun onToolbarMove() {
                toolbarShadow.visibility = View.GONE
            }

            private fun onToolbarExpanded() {
                toolbarShadow.visibility = View.VISIBLE
            }

            private fun onToolBarCollapsed() {
                toolbarShadow.visibility = View.GONE
            }
        })
    }

    private fun updateProgressDrawable() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val progressDrawable = progressBar.indeterminateDrawable
            progressDrawable?.colorFilter =
                PorterDuffColorFilter(ContextCompat.getColor(this, R.color.colorAccent),
                    PorterDuff.Mode.MULTIPLY)
        }
    }

    private fun initializeInjector() {
        val viewComponent = DaggerViewComponent.builder()
            .applicationComponent(applicationComponent).build()
        viewComponent.inject(this)
    }

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return [ApplicationComponent]
     */
    private val applicationComponent: ApplicationComponent
        get() = (application as FlowApp).getApplicationComponent()

    override fun onDestroy() {
        super.onDestroy()
        presenter.destroy()
    }

    fun onDataPointSendTap() {
        trackingHelper.logUploadDataEvent()
        Toast.makeText(applicationContext, R.string.data_upload_will_start_message,
            Toast.LENGTH_LONG).show()
        DataFixWorker.scheduleWork(applicationContext, enableDataSc.isChecked)
        finish()
    }

    fun onDeleteCollectedDataTap() {
        trackingHelper.logDeleteDataPressed()
        presenter.deleteCollectedData()
    }

    private fun onDeleteAllTap() {
        trackingHelper.logDeleteAllPressed()
        presenter.deleteAllData()
    }

    private fun onDownloadFormOptionTap() {
        trackingHelper.logDownloadFormPressed()
        val newFragment: DialogFragment = DownloadFormDialog.newInstance()
        newFragment.show(supportFragmentManager, DownloadFormDialog.TAG)
    }

    private fun onReloadAllFormsOptionTap() {
        trackingHelper.logDownloadFormsPressed()
        val newFragment: DialogFragment = ReloadFormsConfirmationDialog.newInstance()
        newFragment.show(supportFragmentManager, ReloadFormsConfirmationDialog.TAG)
    }

    private fun onGpsFixesTap() {
        trackingHelper.logGpsFixesEvent()
        navigator.navigateToGpsFixes(this)
    }

    private fun onCheckSdCardStateOptionTap() {
        trackingHelper.logStorageEvent()
        navigator.navigateToStorageSettings(this)
    }

    fun onDataCheckChanged(checked: Boolean) {
        if (listenersEnabled) {
            trackingHelper.logMobileDataChanged(checked)
            presenter.saveEnableMobileData(checked)
        }
    }

    fun onScreenOnCheckChanged(checked: Boolean) {
        if (listenersEnabled) {
            trackingHelper.logScreenOnChanged(checked)
            presenter.saveKeepScreenOn(checked)
        }
    }

    fun onImageSizeSelected(position: Int) {
        if (listenersEnabled) {
            trackingHelper.logImageSizeChanged(position)
            presenter.saveImageSize(position)
        }
    }

    override fun showLoading() {
        progressBar.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    override fun displaySettings(viewUserSettings: ViewUserSettings) {
        instanceNameTv.text = BuildConfig.INSTANCE_URL
        deviceIdentifierTv.text = viewUserSettings.identifier
        screenOnSc.isChecked = viewUserSettings.isScreenOn
        enableDataSc.isChecked = viewUserSettings.isDataEnabled
        imageSizeSp.setSelection(viewUserSettings.imageSize)
        delayListeners()
    }

    /**
     * Delay enabling listeners in order to give ui time to draw spinners
     */
    private fun delayListeners() {
        imageSizeSp.postDelayed({ listenersEnabled = true }, 500)
    }

    override fun showDeleteCollectedData() {
        val newFragment: DialogFragment = DeleteResponsesWarningDialog.newInstance(false)
        newFragment.show(supportFragmentManager, DeleteResponsesWarningDialog.TAG)
    }

    override fun showDeleteCollectedDataWithPending() {
        val newFragment: DialogFragment = DeleteResponsesWarningDialog.newInstance(true)
        newFragment.show(supportFragmentManager, DeleteResponsesWarningDialog.TAG)
    }

    override fun showDeleteAllData() {
        val newFragment: DialogFragment = DeleteAllWarningDialog.newInstance(false)
        newFragment.show(supportFragmentManager, DeleteAllWarningDialog.TAG)
    }

    override fun showDeleteAllDataWithPending() {
        val newFragment: DialogFragment = DeleteAllWarningDialog.newInstance(true)
        newFragment.show(supportFragmentManager, DeleteAllWarningDialog.TAG)
    }

    override fun showClearDataError() {
        showMessage(R.string.clear_data_error)
    }

    override fun showClearDataSuccess() {
        showMessage(R.string.clear_data_success)
    }

    override fun deleteResponsesConfirmed() {
        trackingHelper.logDeleteDataConfirmed()
        presenter.deleteResponsesConfirmed()
    }

    override fun deleteAllConfirmed() {
        trackingHelper.logDeleteAllConfirmed()
        presenter.deleteAllConfirmed()
    }

    override fun dismiss() {
        finish()
    }

    override fun downloadForm(formId: String) {
        trackingHelper.logDownloadFormConfirmed(formId)
        presenter.downloadForm(formId)
    }

    override fun reloadFormsConfirmed() {
        trackingHelper.logDownloadFormsConfirmed()
        presenter.reloadForms()
    }

    override fun showDownloadFormsError(numberOfForms: Int) {
        showQuantityMessage(R.plurals.download_forms_error, numberOfForms)
    }

    override fun showDownloadFormsSuccess(numberOfForms: Int) {
        showQuantityMessage(R.plurals.download_forms_success, numberOfForms)
    }

    private fun showMessage(@StringRes resId: Int) {
        snackBarManager.displaySnackBar(instanceNameTv, resId, this)
    }

    private fun showQuantityMessage(resId: Int, quantity: Int) {
        val message = resources.getQuantityString(resId, quantity)
        snackBarManager.displaySnackBar(instanceNameTv, message, this)
    }
}
