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
package org.akvo.flow.presentation.survey

import android.content.Context
import android.graphics.Canvas
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import org.akvo.flow.R

class CustomDrawerArrowDrawable(context: Context) : DrawerArrowDrawable(context) {

    private val customDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_new_forms_icon)

    var isEnabled = false

    override fun draw(canvas: Canvas) {
        if (isEnabled) {
            customDrawable?.let {
                customDrawable.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
                customDrawable.draw(canvas)
            }
        } else {
            super.draw(canvas)
        }
    }

}
