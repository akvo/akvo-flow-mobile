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

package org.akvo.flow.presentation.form.view.groups

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.akvo.flow.presentation.form.view.entity.ViewQuestionGroup
import org.akvo.flow.presentation.form.view.groups.repeatable.RepeatableQuestionGroupFragment

class QuestionGroupsPagerAdapter(fm: FragmentManager) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    var groups: List<ViewQuestionGroup> = mutableListOf()

    override fun getItem(position: Int): Fragment {
        val viewQuestionGroup = groups[position]
        if (viewQuestionGroup.isRepeatable) {
            return RepeatableQuestionGroupFragment.newInstance(viewQuestionGroup.heading,
                viewQuestionGroup.repetitions)
        } else {
            return QuestionGroupFragment.newInstance(viewQuestionGroup.heading,
                viewQuestionGroup.questionAnswers)
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        return groups[position].heading
    }

    override fun getCount(): Int {
        return groups.size
    }
}
