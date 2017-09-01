/*
 *  Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.activity;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.akvo.flow.activity.testhelper.SurveyInstaller;
import org.akvo.flow.activity.testhelper.SurveyRequisite;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.akvo.flow.activity.Constants.TEST_FORM_SURVEY_INSTANCE_ID;
import static org.akvo.flow.activity.FormActivityTestUtil.clickNext;
import static org.akvo.flow.activity.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.FormActivityTestUtil.matchToolbarTitle;
import static org.akvo.flow.activity.FormActivityTestUtil.verifyQuestionTitleDisplayed;
import static org.akvo.flow.activity.FormActivityTestUtil.verifySubmitButtonDisabled;
import static org.akvo.flow.activity.FormActivityTestUtil.verifySubmitButtonEnabled;
import static org.akvo.flow.tests.R.raw.optionsurvey;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class OptionsFormActivityTest {

    private static final String FORM_TITLE = "OptionsQuestionForm";
    private static SurveyInstaller installer;

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            return getFormActivityIntent(42573002L, "43623002", FORM_TITLE);
        }
    };

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new SurveyInstaller(targetContext);
        installer.installSurvey(optionsurvey, InstrumentationRegistry.getContext());
    }

    @After
    public void afterEachTest() {
        installer.deleteResponses(TEST_FORM_SURVEY_INSTANCE_ID);
    }

    @AfterClass
    public static void afterClass() {
        SurveyRequisite.resetRequisites(InstrumentationRegistry.getTargetContext());
        installer.clearSurveys();
    }

    @Test
    public void canFillOptionsQuestion() throws Exception {
        verifyQuestionTitleDisplayed();
        matchToolbarTitle(FORM_TITLE);
        fillOptionsQuestion(0);
        clickNext();
        verifySubmitButtonEnabled();
    }

    private void fillOptionsQuestion(int option) {
        onView(withId(option)).check(matches(isDisplayed())).perform(click());
        onView(withId(option)).check(matches(isDisplayed())).check(matches(isChecked()));
    }

    @Test
    public void cannotSubmitIfNoOptionSelected() throws Exception {
        matchToolbarTitle(FORM_TITLE);
        verifyQuestionTitleDisplayed();
        clickNext();
        verifySubmitButtonDisabled();
    }
}
