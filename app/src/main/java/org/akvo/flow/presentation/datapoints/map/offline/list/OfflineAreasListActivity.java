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

package org.akvo.flow.presentation.datapoints.map.offline.list;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.SnackBarManager;
import org.akvo.flow.presentation.datapoints.map.offline.list.delete.DeleteAreaDialog;
import org.akvo.flow.presentation.datapoints.map.offline.list.entity.ListOfflineArea;
import org.akvo.flow.presentation.datapoints.map.offline.list.entity.MapInfo;
import org.akvo.flow.presentation.datapoints.map.offline.list.rename.RenameAreaDialog;
import org.akvo.flow.ui.Navigator;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class OfflineAreasListActivity extends BackActivity
        implements OfflineAreasListView, OfflineAreasActionListener,
        RenameAreaDialog.RenameAreaListener, DeleteAreaDialog.DeleteAreaListener {

    @Inject
    Navigator navigator;

    @BindView(R.id.empty_iv)
    ImageView emptyIv;

    @BindView(R.id.empty_title_tv)
    TextView emptyTitleTv;

    @BindView(R.id.empty_subtitle_tv)
    TextView emptySubTitleTv;

    @BindView(R.id.offline_areas_rv)
    RecyclerView offlineAreasRv;

    @BindView(R.id.offline_areas_pb)
    ProgressBar offlineAreasPb;

    @Inject
    OfflineAreasListPresenter presenter;

    @Inject
    SnackBarManager snackBarManager;

    private OfflineAreasListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_area_list);
        setupToolBar();
        initializeInjector();
        ButterKnife.bind(this);
        offlineAreasRv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OfflineAreasListAdapter(new ArrayList<>(), this);
        offlineAreasRv.setAdapter(adapter);
        presenter.setView(this);
        presenter.loadAreas();
    }

    private void initializeInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.destroy();
    }

    @OnClick(R.id.create_offline_area_fab)
    protected void onCreateOfflineAreaPressed() {
        navigator.navigateToOfflineMapAreasCreation(this);
        finish();
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
    public void showOfflineRegions(List<ListOfflineArea> viewOfflineAreas) {
        Timber.d("Will show offline regions %s", viewOfflineAreas.size());
        emptyIv.setVisibility(View.GONE);
        emptyTitleTv.setVisibility(View.GONE);
        emptySubTitleTv.setVisibility(View.GONE);
        offlineAreasRv.setVisibility(View.VISIBLE);
        adapter.setOfflineAreas(viewOfflineAreas);
    }

    @Override
    public void showRenameError() {
        snackBarManager.displaySnackBar(offlineAreasRv, R.string.offline_map_rename_error, this);
    }

    @Override
    public void showDeleteError() {
        snackBarManager.displaySnackBar(offlineAreasRv, R.string.offline_map_delete_error, this);
    }

    @Override
    public void selectArea(long areaId) {
        //TODO
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
