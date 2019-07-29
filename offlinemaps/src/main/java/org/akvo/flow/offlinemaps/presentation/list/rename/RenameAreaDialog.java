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

package org.akvo.flow.offlinemaps.presentation.list.rename;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.akvo.flow.offlinemaps.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class RenameAreaDialog extends DialogFragment {

    public static final String TAG = "RenameAreaDialog";

    private static final String PARAM_AREA_NAME = "areaName";
    private static final String PARAM_AREA_ID = "areaId";

    private String areaName;
    private long areaId;

    private EditText nameEt;
    private RenameAreaListener listener;

    public RenameAreaDialog() {
    }

    public static RenameAreaDialog newInstance(String oldAreaName, long areaId) {
        RenameAreaDialog renameAreaDialog = new RenameAreaDialog();
        Bundle args = new Bundle(2);
        args.putString(PARAM_AREA_NAME, oldAreaName);
        args.putLong(PARAM_AREA_ID, areaId);
        renameAreaDialog.setArguments(args);
        return renameAreaDialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof RenameAreaListener) {
            listener = (RenameAreaListener) getActivity();
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
        areaName = getArguments().getString(PARAM_AREA_NAME);
        areaId = getArguments().getLong(PARAM_AREA_ID);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View main = LayoutInflater.from(context).inflate(R.layout.rename_area_dialog, null);
        builder.setTitle(R.string.offline_item_rename_dialog_title);
        nameEt = main.findViewById(R.id.name_et);
        nameEt.setText(areaName);
        nameEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                //EMPTY
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //EMPTY
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateTextChanged();
            }
        });
        builder.setView(main);
        builder.setPositiveButton(R.string.rename_offline_area, (dialog, which) -> {
            String name = nameEt.getText().toString();
            if (name.equals(areaName)) {
                return;
            }

            if (listener != null) {
                listener.renameAreaConfirmed(areaId, name);
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PARAM_AREA_NAME, nameEt.getText().toString());
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            nameEt.setText(savedInstanceState.getString(PARAM_AREA_NAME));
        }
    }

    public void updateTextChanged() {
        String text = nameEt.getText().toString();
        if (TextUtils.isEmpty(text)) {
            disablePositiveButton();
        } else {
            enablePositiveButton();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTextChanged();
    }

    public interface RenameAreaListener {

        void renameAreaConfirmed(long areaId, String name);
    }

    public void disablePositiveButton() {
        Button button = getPositiveButton();
        button.setEnabled(false);
    }

    public void enablePositiveButton() {
        Button button = getPositiveButton();
        button.setEnabled(true);
    }

    private Button getPositiveButton() {
        AlertDialog dialog = (AlertDialog) getDialog();
        return dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    }
}
