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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.ui.Navigator;

import javax.inject.Inject;

public class OfflineAreasListActivity extends BackActivity {

    @Inject
    Navigator navigator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_area_list);
        setupToolBar();
        initializeInjector();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            navigator.navigateToOfflineMapAreasCreation(this);
            finish();
        });
    }

    private void initializeInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }
}
