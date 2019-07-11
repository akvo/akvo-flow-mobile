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

package org.akvo.flow.offlinemaps.presentation.list;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.akvo.flow.mapbox.offline.reactive.DeleteOfflineRegion;
import org.akvo.flow.mapbox.offline.reactive.GetOfflineRegions;
import org.akvo.flow.mapbox.offline.reactive.RegionNameMapper;
import org.akvo.flow.mapbox.offline.reactive.RenameOfflineRegion;
import org.akvo.flow.offlinemaps.R;
import org.akvo.flow.offlinemaps.data.DataPreferenceRepository;
import org.akvo.flow.offlinemaps.domain.GetSelectedOfflineArea;
import org.akvo.flow.offlinemaps.domain.SaveSelectedOfflineArea;
import org.akvo.flow.offlinemaps.presentation.OfflineAreaViewActivity;
import org.akvo.flow.offlinemaps.presentation.ToolBarBackActivity;
import org.akvo.flow.offlinemaps.presentation.list.delete.DeleteAreaDialog;
import org.akvo.flow.offlinemaps.presentation.list.entity.ListOfflineArea;
import org.akvo.flow.offlinemaps.presentation.list.entity.ListOfflineAreaMapper;
import org.akvo.flow.offlinemaps.presentation.list.entity.MapInfo;
import org.akvo.flow.offlinemaps.presentation.list.entity.MapInfoMapper;
import org.akvo.flow.offlinemaps.presentation.list.rename.RenameAreaDialog;
import org.akvo.flow.offlinemaps.presentation.selection.OfflineMapDownloadActivity;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

public class OfflineAreasListActivity extends ToolBarBackActivity
        implements OfflineAreasListView, OfflineAreasActionListener,
        RenameAreaDialog.RenameAreaListener, DeleteAreaDialog.DeleteAreaListener {

    private ImageView emptyIv;
    private TextView emptyTitleTv;
    private TextView emptySubTitleTv;
    private RecyclerView offlineAreasRv;
    private ProgressBar offlineAreasPb;

    private OfflineAreasListPresenter presenter;
    private OfflineAreasListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_area_list);
        setupToolBar();
        setUpViews();
        setUpPresenter();
    }

    private void setUpPresenter() {
        RegionNameMapper regionNameMapper = new RegionNameMapper();
        DataPreferenceRepository userRepository = new DataPreferenceRepository();
        presenter = new OfflineAreasListPresenter(
                new ListOfflineAreaMapper(regionNameMapper, new MapInfoMapper()),
                new GetOfflineRegions(this), new RenameOfflineRegion(this,
                regionNameMapper), new DeleteOfflineRegion(this),
                new SaveSelectedOfflineArea(userRepository),
                new GetSelectedOfflineArea(userRepository));
        presenter.setView(this);
        presenter.loadAreas();
    }

    private void setUpViews() {
        emptyIv = findViewById(R.id.empty_iv);
        emptyTitleTv = findViewById(R.id.empty_title_tv);
        emptySubTitleTv = findViewById(R.id.empty_subtitle_tv);
        offlineAreasRv = findViewById(R.id.offline_areas_rv);
        offlineAreasPb = findViewById(R.id.offline_areas_pb);
        findViewById(R.id.create_offline_area_fab).setOnClickListener(v -> {
            navigateToOfflineMapAreasCreation(OfflineAreasListActivity.this);
            finish();
        });
        offlineAreasRv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OfflineAreasListAdapter(new ArrayList<>(), this);
        offlineAreasRv.setAdapter(adapter);
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
    public void showOfflineRegions(List<ListOfflineArea> viewOfflineAreas, long selectedRegionId) {
        Timber.d("Will show offline regions %s", viewOfflineAreas.size());
        emptyIv.setVisibility(View.GONE);
        emptyTitleTv.setVisibility(View.GONE);
        emptySubTitleTv.setVisibility(View.GONE);
        offlineAreasRv.setVisibility(View.VISIBLE);
        adapter.setOfflineAreas(viewOfflineAreas, selectedRegionId);
    }

    @Override
    public void showRenameError() {
        displaySnackBar(offlineAreasRv, R.string.offline_map_rename_error);
    }

    @Override
    public void showDeleteError() {
        displaySnackBar(offlineAreasRv, R.string.offline_map_delete_error);
    }

    @Override
    public void showSelectError() {
        displaySnackBar(offlineAreasRv, R.string.offline_map_delete_error);
    }

    @Override
    public void selectRegion(long regionId) {
        adapter.selectRegion(regionId);
        presenter.selectRegion(regionId);
    }

    @Override
    public void deSelectRegion() {
        adapter.selectRegion(OfflineAreasListAdapter.NONE_SELECTED);
        presenter.selectRegion(OfflineAreasListAdapter.NONE_SELECTED);
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
        navigateToViewOffline(this, mapName, mapInfo);
    }

    @Override
    public void renameAreaConfirmed(long areaId, String name) {
        presenter.renameArea(areaId, name);
    }

    @Override
    public void deleteAreaConfirmed(long areaId) {
        presenter.deleteArea(areaId);
    }

    public void navigateToViewOffline(@Nullable Context context, String mapName, MapInfo mapInfo) {
        if (context != null) {
            Intent intent = new Intent(context, OfflineAreaViewActivity.class);
            intent.putExtra(OfflineAreaViewActivity.NAME_EXTRA, mapName);
            intent.putExtra(OfflineAreaViewActivity.MAP_INFO_EXTRA, mapInfo);
            context.startActivity(intent);
        }
    }

    public void navigateToOfflineMapAreasCreation(@Nullable Context context) {
        if (context != null) {
            Intent intent = new Intent(context, OfflineMapDownloadActivity.class);
            context.startActivity(intent);
        }
    }
}
