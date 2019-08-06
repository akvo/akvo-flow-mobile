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

package org.akvo.flow.offlinemaps.presentation.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.akvo.flow.offlinemaps.Constants;
import org.akvo.flow.offlinemaps.R;
import org.akvo.flow.offlinemaps.di.DaggerOfflineFeatureComponent;
import org.akvo.flow.offlinemaps.di.OfflineFeatureModule;
import org.akvo.flow.offlinemaps.domain.entity.DomainOfflineArea;
import org.akvo.flow.offlinemaps.presentation.Navigator;
import org.akvo.flow.offlinemaps.presentation.OfflineMapSelectedListener;
import org.akvo.flow.offlinemaps.tracking.TrackingHelper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class OfflineMapsDialog extends DialogFragment implements OfflineMapsView {

    public static final String TAG = "OfflineMapsDialog";

    @Inject
    OfflineMapsPresenter presenter;

    @Inject
    Navigator navigator;

    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private Button addMapButton;
    private TextView noMapsTextView;
    private TextView onLineMapTextView;

    private OfflineAreasAdapter adapter;
    private OfflineMapSelectedListener offlineMapSelectedListener;
    private TrackingHelper trackingHelper;

    public OfflineMapsDialog() {
    }

    public static OfflineMapsDialog newInstance() {
        return new OfflineMapsDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getActivity().getString(R.string.offline_maps_dialog_title));
        View main = LayoutInflater.from(getContext())
                .inflate(R.layout.offline_maps_dialog, null);
        builder.setView(main);
        progressBar = main.findViewById(R.id.progressBar);
        recyclerView = main.findViewById(R.id.recyclerView);
        addMapButton = main.findViewById(R.id.addMapButton);
        addMapButton.setOnClickListener(v -> {
            navigator.navigateToOfflineMapAreasCreation(getActivity(),
                    Constants.CALLING_SCREEN_EXTRA_DIALOG);
            dismiss();
        });
        noMapsTextView = main.findViewById(R.id.noMapsTextView);
        onLineMapTextView = main.findViewById(R.id.onlineMapTextView);
        onLineMapTextView.setOnClickListener(v -> {
            presenter.onOnlineMapSelected();
            if (trackingHelper != null) {
                trackingHelper.logUseOnlineMapSelected();
            }
        });
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if (!(activity instanceof OfflineMapSelectedListener)) {
            throw new IllegalArgumentException(
                    "Activity must implement OfflineMapSelectedListener");
        }
        offlineMapSelectedListener = (OfflineMapSelectedListener) activity;
        trackingHelper = new TrackingHelper(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        offlineMapSelectedListener = null;
        trackingHelper = null;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initialiseInjector();
        adapter = new OfflineAreasAdapter(new ArrayList<>(), offlineMapSelectedListener);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        presenter.setView(this);
        presenter.load();
        if (trackingHelper != null) {
            trackingHelper.logOfflineAreasListDialogOpened();
        }
    }

    private void initialiseInjector() {
        DaggerOfflineFeatureComponent
                .builder()
                .offlineFeatureModule(new OfflineFeatureModule(getActivity().getApplication()))
                .build()
                .inject(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.destroy();
    }

    @Override
    public void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void notifyMapChange() {
        offlineMapSelectedListener.onNewMapAreaSaved();
    }

    @Override
    public void displayRegions(List<DomainOfflineArea> offlineRegions, long selectedRegionId) {
        addMapButton.setVisibility(View.GONE);
        noMapsTextView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.setOfflineAreas(offlineRegions, selectedRegionId);
        if (selectedRegionId == DomainOfflineArea.UNSELECTED_REGION) {
            onLineMapTextView.setSelected(true);
        }
    }

    @Override
    public void displayNoOfflineMaps() {
        recyclerView.setVisibility(View.GONE);
        addMapButton.setVisibility(View.VISIBLE);
        noMapsTextView.setVisibility(View.VISIBLE);
        onLineMapTextView.setSelected(true);
    }

    public void onOfflineAreaSelected(DomainOfflineArea offlineArea) {
        presenter.onOfflineAreaSelected(offlineArea);
        if (trackingHelper != null) {
            trackingHelper.logUseOfflineAreaSelected(offlineArea.getName());
        }
    }
}
