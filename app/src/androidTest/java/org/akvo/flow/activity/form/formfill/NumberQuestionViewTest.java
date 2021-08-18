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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.activity.form.FormActivityTestUtil.addExecutionDelay;
import static org.akvo.flow.activity.form.FormActivityTestUtil.clickNext;
import static org.akvo.flow.activity.form.FormActivityTestUtil.fillFreeTextQuestion;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.hasErrorText;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonDisabled;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonEnabled;
import static org.akvo.flow.tests.R.raw.number_form;
import static org.hamcrest.CoreMatchers.is;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.akvo.flow.activity.form.data.TestSurveyInstaller;
import org.akvo.flow.utils.entity.Form;
import org.akvo.flow.utils.entity.Question;
import org.akvo.flow.utils.entity.ValidationRule;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class NumberQuestionViewTest {

    private static TestSurveyInstaller installer;
    private static Form survey;

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            return getFormActivityIntent(47323002L, "49783002", "NumberForm", 0L);
        }
    };

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new TestSurveyInstaller(targetContext);
        survey = installer.installSurvey(number_form, InstrumentationRegistry.getInstrumentation().getContext()).first;
        Question question = survey.getGroups().get(0).getQuestions().get(0);
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
    public void ensureCanFillNumberQuestion() throws IOException {
        addExecutionDelay(300);
        fillNumberQuestion(50);
        clickNext();
        verifySubmitButtonEnabled();
    }


    @Test
    public void ensureCanNotifyWrongMaxInputNumberQuestion() throws Exception {
        addExecutionDelay(300);
        fillNumberQuestion(2000);

        verifyNumberTooLargeErrorShown();
        clickNext();
        verifySubmitButtonDisabled();
    }

    @Test
    public void ensureCanNotifyWrongMinInputNumberQuestion() throws Exception {
        addExecutionDelay(300);
        fillNumberQuestion(0);

        verifyNumberTooSmallErrorShown();
        clickNext();
        verifySubmitButtonDisabled();
    }

    @Test
    public void ensureCannotEnterText() {
        addExecutionDelay(300);
        fillFreeTextQuestion("This is an answer to your question");

        onView(withId(R.id.input_et)).check(matches(withText("")));
    }

    @Test
    public void ensureCannotEnterSigned() {
        addExecutionDelay(300);
        fillFreeTextQuestion("-1");

        onView(withId(R.id.input_et)).check(matches(withText("1")));
    }

    @Test
    public void ensureCannotEnterDecimal() {
        addExecutionDelay(300);
        fillFreeTextQuestion("1.1");

        onView(withId(R.id.input_et)).check(matches(withText("11")));
    }

    @NonNull
    private String getString(@StringRes int stringResId) {
        return rule.getActivity().getApplicationContext().getResources()
                .getString(stringResId);
    }

    private void verifyNumberTooLargeErrorShown() {
        int maxValue = getValidationRule().getMaxVal().intValue();
        String tooLargeError = getString(R.string.toolargeerr);
        onView(withId(R.id.input_et)).check(matches(hasErrorText(is(tooLargeError + maxValue))));
    }

    private void verifyNumberTooSmallErrorShown() {
        int minValue = getValidationRule().getMinVal().intValue();
        String tooSmallError = getString(R.string.toosmallerr);
        onView(withId(R.id.input_et)).check(matches(hasErrorText(is(tooSmallError + minValue))));
    }

    private ValidationRule getValidationRule() {
        return survey.getGroups().get(0).getQuestions().get(0).getValidationRule();
    }

    private void fillNumberQuestion(int firstValue) {
        onView(withId(R.id.input_et)).perform(typeText(String.valueOf(firstValue)));
        Espresso.closeSoftKeyboard();
    }
}
