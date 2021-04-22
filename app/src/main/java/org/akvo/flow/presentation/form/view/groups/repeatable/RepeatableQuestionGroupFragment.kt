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

package org.akvo.flow.presentation.form.view.groups.repeatable

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.akvo.flow.R

class RepeatableQuestionGroupFragment : Fragment(){

    private lateinit var questionGroupTitle: String
    private lateinit var groupRepetitions: ArrayList<GroupRepetition>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questionGroupTitle = arguments!!.getString(QUESTION_GROUP_TITLE, "")
        groupRepetitions = arguments!!.getParcelableArrayList(REPETITIONS_LIST)?: arrayListOf()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.repeatable_question_group_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<RecyclerView>(R.id.repetitionsRv).apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = RepeatableGroupQuestionAdapter(groupRepetitions)
        }
    }

    companion object {

        private const val QUESTION_GROUP_TITLE = "group_title"
        private const val REPETITIONS_LIST = "repetitions_list"

        @JvmStatic
        fun newInstance(
            questionGroupTitle: String,
            questionAnswers: ArrayList<GroupRepetition>
        ): RepeatableQuestionGroupFragment {
            return RepeatableQuestionGroupFragment().apply {
                arguments = Bundle().apply {
                    putString(QUESTION_GROUP_TITLE, questionGroupTitle)
                    putParcelableArrayList(REPETITIONS_LIST, questionAnswers)
                }
            }
        }
    }
}
