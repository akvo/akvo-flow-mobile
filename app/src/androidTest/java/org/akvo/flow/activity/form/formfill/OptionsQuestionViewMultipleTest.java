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
import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.CheckBox;

import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.SurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.activity.form.FormActivityTestUtil.clickNext;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyQuestionTitleDisplayed;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonDisabled;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonEnabled;
import static org.akvo.flow.tests.R.raw.option_multiple_other_form;
import static org.hamcrest.Matchers.allOf;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class OptionsQuestionViewMultipleTest {

    private static final String FORM_TITLE = "OptionsQuestionForm";
    private static SurveyInstaller installer;

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            return getFormActivityIntent(42573002L, "43623002", FORM_TITLE, 0L, false);
        }
    };

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new SurveyInstaller(targetContext);
        installer.installSurvey(option_multiple_other_form, InstrumentationRegistry.getContext());
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
    public void ensureCanFillOneOptionsQuestion() throws Exception {
        verifyQuestionTitleDisplayed();

        fillOptionsQuestion(0);

        verifyOptionSelected(0);

        clickNext();

        verifySubmitButtonEnabled();
    }

    @Test
    public void ensureCanFillMultipleOptionsQuestion() throws Exception {
        verifyQuestionTitleDisplayed();

        fillOptionsQuestion(0);
        fillOptionsQuestion(1);

        verifyOptionSelected(0);
        verifyOptionSelected(1);

        clickNext();

        verifySubmitButtonEnabled();
    }

    @Test
    public void ensureCanFillOtherOptionsQuestion() throws Exception {
        verifyQuestionTitleDisplayed();

        fillOptionsQuestion(2);
        fillOtherValue("other option");

        verifyOtherOption(2, "other option");

        clickNext();

        verifySubmitButtonEnabled();
    }

    private void fillOtherValue(String text) {
        onView(withId(R.id.other_option_input)).perform(typeText(text));
        onView(withId(android.R.id.button1)).perform(click());
    }

    private void verifyOtherOption(int option, String text) {
        ViewInteraction otherOption = getCheckbox(option);
        otherOption.check(matches(isChecked()));
        onView(withId(R.id.other_option_text))
                .check(matches(allOf(isDisplayed(), withText(text))));
    }

    @Test
    public void ensureCannotSubmitIfNoOptionSelected() throws Exception {
        verifyQuestionTitleDisplayed();

        clickNext();

        verifySubmitButtonDisabled();
    }

    private void verifyOptionSelected(int option) {
        ViewInteraction singleChoiceOption = getCheckbox(option);
        singleChoiceOption.check(matches(isChecked()));
    }

    private void fillOptionsQuestion(int option) {
        ViewInteraction checkbox = getCheckbox(option);
        checkbox.perform(click());
    }

    private ViewInteraction getCheckbox(int option) {
        return onView(allOf(withId(option), IsInstanceOf.<View>instanceOf(CheckBox.class)));
    }
}
