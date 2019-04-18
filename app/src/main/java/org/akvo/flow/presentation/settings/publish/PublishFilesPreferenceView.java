/*
 * Copyright (C) 2018-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.presentation.settings.publish;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.SnackBarManager;
import org.akvo.flow.tracking.TrackingHelper;
import org.akvo.flow.util.AlarmHelper;
import org.akvo.flow.util.BootReceiverHelper;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PublishFilesPreferenceView extends LinearLayout
        implements IPublishFilesPreferenceView {

    @BindView(R.id.preferenceProgress)
    ProgressBar progressBar;

    @BindView(R.id.preferenceProgressLayout)
    FrameLayout progressLayout;

    @BindView(R.id.preferenceProgressText)
    TextView progressTextView;

    @BindView(R.id.preference_publish_data_title)
    TextView publishDataTitleTextView;

    @BindView(R.id.preference_publish_data_subtitle)
    TextView publishDataSubtitleTextView;

    @Inject
    PublishFilesPreferencePresenter presenter;

    @Inject
    AlarmHelper alarmHelper;

    @Inject
    BootReceiverHelper bootReceiverHelper;

    @Inject
    SnackBarManager snackBarManager;

    private TrackingHelper trackingHelper;

    public PublishFilesPreferenceView(Context context) {
        this(context, null);
    }

    public PublishFilesPreferenceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        inflate(getContext(), R.layout.preference_publish_data, this);
        initialiseInjector();
        ButterKnife.bind(this);
        presenter.setView(this);
        trackingHelper = new TrackingHelper(getContext());
    }

    private void initialiseInjector() {
        ViewComponent viewComponent =
                DaggerViewComponent.builder().applicationComponent(getApplicationComponent())
                        .build();
        viewComponent.inject(this);
    }

    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getContext().getApplicationContext()).getApplicationComponent();
    }

    @OnClick(R.id.publish_files_preference)
    void onPublishClick() {
        presenter.onPublishClick();
        trackingHelper.logPublishPressed();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.load();
    }

    @Override
    public void showPublished(int progress) {
        setEnabled(false);
        progressBar.setIndeterminate(false);
        progressLayout.setVisibility(VISIBLE);
        progressTextView.setVisibility(VISIBLE);
        progressBar.setProgress(progress);
        Context context = getContext();
        progressTextView.setText(context.getString(R.string.preference_publish_data_time_left,
                progress));
        publishDataTitleTextView
                .setTextColor(ContextCompat.getColor(context, R.color.black_disabled));
        publishDataSubtitleTextView
                .setText(context.getString(R.string.preference_publish_data_subtitle_published));
    }

    @Override
    public void showUnPublished() {
        setEnabled(true);
        progressBar.setIndeterminate(false);
        progressLayout.setVisibility(GONE);
        Context context = getContext();
        publishDataTitleTextView
                .setTextColor(ContextCompat.getColor(context, R.color.black_main));
        publishDataSubtitleTextView.setText(
                context.getString(R.string.preference_publish_data_subtitle));
    }

    @Override
    public void showLoading() {
        setEnabled(false);
        progressBar.setIndeterminate(true);
        progressTextView.setVisibility(INVISIBLE);
        progressLayout.setVisibility(VISIBLE);
        Context context = getContext();
        publishDataSubtitleTextView
                .setText(context.getString(R.string.preference_publish_data_subtitle_publishing));
        publishDataTitleTextView
                .setTextColor(ContextCompat.getColor(context, R.color.black_disabled));
    }

    @Override
    public void scheduleAlarm() {
        alarmHelper.scheduleAlarm(PublishedTimeHelper.MAX_PUBLISH_TIME_IN_MS);
        bootReceiverHelper.enableBootReceiver();
    }

    @Override
    public void showNoDataToPublish() {
        snackBarManager.displaySnackBar(this, R.string.preference_publish_data_error_no_data,
                getContext());
    }

    @Override
    public void showGenericPublishError() {
        snackBarManager
                .displaySnackBarWithAction(this, R.string.preference_publish_data_error_generic,
                        R.string.action_retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                presenter.onPublishClick();
                            }
                        },
                        getContext());
    }

    @Override
    public void showNoSpaceLeftError() {
        snackBarManager.displaySnackBar(this, R.string.preference_publish_data_error_no_space,
                getContext());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.destroy();
    }
}
