/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import org.akvo.flow.R;
import org.akvo.flow.service.SurveyDownloadService;

public class ReloadFormsConfirmationDialog extends DialogFragment {

    public static final String TAG = "ReloadFormsConfirmationDialog";

    public ReloadFormsConfirmationDialog() {
    }

    public static ReloadFormsConfirmationDialog newInstance() {
        return new ReloadFormsConfirmationDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.reload_forms_title);
        builder.setPositiveButton(R.string.reload, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                FragmentActivity activity = getActivity();
                if (activity != null) {
                    Intent i = new Intent(activity, SurveyDownloadService.class);
                    activity.startService(i);
                }
            }
        });
        builder.setNegativeButton(R.string.cancelbutton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        return builder.create();
    }
}
