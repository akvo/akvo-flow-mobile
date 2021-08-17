/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.activity.form.formfill

import android.Manifest
import android.content.Intent
import android.graphics.Point
import android.os.SystemClock.uptimeMillis
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.IntDef
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.akvo.flow.R
import org.akvo.flow.activity.FormActivity
import org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent
import org.akvo.flow.activity.form.FormActivityTestUtil.verifyQuestionIteration
import org.akvo.flow.activity.form.FormActivityTestUtil.verifyRepeatHeaderText
import org.akvo.flow.activity.form.data.TestSurveyInstaller
import org.akvo.flow.activity.form.data.TestSurveyInstaller.generateRepeatedOneGroupResponseData
import org.akvo.flow.activity.form.data.SurveyRequisite
import org.akvo.flow.tests.R.raw.repeated_one_group_form
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.junit.AfterClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RepeatedGroupsFormActivityTest {

    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_PHONE_STATE
    )

    @get:Rule
    var rule: ActivityTestRule<FormActivity> = object : ActivityTestRule<FormActivity>(
        FormActivity::class.java
    ) {
        override fun getActivityIntent(): Intent {
            val targetContext = getInstrumentation().targetContext
            SurveyRequisite.setRequisites(targetContext)
            val installer = TestSurveyInstaller(
                targetContext)
            val survey =
                installer.installSurvey(repeated_one_group_form, getInstrumentation().context)
            val id =
                installer.createDataPoint(
                    survey.second,
                    *generateRepeatedOneGroupResponseData()
                ).first!!
            return getFormActivityIntent(207569117L, "200389118", SURVEY_TITLE, id)
        }
    }

    companion object {

        private const val SURVEY_TITLE = "RepeatedGroup"

        @AfterClass
        fun afterClass() {
            val targetContext = getInstrumentation().targetContext
            SurveyRequisite.resetRequisites(targetContext)
            val installer = TestSurveyInstaller(
                targetContext)
            installer.clearSurveys()
        }
    }

    @Test
    fun verifyRepetitionDelete_IfConfirmed() {
        verifyRepeatHeaderText("Repetitions: 3")
        verifyQuestionIteration(0, "test1")
        verifyQuestionIteration(1, "test2")
        verifyQuestionIteration(2, "test3")

        onView(withText("RepeatedTextGroup - 2")).perform(ClickDrawableAction(ClickDrawableAction.RIGHT))
        onView(allOf(withId(android.R.id.button1), withText("OK"))).inRoot(isDialog())
            .perform(click())

        onView(withText("test2")).check(doesNotExist())
    }

    @Test
    fun verifyRepetitionNotDeleted_IfCancelled() {
        verifyRepeatHeaderText("Repetitions: 3")
        verifyQuestionIteration(0, "test1")
        verifyQuestionIteration(1, "test2")
        verifyQuestionIteration(2, "test3")

        onView(withText("RepeatedTextGroup - 2")).perform(ClickDrawableAction(ClickDrawableAction.RIGHT))
        onView(allOf(withId(android.R.id.button2), withText("CANCEL"))).inRoot(isDialog())
            .perform(click())

        verifyQuestionIteration(1, "test2")
    }

    @Test
    fun verifyRepeatHeader_IfRepeatPressed() {
        verifyRepeatHeaderText("Repetitions: 3")
        verifyQuestionIteration(0, "test1")
        verifyQuestionIteration(1, "test2")
        verifyQuestionIteration(2, "test3")

        onView(withId(R.id.repeat_btn)).perform(scrollTo()).check(matches(isDisplayed()))
            .perform(click())
        verifyRepeatHeaderText("Repetitions: 4")
    }

    class ClickDrawableAction(@param:Location @field:Location private val drawableLocation: Int) :
        ViewAction {
        override fun getConstraints(): Matcher<View> {
            return allOf(isAssignableFrom(TextView::class.java),
                object :
                    BoundedMatcher<View, TextView>(TextView::class.java) {
                    override fun matchesSafely(tv: TextView): Boolean {
                        val requestFocusFromTouch = tv.requestFocusFromTouch()
                        val drawable = tv.compoundDrawables[drawableLocation]
                        return requestFocusFromTouch && drawable != null
                    }

                    override fun describeTo(description: org.hamcrest.Description?) {
                        description!!.appendText("has drawable")
                    }
                })
        }

        override fun getDescription(): String {
            return "click drawable "
        }

        override fun perform(uiController: UiController, view: View) {
            val tv = view as TextView
            if (tv.requestFocusFromTouch()) {
                val drawableBounds = tv.compoundDrawables[drawableLocation].bounds
                val clickPoint = arrayOfNulls<Point>(SIZE_CLICK_POINT)
                clickPoint[LEFT] = Point(
                    tv.left + drawableBounds.width() / HALF_DIVISOR,
                    (tv.pivotY + drawableBounds.height() / HALF_DIVISOR).toInt()
                )
                clickPoint[TOP] = Point(
                    (tv.pivotX + drawableBounds.width() / HALF_DIVISOR).toInt(),
                    tv.top + drawableBounds.height() / HALF_DIVISOR
                )
                clickPoint[RIGHT] = Point(
                    tv.right + drawableBounds.width() / HALF_DIVISOR,
                    (tv.pivotY + drawableBounds.height() / HALF_DIVISOR).toInt()
                )
                clickPoint[BOTTOM] = Point(
                    (tv.pivotX + drawableBounds.width() / HALF_DIVISOR).toInt(),
                    tv.bottom + drawableBounds.height() / HALF_DIVISOR
                )
                clickPoint[drawableLocation]?.let { point ->
                    if (dispatchTextViewTouchEvent(tv, point, MotionEvent.ACTION_DOWN)) {
                        dispatchTextViewTouchEvent(tv, point, MotionEvent.ACTION_UP)
                    }
                }

            }
        }

        private fun dispatchTextViewTouchEvent(tv: TextView, point: Point, event: Int): Boolean {
            return tv.dispatchTouchEvent(
                MotionEvent.obtain(
                    uptimeMillis(),
                    uptimeMillis(),
                    event,
                    point.x.toFloat(),
                    point.y.toFloat(),
                    0
                )
            )
        }

        @IntDef(LEFT, TOP, RIGHT, BOTTOM)
        @Retention(AnnotationRetention.SOURCE)
        annotation class Location

        companion object {
            const val LEFT = 0
            const val TOP = 1
            const val RIGHT = 2
            const val BOTTOM = 3
            const val SIZE_CLICK_POINT = 4
            const val HALF_DIVISOR = 2
        }
    }
}
