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

package org.akvo.flow.presentation.form.mobiledata;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;

import javax.inject.Inject;

public class MobileDataSettingDialog extends DialogFragment implements MobileDataSettingView {

    private static final String PARAM_INSTANCE_ID = "instance_id";
    public static final String TAG = "MobileDataSettingDialog";
    private long instanceId;
    private MobileDataSettingListener listener;

    @Inject
    MobileDataSettingPresenter presenter;

    public MobileDataSettingDialog() {
    }

    public static MobileDataSettingDialog newInstance(long instanceId) {
        Bundle arguments = new Bundle(1);
        arguments.putLong(PARAM_INSTANCE_ID, instanceId);
        MobileDataSettingDialog warningDialog = new MobileDataSettingDialog();
        warningDialog.setArguments(arguments);
        return warningDialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if (activity instanceof MobileDataSettingListener) {
            listener = (MobileDataSettingListener) activity;
        } else {
            throw new IllegalArgumentException("Activity must implement MobileDataSettingListener");
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
        instanceId = getArguments().getLong(PARAM_INSTANCE_ID);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initializeInjector();
        presenter.setView(this);
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent())
                .build();
        viewComponent.inject(this);
    }

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return {@link ApplicationComponent}
     */
    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getActivity().getApplication()).getApplicationComponent();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.mobile_data_dialog_title)
                .setMessage(R.string.mobile_data_dialog_subtitle)
                .setNegativeButton(R.string.no_thanks_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.saveEnableMobileData(false);
                    }
                })
                .setPositiveButton(R.string.enable_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        presenter.saveEnableMobileData(true);
                    }
                });
        return builder.create();
    }

    @Override
    public void dismissView() {
        if (listener != null) {
            listener.onMobileUploadSet(instanceId);
        }
    }

    public interface MobileDataSettingListener {

        void onMobileUploadSet(long instanceId);
    }
}
