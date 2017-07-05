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
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.util.ConstantUtil;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;

public class FormActivityTestUtil {

    @NonNull
    static Intent getFormActivityIntent(long surveyGroupId, String formId, String formTitle) {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent result = new Intent(targetContext, FormActivity.class);
        result.putExtra(ConstantUtil.FORM_ID_EXTRA, formId);
        result.putExtra(ConstantUtil.RESPONDENT_ID_EXTRA, 0L);
        result.putExtra(ConstantUtil.SURVEY_GROUP_EXTRA,
                new SurveyGroup(surveyGroupId, formTitle, null, false));
        result.putExtra(ConstantUtil.SURVEYED_LOCALE_ID_EXTRA,
                Constants.TEST_FORM_SURVEY_INSTANCE_ID);
        return result;
    }

    static void verifySubmitButtonEnabled() {
        onView(allOf(withClassName(endsWith("Button")), withText(R.string.submitbutton)))
                .check(matches(isEnabled()));
    }

    static void verifySubmitButtonDisabled() {
        onView(allOf(withClassName(endsWith("Button")), withText(R.string.submitbutton)))
                .check(matches(not(isEnabled())));
    }

    static ViewInteraction matchToolbarTitle(String title) {
        return onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.toolbar)),
                withText(title)))
                .check(matches(isDisplayed()));
    }

    static void verifyQuestionTitleDisplayed() {
        onView(withId(R.id.question_tv)).check(matches(isDisplayed()));
    }

    static void clickNext() {
        onView(withId(R.id.next_btn)).perform(click());
    }
}