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

package org.akvo.flow.presentation.geoshape.properties;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;
import org.akvo.flow.presentation.geoshape.entities.Shape;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class PropertiesDialog extends DialogFragment implements PropertiesView {

    public static final String TAG = "PropertiesDialog";

    private static final String SHAPE_PARAM = "shape";

    private Shape shape;
    private ArrayAdapter<String> adapter;

    @Inject
    PropertiesPresenter presenter;

    public PropertiesDialog() {
    }

    public static PropertiesDialog newInstance(Shape shape) {
        PropertiesDialog fragment = new PropertiesDialog();
        Bundle bundle = new Bundle();
        bundle.putParcelable(SHAPE_PARAM, shape);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            shape = arguments.getParcelable(SHAPE_PARAM);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line);
        return new AlertDialog.Builder(getContext()).setTitle(R.string.geoshape_properties_title)
                .setAdapter(adapter, null).create();
    }

    @Override
    public void displayShapeCount(String count, String length, String area) {
        adapter.add(getString(R.string.geoshape_properties_point_count, count));
        if (!TextUtils.isEmpty(length)) {
            adapter.add(getString(R.string.geoshape_properties_length, length));
        }
        if (!TextUtils.isEmpty(area)) {
            adapter.add(getString(R.string.geoshape_properties_area, area));
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initializeInjector();
        presenter.setView(this);
        presenter.countProperties(shape);
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent())
                .build();
        viewComponent.inject(this);
    }

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return {@link ApplicationComponent}
     */
    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getActivity().getApplication()).getApplicationComponent();
    }

}
