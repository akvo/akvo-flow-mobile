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
import org.akvo.flow.presentation.form.view.groups.entity.ViewQuestionAnswer
import org.akvo.flow.presentation.form.view.groups.entity.ViewQuestionAnswer.NumberViewQuestionAnswer
import org.akvo.flow.presentation.form.view.groups.entity.exhaustive

class GroupQuestionsAdapter<T: QuestionViewHolder<ViewQuestionAnswer>>(val questionAnswers: MutableList<ViewQuestionAnswer> = mutableListOf()) :
    RecyclerView.Adapter<T>() {

    enum class ViewType {
        SINGLE_INPUT,
        DOUBLE_INPUT,
        OPTION, //options have subtypes,
        CASCADE,
        LOCATION,
        PHOTO,
        VIDEO,
        SHAPE,
        SIGNATURE,
        CADDISFLY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T {
        return when (viewType) {
            ViewType.SINGLE_INPUT.ordinal -> {
                QuestionViewHolder.SingleQuestionViewHolder(
                    inflate(
                        parent,
                        R.layout.single_input_field_question_view
                    )
                ) as T
            }
            ViewType.DOUBLE_INPUT.ordinal -> {
                QuestionViewHolder.DoubleQuestionViewHolder(
                    inflate(
                        parent,
                        R.layout.double_input_field_question_view
                    )
                ) as T
            }
            ViewType.PHOTO.ordinal -> {
                QuestionViewHolder.PhotoQuestionViewHolder(
                    inflate(
                        parent,
                        R.layout.media_question_view
                    )
                ) as T
            }
            ViewType.VIDEO.ordinal -> {
                QuestionViewHolder.VideoQuestionViewHolder(
                    inflate(
                        parent,
                        R.layout.media_question_view
                    )
                ) as T
            }
            else -> {
                QuestionViewHolder.PhotoQuestionViewHolder(
                    inflate(
                        parent,
                        R.layout.single_input_field_question_view
                    )
                ) as T
            }
        }
    }

    private fun inflate(parent: ViewGroup, @LayoutRes layoutResId: Int) =
        LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)

    override fun getItemCount(): Int = questionAnswers.size


    override fun onBindViewHolder(holder: T, position: Int) {
        holder.setUpView(questionAnswers[position])
    }

    override fun getItemViewType(position: Int): Int {
        val questionAnswer = questionAnswers[position]
        return when (questionAnswer) {
            is ViewQuestionAnswer.FreeTextViewQuestionAnswer -> {
                checkRepeatableType(questionAnswer.requireDoubleEntry)
            }
            is NumberViewQuestionAnswer -> {
                checkRepeatableType(questionAnswer.requireDoubleEntry)
            }
            is ViewQuestionAnswer.OptionViewQuestionAnswer -> TODO()
            is ViewQuestionAnswer.CascadeViewQuestionAnswer -> TODO()
            is ViewQuestionAnswer.LocationViewQuestionAnswer -> TODO()
            is ViewQuestionAnswer.PhotoViewQuestionAnswer -> {
                ViewType.PHOTO.ordinal
            }
            is ViewQuestionAnswer.VideoViewQuestionAnswer -> {
                ViewType.VIDEO.ordinal
            }
            is ViewQuestionAnswer.DateViewQuestionAnswer -> {
                ViewType.SINGLE_INPUT.ordinal
            }
            is ViewQuestionAnswer.BarcodeViewQuestionAnswer -> {
                //TODO: barcode can be multiple
                ViewType.SINGLE_INPUT.ordinal
            }
            is ViewQuestionAnswer.GeoShapeViewQuestionAnswer -> TODO()
            is ViewQuestionAnswer.SignatureViewQuestionAnswer -> TODO()
            is ViewQuestionAnswer.CaddisflyViewQuestionAnswer -> TODO()
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

sealed class QuestionViewHolder<T: ViewQuestionAnswer>(val view: View) : RecyclerView.ViewHolder(view) {

    abstract fun setUpView(questionAnswer: T)

    private fun mandatoryText(mandatory: Boolean): String {
        return when {
            mandatory -> {
                " *"
            }
            else -> {
                ""
            }
        }
    }

    fun setUpTitle(title: String, mandatory: Boolean) {
        view.findViewById<TextView>(R.id.questionTitle).text = "$title${mandatoryText(mandatory)}"
    }

    fun setUpInputText(answer: String, textInputEditText: TextInputEditText) {
        if (answer.isEmpty()) {
            textInputEditText.visibility = View.GONE
        } else {
            textInputEditText.visibility = View.VISIBLE
            textInputEditText.setText(answer)
        }
    }

    class SingleQuestionViewHolder(singleView: View) : QuestionViewHolder<ViewQuestionAnswer>(singleView) {
        override fun setUpView(questionAnswer: ViewQuestionAnswer) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            val textInputEditText = view.findViewById<TextInputEditText>(R.id.questionResponseInput)
            val answer = "" //TODO: fix
            setUpInputText(answer, textInputEditText)
        }
    }

    class DoubleQuestionViewHolder(singleView: View) : QuestionViewHolder<ViewQuestionAnswer>(singleView) {
        override fun setUpView(questionAnswer: ViewQuestionAnswer) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            val textInputEditText = view.findViewById<TextInputEditText>(R.id.questionResponseInput)
            val answer = "" //TODO: fix
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

    class PhotoQuestionViewHolder(mediaView: View) :
        QuestionViewHolder<ViewQuestionAnswer.PhotoViewQuestionAnswer>(mediaView) {
        override fun setUpView(questionAnswer: ViewQuestionAnswer.PhotoViewQuestionAnswer) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
        }
    }

    class VideoQuestionViewHolder(mediaView: View) :
        QuestionViewHolder<ViewQuestionAnswer.VideoViewQuestionAnswer>(mediaView) {
        override fun setUpView(questionAnswer: ViewQuestionAnswer.VideoViewQuestionAnswer) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
        }
    }
}
