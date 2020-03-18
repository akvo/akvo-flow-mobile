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
import kotlinx.android.synthetic.main.question_group_fragment.questionsRv
import org.akvo.flow.R
import org.akvo.flow.presentation.form.view.groups.entity.Question
import org.akvo.flow.presentation.form.view.groups.entity.QuestionAnswer

class QuestionGroupFragmentFragment : Fragment() {

    private lateinit var questionGroupTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questionGroupTitle = arguments!!.getString(QUESTION_GROUP_TITLE, "")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.question_group_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        questionsRv.layoutManager = LinearLayoutManager(activity)
        val questionAnswer1 = QuestionAnswer(
            Question.FreeTextQuestion(
                "123",
                "1. text question",
                false,
                emptyList(),
                true
            ), "valeria"
        )
        val questionAnswer2 = QuestionAnswer(
            Question.NumberQuestion(
                "123", "2. number question", false, emptyList(),
                requireDoubleEntry = true,
                allowSign = true,
                allowDecimalPoint = true,
                minimumValue = 0.0,
                maximumValue = 10.0
            ), "2.0"
        )

        val barcodeAnswer = QuestionAnswer(
            Question.BarcodeQuestion(
                "123", "3. barcode question", false, emptyList()), "123456789"
        )
        val emptyQuestionAnswer = QuestionAnswer(
            Question.FreeTextQuestion(
                "123",
                "4. empty question",
                false,
                emptyList(),
                true
            ), ""
        )

        val dateAnswer = QuestionAnswer(
            Question.DateQuestion(
                "123", "5. date question", false, emptyList()), "Oct 23, 2019"
        )
        questionsRv.adapter = GroupQuestionsAdapter(
            mutableListOf(
                questionAnswer1,
                questionAnswer2,
                barcodeAnswer,
                emptyQuestionAnswer,
                dateAnswer
            )
        )
    }

    companion object {

        private const val QUESTION_GROUP_TITLE = "group_title"

        @JvmStatic
        fun newInstance(questionGroupTitle: String): QuestionGroupFragmentFragment {
            return QuestionGroupFragmentFragment().apply {
                arguments = Bundle().apply {
                    putString(QUESTION_GROUP_TITLE, questionGroupTitle)
                }
            }
        }
    }
}
