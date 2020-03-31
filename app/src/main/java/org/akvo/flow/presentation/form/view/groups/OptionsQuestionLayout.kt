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
import android.util.AttributeSet
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import org.akvo.flow.R
import org.akvo.flow.presentation.form.view.groups.entity.ViewOption
import org.akvo.flow.presentation.form.view.groups.entity.ViewQuestionAnswer

class OptionsQuestionLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    fun setUpViews(questionAnswer: ViewQuestionAnswer.OptionViewQuestionAnswer) {
        val selected = questionAnswer.selected
        if (questionAnswer.allowMultiple) {
            // checkboxes
            for ((i, option) in questionAnswer.availableOptions.withIndex()) {
                val view = newCheckbox(option, i)
                addView(view)
                view.isEnabled = false
                view.id = i // View ID will match option position within the array
                view.isChecked = i in selected

            }
        } else {
            //radio button
            val radioGroup = RadioGroup(context)
            addView(radioGroup)
            for ((i, option) in questionAnswer.availableOptions.withIndex()) {
                val view = newRadioButton(option, i)
                radioGroup.addView(view)
                view.isEnabled = false
                view.id = i // View ID will match option position within the array
            }
            radioGroup.check(selected[0]) // only one can be selected
        }
    }

    private fun newRadioButton(option: ViewOption, i: Int): RadioButton {
        val rb = RadioButton(context)
        val params = RadioGroup.LayoutParams(
            RadioGroup.LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        if (i != 0) {
            params.topMargin = context.resources.getDimension(R.dimen.small_padding).toInt()
        }
        rb.layoutParams = params
        rb.text = option.name
        return rb
    }

    private fun newCheckbox(option: ViewOption, i: Int): CheckBox {
        val box = CheckBox(context)
        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        if (i != 0) {
            params.topMargin = context.resources.getDimension(R.dimen.small_padding).toInt()
        }
        box.layoutParams = params
        box.text = option.name
        return box
    }
}