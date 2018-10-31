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

package org.akvo.flow.presentation.form.mobiledata;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;

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
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.mobile_setting, null);
        builder.setTitle(R.string.mobile_data_setting_dialog_message)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.okbutton, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SwitchCompat viewById = view.findViewById(R.id.switch_enable_data);
                        presenter.saveEnableMobileData(viewById.isChecked());
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
