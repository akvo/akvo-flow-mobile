/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.settings;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.view.View;
import android.widget.TextView;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.ui.Navigator;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PreferenceActivity extends BackActivity {

    @Inject
    Navigator navigator;

    @BindView(R.id.appbar_layout)
    AppBarLayout appBarLayout;

    @BindView(R.id.toolbar_shadow)
    View toolbarShadow;

    @BindView(R.id.preference_identifier_value)
    TextView deviceIdentifierTv;

    @BindView(R.id.preference_instance_value)
    TextView instanceNameTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);
        ButterKnife.bind(this);
        initializeInjector();
        setupToolBar();
        setUpToolBarAnimationListener();
        setUpPreferences();
    }

    private void setUpToolBarAnimationListener() {
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                    onToolBarCollapsed();
                } else if (verticalOffset == 0) {
                    onToolbarExpanded();
                } else {
                    onToolbarMove();
                }
            }

            private void onToolbarMove() {
                toolbarShadow.setVisibility(View.GONE);
            }

            private void onToolbarExpanded() {
                toolbarShadow.setVisibility(View.VISIBLE);
            }

            private void onToolBarCollapsed() {
                toolbarShadow.setVisibility(View.GONE);
            }
        });
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent()).build();
        viewComponent.inject(this);
    }

    private void setUpPreferences() {
        //TODO: retrieve values in preferences
        instanceNameTv.setText(BuildConfig.INSTANCE_URL);
    }
}
