/*
 * Copyright (C) 2021 Stichting Akvo (Akvo Foundation)
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

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.widget.EditText
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.DialogFragment
import org.akvo.flow.BuildConfig
import org.akvo.flow.R

class SendDeviceInfoDialog : DialogFragment() {

    private var listener: SendDeviceInfoListener? = null

    private lateinit var userName: String
    private lateinit var deviceId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userName = arguments?.getString(PARAM_USER_NAME, "") as String
        deviceId = arguments?.getString(PARAM_DEVICE_ID, "") as String

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        listener = if (activity is SendDeviceInfoListener) {
            activity
        } else {
            throw IllegalArgumentException("Activity must implement DownloadFormListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = activity
        val inputDialog = AlertDialog.Builder(activity)
        inputDialog.setTitle(R.string.preference_send_info)
        var instance = BuildConfig.INSTANCE_URL
        instance = instance.removePrefix("https://")
        instance = instance.removeSuffix(".akvoflow.org")
        instance = instance.removeSuffix(".appspot.com")

        val version = Build.VERSION.RELEASE
        val model = Build.MANUFACTURER + Build.MODEL

        val body =
            "Android Device Info:" + "\r\n" +
                    "Android Version: $version" + "\r\n" +
                    "Device model: $model" + "\r\n" +
                    "App version: ${BuildConfig.VERSION_NAME}" + "\r\n" +
                    "User name: $userName" + "\r\n" +
                    "Device Identifier: $deviceId" + "\r\n" +
                    "Instance: $instance"
        val message = getString(R.string.send_support_detail, body)
        inputDialog.setMessage(message)
        val main = LayoutInflater.from(activity).inflate(R.layout.send_device_info_dialog, null)
        val input = main.findViewById<EditText>(R.id.reference_id_et)
        input.keyListener = DigitsKeyListener(false, false)
        inputDialog.setView(main)
        inputDialog.setPositiveButton(R.string.okbutton) { _, _ ->
            val referenceText = input.text.toString()
            listener?.let { listener ->
                if (referenceText.isNotEmpty() && referenceText.isDigitsOnly()) {
                    listener.sendDeviceInfo(referenceText.toInt(), body)
                }
            }
        }
        inputDialog.setNegativeButton(R.string.cancelbutton) { dialog, _ -> dialog.dismiss() }
        return inputDialog.create()
    }

    interface SendDeviceInfoListener {
        fun sendDeviceInfo(toInt: Int, body: String)
    }

    companion object {
        const val TAG = "ReloadFormsConfirmationDialog"
        const val PARAM_USER_NAME = "user_name"
        const val PARAM_DEVICE_ID = "device_id"

        fun newInstance(userName: String, deviceId: String): SendDeviceInfoDialog {
            val sendDeviceInfoDialog = SendDeviceInfoDialog()
            val args = Bundle(2)
            args.putString(PARAM_USER_NAME, userName)
            args.putString(PARAM_DEVICE_ID, deviceId)
            sendDeviceInfoDialog.arguments = args
            return sendDeviceInfoDialog
        }
    }
}
