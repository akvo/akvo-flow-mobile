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

package org.akvo.flow.presentation.walkthrough;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.View;

import org.akvo.flow.R;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.BaseActivity;
import org.akvo.flow.ui.Navigator;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class WalkThroughActivity extends BaseActivity implements
        WalkThrough1Fragment.NextListener, WalkThrough2Fragment.OkListener, WalkthroughView {

    @BindView(R.id.walkthrough_pager)
    ViewPager viewPager;

    @BindView(R.id.walkthrough_indicator)
    DotIndicator indicator;

    @Inject
    WalkthroughPresenter presenter;

    @Inject
    Navigator navigator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walkthrough);
        initializeInjector();
        ButterKnife.bind(this);
        presenter.setView(this);
        presenter.setWalkThroughSeen();
        setStatusBackgroundColor();
        hideActionBar();
        setUpViews();
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent()).build();
        viewComponent.inject(this);
    }

    private void setUpViews() {
        WalthroughFragmentAdapter adapter = new WalthroughFragmentAdapter(
                getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(indicator);
    }

    private void setStatusBackgroundColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black_main));
        }
    }

    private void hideActionBar() {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.hide();
        }
    }

    @Override
    public void onNextClicked() {
        viewPager.setCurrentItem(1, true);
    }

    @Override
    public void onOkClicked() {
        presenter.onOkClicked();
    }

    @Override
    public void navigateToDeviceSetUp() {
        navigator.navigateToAddUser(this);
        finish();
    }

    @Override
    public void navigateToSurvey() {
        navigator.navigateToSurveyActivity(this);
        finish();
    }
}
