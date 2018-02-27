/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.akvo.flow.R;
import org.akvo.flow.presentation.navigation.ViewUser;
import org.akvo.flow.util.ConstantUtil;

public class EditUserDialog extends DialogFragment {

    public static final String TAG = "EditUserDialog";

    private ViewUser viewUser;
    private EditUserListener listener;
    private EditText userNameEt;

    public EditUserDialog() {
    }

    public static EditUserDialog newInstance(ViewUser viewUser) {
        EditUserDialog fragment = new EditUserDialog();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ConstantUtil.VIEW_USER_EXTRA, viewUser);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewUser = getArguments().getParcelable(ConstantUtil.VIEW_USER_EXTRA);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (EditUserListener) getActivity();
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
        Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View main = LayoutInflater.from(context).inflate(R.layout.user_name_input_dialog, null);
        builder.setTitle(R.string.edit_user);
        userNameEt = (EditText) main.findViewById(R.id.user_name_et);
        userNameEt.setText(viewUser.getName());
        builder.setView(main);
        builder.setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = userNameEt.getText().toString();
                if (name.equals(viewUser.getName())) {
                    return;
                }

                if (listener != null) {
                    listener.editUser(new ViewUser(viewUser.getId(), name));
                }
                dismiss();
            }
        });

        builder.setNegativeButton(R.string.cancelbutton,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                });

        return builder.create();
    }

    public interface EditUserListener {

        void editUser(ViewUser user);
    }
}
