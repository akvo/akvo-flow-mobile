/*
 * Copyright (C) 2018-2019 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.ui.view.geolocation

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.akvo.flow.R
import org.akvo.flow.util.ConstantUtil

class GeoFieldsResetConfirmDialogFragment : DialogFragment() {

    private var questionId: String? = null
    private var listener: GeoFieldsResetConfirmListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity: Activity? = activity
        listener = if (activity is GeoFieldsResetConfirmListener) {
            activity
        } else {
            throw IllegalArgumentException(
                "Activity must implement GeoFieldsResetConfirmListener"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questionId = arguments!!.getString(ConstantUtil.QUESTION_ID_EXTRA)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity!!)
            .setTitle(R.string.geo_fields_update_title)
            .setCancelable(true)
            .setPositiveButton(R.string.update) { _, _ ->
                if (listener != null) {
                    listener!!.confirmGeoFieldReset(questionId)
                }
            }
            .setNegativeButton(R.string.cancelbutton) { dialog, _ -> dialog.cancel() }.create()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface GeoFieldsResetConfirmListener {
        fun confirmGeoFieldReset(questionId: String?)
    }

    companion object {

        const val GEO_DIALOG_TAG = "geo_dialog"

        @JvmStatic
        fun newInstance(questionId: String?): GeoFieldsResetConfirmDialogFragment {
            val dialogFragment =
                GeoFieldsResetConfirmDialogFragment()
            val args = Bundle()
            args.putString(ConstantUtil.QUESTION_ID_EXTRA, questionId)
            dialogFragment.arguments = args
            return dialogFragment
        }
    }
}