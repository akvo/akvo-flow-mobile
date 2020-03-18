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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import org.akvo.flow.R
import org.akvo.flow.presentation.form.view.groups.entity.Question
import org.akvo.flow.presentation.form.view.groups.entity.Question.NumberQuestion
import org.akvo.flow.presentation.form.view.groups.entity.QuestionAnswer
import org.akvo.flow.presentation.form.view.groups.entity.exhaustive

class GroupQuestionsAdapter(val questionAnswers: MutableList<QuestionAnswer> = mutableListOf()) :
    RecyclerView.Adapter<QuestionViewHolder>() {

    enum class ViewType {
        SINGLE_INPUT,
        DOUBLE_INPUT
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        return when (viewType) {
            ViewType.SINGLE_INPUT.ordinal -> {
                QuestionViewHolder.SingleQuestionViewHolder(
                    inflate(
                        parent,
                        R.layout.single_input_field_question_view
                    )
                )
            }
            else -> {
                QuestionViewHolder.DoubleQuestionViewHolder(
                    inflate(
                        parent,
                        R.layout.double_input_field_question_view
                    )
                )
            }
        }
    }

    private fun inflate(parent: ViewGroup, @LayoutRes layoutResId: Int) =
        LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)

    override fun getItemCount(): Int = questionAnswers.size

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.setUpView(questionAnswers[position])
    }

    override fun getItemViewType(position: Int): Int {
        val question = questionAnswers[position].question
        return when (question) {
            is Question.FreeTextQuestion -> {
                checkRepeatableType(question.requireDoubleEntry)
            }
            is NumberQuestion -> {
                checkRepeatableType(question.requireDoubleEntry)
            }
            is Question.OptionQuestion -> TODO()
            is Question.CascadeQuestion -> TODO()
            is Question.LocationQuestion -> TODO()
            is Question.PhotoQuestion -> TODO()
            is Question.VideoQuestion -> TODO()
            is Question.DateQuestion -> {
                ViewType.SINGLE_INPUT.ordinal
            }
            is Question.BarcodeQuestion -> {
                //TODO: barcode can be multiple
                ViewType.SINGLE_INPUT.ordinal
            }
            is Question.GeoShapeQuestion -> TODO()
            is Question.SignatureQuestion -> TODO()
            is Question.CaddisflyQuestion -> TODO()
        }.exhaustive
    }

    private fun checkRepeatableType(repeatable: Boolean): Int {
        return when {
            repeatable -> {
                ViewType.DOUBLE_INPUT.ordinal
            }
            else -> {
                ViewType.SINGLE_INPUT.ordinal
            }
        }
    }
}

sealed class QuestionViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

    abstract fun setUpView(questionAnswer: QuestionAnswer)

    private fun mandatoryText(questionAnswer: QuestionAnswer): String {
        return when {
            questionAnswer.question.mandatory -> {
                " *"
            }
            else -> {
                ""
            }
        }
    }

    fun setUpTitle(questionAnswer: QuestionAnswer) {
        view.findViewById<TextView>(R.id.questionTitle).text =
            "${questionAnswer.question.title}${mandatoryText(questionAnswer)}"
    }

    fun setUpInputText(answer: String, textInputEditText: TextInputEditText) {
        if (answer.isEmpty()) {
            textInputEditText.visibility = View.GONE
        } else {
            textInputEditText.visibility = View.VISIBLE
            textInputEditText.setText(answer)
        }
    }

    class SingleQuestionViewHolder(singleView: View) : QuestionViewHolder(singleView) {
        override fun setUpView(questionAnswer: QuestionAnswer) {
            setUpTitle(questionAnswer)
            val answer = questionAnswer.answer
            val textInputEditText = view.findViewById<TextInputEditText>(R.id.questionResponseInput)
            setUpInputText(answer, textInputEditText)
        }
    }

    class DoubleQuestionViewHolder(singleView: View) : QuestionViewHolder(singleView) {
        override fun setUpView(questionAnswer: QuestionAnswer) {
            setUpTitle(questionAnswer)
            val answer = questionAnswer.answer
            val textInputEditText = view.findViewById<TextInputEditText>(R.id.questionResponseInput)
            setUpInputText(answer, textInputEditText)
            val repeatedInput = view.findViewById<TextInputEditText>(R.id.questionResponseRepeated)
            setUpInputText(answer, repeatedInput)
            val repeatTitle = view.findViewById<TextView>(R.id.repeatTitle)
            if (answer.isEmpty()) {
                repeatTitle.visibility = View.GONE
            } else {
                repeatTitle.visibility = View.VISIBLE
            }
        }
    }
}
