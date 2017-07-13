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

package org.akvo.flow.ui.view.navigation;

import android.content.Context;
import android.support.design.widget.NavigationView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import org.akvo.flow.R;

import butterknife.ButterKnife;

public class AkvoNavigationView extends NavigationView {

    private TextView currentUserTv;
    private TextView surveyTitleTv;
    private RecyclerView surveysRv;
    private RecyclerView usersRv;

    public AkvoNavigationView(Context context) {
        this(context, null);
    }

    public AkvoNavigationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AkvoNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View headerView = getHeaderView(0);
        currentUserTv = ButterKnife.findById(headerView, R.id.current_user_name);
        surveyTitleTv = ButterKnife.findById(headerView, R.id.surveys_title_tv);
        currentUserTv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (surveysRv.getVisibility() == VISIBLE) {
                    surveyTitleTv.setVisibility(GONE);
                    surveysRv.setVisibility(GONE);
                    usersRv.setVisibility(VISIBLE);
                } else {
                    surveyTitleTv.setVisibility(VISIBLE);
                    surveysRv.setVisibility(VISIBLE);
                    usersRv.setVisibility(GONE);
                }
            }
        });
        surveysRv = ButterKnife.findById(headerView, R.id.surveys_rv);
        usersRv = ButterKnife.findById(headerView, R.id.users_rv);
        surveysRv.setLayoutManager(new LinearLayoutManager(getContext()));
        surveysRv.setAdapter(new SurveyAdapter());
    }
}
