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

package org.akvo.flow.presentation.help;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.SnackBarManager;
import org.akvo.flow.ui.Navigator;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HelpActivity extends BackActivity implements HelpView {

    private static final String SUPPORT_URL = "http://flowsupport.akvo.org/container/show/akvo-flow-app";

    @BindView(R.id.help_wv)
    WebView helpWv;

    @BindView(R.id.help_pb)
    ProgressBar helpPb;

    @BindView(R.id.coordinator_layout)
    CoordinatorLayout rootView;

    @Inject
    HelpPresenter presenter;

    @Inject
    Navigator navigator;

    @Inject
    SnackBarManager snackBarManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        initializeInjector();
        ButterKnife.bind(this);
        setupToolBar();
        showProgress();
        setUpWebView();
        presenter.setView(this);
    }

    private void initializeInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.load();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.destroy();
    }

    private void setUpWebView() {
        helpWv.setWebViewClient(new HelpWebViewClient(this));
        helpWv.getSettings().setJavaScriptEnabled(true);
    }

    @Override
    public void displayError() {
        snackBarManager.displaySnackBarWithAction(rootView, R.string.error_loading_help,
                R.string.action_retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        presenter.load();
                    }
                }, this);
    }

    @Override
    public void loadWebView() {
        helpWv.loadUrl(SUPPORT_URL);
    }

    @Override
    public void displayErrorDataSyncDisabled() {
        snackBarManager.displaySnackBarWithAction(rootView, R.string.error_mobile_data_sync,
                R.string.action_settings, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        navigator.navigateToPreferences(HelpActivity.this);
                    }
                }, this);
    }

    @Override
    public void hideProgress() {
        helpPb.setVisibility(View.GONE);
    }

    @Override
    public void showProgress() {
        helpPb.setVisibility(View.VISIBLE);
    }

    static class HelpWebViewClient extends WebViewClient {

        private final WeakReference<HelpActivity> activityWeakReference;

        HelpWebViewClient(HelpActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            hideProgress();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request,
                WebResourceError error) {
            super.onReceivedError(view, request, error);
            hideProgress();
            displayError();
        }

        private void hideProgress() {
            HelpActivity helpActivity = activityWeakReference.get();
            if (helpActivity != null) {
                helpActivity.hideProgress();
            }
        }

        private void displayError() {
            HelpActivity helpActivity = activityWeakReference.get();
            if (helpActivity != null) {
                helpActivity.displayError();
            }
        }
    }

}
