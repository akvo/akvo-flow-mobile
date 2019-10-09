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

package org.akvo.flow.presentation.geoshape;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import org.akvo.flow.R;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class DeletePointDialog extends DialogFragment {

    public static final String TAG = "DeletePointDialog";

    private PointDeleteListener listener;

    public DeletePointDialog() {
    }

    public static DeletePointDialog newInstance() {
        return new DeletePointDialog();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = getActivity();
        if (activity instanceof PointDeleteListener) {
            listener = (PointDeleteListener) activity;
        } else {
            throw new IllegalArgumentException("Activity must implement PointDeleteListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity()).setTitle(R.string.delete_point_title)
                .setCancelable(true)
                .setPositiveButton(R.string.delete,
                        (dialog, id) -> {
                            if (listener != null) {
                                listener.deletePoint();
                            }
                        })
                .setNegativeButton(R.string.cancelbutton,
                        (dialog, id) -> dialog.cancel()).create();
    }

    public interface PointDeleteListener {

        void deletePoint();
    }
}
