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

package org.akvo.flow.presentation.settings.publish;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
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
        progressBar.setIndeterminateDrawable(ContextCompat
                .getDrawable(getContext(), R.drawable.circle_progress_drawable_moving));
        progressTextView.setVisibility(INVISIBLE);
        progressLayout.setVisibility(VISIBLE);
    }

    @Override
    public void scheduleAlarm() {
        alarmHelper.scheduleAlarm(PublishedTimeHelper.MAX_PUBLISH_TIME_IN_MS);
        bootReceiverHelper.enableBootReceiver();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.destroy();
    }
}
