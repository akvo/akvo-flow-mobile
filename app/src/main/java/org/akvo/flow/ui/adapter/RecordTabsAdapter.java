/*
 * Copyright (C) 2017,2019-2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.adapter;

import org.akvo.flow.ui.fragment.FormListFragment;
import org.akvo.flow.ui.fragment.ResponseListFragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class RecordTabsAdapter extends FragmentPagerAdapter {

    private static final int POSITION_SURVEYS = 0;

    private String[] tabsTitles;

    public RecordTabsAdapter(FragmentManager fm, String[] tabTitles) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.tabsTitles = tabTitles;
    }

    @Override
    public int getCount() {
        return tabsTitles.length;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position == POSITION_SURVEYS) {
            return FormListFragment.newInstance();
        } else {
            return ResponseListFragment.newInstance();
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabsTitles[position];
    }
}
