/*
 * Copyright (C) 2017,2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.navigation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import org.akvo.flow.R;

import static org.akvo.flow.util.ConstantUtil.SURVEY_GROUP_ID_EXTRA;

public class SurveyDeleteConfirmationDialog extends DialogFragment {

    public static final String TAG = "SurveyDeleteConfirmationDialog";

    private ViewSurvey viewSurvey;
    private SurveyDeleteListener listener;

    public SurveyDeleteConfirmationDialog() {
    }

    public static SurveyDeleteConfirmationDialog newInstance(ViewSurvey viewSurvey) {
        SurveyDeleteConfirmationDialog fragment = new SurveyDeleteConfirmationDialog();
        Bundle args = new Bundle();
        args.putParcelable(SURVEY_GROUP_ID_EXTRA, viewSurvey);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewSurvey = getArguments().getParcelable(SURVEY_GROUP_ID_EXTRA);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (SurveyDeleteListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getTargetFragment().toString()
                    + " must implement SurveyDeleteListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        String name = viewSurvey == null ? getString(R.string.survey) : viewSurvey.getName();
        builder.setMessage(getString(R.string.delete_survey_dialog_message, name))
                .setPositiveButton(R.string.delete_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (listener != null && viewSurvey != null) {
                                    listener.onSurveyDeleteConfirmed(viewSurvey.getId());
                                }

                            }
                        })
                .setNegativeButton(R.string.cancelbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        return builder.create();
    }

    public interface SurveyDeleteListener {

        void onSurveyDeleteConfirmed(long surveyGroupId);
    }
}
