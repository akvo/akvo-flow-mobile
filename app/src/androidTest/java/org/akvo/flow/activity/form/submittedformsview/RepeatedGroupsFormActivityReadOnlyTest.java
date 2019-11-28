/*
 * Copyright (C) 2017,2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.activity.form.submittedformsview;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.SurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.util.ConstantUtil;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.activity.MultiItemByPositionMatcher.getElementFromMatchAtPosition;
import static org.akvo.flow.activity.form.FormActivityTestUtil.addExecutionDelay;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.tests.R.raw.repeated_groups_form;
import static org.hamcrest.core.AllOf.allOf;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class RepeatedGroupsFormActivityReadOnlyTest {

    private static final String SURVEY_TITLE = "RepeatedBarcodeGroup";

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule
            .grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Rule
    public GrantPermissionRule permissionRule2 = GrantPermissionRule
            .grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Rule
    public GrantPermissionRule permissionRule3 = GrantPermissionRule
            .grant(Manifest.permission.READ_PHONE_STATE);

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
            SurveyRequisite.setRequisites(targetContext);
            SurveyInstaller installer = new SurveyInstaller(targetContext);
            Survey survey = installer
                    .installSurvey(repeated_groups_form, InstrumentationRegistry.getInstrumentation().getContext());
            long id = installer
                    .createDataPoint(survey.getSurveyGroup(), generateTestResponseData()).first;
            return getFormActivityIntent(207569117L, "200389118", SURVEY_TITLE, id, true);
        }
    };

    @AfterClass
    public static void afterClass() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SurveyRequisite.resetRequisites(targetContext);
        SurveyInstaller installer = new SurveyInstaller(targetContext);
        installer.clearSurveys();
    }

    @Test
    public void verifyOneRepetition() {
        clickOnTabNamed("RepeatedBarcodeGroup");
        onView(allOf(withId(R.id.barcode_input), isDisplayed())).check(matches(withText("123456")));
        verifyRepeatHeaderText("Repetition: 1");
    }

    private void verifyRepeatHeaderText(String text) {
        onView(allOf(withId(R.id.repeat_header), isDisplayed()))
                .check(matches(withText(text)));
    }

    @Test
    public void verifyThreeRepetitions() {
        clickOnTabNamed("RepeatedTextGroup");
        verifyRepeatHeaderText("Repetitions: 3");

        verifyQuestionIteration(0, "test1");
        verifyQuestionIteration(1, "test2");
        verifyQuestionIteration(2, "test3");
    }

    private void clickOnTabNamed(String tabText) {
        onView(withText(tabText)).perform(click());
        addExecutionDelay(800);
    }

    private void verifyQuestionIteration(int position, String textToVerify) {
        onView(allOf(getElementFromMatchAtPosition(withId(R.id.input_et), position),
                isDisplayed())).check(matches(withText(textToVerify)));
    }

    @NonNull
    private QuestionResponse.QuestionResponseBuilder[] generateTestResponseData() {
        return new QuestionResponse.QuestionResponseBuilder[] {
                new QuestionResponse.QuestionResponseBuilder()
                        .setValue("123456")
                        .setType(ConstantUtil.VALUE_RESPONSE_TYPE)
                        .setQuestionId("205929117")
                        .setIteration(0), new QuestionResponse.QuestionResponseBuilder()
                .setValue("test1")
                .setType(ConstantUtil.VALUE_RESPONSE_TYPE)
                .setQuestionId("205929118")
                .setIteration(0), new QuestionResponse.QuestionResponseBuilder()
                .setValue("test2")
                .setType(ConstantUtil.VALUE_RESPONSE_TYPE)
                .setQuestionId("205929118")
                .setIteration(1),
                new QuestionResponse.QuestionResponseBuilder()
                        .setValue("test3")
                        .setType(ConstantUtil.VALUE_RESPONSE_TYPE)
                        .setQuestionId("205929118")
                        .setIteration(2)
        };
    }
}
