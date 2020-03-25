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

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import org.akvo.flow.R
import org.akvo.flow.presentation.form.view.groups.entity.ViewQuestionAnswer
import org.akvo.flow.util.image.GlideImageLoader
import org.akvo.flow.util.image.ImageLoader
import org.akvo.flow.util.image.DrawableLoadListener
import timber.log.Timber
import java.io.File

sealed class QuestionViewHolder<T : ViewQuestionAnswer>(val view: View) :
    RecyclerView.ViewHolder(view) {

    abstract fun setUpView(questionAnswer: T, index: Int)

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

    class SingleQuestionViewHolder(singleView: View) :
        QuestionViewHolder<ViewQuestionAnswer>(singleView) {
        override fun setUpView(questionAnswer: ViewQuestionAnswer, index: Int) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            val textInputEditText = view.findViewById<TextInputEditText>(
                R.id.questionResponseInput
            )
            val answer = "" //TODO: fix
            setUpInputText(answer, textInputEditText)
        }
    }

    class DoubleQuestionViewHolder(singleView: View) :
        QuestionViewHolder<ViewQuestionAnswer>(singleView) {
        override fun setUpView(questionAnswer: ViewQuestionAnswer, index: Int) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            val textInputEditText = view.findViewById<TextInputEditText>(
                R.id.questionResponseInput
            )
            val answer = "" //TODO: fix
            setUpInputText(answer, textInputEditText)
            val repeatedInput = view.findViewById<TextInputEditText>(
                R.id.questionResponseRepeated
            )
            setUpInputText(answer, repeatedInput)
            val repeatTitle = view.findViewById<TextView>(
                R.id.repeatTitle
            )
            if (answer.isEmpty()) {
                repeatTitle.visibility = View.GONE
            } else {
                repeatTitle.visibility = View.VISIBLE
            }
        }
    }

    class PhotoQuestionViewHolder(mediaView: View) :
        QuestionViewHolder<ViewQuestionAnswer.PhotoViewQuestionAnswer>(mediaView) {

        private val mediaLayout: MediaQuestionViewLayout = view.findViewById(R.id.preview_container)

        override fun setUpView(
            questionAnswer: ViewQuestionAnswer.PhotoViewQuestionAnswer,
            index: Int
        ) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            mediaLayout.setUpImageDisplay(index, questionAnswer.filePath)
            setupLocation(questionAnswer)
        }

        private fun setupLocation(questionAnswer: ViewQuestionAnswer.PhotoViewQuestionAnswer) {
            val locationTv = view.findViewById(R.id.location_info) as TextView
            val location = questionAnswer.location
            if (location != null) {
                val locationText: String = view.context
                    .getString(
                        R.string.image_location_coordinates,
                        location.latitude,
                        location.longitude
                    )
                locationTv.text = locationText
                locationTv.visibility = View.VISIBLE
            } else {
                locationTv.visibility = View.GONE
            }
        }
    }

    class VideoQuestionViewHolder(mediaView: View) :
        QuestionViewHolder<ViewQuestionAnswer.VideoViewQuestionAnswer>(mediaView) {

        private val mediaLayout: MediaQuestionViewLayout = view.findViewById(R.id.preview_container)

        override fun setUpView(
            questionAnswer: ViewQuestionAnswer.VideoViewQuestionAnswer,
            index: Int
        ) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            mediaLayout.setUpImageDisplay(index, questionAnswer.filePath)
        }
    }
}