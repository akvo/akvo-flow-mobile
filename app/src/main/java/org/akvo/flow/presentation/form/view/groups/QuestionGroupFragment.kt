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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.akvo.flow.R
import org.akvo.flow.presentation.form.view.groups.entity.ViewQuestionAnswer

class QuestionGroupFragment : Fragment() {

    private lateinit var questionGroupTitle: String
    private lateinit var questionAnswers: ArrayList<ViewQuestionAnswer>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questionGroupTitle = arguments!!.getString(QUESTION_GROUP_TITLE, "")
        questionAnswers = arguments!!.getParcelableArrayList(QUESTION_ANSWERS)?: arrayListOf()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.question_group_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<RecyclerView>(R.id.questionsRv).apply {
            layoutManager = LinearLayoutManager(activity)
            recycledViewPool.setMaxRecycledViews(
                GroupQuestionsAdapter.ViewType.OPTION.ordinal,
                0
            )
            recycledViewPool.setMaxRecycledViews(
                GroupQuestionsAdapter.ViewType.BARCODE.ordinal,
                0
            )
            recycledViewPool.setMaxRecycledViews(
                GroupQuestionsAdapter.ViewType.CASCADE.ordinal,
                0
            )
            adapter = GroupQuestionsAdapter<QuestionViewHolder<ViewQuestionAnswer>>(questionAnswers)
        }
    }

    companion object {

        private const val QUESTION_GROUP_TITLE = "group_title"
        private const val QUESTION_ANSWERS = "answers"

        @JvmStatic
        fun newInstance(
            questionGroupTitle: String,
            questionAnswers: ArrayList<ViewQuestionAnswer>
        ): QuestionGroupFragment {
            return QuestionGroupFragment().apply {
                arguments = Bundle().apply {
                    putString(QUESTION_GROUP_TITLE, questionGroupTitle)
                    putParcelableArrayList(QUESTION_ANSWERS, questionAnswers)
                }
            }
        }
    }
}
