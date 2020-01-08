/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.offlinemaps.presentation.infowindow;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.akvo.flow.offlinemaps.R;

import androidx.annotation.Nullable;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class InfoWindowLayout extends LinearLayout {

    private TextView titleTextView;
    private TextView snippetTextView;
    private InfoWindowSelectionListener listener;

    public InfoWindowLayout(Context context) {
        this(context, null);
    }

    public InfoWindowLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InfoWindowLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.symbol_layer_info_window_layout_callout, this);
        setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        titleTextView = findViewById(R.id.info_window_title);
        snippetTextView = findViewById(R.id.info_window_description);
        setOnClickListener(v -> onInfoWindowClick((String) getTag()));
    }

    public void setSelectionListener(InfoWindowSelectionListener listener) {
        this.listener = listener;
    }

    private void onInfoWindowClick(String tag) {
       if (listener != null) {
           listener.onWindowSelected(tag);
       }
    }

    public void setUpMarkerInfo(String id, String name) {
        setTag(id);
        titleTextView.setText(name);
        snippetTextView.setText(id);
    }

    public interface InfoWindowSelectionListener {

        void onWindowSelected(String id);
    }
}
