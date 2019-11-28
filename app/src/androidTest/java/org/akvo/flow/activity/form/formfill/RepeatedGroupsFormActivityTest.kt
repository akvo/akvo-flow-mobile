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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.akvo.flow.R
import org.akvo.flow.activity.FormActivity
import org.akvo.flow.activity.MultiItemByPositionMatcher.getElementFromMatchAtPosition
import org.akvo.flow.activity.form.FormActivityTestUtil.addExecutionDelay
import org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent
import org.akvo.flow.activity.form.data.SurveyInstaller
import org.akvo.flow.activity.form.data.SurveyRequisite
import org.akvo.flow.domain.QuestionResponse.QuestionResponseBuilder
import org.akvo.flow.tests.R.raw
import org.akvo.flow.util.ConstantUtil
import org.hamcrest.CoreMatchers.allOf
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
            val installer = SurveyInstaller(targetContext)
            val survey =
                installer.installSurvey(raw.repeated_groups_form, getInstrumentation().context)
            val id =
                installer.createDataPoint(survey.surveyGroup, *generateTestResponseData()).first!!
            return getFormActivityIntent(207569117L, "200389118", SURVEY_TITLE, id, false)
        }
    }

    @AfterClass
    fun afterClass() {
        val targetContext = getInstrumentation().targetContext
        SurveyRequisite.resetRequisites(targetContext)
        val installer = SurveyInstaller(targetContext)
        installer.clearSurveys()
    }

    @Test
    fun verifyOneRepetition() {
        clickOnTabNamed("RepeatedBarcodeGroup")
        onView(allOf(withId(R.id.barcode_input), isDisplayed())).check(matches(withText("123456")))
        verifyRepeatHeaderText("Repetition: 1")
    }

    private fun verifyRepeatHeaderText(text: String) {
        onView(allOf(withId(R.id.repeat_header), isDisplayed())).check(matches(withText(text)))
    }

    @Test
    fun verifyThreeRepetitions() {
        clickOnTabNamed("RepeatedTextGroup")
        verifyRepeatHeaderText("Repetitions: 3")
        verifyQuestionIteration(0, "test1")
        verifyQuestionIteration(1, "test2")
        verifyQuestionIteration(2, "test3")
    }

    private fun clickOnTabNamed(tabText: String) {
        onView(withText(tabText)).perform(click())
        addExecutionDelay(800)
    }

    private fun verifyQuestionIteration(position: Int, textToVerify: String) {
        onView(
            allOf(
                getElementFromMatchAtPosition(withId(R.id.input_et), position),
                isDisplayed()
            )
        ).check(matches(withText(textToVerify)))
    }

    private fun generateTestResponseData(): Array<QuestionResponseBuilder> {
        return arrayOf(
            QuestionResponseBuilder()
                .setValue("123456")
                .setType(ConstantUtil.VALUE_RESPONSE_TYPE)
                .setQuestionId("205929117")
                .setIteration(0), QuestionResponseBuilder()
                .setValue("test1")
                .setType(ConstantUtil.VALUE_RESPONSE_TYPE)
                .setQuestionId("205929118")
                .setIteration(0), QuestionResponseBuilder()
                .setValue("test2")
                .setType(ConstantUtil.VALUE_RESPONSE_TYPE)
                .setQuestionId("205929118")
                .setIteration(1),
            QuestionResponseBuilder()
                .setValue("test3")
                .setType(ConstantUtil.VALUE_RESPONSE_TYPE)
                .setQuestionId("205929118")
                .setIteration(2)
        )
    }

    companion object {
        private const val SURVEY_TITLE = "RepeatedBarcodeGroup"
    }
}