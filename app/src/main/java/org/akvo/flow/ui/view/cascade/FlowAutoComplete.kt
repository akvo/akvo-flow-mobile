/*
 * Copyright (C) 2021 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.ui.view.cascade

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnFocusChangeListener
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import org.akvo.flow.R
import org.akvo.flow.domain.Node
import org.akvo.flow.ui.view.cascade.CascadeQuestionView.POSITION_NONE

class FlowAutoComplete @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.autoCompleteTextViewStyle
) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {

    override fun enoughToFilter(): Boolean {
        return text.length >= 0
    }

    fun updateAutoComplete(
        position: Int,
        values: List<Node>,
        selection: Int,
        readOnly: Boolean
    ) {
        tag = position // Tag the textView with its position within the container
        isEnabled = !readOnly
        isFocusable = !readOnly
        isClickable = !readOnly

        if (selection != POSITION_NONE) {
            setText(values[selection].name)
        }
        if (!readOnly) {
            val adapter = CascadeAdapter(context, values)
            setAdapter(adapter)
            setOnClickListener { showDropDown() }
            onFocusChangeListener =
                OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                    if (hasFocus) {
                        showDropDown()
                    }
                }
            validator = object : Validator {
                override fun isValid(text: CharSequence): Boolean {
                    return adapter.getItem(text.toString()) != null
                }

                override fun fixText(invalidText: CharSequence): CharSequence {
                    return ""
                }
            }
        }
    }

    fun getSelectedItem(): String? {
        return (adapter as CascadeAdapter).getItem(text.toString())?.name
    }
}
