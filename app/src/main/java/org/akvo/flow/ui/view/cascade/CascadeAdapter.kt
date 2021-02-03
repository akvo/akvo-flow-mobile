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
import android.widget.ArrayAdapter
import org.akvo.flow.R
import org.akvo.flow.domain.Node

internal class CascadeAdapter(context: Context?, nodes: List<Node>?) : ArrayAdapter<Node?>(
    context!!, R.layout.cascade_spinner_item, R.id.cascade_spinner_item_text, nodes!!
) {
    private val nodes: List<Node>?

    init {
        setDropDownViewResource(R.layout.cascade_spinner_item)
        this.nodes = nodes
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
