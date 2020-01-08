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

package org.akvo.flow.offlinemaps.presentation;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.akvo.flow.offlinemaps.R;
import org.akvo.flow.offlinemaps.presentation.dialog.OfflineMapsDialog;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

public class OfflineMapsFloatingActionButton extends FloatingActionButton {

    public OfflineMapsFloatingActionButton(Context context) {
        this(context, null);
    }

    public OfflineMapsFloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.floatingActionButtonStyle);
    }

    public OfflineMapsFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOnClickListener(v -> {
            DialogFragment dialogFragment = OfflineMapsDialog.newInstance();
            dialogFragment
                    .show(((FragmentActivity) context).getSupportFragmentManager(),
                            OfflineMapsDialog.TAG);
        });
    }
}
