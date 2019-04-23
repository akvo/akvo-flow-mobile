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

package org.akvo.flow.presentation.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.akvo.flow.R;

public class DownloadFormDialog extends DialogFragment {

    public static final String TAG = "ReloadFormsConfirmationDialog";

    private DownloadFormListener listener;

    public DownloadFormDialog() {
    }

    public static DownloadFormDialog newInstance() {
        return new DownloadFormDialog();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if (activity instanceof DownloadFormListener) {
            listener = (DownloadFormListener)activity;
        } else {
            throw new IllegalArgumentException("Activity must implement DownloadFormListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(activity);
        inputDialog.setTitle(R.string.downloadsurveylabel);
        inputDialog.setMessage(R.string.downloadsurveyinstr);
        View main = LayoutInflater.from(activity).inflate(R.layout.download_form_dialog, null);
        final EditText input = main.findViewById(R.id.form_id_et);
        input.setKeyListener(new DigitsKeyListener(false, false));
        inputDialog.setView(main);
        inputDialog.setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String surveyId = input.getText().toString().trim();
                if (!TextUtils.isEmpty(surveyId) && listener != null) {
                   listener.downloadForm(surveyId);
                }
            }
        });
        inputDialog.setNegativeButton(R.string.cancelbutton, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        return inputDialog.create();
    }

    public interface DownloadFormListener {

        void downloadForm(String formId);
    }
}
