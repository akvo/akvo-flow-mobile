/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.uicomponents.BackActivity;
import org.akvo.flow.uicomponents.SnackBarManager;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import butterknife.BindView;
import butterknife.ButterKnife;

public class HelpActivity extends BackActivity {

    private static final String SUPPORT_URL = "https://flowsupport.akvo.org/container/show/akvo-flow-app";

    @BindView(R.id.help_wv)
    WebView helpWv;

    @BindView(R.id.help_pb)
    ProgressBar helpPb;

    @BindView(R.id.coordinator_layout)
    CoordinatorLayout rootView;

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
        loadWebView();
    }

    @Override
    public void applyOverrideConfiguration(final Configuration overrideConfiguration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            overrideConfiguration.uiMode &= ~Configuration.UI_MODE_NIGHT_MASK;
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    private void initializeInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return {@link ApplicationComponent}
     */
    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getApplication()).getApplicationComponent();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setUpWebView() {
        helpWv.setWebViewClient(new HelpWebViewClient(this));
        helpWv.getSettings().setJavaScriptEnabled(true);
    }

    public void displayError() {
        snackBarManager.displaySnackBarWithAction(rootView, R.string.error_loading_help,
                R.string.action_retry, v -> loadWebView(), this);
    }

    public void loadWebView() {
        helpWv.loadUrl(SUPPORT_URL);
    }

    public void hideProgress() {
        helpPb.setVisibility(View.GONE);
    }

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
