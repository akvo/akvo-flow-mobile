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
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import org.akvo.flow.R
import org.akvo.flow.presentation.form.view.groups.entity.ViewQuestionAnswer
import org.akvo.flow.presentation.form.view.groups.entity.ViewQuestionAnswer.NumberViewQuestionAnswer
import org.akvo.flow.presentation.form.view.groups.entity.exhaustive

class GroupQuestionsAdapter<T : QuestionViewHolder<ViewQuestionAnswer>>(val questionAnswers: MutableList<ViewQuestionAnswer> = mutableListOf()) :
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
            ViewType.SIGNATURE.ordinal -> {
                QuestionViewHolder.SignatureQuestionViewHolder(
                    inflate(
                        parent,
                        R.layout.signature_field_question_view
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
        holder.setUpView(questionAnswers[position], position)
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
            is ViewQuestionAnswer.SignatureViewQuestionAnswer -> {
                ViewType.SIGNATURE.ordinal
            }
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

    fun showDownLoadSuccess(viewIndex: Int) {
        notifyItemChanged(viewIndex)
    }

    fun showDownLoadFailed(viewIndex: Int) {
        notifyItemChanged(viewIndex)
    }
}
