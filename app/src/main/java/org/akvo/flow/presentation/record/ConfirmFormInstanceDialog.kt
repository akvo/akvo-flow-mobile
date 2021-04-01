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
import org.akvo.flow.domain.entity.User

class ConfirmFormInstanceDialog : DialogFragment() {

    private var listener: ConfirmFormInstanceDialogListener? = null
    private lateinit var formName: String
    private lateinit var formId: String
    private lateinit var user: User

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
        formId = arguments!!.getString(FORM_ID)!!
        formName = arguments!!.getString(FORM_NAME)!!
        user = arguments!!.getParcelable(USER)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity!!)
            .setTitle(R.string.confirm_new_submission_title)
            .setMessage(getString(R.string.confirm_new_submission_message, formName))
            .setCancelable(true)
            .setPositiveButton(R.string.confirm_new_submission) { _, _ ->
                listener?.onUserConfirmed(
                    formId,
                    user
                )
            }
            .setNegativeButton(R.string.cancelbutton) { _, _ -> dismiss() }
            .create()
    }

    companion object {

        private const val FORM_ID = "form_id"
        private const val FORM_NAME = "form_name"
        private const val USER = "user"

        const val TAG = "ConfirmFormInstanceDialog"

        @JvmStatic
        fun newInstance(
            formName: String,
            formId: String,
            user: User
        ): ConfirmFormInstanceDialog {
            return ConfirmFormInstanceDialog().apply {
                arguments = Bundle().apply {
                    putString(FORM_ID, formId)
                    putString(FORM_NAME, formName)
                    putParcelable(USER, user)
                }
            }
        }
    }
}
