/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.offlinemaps.presentation.list.delete;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import org.akvo.flow.offlinemaps.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class DeleteAreaDialog extends DialogFragment {

    public static final String TAG = "DeleteAreaDialog";

    private static final String PARAM_AREA_NAME = "areaName";
    private static final String PARAM_AREA_ID = "areaId";

    private long areaId;
    private String areaName;

    private DeleteAreaListener listener;

    public DeleteAreaDialog() {
    }

    public static DeleteAreaDialog newInstance(long areaId, String name) {
        DeleteAreaDialog renameAreaDialog = new DeleteAreaDialog();
        Bundle args = new Bundle(2);
        args.putString(PARAM_AREA_NAME, name);
        args.putLong(PARAM_AREA_ID, areaId);
        renameAreaDialog.setArguments(args);
        return renameAreaDialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof DeleteAreaListener) {
            listener = (DeleteAreaListener) getActivity();
        } else {
            throw new IllegalArgumentException("Activity must implement RenameAreaListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        areaId = getArguments().getLong(PARAM_AREA_ID);
        areaName = getArguments().getString(PARAM_AREA_NAME);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.offline_item_delete_dialog_title);
        builder.setMessage(getString(R.string.offline_item_delete_dialog_message, areaName));
        builder.setPositiveButton(R.string.delete, (dialog, which) -> {
            if (listener != null) {
                listener.deleteAreaConfirmed(areaId);
            }
            dismiss();
        });

        builder.setNegativeButton(android.R.string.cancel,
                (dialog, which) -> {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }

    public interface DeleteAreaListener {

        void deleteAreaConfirmed(long areaId);
    }
}
