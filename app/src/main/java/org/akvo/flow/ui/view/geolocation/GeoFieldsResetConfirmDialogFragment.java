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

package org.akvo.flow.ui.view.geolocation;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import org.akvo.flow.R;
import org.akvo.flow.util.ConstantUtil;

public class GeoFieldsResetConfirmDialogFragment extends DialogFragment {

    public static final String GEO_DIALOG_TAG = "geo_dialog";

    private String questionId;
    private GeoFieldsResetConfirmListener listener;

    public static GeoFieldsResetConfirmDialogFragment newInstance(String questionId) {
        GeoFieldsResetConfirmDialogFragment dialogFragment = new GeoFieldsResetConfirmDialogFragment();
        Bundle args = new Bundle();
        args.putString(ConstantUtil.QUESTION_ID_EXTRA, questionId);
        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = getActivity();
        if (activity instanceof GeoFieldsResetConfirmListener) {
            listener = (GeoFieldsResetConfirmListener) activity;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implement GeoFieldsResetConfirmListener");
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        questionId = getArguments().getString(ConstantUtil.QUESTION_ID_EXTRA);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity()).setTitle(R.string.geo_fields_update_title)
                .setCancelable(true)
                .setPositiveButton(R.string.update,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (listener != null) {
                                    listener.confirmGeoFieldReset(questionId);
                                }
                            }
                        })
                .setNegativeButton(R.string.cancelbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).create();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface GeoFieldsResetConfirmListener {

        void confirmGeoFieldReset(String questionId);
    }
}
