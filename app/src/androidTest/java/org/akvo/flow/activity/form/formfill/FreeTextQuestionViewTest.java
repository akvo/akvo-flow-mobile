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

package org.akvo.flow.activity.form.formfill;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.SurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.akvo.flow.activity.form.FormActivityTestUtil.clickNext;
import static org.akvo.flow.activity.form.FormActivityTestUtil.fillFreeTextQuestion;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyQuestionTitleDisplayed;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonDisabled;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonEnabled;
import static org.akvo.flow.tests.R.raw.freetext_form;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class FreeTextQuestionViewTest {

    private static SurveyInstaller installer;

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            return getFormActivityIntent(44173002L, "47313002", "FreeTextForm", 0L, false);
        }
    };

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new SurveyInstaller(targetContext);
        installer.installSurvey(freetext_form, InstrumentationRegistry.getContext());
    }

    @After
    public void afterEachTest() {
        installer.deleteResponses();
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
}
