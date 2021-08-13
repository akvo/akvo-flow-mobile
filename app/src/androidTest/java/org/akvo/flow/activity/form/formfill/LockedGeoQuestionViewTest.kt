/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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
 *
 */
package org.akvo.flow.activity.form.formfill

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isFocusable
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.RequiresDevice
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.akvo.flow.R
import org.akvo.flow.activity.FormActivity
import org.akvo.flow.activity.form.FormActivityTestUtil.addExecutionDelay
import org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent
import org.akvo.flow.activity.form.FormActivityTestUtil.getString
import org.akvo.flow.activity.form.data.SurveyInstaller
import org.akvo.flow.activity.form.data.SurveyRequisite
import org.akvo.flow.event.TimedLocationListener
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.core.AllOf.allOf
import org.hamcrest.core.IsNot.not
import org.junit.AfterClass
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DecimalFormat

@LargeTest
@RunWith(AndroidJUnit4::class)
class LockedGeoQuestionViewTest {
    private val accuracyFormat = DecimalFormat("#")
    private var isTestLab: Boolean = false

    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_PHONE_STATE,
    )

    @get:Rule
    var activityTestRule: ActivityTestRule<FormActivity> = object : ActivityTestRule<FormActivity>(
        FormActivity::class.java
    ) {
        override fun getActivityIntent(): Intent {

            val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
            SurveyRequisite.setRequisites(targetContext)
            val installer = SurveyInstaller(targetContext)
            installer.installSurvey(
                raw.locked_geo_form,
                InstrumentationRegistry.getInstrumentation().context
            )
            return getFormActivityIntent(
                155852019L,
                "156792019",
                "LockedGeoForm",
                123L
            )
        }
    }
    @Before
    fun setUp() {
        val testLabSetting: String? = Settings.System.getString(
            InstrumentationRegistry.getInstrumentation()
                .targetContext.contentResolver, "firebase.test.lab")
        isTestLab = "true" == testLabSetting
    }

    /**
     * Does not work on emulator from firebase as location is provided too fast before progress can
     * be shown
     */
    @RequiresDevice
    @Test
    fun ensureGeoQuestionProgressDisplayedOnButtonClick() {
        if (!isTestLab) {
            //reset values just in case
            resetFields()
            onView(withId(R.id.acc_tv))
                .perform(
                    replaceTextInTextView(
                        getString(
                            R.string.geo_location_accuracy_default,
                            activityTestRule
                        )
                    )
                )
            closeSoftKeyboard()
            clickGeoButton()
            verifyProgressDisplayed()
        }
    }

    @RequiresDevice
    @Test
    fun ensureSnackBarRetryShowsProgress() {
        if (!isTestLab) {
            resetFields()
            clickGeoButton()
            simulateLocationTimeout()
            clickSnackBarRetry()
            verifyProgressDisplayed()
        }
    }

    @Test
    fun ensureErrorSnackBarDisplayedUponTimeout() {
        resetFields()
        closeSoftKeyboard()
        clickGeoButton()
        simulateLocationTimeout()
        verifyErrorSnackBarDisplayed()
    }

    @Test
    fun ensureLocationValuesDisplayedCorrectly() {
        //reset values just in case
        resetFields()
        clickGeoButton()
        simulateLocationReceived(MOCK_ACCURACY_ACCURATE)

        verifyGeoInput(R.id.lat_et, MOCK_LATITUDE.toString())
        verifyGeoInput(R.id.lon_et, MOCK_LONGITUDE.toString())
        verifyGeoInput(R.id.height_et, MOCK_ALTITUDE.toString())
        verifyAccuracy(accuracyFormat.format(MOCK_ACCURACY_ACCURATE.toDouble()), Color.GREEN)
    }

    @Test
    fun ensureLocationValuesDisplayedCorrectlyIfInaccurate() {
        resetFields()
        clickGeoButton()
        simulateLocationReceived(MOCK_ACCURACY_INACCURATE)
        addExecutionDelay(100)
        verifyAccuracy(accuracyFormat.format(MOCK_ACCURACY_INACCURATE.toDouble()), Color.RED)
    }

    @Test
    fun ensureDialogShownWhenFieldsNotEmpty() {
        onView(withId(R.id.lat_et)).perform(replaceText(MOCK_LATITUDE.toString()))
        onView(withId(R.id.lon_et)).perform(replaceText(MOCK_LONGITUDE.toString()))
        onView(withId(R.id.height_et)).perform(replaceText(MOCK_ALTITUDE.toString()))
        closeSoftKeyboard()
        onView(withId(R.id.acc_tv)).perform(
            replaceTextInTextView(
                getString(
                    R.string.geo_location_accuracy, activityTestRule,
                    accuracyFormat.format(MOCK_ACCURACY_ACCURATE.toDouble())
                )
            )
        )

        clickGeoButton()
        onView(withId(android.R.id.button1)).check(matches(isDisplayed()))
    }

    @RequiresDevice
    @Test
    fun ensureLocationValuesDisplayedCorrectlyWhenCancelled() {
        if (!isTestLab) {
            resetFields()
            closeSoftKeyboard()

            clickGeoButton()
            addExecutionDelay(100)
            clickCancelButton()

            onView(withId(R.id.lat_et)).check(matches(withText("")))
            onView(withId(R.id.lon_et)).check(matches(withText("")))
            onView(withId(R.id.height_et)).check(matches(withText("")))
            onView(withId(R.id.acc_tv)).check(matches(withText(R.string.geo_location_accuracy_default)))
        }
    }

    private fun resetFields() {
        onView(withId(R.id.lat_et)).perform(replaceText(""))
        onView(withId(R.id.lon_et)).perform(replaceText(""))
        onView(withId(R.id.height_et)).perform(replaceText(""))
    }

    private fun verifyAccuracy(accuracy: String, textColor: Int) {
        val input = onView(withId(R.id.acc_tv))
        input.check(matches(isDisplayed()))
        input.check(
            matches(
                withText(
                    getString(
                        R.string.geo_location_accuracy,
                        activityTestRule,
                        accuracy
                    )
                )
            )
        )
        input.check(matches(hasTextColor(textColor)))
    }

    private fun simulateLocationReceived(accuracy: Float) {
        val geoQuestion =
            activityTestRule.activity.findViewById<View>(R.id.geo_question_view)
        geoQuestion.post {
            (geoQuestion as TimedLocationListener.Listener).onLocationReady(
                MOCK_LATITUDE, MOCK_LONGITUDE, MOCK_ALTITUDE, accuracy
            )
        }
    }

    private fun verifyGeoInput(resId: Int, text: String) {
        val input = onView(withId(resId))
        input.check(matches(isDisplayed()))
        input.check(matches(withText(text)))
        input.check(matches(not(isFocusable())))
    }

    private fun clickGeoButton() {
        onView(withId(R.id.geo_btn))
            .check(matches(isCompletelyDisplayed()))
            .perform(click())
    }

    private fun clickCancelButton() {
        onView(allOf(withId(R.id.geo_btn), withText(R.string.cancelbutton)))
            .check(matches(isCompletelyDisplayed()))
            .perform(click())
    }

    private fun verifyProgressDisplayed() {
        val progress = onView(withId(R.id.auto_geo_location_progress))
        progress.check(matches(isDisplayed()))
    }

    private fun verifyErrorSnackBarDisplayed() {
        onView(
            allOf(
                withId(com.google.android.material.R.id.snackbar_text),
                withText(R.string.location_timeout)
            )
        ).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

    private fun simulateLocationTimeout() {
        val geoQuestion =
            activityTestRule.activity.findViewById<View>(R.id.geo_question_view)
        geoQuestion.post { (geoQuestion as TimedLocationListener.Listener).onTimeout() }
    }

    private fun clickSnackBarRetry() {
        onView(withId(com.google.android.material.R.id.snackbar_action))
            .check(matches(allOf(isEnabled(), isClickable())))
            .perform(object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return isEnabled()
                }

                override fun getDescription(): String {
                    return "click SnackBar Retry Button"
                }

                override fun perform(uiController: UiController, view: View) {
                    view.performClick()
                }
            })
    }

    private fun hasTextColor(color: Int): Matcher<View> {
        return object : BoundedMatcher<View, TextView>(TextView::class.java) {
            public override fun matchesSafely(warning: TextView): Boolean {
                return color == warning.currentTextColor
            }

            override fun describeTo(description: Description) {
                description.appendText("with text color: ")
            }
        }
    }

    private fun replaceTextInTextView(value: String): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return allOf(isDisplayed(), isAssignableFrom(TextView::class.java))
            }

            override fun perform(uiController: UiController, view: View) {
                (view as TextView).text = value
            }

            override fun getDescription(): String {
                return "replace text"
            }
        }
    }

    companion object {
        private const val MOCK_LATITUDE = 10.0
        private const val MOCK_LONGITUDE = 20.0
        private const val MOCK_ALTITUDE = 1.0
        private const val MOCK_ACCURACY_ACCURATE = 4.0f
        private const val MOCK_ACCURACY_INACCURATE = 101.0f

        @AfterClass
        fun afterClass() {
            val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
            SurveyRequisite.resetRequisites(targetContext)
            val installer = SurveyInstaller(targetContext)
            installer.clearSurveys()
        }
    }
}
