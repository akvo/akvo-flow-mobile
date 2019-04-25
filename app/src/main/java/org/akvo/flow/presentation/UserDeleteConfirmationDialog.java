/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import org.akvo.flow.R;
import org.akvo.flow.presentation.navigation.ViewUser;

import static org.akvo.flow.util.ConstantUtil.VIEW_USER_EXTRA;

public class UserDeleteConfirmationDialog extends DialogFragment {

    public static final String TAG = "UserDeleteConfirmationDialog";

    private ViewUser viewUser;
    private UserDeleteListener listener;

    public UserDeleteConfirmationDialog() {
    }

    public static UserDeleteConfirmationDialog newInstance(ViewUser viewUser) {
        UserDeleteConfirmationDialog fragment = new UserDeleteConfirmationDialog();
        Bundle args = new Bundle();
        args.putParcelable(VIEW_USER_EXTRA, viewUser);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewUser = getArguments().getParcelable(VIEW_USER_EXTRA);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (UserDeleteListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getTargetFragment().toString()
                    + " must implement UserDeleteListener");
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
        builder.setTitle(R.string.delete_user_confirmation)
                .setPositiveButton(R.string.delete_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (listener != null) {
                                    listener.onUserDeleteConfirmed(viewUser);
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

    public interface UserDeleteListener {

        void onUserDeleteConfirmed(ViewUser surveyGroupId);
    }
}
