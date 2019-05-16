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
import org.akvo.flow.presentation.datapoints.map.offline.ViewOfflineArea;
import org.akvo.flow.ui.Navigator;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class OfflineAreasListActivity extends BackActivity implements OfflineAreasListView {

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

    private OfflineAreasListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_area_list);
        setupToolBar();
        initializeInjector();
        ButterKnife.bind(this);
        offlineAreasRv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OfflineAreasListAdapter(new ArrayList<>());
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
    public void showOfflineRegions(List<ViewOfflineArea> viewOfflineAreas) {
        Timber.d("Will show offline regions %s", viewOfflineAreas.size());
        emptyIv.setVisibility(View.GONE);
        emptyTitleTv.setVisibility(View.GONE);
        emptySubTitleTv.setVisibility(View.GONE);
        offlineAreasRv.setVisibility(View.VISIBLE);
        adapter.setOfflineAreas(viewOfflineAreas);
    }
}
