/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.service.UserRequestedApkUpdateService;
import org.akvo.flow.ui.Navigator;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AboutActivity extends BackActivity {

    @Inject
    Navigator navigator;

    @BindView(R.id.text_version)
    TextView version;

    @BindView(R.id.text_copyright)
    TextView copyright;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);
        initializeInjector();
        setupToolBar();
        initializeViews();
    }

    private void initializeViews() {
        version.setText(getString(R.string.about_view_version, BuildConfig.VERSION_NAME));
        copyright.setText(getString(R.string.about_view_copyright, BuildConfig.BUILD_YEAR));
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent()).build();
        viewComponent.inject(this);
    }

    @OnClick(R.id.text_check_updates)
    void onCheckUpdatesTap() {
        startService(new Intent(this, UserRequestedApkUpdateService.class));
    }

    @OnClick(R.id.text_release_notes)
    void onViewReleaseNotesTap() {
        navigator.navigateToReleaseNotes(this);
    }

    @OnClick(R.id.text_legal_info)
    void onViewLegalInfoTap() {
        navigator.navigateToLegalInfo(this);
    }

    @OnClick(R.id.text_terms)
    void onViewTermsTap() {
        navigator.navigateToTerms(this);
    }
}
