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

package org.akvo.flow.presentation

import android.app.Activity
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.akvo.flow.R
import org.akvo.flow.activity.ToolBarTitleSubtitleMatcher.withToolbarTitle
import org.akvo.flow.activity.form.FormActivityTestUtil.addExecutionDelay
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf

abstract class ScreenRobot<T : ScreenRobot<T>> {

    private var activityContext: Activity? = null // Only required for some calls

    fun checkTitleIs(stringRes: Int) {
        checkTitleIs(activityContext!!.getString(stringRes))
    }

    fun checkTitleIs(text: String) {
        onView(withId(R.id.toolbar)).check(matches(withToolbarTitle(`is`<String>(text))))
    }

    fun clickOnViewWithId(@IdRes viewId: Int): T {
        onView(withId(viewId)).perform(click())
        return this as T
    }

    fun clickOnViewWithText(@StringRes viewId: Int): T {
        onView(withText(viewId)).perform(click())
        return this as T
    }

    fun provideActivityContext(activityContext: Activity): T {
        this.activityContext = activityContext
        return this as T
    }

    fun checkSnackBarDisplayedWithText(stringRes: Int): T {
        onView(
            allOf(
                withId(com.google.android.material.R.id.snackbar_text),
                withText(stringRes)
            )
        ).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        return this as T
    }

    fun checkActivityDisplayed(name: String): T {
        addExecutionDelay(100)
        intended(hasComponent(name))
        return this as T
    }

    companion object {

        fun <T : ScreenRobot<*>> withRobot(screenRobotClass: Class<T>?): T {
            if (screenRobotClass == null) {
                throw IllegalArgumentException("instance class == null")
            }

            try {
                return screenRobotClass.newInstance()
            } catch (iae: IllegalAccessException) {
                throw RuntimeException("IllegalAccessException", iae)
            } catch (ie: InstantiationException) {
                throw RuntimeException("InstantiationException", ie)
            }

        }
    }
}
