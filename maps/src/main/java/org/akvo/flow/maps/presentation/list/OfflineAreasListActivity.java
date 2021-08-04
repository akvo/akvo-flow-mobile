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

package org.akvo.flow.maps.presentation.list;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.akvo.flow.maps.Constants;
import org.akvo.flow.maps.R;
import org.akvo.flow.maps.di.DaggerOfflineFeatureComponent;
import org.akvo.flow.maps.di.OfflineFeatureModule;
import org.akvo.flow.maps.domain.entity.DomainOfflineArea;
import org.akvo.flow.maps.domain.entity.MapInfo;
import org.akvo.flow.maps.presentation.Navigator;
import org.akvo.flow.maps.presentation.list.delete.DeleteAreaDialog;
import org.akvo.flow.maps.presentation.list.rename.RenameAreaDialog;
import org.akvo.flow.maps.tracking.TrackingHelper;
import org.akvo.flow.uicomponents.BackActivity;
import org.akvo.flow.uicomponents.SnackBarManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

public class OfflineAreasListActivity extends BackActivity
        implements OfflineAreasListView, OfflineAreasActionListener,
        RenameAreaDialog.RenameAreaListener, DeleteAreaDialog.DeleteAreaListener {

    private ImageView emptyIv;
    private TextView emptyTitleTv;
    private TextView emptySubTitleTv;
    private RecyclerView offlineAreasRv;
    private ProgressBar offlineAreasPb;
    private OfflineAreasListAdapter adapter;
    private TrackingHelper trackingHelper;

    @Inject
    OfflineAreasListPresenter presenter;

    @Inject
    SnackBarManager snackBarManager;

    @Inject
    Navigator navigator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_area_list);
        initialiseInjector();
        setupToolBar();
        setUpViews();
        setUpPresenter();
        trackingHelper = new TrackingHelper(this);
    }

    private void initialiseInjector() {
        DaggerOfflineFeatureComponent
                .builder()
                .offlineFeatureModule(new OfflineFeatureModule(getApplication()))
                .build()
                .inject(this);
    }

    private void setUpPresenter() {
        presenter.setView(this);
    }

    private void setUpViews() {
        emptyIv = findViewById(R.id.empty_iv);
        emptyTitleTv = findViewById(R.id.empty_title_tv);
        emptySubTitleTv = findViewById(R.id.empty_subtitle_tv);
        offlineAreasRv = findViewById(R.id.offline_areas_rv);
        View fab = findViewById(R.id.create_offline_area_fab);
        fab.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int fabHeight = bottom - top;
                if (fabHeight > 0) {
                    fab.removeOnLayoutChangeListener(this);
                    offlineAreasRv
                            .setPadding(offlineAreasRv.getPaddingLeft(), offlineAreasRv.getPaddingTop(),
                                    offlineAreasRv.getPaddingRight(),
                                    offlineAreasRv.getPaddingBottom() + fabHeight);
                }
            }
        });

        offlineAreasPb = findViewById(R.id.offline_areas_pb);
        fab.setOnClickListener(
                v -> navigator.navigateToOfflineMapAreasCreation(OfflineAreasListActivity.this,
                        Constants.CALLING_SCREEN_EXTRA_LIST));
        offlineAreasRv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OfflineAreasListAdapter(new ArrayList<>(), this);
        offlineAreasRv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.loadAreas();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.destroy();
    }

    @Override
    public void showLoading() {
        offlineAreasPb.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        offlineAreasPb.setVisibility(View.GONE);
    }

    @Override
    public void displayNoOfflineMaps() {
        emptyIv.setVisibility(View.VISIBLE);
        emptyTitleTv.setVisibility(View.VISIBLE);
        emptySubTitleTv.setVisibility(View.VISIBLE);
        offlineAreasRv.setVisibility(View.GONE);
    }

    @Override
    public void showOfflineRegions(List<DomainOfflineArea> viewOfflineAreas, long selectedRegionId) {
        Timber.d("Will show offline regions %s", viewOfflineAreas.size());
        emptyIv.setVisibility(View.GONE);
        emptyTitleTv.setVisibility(View.GONE);
        emptySubTitleTv.setVisibility(View.GONE);
        offlineAreasRv.setVisibility(View.VISIBLE);
        adapter.setOfflineAreas(viewOfflineAreas, selectedRegionId);
    }

    @Override
    public void showRenameError() {
        snackBarManager.displaySnackBar(offlineAreasRv, R.string.offline_map_rename_error);
    }

    @Override
    public void showDeleteError() {
        snackBarManager.displaySnackBar(offlineAreasRv, R.string.offline_map_delete_error);
    }

    @Override
    public void showSelectError() {
        snackBarManager.displaySnackBar(offlineAreasRv, R.string.offline_map_delete_error);
    }

    @Override
    public void selectRegion(DomainOfflineArea offlineArea) {
        adapter.selectRegion(offlineArea.getId());
        presenter.selectRegion(offlineArea.getId());
        if (trackingHelper != null) {
            trackingHelper.logUseOfflineAreaSelected(offlineArea.getName());
        }
    }

    @Override
    public void deSelectRegion() {
        adapter.selectRegion(OfflineAreasListAdapter.NONE_SELECTED);
        presenter.selectRegion(OfflineAreasListAdapter.NONE_SELECTED);
        if (trackingHelper != null) {
            trackingHelper.logUseOnlineMapSelected();
        }
    }

    @Override
    public void renameArea(long areaId, String oldName) {
        DialogFragment dialog = RenameAreaDialog.newInstance(oldName, areaId);
        dialog.show(getSupportFragmentManager(), RenameAreaDialog.TAG);
    }

    @Override
    public void deleteArea(long areaId, String name) {
        DialogFragment dialog = DeleteAreaDialog.newInstance(areaId, name);
        dialog.show(getSupportFragmentManager(), DeleteAreaDialog.TAG);
    }

    @Override
    public void viewArea(String mapName, MapInfo mapInfo) {
        navigator.navigateToViewOffline(this, mapName, mapInfo);
    }

    @Override
    public void renameAreaConfirmed(long areaId, String name) {
        presenter.renameArea(areaId, name);
    }

    @Override
    public void deleteAreaConfirmed(long areaId) {
        presenter.deleteArea(areaId);
    }
}
