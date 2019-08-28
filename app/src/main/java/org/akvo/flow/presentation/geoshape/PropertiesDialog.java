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

package org.akvo.flow.presentation.geoshape;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import org.akvo.flow.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class PropertiesDialog extends DialogFragment {

    public static final String TAG = "PropertiesDialog";

    private static final String POINT_COUNT_PARAM = "point_count";
    private static final String LENGTH_PARAM = "length";
    private static final String AREA_PARAM = "area";

    private String pointCount;
    private String length;
    private String area;

    public PropertiesDialog() {
    }

    public static PropertiesDialog newInstance(String pointCount, String length, String area) {
        PropertiesDialog fragment = new PropertiesDialog();
        Bundle bundle = new Bundle();
        bundle.putString(POINT_COUNT_PARAM, pointCount);
        bundle.putString(LENGTH_PARAM, length);
        bundle.putString(AREA_PARAM, area);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            pointCount = arguments.getString(POINT_COUNT_PARAM);
            length = arguments.getString(LENGTH_PARAM);
            area = arguments.getString(AREA_PARAM);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line);
        adapter.add(getString(R.string.geoshape_properties_point_count, pointCount));
        if (!TextUtils.isEmpty(length)) {
            adapter.add(getString(R.string.geoshape_properties_length, length));
        }
        if (!TextUtils.isEmpty(area)) {
            adapter.add(getString(R.string.geoshape_properties_area, area));
        }
        return new AlertDialog.Builder(getContext()).setTitle(R.string.geoshape_properties_title)
                .setAdapter(adapter, null).create();
    }
}
