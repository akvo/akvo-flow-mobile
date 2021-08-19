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
package org.akvo.flow.activity.form.formsview

import android.Manifest
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.akvo.flow.R
import org.akvo.flow.activity.FormActivity
import org.akvo.flow.activity.form.FormActivityTestUtil.addExecutionDelay
import org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent
import org.akvo.flow.activity.form.FormActivityTestUtil.verifyRepeatHeaderText
import org.akvo.flow.activity.form.FormActivityTestUtil.withQuestionViewParent
import org.akvo.flow.activity.form.data.TestSurveyInstaller
import org.akvo.flow.activity.form.data.TestSurveyInstaller.generatePartialRepeatedGroupResponseData
import org.akvo.flow.activity.form.data.SurveyRequisite
import org.akvo.flow.tests.R.raw.repeated_one_group_form_2questions
import org.akvo.flow.ui.view.FreetextQuestionView
import org.akvo.flow.ui.view.barcode.BarcodeQuestionViewSingle
import org.hamcrest.CoreMatchers.allOf
import org.junit.AfterClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RepeatedGroupFormViewTest {

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
            val installer = TestSurveyInstaller(targetContext)
            val result =
                installer.installSurvey(
                    repeated_one_group_form_2questions,
                    getInstrumentation().context
                )
            val id =
                installer.createDataPoint(
                    result.second,
                    *generatePartialRepeatedGroupResponseData()
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
            val installer = TestSurveyInstaller(targetContext)
            installer.clearSurveys()
        }
    }

    @Test
    fun verifyRepetitionsDisplayedCorrectly() {
        verifyRepeatHeaderText("Repetitions: 2")
        verifyFirstIterationDisplayedInFull()
        addExecutionDelay(10000)
        verifySecondIterationPartiallyDisplayed()
    }

    private fun verifyFirstIterationDisplayedInFull() {
        onView(
            allOf(
                withId(R.id.input_et),
                withQuestionViewParent(FreetextQuestionView::class.java, "205929118|0"),
                withText("text-response-rep-one")
            )
        ).check(
            matches(isDisplayed())
        )

        onView(
            allOf(
                withId(R.id.barcode_input),
                withQuestionViewParent(BarcodeQuestionViewSingle::class.java, "205929119|0"),
                withText("1234567")
            )
        ).check(
            matches(isDisplayed())
        )
    }

    private fun verifySecondIterationPartiallyDisplayed() {
        onView(
            allOf(
                withId(R.id.input_et),
                withQuestionViewParent(FreetextQuestionView::class.java, "205929118|1"),
                withText("text-response-rep-two")
            )
        ).check(
            matches(isDisplayed())
        )

        onView(
            allOf(
                withId(R.id.barcode_input),
                withQuestionViewParent(BarcodeQuestionViewSingle::class.java, "205929119|1"),
                withText("")
            )
        ).check(
            matches(isEnabled())
        )
    }
}
