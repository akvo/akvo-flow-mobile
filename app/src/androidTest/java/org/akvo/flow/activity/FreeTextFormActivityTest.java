/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.activity;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.akvo.flow.R;
import org.akvo.flow.activity.testhelper.SurveyInstaller;
import org.akvo.flow.activity.testhelper.SurveyRequisite;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.akvo.flow.activity.Constants.TEST_FORM_SURVEY_INSTANCE_ID;
import static org.akvo.flow.activity.FormActivityTestUtil.clickNext;
import static org.akvo.flow.activity.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.FormActivityTestUtil.verifyQuestionTitleDisplayed;
import static org.akvo.flow.activity.FormActivityTestUtil.verifySubmitButtonDisabled;
import static org.akvo.flow.activity.FormActivityTestUtil.verifySubmitButtonEnabled;
import static org.akvo.flow.tests.R.raw.freetextsurvey;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class FreeTextFormActivityTest {

    private static SurveyInstaller installer;

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            return getFormActivityIntent(44173002L, "47313002", "FreeTextForm");
        }
    };

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new SurveyInstaller(targetContext);
        installer.installSurvey(freetextsurvey, InstrumentationRegistry.getContext());
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
    public void canFillFreeTextQuestion() throws Exception {
        verifyQuestionTitleDisplayed();
        fillFreeTextQuestion("This is an answer to your question");
        clickNext();
        verifySubmitButtonEnabled();
    }

    @Test
    public void ensureCantSubmitEmptyFreeText() throws Exception {
        verifyQuestionTitleDisplayed();
        fillFreeTextQuestion("");
        clickNext();
        verifySubmitButtonDisabled();
    }

    private void fillFreeTextQuestion(String text) throws IOException {
        onView(withId(R.id.input_et)).perform(typeText(text));
        Espresso.closeSoftKeyboard();
    }
}
