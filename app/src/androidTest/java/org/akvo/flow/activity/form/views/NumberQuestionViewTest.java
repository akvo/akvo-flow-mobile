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

package org.akvo.flow.activity.form.views;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.SurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.ValidationRule;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasErrorText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.activity.Constants.TEST_FORM_SURVEY_INSTANCE_ID;
import static org.akvo.flow.activity.form.FormActivityTestUtil.clickNext;
import static org.akvo.flow.activity.form.FormActivityTestUtil.fillFreeTextQuestion;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonDisabled;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonEnabled;
import static org.akvo.flow.tests.R.raw.numbersurvey;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class NumberQuestionViewTest {

    private static SurveyInstaller installer;
    private static Survey survey;

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            return getFormActivityIntent(47323002L, "49783002", "NumberForm");
        }
    };

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new SurveyInstaller(targetContext);
        survey = installer.installSurvey(numbersurvey, InstrumentationRegistry.getContext());
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
    public void canFillNumberQuestion() throws IOException {
        fillNumberQuestion(50);
        clickNext();
        verifySubmitButtonEnabled();
    }

    @NonNull
    private String getString(@StringRes int stringResId) {
        return rule.getActivity().getApplicationContext().getResources()
                .getString(stringResId);
    }

    @Test
    public void canNotifyWrongMaxInputNumberQuestion() throws Exception {
        fillNumberQuestion(2000);

        verifyNumberTooLargeErrorShown();
        clickNext();
        verifySubmitButtonDisabled();
    }

    @Test
    public void canNotifyWrongMinInputNumberQuestion() throws Exception {
        fillNumberQuestion(0);

        verifyNumberTooSmallErrorShown();
        clickNext();
        verifySubmitButtonDisabled();
    }

    @Test
    public void cannotEnterText() throws Exception {
        fillFreeTextQuestion("This is an answer to your question");

        onView(withId(R.id.input_et)).check(matches(withText("")));
    }

    @Test
    public void cannotEnterSigned() throws Exception {
        fillFreeTextQuestion("-1");

        onView(withId(R.id.input_et)).check(matches(withText("1")));
    }

    @Test
    public void cannotEnterDecimal() throws Exception {
        fillFreeTextQuestion("1.1");

        onView(withId(R.id.input_et)).check(matches(withText("11")));
    }


    private void verifyNumberTooLargeErrorShown() {
        int maxValue = getValidationRule().getMaxVal().intValue();
        String tooLargeError = getString(R.string.toolargeerr);
        onView(withId(R.id.input_et)).check(matches(hasErrorText(tooLargeError + maxValue)));
    }

    private void verifyNumberTooSmallErrorShown() {
        int minValue = getValidationRule().getMinVal().intValue();
        String tooSmallError = getString(R.string.toosmallerr);
        onView(withId(R.id.input_et)).check(matches(hasErrorText(tooSmallError + minValue)));
    }

    private ValidationRule getValidationRule() {
        return survey.getQuestionGroups().get(0).getQuestions().get(0).getValidationRule();
    }

    private void fillNumberQuestion(int firstValue) throws IOException {
        onView(withId(R.id.input_et)).perform(typeText(String.valueOf(firstValue)));
        Espresso.closeSoftKeyboard();
    }
}
