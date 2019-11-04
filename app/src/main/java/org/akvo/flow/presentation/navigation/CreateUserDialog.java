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

package org.akvo.flow.presentation.navigation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.akvo.flow.R;
import org.akvo.flow.uicomponents.NameInputTextWatcher;
import org.akvo.flow.uicomponents.PositiveButtonHandler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

public class CreateUserDialog extends DialogFragment implements
        NameInputTextWatcher.UsernameWatcherListener {

    public static final String TAG = "CreateUserDialog";
    private static final String USER_NAME_PARAM = "user_name";

    private PositiveButtonHandler positiveButtonHandler;
    private CreateUserListener listener;
    private EditText userNameEt;

    public CreateUserDialog() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if (activity instanceof CreateUserListener) {
            listener = (CreateUserListener) activity;
        } else {
            throw new IllegalArgumentException("Activity must implement CreateUserListener");
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
        Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View main = LayoutInflater.from(context).inflate(R.layout.user_name_input_dialog, null);
        builder.setTitle(R.string.add_user);
        builder.setMessage(R.string.add_user_message);
        userNameEt = main.findViewById(R.id.user_name_et);
        userNameEt.addTextChangedListener(new NameInputTextWatcher(this));
        builder.setView(main);
        builder.setPositiveButton(R.string.okbutton, (dialog, which) -> {
            String name = userNameEt.getText().toString();
            if (listener != null) {
                listener.createUser(name);
            }
            dismiss();
        });

        builder.setNegativeButton(R.string.cancelbutton,
                (dialog, which) -> dismiss());

        return builder.create();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        positiveButtonHandler = new PositiveButtonHandler(this);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(USER_NAME_PARAM, userNameEt.getText().toString());
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            userNameEt.setText(savedInstanceState.getString(USER_NAME_PARAM));
        }
    }

    @Override
    public void updateTextChanged() {
        String text = userNameEt.getText().toString();
        if (TextUtils.isEmpty(text)) {
            positiveButtonHandler.disablePositiveButton();
        } else {
            positiveButtonHandler.enablePositiveButton();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTextChanged();
    }

    public interface CreateUserListener {

        void createUser(String userName);
    }
}
