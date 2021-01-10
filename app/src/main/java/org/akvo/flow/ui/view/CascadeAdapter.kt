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
package org.akvo.flow.ui.view

import android.content.Context
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.akvo.flow.R
import org.akvo.flow.domain.Node
import timber.log.Timber

internal class CascadeAdapter(context: Context?, nodes: List<Node>?) : ArrayAdapter<Node?>(
    context!!, R.layout.cascade_spinner_item, R.id.cascade_spinner_item_text, nodes!!
) {
    private val nodes: List<Node>?

    init {
        setDropDownViewResource(R.layout.cascade_spinner_item)
        this.nodes = nodes
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        setStyle(view, position)
        return view
    }

    override fun getDropDownView(
        position: Int, convertView: View,
        parent: ViewGroup
    ): View {
        val view = super.getDropDownView(position, convertView, parent)
        setStyle(view, position)
        return view
    }

    private fun setStyle(view: View, position: Int) {
        try {
            val text = view.findViewById<TextView>(R.id.cascade_spinner_item_text)
            var flags = text.paintFlags
            flags = if (position == 0) {
                flags or Paint.FAKE_BOLD_TEXT_FLAG
            } else {
                flags and Paint.FAKE_BOLD_TEXT_FLAG.inv()
            }
            text.paintFlags = flags
        } catch (e: ClassCastException) {
            Timber.e("View cannot be casted to TextView!")
        }
    }

    fun getItem(itemText: String?): Node? {
        if (itemText == null || nodes == null) {
            return null
        }
        for (n in nodes) {
            if (itemText == n.name) {
                return n
            }
        }
        return null
    }

}