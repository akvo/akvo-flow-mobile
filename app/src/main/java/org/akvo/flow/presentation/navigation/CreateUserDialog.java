/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.akvo.flow.R;

//TODO: rotating dialog, the text gets lost
public class CreateUserDialog extends DialogFragment implements
        UsernameInputTextWatcher.UsernameWatcherListener {

    public static final String TAG = "CreateUserDialog";

    private PositiveButtonHandler positiveButtonHandler;
    private CreateUserListener listener;
    private EditText userNameEt;

    public CreateUserDialog() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //TODO verify instanceof
        listener = (CreateUserListener)getActivity();
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
        //TODO: use xml
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LinearLayout main = new LinearLayout(getContext());
        main.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        main.setOrientation(LinearLayout.VERTICAL);
        builder.setTitle(R.string.add_user);
        builder.setMessage(R.string.username);
        userNameEt = new EditText(getActivity());
        userNameEt.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        userNameEt.setSingleLine();
        userNameEt.addTextChangedListener(new UsernameInputTextWatcher(this));
        main.addView(userNameEt);
        builder.setView(main);
        builder.setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = userNameEt.getText().toString();
                if (listener != null) {
                    listener.createUser(name);
                }
                dismiss();
            }
        });

        builder.setNegativeButton(R.string.cancelbutton,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       dismiss();
                    }
                });

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
