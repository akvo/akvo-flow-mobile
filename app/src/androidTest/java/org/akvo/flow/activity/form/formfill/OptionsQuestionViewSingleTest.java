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

package org.akvo.flow.activity.form.formfill;

import android.content.Context;
import android.content.Intent;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.SurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static org.akvo.flow.activity.form.FormActivityTestUtil.clickNext;
import static org.akvo.flow.activity.form.FormActivityTestUtil.fillSingleOptionsQuestion;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getSingleChoiceRadioButton;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyQuestionTitleDisplayed;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonDisabled;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonEnabled;
import static org.akvo.flow.tests.R.raw.option_form;
import static org.hamcrest.core.IsNot.not;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class OptionsQuestionViewSingleTest {

    private static final String FORM_TITLE = "OptionsQuestionForm";
    private static SurveyInstaller installer;

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            return getFormActivityIntent(42573002L, "43623002", FORM_TITLE, 0L);
        }
    };

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new SurveyInstaller(targetContext);
        installer.installSurvey(option_form, InstrumentationRegistry.getInstrumentation().getContext());
    }

    @After
    public void afterEachTest() {
        installer.deleteResponses();
    }

    @AfterClass
    public static void afterClass() {
        SurveyRequisite.resetRequisites(InstrumentationRegistry.getInstrumentation().getTargetContext());
        installer.clearSurveys();
    }

    @Test
    public void ensureCanFillOptionsQuestion() {
        verifyQuestionTitleDisplayed();

        fillSingleOptionsQuestion(0);

        verifyOptionSelected();
        verifyOtherOptionUnselected();

        clickNext();

        verifySubmitButtonEnabled();
    }

    @Test
    public void ensureCannotSubmitIfNoOptionSelected() {
        verifyQuestionTitleDisplayed();

        clickNext();

        verifySubmitButtonDisabled();
    }

    private void verifyOptionSelected() {
        ViewInteraction singleChoiceOption = getSingleChoiceRadioButton(0);
        singleChoiceOption.check(matches(isChecked()));
    }

    private void verifyOtherOptionUnselected() {
        ViewInteraction singleChoiceOption = getSingleChoiceRadioButton(1);
        singleChoiceOption.check(matches(not(isChecked())));
    }
}
