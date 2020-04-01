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

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import org.akvo.flow.presentation.form.view.groups.entity.ViewQuestionAnswer

class CascadeQuestionLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    fun setUpViews(questionAnswer: ViewQuestionAnswer.CascadeViewQuestionAnswer) {
        for (cascadeLevel in questionAnswer.answers) {
            val textView = TextView(context)
            textView.text = cascadeLevel.level
            textView.setTypeface(null, Typeface.BOLD)
            textView.textSize = 18.0f
            addView(textView)
            val textInputEditText = TextInputEditText(context)
            textInputEditText.isEnabled = false
            textInputEditText.setText(cascadeLevel.answer)
            addView(textInputEditText)
        }
    }
}
