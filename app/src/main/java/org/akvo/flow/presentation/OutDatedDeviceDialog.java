/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;

import org.akvo.flow.R;
import org.akvo.flow.data.preference.Prefs;

/**
 * Will be removed after v2.5.0
 */
public class OutDatedDeviceDialog extends DialogFragment {

    public static final String TAG = "OutDatedDeviceDialog";

    public OutDatedDeviceDialog() {
    }

    public static OutDatedDeviceDialog newInstance() {
        return new OutDatedDeviceDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(
                LayoutInflater.from(getActivity()).inflate(R.layout.dialog_outdated_device, null))
                .setPositiveButton(R.string.okbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dismiss();
                            }
                        })
                .setNegativeButton(R.string.do_not_show_again,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Prefs prefs = new Prefs(getActivity().getApplicationContext());
                                prefs.setBoolean(Prefs.KEY_STOP_SHOWING_DINO, true);
                                dismiss();
                            }
                        });
        return builder.create();
    }
}
