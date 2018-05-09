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

package org.akvo.flow.presentation.walkthrough;

import android.content.Context;
import android.graphics.drawable.ClipDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.akvo.flow.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class WalkThrough1Fragment extends Fragment {

    private NextListener listener;

    public WalkThrough1Fragment() {
    }

    public static WalkThrough1Fragment newInstance() {
        return new WalkThrough1Fragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if (activity instanceof NextListener) {
            listener = (NextListener) activity;
        } else {
            throw new IllegalArgumentException("Activity must implement NextListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.walkthrough_fragment_page1, container, false);
        ImageView imageview = (ImageView) view.findViewById(R.id.lock_icon);
        ClipDrawable drawable = (ClipDrawable) imageview.getBackground();
        drawable.setLevel(9000);
        ButterKnife.bind(this, view);
        return view;
    }

    @OnClick(R.id.button_next)
    void onNextClicked() {
        if (listener != null) {
            listener.onNextClicked();
        }
    }

    public interface NextListener {

        void onNextClicked();
    }
}
