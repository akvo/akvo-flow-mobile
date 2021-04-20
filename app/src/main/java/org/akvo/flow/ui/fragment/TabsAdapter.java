/*
 * Copyright (C) 2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.fragment;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.presentation.datapoints.list.DataPointsListFragment;
import org.akvo.flow.presentation.datapoints.map.DataPointsMapFragment;

import java.util.Map;
import java.util.WeakHashMap;

public class TabsAdapter extends FragmentPagerAdapter {

    private final String[] tabs;
    private SurveyGroup surveyGroup;
    private final Map<Integer, Fragment> fragmentsRef = new WeakHashMap<>(2);

    public static final int POSITION_LIST = 0;
    public static final int POSITION_MAP = 1;

    public TabsAdapter(FragmentManager fm, String[] tabs, SurveyGroup surveyGroup) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.tabs = tabs;
        this.surveyGroup = surveyGroup;
    }

    @Override
    public int getCount() {
        return tabs.length;
    }

    public void refreshFragments(SurveyGroup newSurveyGroup) {
        this.surveyGroup = newSurveyGroup;
        DataPointsListFragment listFragment = (DataPointsListFragment) fragmentsRef
                .get(POSITION_LIST);
        DataPointsMapFragment mapFragment = getMapFragment();

        if (listFragment != null) {
            listFragment.onNewSurveySelected(surveyGroup);
        }
        if (mapFragment != null) {
            mapFragment.onNewSurveySelected(surveyGroup);
        }
    }

    public DataPointsMapFragment getMapFragment() {
        return (DataPointsMapFragment) fragmentsRef.get(POSITION_MAP);
    }

    public DataPointsListFragment getListFragment() {
        return (DataPointsListFragment) fragmentsRef.get(POSITION_LIST);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        if (position == POSITION_LIST) {
            DataPointsListFragment dataPointsListFragment = (DataPointsListFragment) super
                    .instantiateItem(container, position);
            fragmentsRef.put(POSITION_LIST, dataPointsListFragment);
            return dataPointsListFragment;
        } else {
            DataPointsMapFragment mapFragment = (DataPointsMapFragment) super
                    .instantiateItem(container, position);
            fragmentsRef.put(POSITION_MAP, mapFragment);
            return mapFragment;
        }
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position == POSITION_LIST) {
            return DataPointsListFragment.newInstance(surveyGroup);
        }
        return DataPointsMapFragment.newInstance(surveyGroup);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabs[position];
    }

    public void refreshMap() {
        getMapFragment().refreshView();
    }
}
