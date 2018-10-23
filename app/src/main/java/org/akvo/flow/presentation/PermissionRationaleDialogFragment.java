/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import org.akvo.flow.R;
import org.akvo.flow.util.ConstantUtil;

public class PermissionRationaleDialogFragment extends DialogFragment {

    public static final String TAG = "permissions";
    public static final String PERMISSIONS_EXTRA = "PERMISSIONS_EXTRA";
    public static final String REQUEST_CODE_EXTRA = "REQUEST_CODE_EXTRA";
    public static final String QUESTION_ID_EXTRA = "QUESTION_ID_EXTRA";

    private PermissionRequestListener permissionRequestListener;
    private String[] permissions;
    private int code;
    private String questionId;

    public PermissionRationaleDialogFragment() {
    }

    public static PermissionRationaleDialogFragment newInstance(String[] permissions,
            int code, String questionId) {
        PermissionRationaleDialogFragment fragment = new PermissionRationaleDialogFragment();
        Bundle args = new Bundle(3);
        args.putStringArray(PERMISSIONS_EXTRA, permissions);
        args.putInt(REQUEST_CODE_EXTRA, code);
        args.putString(QUESTION_ID_EXTRA, questionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof PermissionRequestListener)) {
            throw new IllegalArgumentException("Activity must implement PermissionRequestListener");
        }
        permissionRequestListener = (PermissionRequestListener) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        permissions = arguments.getStringArray(PERMISSIONS_EXTRA);
        code = arguments.getInt(REQUEST_CODE_EXTRA);
        questionId = arguments.getString(QUESTION_ID_EXTRA);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(getPermissionExplanation(code))
                .setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        permissionRequestListener.requestPermissions(permissions, code, questionId);
                    }
                })
                .setNegativeButton(R.string.cancelbutton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        permissionRequestListener
                                .requestPermissionsCancelled(permissions, code, questionId);
                    }
                })
                .create();
    }

    private int getPermissionExplanation(int code) {
        switch (code) {
            case ConstantUtil.LOCATION_PERMISSION_CODE:
                return R.string.location_permission_explanation;
            default:
                return R.string.location_permission_explanation;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        permissionRequestListener = null;
    }

    public interface PermissionRequestListener {

        void requestPermissions(String [] permissions, int code, String questionId);

        void requestPermissionsCancelled(String[] permissions, int requestCode, String questionId);
    }
}
