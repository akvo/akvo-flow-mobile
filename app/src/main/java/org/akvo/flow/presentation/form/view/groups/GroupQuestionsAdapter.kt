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

class GroupQuestionsAdapter<T : QuestionViewHolder<ViewQuestionAnswer>>(private val questionAnswers: MutableList<ViewQuestionAnswer> = mutableListOf()) :
    RecyclerView.Adapter<T>() {

    enum class ViewType {
        TEXT,
        NUMBER,
        OPTION, //options have subtypes,
        CASCADE,
        LOCATION,
        DATE,
        PHOTO,
        VIDEO,
        SHAPE,
        SIGNATURE,
        CADDISFLY,
        BARCODE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T {
        return when (viewType) {
            ViewType.NUMBER.ordinal -> {
                QuestionViewHolder.NumberQuestionViewHolder(
                    inflate(
                        parent,
                        R.layout.text_input_field_question_view
                    )
                ) as T
            }
            ViewType.TEXT.ordinal -> {
                QuestionViewHolder.TextQuestionViewHolder(
                    inflate(
                        parent,
                        R.layout.text_input_field_question_view
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
            ViewType.DATE.ordinal -> {
                QuestionViewHolder.DateQuestionViewHolder(
                    inflate(
                        parent,
                        R.layout.date_input_field_question_view
                    )
                ) as T
            }
            ViewType.LOCATION.ordinal -> {
                QuestionViewHolder.LocationQuestionViewHolder(
                    inflate(
                        parent,
                        R.layout.location_question_view
                    )
                ) as T
            }
            ViewType.SHAPE.ordinal -> {
                QuestionViewHolder.ShapeQuestionViewHolder(
                    inflate(
                        parent,
                        R.layout.shape_question_view
                    )
                ) as T
            }
            ViewType.OPTION.ordinal -> {
                QuestionViewHolder.OptionsViewHolder(
                    inflate(
                        parent,
                        R.layout.options_question_view
                    )
                ) as T
            }
            ViewType.BARCODE.ordinal -> {
                QuestionViewHolder.BarcodeViewHolder(
                    inflate(
                        parent,
                        R.layout.barcodes_question_view
                    )
                ) as T
            }
            else -> {
                QuestionViewHolder.PhotoQuestionViewHolder(
                    inflate(
                        parent,
                        R.layout.media_question_view
                    )
                ) as T
            }
        }.exhaustive
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
                ViewType.TEXT.ordinal
            }
            is NumberViewQuestionAnswer -> {
                ViewType.NUMBER.ordinal
            }
            is ViewQuestionAnswer.OptionViewQuestionAnswer -> {
                    ViewType.OPTION.ordinal
            }
            is ViewQuestionAnswer.CascadeViewQuestionAnswer -> TODO()
            is ViewQuestionAnswer.LocationViewQuestionAnswer -> {
                ViewType.LOCATION.ordinal
            }
            is ViewQuestionAnswer.PhotoViewQuestionAnswer -> {
                ViewType.PHOTO.ordinal
            }
            is ViewQuestionAnswer.VideoViewQuestionAnswer -> {
                ViewType.VIDEO.ordinal
            }
            is ViewQuestionAnswer.DateViewQuestionAnswer -> {
                ViewType.DATE.ordinal
            }
            is ViewQuestionAnswer.BarcodeViewQuestionAnswer -> {
                ViewType.BARCODE.ordinal
            }
            is ViewQuestionAnswer.GeoShapeViewQuestionAnswer -> {
                ViewType.SHAPE.ordinal
            }
            is ViewQuestionAnswer.SignatureViewQuestionAnswer -> {
                ViewType.SIGNATURE.ordinal
            }
            is ViewQuestionAnswer.CaddisflyViewQuestionAnswer -> TODO()
        }.exhaustive
    }

    fun showDownLoadSuccess(viewIndex: Int) {
        notifyItemChanged(viewIndex)
    }

    fun showDownLoadFailed(viewIndex: Int) {
        notifyItemChanged(viewIndex)
    }
}
