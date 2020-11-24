/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.tracking

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import org.akvo.flow.domain.util.ImageSize

class TrackingHelper(context: Context) {

    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    fun logStatsEvent(tabName: String?) {
        val params = Bundle()
        params.putString("from_tab", tabName)
        firebaseAnalytics.logEvent("menu_stats_pressed", params)
    }

    fun logSortEvent() {
        firebaseAnalytics.logEvent("list_menu_sort_pressed", null)
    }

    fun logDownloadEvent(tabName: String?) {
        val params = Bundle()
        params.putString("from_tab", tabName)
        firebaseAnalytics.logEvent("menu_download_pressed", params)
    }

    fun logUploadEvent(tabName: String?) {
        val params = Bundle()
        params.putString("from_tab", tabName)
        firebaseAnalytics.logEvent("menu_upload_pressed", params)
    }

    fun logSortEventChosen(orderSuffix: String) {
        firebaseAnalytics.logEvent("sort_by_$orderSuffix", null)
    }

    fun logSearchEvent() {
        firebaseAnalytics.logEvent("list_search_pressed", null)
    }

    fun logUploadDataEvent() {
        firebaseAnalytics.logEvent("setting_send_datapoints_pressed", null)
    }

    fun logGpsFixesEvent() {
        firebaseAnalytics.logEvent("setting_gps_fix_pressed", null)
    }

    fun logStorageEvent() {
        firebaseAnalytics.logEvent("setting_storage_pressed", null)
    }

    fun logMobileDataChanged(checked: Boolean) {
        val params = Bundle()
        params.putBoolean("status", checked)
        firebaseAnalytics.logEvent("setting_mobile_data_changed", params)
    }

    fun logScreenOnChanged(checked: Boolean) {
        val params = Bundle()
        params.putBoolean("status", checked)
        firebaseAnalytics.logEvent("setting_screen_on_changed", params)
    }

    fun logImageSizeChanged(imageSize: Int) {
        val size = when (imageSize) {
            ImageSize.IMAGE_SIZE_320_240 -> "small"
            ImageSize.IMAGE_SIZE_640_480 -> "medium"
            ImageSize.IMAGE_SIZE_1280_960 -> "large"
            else -> ""
        }
        val params = Bundle()
        params.putString("image_size", size)
        firebaseAnalytics.logEvent("setting_image_size_changed", params)
    }

    fun logPublishPressed() {
        firebaseAnalytics.logEvent("setting_publish_data_pressed", null)
    }

    fun logDeleteDataPressed() {
        firebaseAnalytics.logEvent("setting_delete_data_pressed", null)
    }

    fun logDeleteAllPressed() {
        firebaseAnalytics.logEvent("setting_delete_all_pressed", null)
    }

    fun logDownloadFormPressed() {
        firebaseAnalytics.logEvent("setting_download_form_pressed", null)
    }

    fun logDownloadFormsPressed() {
        firebaseAnalytics.logEvent("setting_download_forms_pressed", null)
    }

    fun logDeleteDataConfirmed() {
        firebaseAnalytics.logEvent("setting_delete_data_confirmed", null)
    }

    fun logDeleteAllConfirmed() {
        firebaseAnalytics.logEvent("setting_delete_all_confirmed", null)
    }

    fun logDownloadFormConfirmed(formId: String?) {
        val params = Bundle()
        params.putString("form_id", formId)
        firebaseAnalytics.logEvent("setting_download_form_confirmed", params)
    }

    fun logDownloadFormsConfirmed() {
        firebaseAnalytics.logEvent("setting_download_forms_confirmed", null)
    }

    fun logCheckUpdatePressed() {
        firebaseAnalytics.logEvent("about_check_update_pressed", null)
    }

    fun logViewNotesPressed() {
        firebaseAnalytics.logEvent("about_view_notes_pressed", null)
    }

    fun logViewLegalPressed() {
        firebaseAnalytics.logEvent("about_view_legal_pressed", null)
    }

    fun logViewTermsPressed() {
        firebaseAnalytics.logEvent("about_view_terms_pressed", null)
    }

    fun logFormSubmissionRepeatConfirmationDialogEvent() {
        firebaseAnalytics.logEvent("monitoring_form_repeat_dialog_shown", null)
    }

    fun logHistoryTabViewed() {
        firebaseAnalytics.logEvent("history_tab_viewed", null)
    }
}
