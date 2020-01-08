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
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.akvo.flow.R;
import org.akvo.flow.util.ConstantUtil;

public class UserOptionsDialog extends DialogFragment {

    public static final String TAG = "UserOptionsDialog";
    
    private static final int OPTION_EDIT_USER = 0;

    private ViewUser viewUser;
    private UserOptionListener listener;

    public UserOptionsDialog() {
    }

    public static UserOptionsDialog newInstance(ViewUser viewUser) {
        UserOptionsDialog fragment = new UserOptionsDialog();
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
        listener = (UserOptionListener) getActivity();
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
        builder.setTitle(viewUser.getName())
                .setItems(R.array.user_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedOption) {
                        if (selectedOption == OPTION_EDIT_USER) {
                            if (listener != null) {
                                listener.onEditUser(viewUser);
                            }
                        } else {
                            if (listener != null) {
                                listener.onDeleteUser(viewUser);
                            }
                        }
                        dismiss();
                    }
                });
        return builder.create();
    }

    public interface UserOptionListener {

        void onEditUser(ViewUser viewUser);

        void onDeleteUser(ViewUser viewUser);
    }
}
