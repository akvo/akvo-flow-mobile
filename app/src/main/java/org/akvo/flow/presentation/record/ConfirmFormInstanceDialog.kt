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

package org.akvo.flow.presentation.record

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.akvo.flow.R
import org.akvo.flow.domain.entity.DomainFormInstance

class ConfirmFormInstanceDialog : DialogFragment() {

    private var listener: ConfirmFormInstanceDialogListener? = null
    private lateinit var formInstance: DomainFormInstance
    private lateinit var formName: String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity: Activity? = activity
        if (activity is ConfirmFormInstanceDialogListener) {
            listener = activity
        } else {
            throw IllegalArgumentException("Activity must implement InstanceConfirmationDialogListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        formInstance = arguments!!.getParcelable(FORM_INSTANCE)!!
        formName = arguments!!.getString(FORM_NAME)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity!!)
            .setTitle(R.string.confirm_new_submission_title)
            .setMessage(getString(R.string.confirm_new_submission_message, formName))
            .setCancelable(true)
            .setPositiveButton(R.string.confirm_new_submission) { _, _ ->
                listener?.onUserConfirmed(
                    formInstance
                )
            }
            .setNegativeButton(R.string.cancelbutton) { _, _ -> dismiss() }
            .create()
    }

    companion object {

        private const val FORM_INSTANCE = "form_instance"
        private const val FORM_NAME = "form_name"

        const val TAG = "ConfirmFormInstanceDialog"

        @JvmStatic
        fun newInstance(
            formInstance: DomainFormInstance,
            formName: String
        ): ConfirmFormInstanceDialog {
            return ConfirmFormInstanceDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(FORM_INSTANCE, formInstance)
                    putString(FORM_NAME, formName)
                }
            }
        }
    }
}
