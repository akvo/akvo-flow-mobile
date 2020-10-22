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
package org.akvo.flow.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.akvo.flow.ui.fragment.FormListFragment
import org.akvo.flow.ui.fragment.ResponseListFragment

class RecordTabsAdapter(fm: FragmentManager?, private val tabsTitles: Array<String>) :
    FragmentPagerAdapter(fm!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int {
        return tabsTitles.size
    }

    override fun getItem(position: Int): Fragment {
        return if (position == POSITION_SURVEYS) {
            FormListFragment.newInstance()
        } else {
            ResponseListFragment.newInstance()
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return tabsTitles[position]
    }

    companion object {
        private const val POSITION_SURVEYS = 0
    }
}
