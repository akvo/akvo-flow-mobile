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
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import org.akvo.flow.R;
import org.akvo.flow.activity.BackActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HelpActivity extends BackActivity {

    private static final String SUPPORT_URL = "http://flowsupport.akvo.org/container/show/akvo-flow-app";

    @BindView(R.id.help_wv)
    WebView helpWv;

    @BindView(R.id.help_pb)
    ProgressBar helpPb;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        ButterKnife.bind(this);
        setupToolBar();
        helpPb.setVisibility(View.VISIBLE);
        helpWv.setWebViewClient(new HelpWebViewClient());
        helpWv.loadUrl(SUPPORT_URL);
        helpWv.getSettings().setJavaScriptEnabled(true);
    }

    //TODO: make static
    class HelpWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            helpPb.setVisibility(View.GONE);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request,
                WebResourceError error) {
            helpPb.setVisibility(View.GONE);
            super.onReceivedError(view, request, error);
        }
    }
}
