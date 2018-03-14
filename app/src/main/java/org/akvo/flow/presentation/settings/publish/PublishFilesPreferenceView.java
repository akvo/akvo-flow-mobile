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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class PublishFilesPreferenceView extends LinearLayout {

    @BindView(R.id.preferenceProgress)
    ProgressBar preferenceProgress;

    @BindView(R.id.preferenceProgressLayout)
    FrameLayout preferenceProgressLayout;

    @BindView(R.id.preferenceProgressText)
    TextView preferenceProgressText;

    @BindView(R.id.preference_publish_data_title)
    TextView preferencePublishDataTitle;

    @BindView(R.id.preference_publish_data_subtitle)
    TextView preferencePublishDataSubTitle;

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
        ButterKnife.bind(this);
    }

    @OnClick(R.id.publish_files_preference)
    void onPublishClick() {
        Timber.d("onPublishClick");
        setEnabled(false);
        preferenceProgressLayout.setVisibility(VISIBLE);
        Context context = getContext();
        preferenceProgressText.setText(context
                .getString(R.string.preference_publish_data_time_left,
                        preferenceProgress.getProgress()));
        preferencePublishDataTitle
                .setTextColor(ContextCompat.getColor(context, R.color.black_disabled));
        preferencePublishDataSubTitle.setText(
                context.getString(R.string.preference_publish_data_subtitle_published));
    }
}
