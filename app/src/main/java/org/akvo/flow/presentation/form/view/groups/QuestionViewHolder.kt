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
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import org.akvo.flow.R
import org.akvo.flow.presentation.form.view.groups.entity.ViewQuestionAnswer
import org.akvo.flow.util.image.GlideImageLoader
import org.akvo.flow.util.image.ImageLoader

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

    class NumberQuestionViewHolder(singleView: View) :
        QuestionViewHolder<ViewQuestionAnswer.NumberViewQuestionAnswer>(singleView) {
        override fun setUpView(
            questionAnswer: ViewQuestionAnswer.NumberViewQuestionAnswer,
            index: Int
        ) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            setUpAnswer(questionAnswer.answer)
            setUpRepetition(questionAnswer.requireDoubleEntry, questionAnswer.answer)
        }

        //TODO: repeated code from number/text... refactor
        private fun setUpRepetition(repeated: Boolean, answer: String) {
            val repeatedInput = view.findViewById<TextInputEditText>(
                R.id.questionResponseRepeated
            )
            val repeatTitle = view.findViewById<TextView>(
                R.id.repeatTitle
            )
            if (repeated) {
                setUpInputText(answer, repeatedInput)
                if (answer.isEmpty()) {
                    repeatTitle.visibility = View.GONE
                } else {
                    repeatTitle.visibility = View.VISIBLE
                }
            } else {
                repeatTitle.visibility = View.GONE
                repeatedInput.visibility = View.GONE
            }
        }

        private fun setUpAnswer(answer: String) {
            val textInputEditText = view.findViewById<TextInputEditText>(
                R.id.questionResponseInput
            )
            setUpInputText(answer, textInputEditText)
        }
    }

    class TextQuestionViewHolder(singleView: View) :
        QuestionViewHolder<ViewQuestionAnswer.FreeTextViewQuestionAnswer>(singleView) {

        override fun setUpView(
            questionAnswer: ViewQuestionAnswer.FreeTextViewQuestionAnswer,
            index: Int
        ) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            setUpAnswer(questionAnswer.answer)
            setUpRepetition(questionAnswer.requireDoubleEntry, questionAnswer.answer)
        }

        private fun setUpAnswer(answer: String) {
            val textInputEditText = view.findViewById<TextInputEditText>(
                R.id.questionResponseInput
            )
            setUpInputText(answer, textInputEditText)
        }

        private fun setUpRepetition(repeatable: Boolean, answer: String) {
            val repeatedInput = view.findViewById<TextInputEditText>(
                R.id.questionResponseRepeated
            )
            val repeatTitle = view.findViewById<TextView>(
                R.id.repeatTitle
            )
            if (repeatable) {
                setUpInputText(answer, repeatedInput)
                if (answer.isEmpty()) {
                    repeatTitle.visibility = View.GONE
                } else {
                    repeatTitle.visibility = View.VISIBLE
                }
            } else {
                repeatTitle.visibility = View.GONE
                repeatedInput.visibility = View.GONE
            }
        }
    }

    class PhotoQuestionViewHolder(mediaView: View) :
        QuestionViewHolder<ViewQuestionAnswer.PhotoViewQuestionAnswer>(mediaView) {

        private val mediaLayout: MediaQuestionLayout = view.findViewById(R.id.preview_container)

        override fun setUpView(
            questionAnswer: ViewQuestionAnswer.PhotoViewQuestionAnswer,
            index: Int
        ) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            if (questionAnswer.filePath.isNotEmpty()) {
                mediaLayout.visibility = View.VISIBLE
                mediaLayout.setUpImageDisplay(index, questionAnswer.filePath)
                setupLocation(questionAnswer)
            } else {
                mediaLayout.visibility = View.GONE
            }
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

        private val mediaLayout: MediaQuestionLayout = view.findViewById(R.id.preview_container)

        override fun setUpView(
            questionAnswer: ViewQuestionAnswer.VideoViewQuestionAnswer,
            index: Int
        ) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            if (questionAnswer.filePath.isNotEmpty()) {
                mediaLayout.visibility = View.VISIBLE
                mediaLayout.setUpImageDisplay(index, questionAnswer.filePath)
            } else {
                mediaLayout.visibility = View.GONE
            }
        }
    }

    class SignatureQuestionViewHolder(signView: View) :
        QuestionViewHolder<ViewQuestionAnswer.SignatureViewQuestionAnswer>(signView) {

        private val imageIv: ImageView = view.findViewById(R.id.signatureIv)
        private val nameTv: TextView = view.findViewById(R.id.signatureTv)
        private val questionLayout: LinearLayout = view.findViewById(R.id.questionContentLayout)
        private var imageLoader: ImageLoader = GlideImageLoader(view.context)

        override fun setUpView(
            questionAnswer: ViewQuestionAnswer.SignatureViewQuestionAnswer,
            index: Int
        ) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            if (questionAnswer.base64ImageString.isNotEmpty() && questionAnswer.name.isNotEmpty()) {
                questionLayout.visibility = View.VISIBLE
                imageIv.visibility = View.VISIBLE
                imageLoader.loadFromBase64String(questionAnswer.base64ImageString, imageIv)
                nameTv.text = questionAnswer.name
            } else {
                questionLayout.visibility = View.GONE
            }
        }
    }

    class DateQuestionViewHolder(dateView: View) :
        QuestionViewHolder<ViewQuestionAnswer.DateViewQuestionAnswer>(dateView) {
        override fun setUpView(
            questionAnswer: ViewQuestionAnswer.DateViewQuestionAnswer,
            index: Int
        ) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            val textInputEditText = view.findViewById<TextInputEditText>(
                R.id.questionResponseInput
            )
            setUpInputText(questionAnswer.answer, textInputEditText)
        }
    }

    class LocationQuestionViewHolder(locationView: View) :
        QuestionViewHolder<ViewQuestionAnswer.LocationViewQuestionAnswer>(locationView) {
        override fun setUpView(
            questionAnswer: ViewQuestionAnswer.LocationViewQuestionAnswer,
            index: Int
        ) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            if (questionAnswer.viewLocation.isValid()) {
                view.findViewById<TextInputEditText>(R.id.lat_et)
                    .setText(questionAnswer.viewLocation.latitude)
                view.findViewById<TextInputEditText>(R.id.lon_et)
                    .setText(questionAnswer.viewLocation.longitude)
                view.findViewById<TextInputEditText>(R.id.height_et)
                    .setText(questionAnswer.viewLocation.altitude)
                val accuracy = questionAnswer.viewLocation.accuracy
                if (accuracy.isNotEmpty()) {
                    view.findViewById<TextInputEditText>(R.id.height_et).setText(accuracy)
                } else {
                    view.findViewById<TextInputEditText>(R.id.height_et)
                        .setText(R.string.geo_location_accuracy_default)
                }
            }
        }
    }

    class ShapeQuestionViewHolder(geoshapeView: View) :
        QuestionViewHolder<ViewQuestionAnswer.GeoShapeViewQuestionAnswer>(geoshapeView) {

        private val viewShapeBt = view.findViewById<Button>(R.id.view_shape_btn)
        private val shapeTv = view.findViewById<TextView>(R.id.geo_shape_captured_text)

        override fun setUpView(
            questionAnswer: ViewQuestionAnswer.GeoShapeViewQuestionAnswer,
            index: Int
        ) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            if (questionAnswer.geojsonAnswer.isNotEmpty()) {
                viewShapeBt.visibility = View.VISIBLE
                shapeTv.visibility = View.VISIBLE
                viewShapeBt.setOnClickListener {
                    val activityContext = view.context
                    if (activityContext is GeoShapeListener) {
                        activityContext.viewShape(questionAnswer.geojsonAnswer)
                    } else {
                        throw  throw IllegalArgumentException("Activity must implement GeoShapeListener")
                    }
                }
            } else {
                viewShapeBt.visibility = View.GONE
                shapeTv.visibility = View.GONE
            }
        }
    }

    class OptionsViewHolder(optionsView: View) :
        QuestionViewHolder<ViewQuestionAnswer.OptionViewQuestionAnswer>(optionsView) {
        override fun setUpView(
            questionAnswer: ViewQuestionAnswer.OptionViewQuestionAnswer,
            index: Int
        ) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            val optionsQuestionLayout = view.findViewById<OptionsQuestionLayout>(R.id.options_layout)
            if (questionAnswer.options.isNotEmpty()){
                optionsQuestionLayout.visibility = View.VISIBLE
                optionsQuestionLayout.setUpViews(questionAnswer)
            } else {
                optionsQuestionLayout.visibility = View.GONE
            }
        }
    }

    class BarcodeViewHolder(barcodeView: View) :
        QuestionViewHolder<ViewQuestionAnswer.BarcodeViewQuestionAnswer>(barcodeView) {
        override fun setUpView(
            questionAnswer: ViewQuestionAnswer.BarcodeViewQuestionAnswer,
            index: Int
        ) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            val barcodesQuestionLayout =
                view.findViewById<BarcodesQuestionLayout>(R.id.barcodes_layout)
            if (questionAnswer.answers.isNotEmpty()) {
                barcodesQuestionLayout.visibility = View.VISIBLE
                barcodesQuestionLayout.setUpViews(questionAnswer)
            } else {
                barcodesQuestionLayout.visibility = View.GONE
            }
        }
    }

    class CascadeViewHolder(barcodeView: View) :
        QuestionViewHolder<ViewQuestionAnswer.CascadeViewQuestionAnswer>(barcodeView) {
        override fun setUpView(
            questionAnswer: ViewQuestionAnswer.CascadeViewQuestionAnswer,
            index: Int
        ) {
            setUpTitle(questionAnswer.title, questionAnswer.mandatory)
            val barcodesQuestionLayout =
                view.findViewById<CascadeQuestionLayout>(R.id.barcodes_layout)
            if (questionAnswer.answers.isNotEmpty()) {
                barcodesQuestionLayout.visibility = View.VISIBLE
                barcodesQuestionLayout.setUpViews(questionAnswer)
            } else {
                barcodesQuestionLayout.visibility = View.GONE
            }
        }
    }
}