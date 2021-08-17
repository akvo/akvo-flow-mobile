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

import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.TestSurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.Espresso;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.akvo.flow.activity.form.FormActivityTestUtil.clickNext;
import static org.akvo.flow.activity.form.FormActivityTestUtil.fillFreeTextQuestion;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.hasErrorText;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyQuestionTitleDisplayed;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonDisabled;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonEnabled;
import static org.akvo.flow.tests.R.raw.freetext_double_entry_form;
import static org.hamcrest.Matchers.is;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class FreeTextDoubleQuestionViewTest {

    private static final String FORM_TITLE = "FreeTextForm";
    private static TestSurveyInstaller installer;

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            return getFormActivityIntent(44173002L, "47313002", FORM_TITLE, 0L);
        }
    };

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new TestSurveyInstaller(targetContext);
        installer.installSurvey(freetext_double_entry_form, InstrumentationRegistry.getInstrumentation().getContext());
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
    public void ensureCannotSubmitIfSecondEntryMissing() {
        verifyQuestionTitleDisplayed();
        fillFreeTextQuestion("This is an answer to your question");
        clickNext();
        verifySubmitButtonDisabled();
    }

    @Test
    public void ensureCannotSubmitEmptyFreeText() {
        verifyQuestionTitleDisplayed();
        fillFreeTextQuestion("");
        clickNext();
        verifySubmitButtonDisabled();
    }

    @Test
    public void ensureCannotSubmitDifferentAnswers() {
        verifyQuestionTitleDisplayed();

        fillFreeTextQuestion("This is an answer to your question");
        fillDoubleEntry("Something else");

        verifyDoubleEntryMisMatchErrorDisplayed();

        clickNext();
        verifySubmitButtonDisabled();
    }

    @Test
    public void ensureCanSubmitCorrectQuestion() {
        verifyQuestionTitleDisplayed();

        fillFreeTextQuestion("This is an answer to your question");
        fillDoubleEntry("This is an answer to your question");

        clickNext();
        verifySubmitButtonEnabled();
    }

    private void verifyDoubleEntryMisMatchErrorDisplayed() {
        String expectedError = getString(R.string.error_answer_match);
        onView(withId(R.id.double_entry_et))
                .check(matches(hasErrorText(is(expectedError))));
    }

    private void fillDoubleEntry(String text) {
        onView(withId(R.id.double_entry_et)).perform(typeText(text));
        Espresso.closeSoftKeyboard();
    }

    @NonNull
    private String getString(@StringRes int stringResId) {
        return rule.getActivity().getApplicationContext().getResources()
                .getString(stringResId);
    }
}
