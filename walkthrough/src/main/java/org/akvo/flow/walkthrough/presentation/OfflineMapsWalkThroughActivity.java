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

package org.akvo.flow.walkthrough.presentation;

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import org.akvo.flow.walkthrough.R;
import org.akvo.flow.walkthrough.di.DaggerWalkThroughFeatureComponent;
import org.akvo.flow.walkthrough.di.WalkThroughFeatureModule;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class OfflineMapsWalkThroughActivity extends AppCompatActivity {

    @Inject
    OfflineMapsWalkThroughPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_maps_walkthrough);
        initialiseInjector();
        setStatusBackgroundColor();
        setupFab();
        presenter.setWalkThroughSeen();
    }

    private void setupFab() {
        findViewById(R.id.floatingActionButton).setOnClickListener(v -> {
            finish();
        });
    }

    private void setStatusBackgroundColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black_main));
        }
    }

    private void initialiseInjector() {
        DaggerWalkThroughFeatureComponent
                .builder()
                .walkThroughFeatureModule(new WalkThroughFeatureModule(getApplication()))
                .build()
                .inject(this);
    }
}
