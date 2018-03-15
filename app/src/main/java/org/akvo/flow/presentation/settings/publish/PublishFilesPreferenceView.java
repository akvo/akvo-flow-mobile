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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.broadcast.BootReceiver;
import org.akvo.flow.broadcast.DataTimeoutReceiver;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PublishFilesPreferenceView extends LinearLayout implements IPublishFilesPreferenceView {

    @BindView(R.id.preferenceProgress)
    ProgressBar progressBar;

    @BindView(R.id.preferenceProgressLayout)
    FrameLayout progressLayout;

    @BindView(R.id.preferenceProgressText)
    TextView progressTextView;

    @BindView(R.id.preference_publish_data_title)
    TextView publishDateTitleTextView;

    @BindView(R.id.preference_publish_data_subtitle)
    TextView publishDateSubtitleTextView;

    @Inject
    PublishFilesPreferencePresenter presenter;

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
        progressLayout.setVisibility(VISIBLE);
        progressBar.setProgress(progress);
        Context context = getContext();
        progressTextView.setText(context.getString(R.string.preference_publish_data_time_left,
                progress));
        publishDateTitleTextView
                .setTextColor(ContextCompat.getColor(context, R.color.black_disabled));
        publishDateSubtitleTextView
                .setText(context.getString(R.string.preference_publish_data_subtitle_published));
    }

    @Override
    public void showUnPublished() {
        setEnabled(true);
        progressLayout.setVisibility(GONE);
        Context context = getContext();
        publishDateTitleTextView
                .setTextColor(ContextCompat.getColor(context, R.color.black_main));
        publishDateSubtitleTextView.setText(
                context.getString(R.string.preference_publish_data_subtitle));
    }

    @Override
    public void scheduleAlarm() {
        Context context = getContext().getApplicationContext();
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DataTimeoutReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        //TODO: change to MAX_PUBLISH_TIME_IN_MS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmMgr.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 90 * 1000, alarmIntent);
        } else {
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 90 * 1000, alarmIntent);
        }

        enableAlarmBootReceiver(context);
    }

    private void enableAlarmBootReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.destroy();
    }
}
