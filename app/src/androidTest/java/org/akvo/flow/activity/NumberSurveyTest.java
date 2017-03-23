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
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.matcher.RootMatchers;
import android.support.test.rule.ActivityTestRule;

import org.akvo.flow.R;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.testhelper.SurveyInstaller;
import org.akvo.flow.testhelper.SurveyRequisite;
import org.akvo.flow.util.FileUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.tests.R.raw.numbersurvey;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;


public class NumberSurveyTest {

    @Rule
    public ActivityTestRule<SurveyActivity> rule = new ActivityTestRule<>(SurveyActivity.class);
    private SurveyInstaller installer;

    @Before
    public void init()
    {
        Context context = rule.getActivity();
        installer       = new SurveyInstaller(context, new SurveyDbAdapter(context));
    }

    @BeforeClass
    public static void setRequisite() {
        SurveyRequisite.setRequisites(InstrumentationRegistry.getTargetContext());
    }

    @After
    public void clearSurveys() {
        installer.clearSurveys();
    }

    @AfterClass
    public static void tearDown() {
        SurveyRequisite.resetRequisites(InstrumentationRegistry.getTargetContext());
    }

    @Test
    public void canFillNumberQuestion() throws IOException {
        fillNumberQuestion(numbersurvey, 50, 50);
        onView(withId(R.id.next_btn)).perform(click());
        onView(allOf(withClassName(endsWith("Button")), withText(R.string.submitbutton))).check(matches((isEnabled())));
    }

    @Test
    public void canNotifyWrongDoubleInputNumberQuestion() throws IOException {
        fillNumberQuestion(numbersurvey, 50, 60);
        //Ensure Popup shows the "Answers do not match" text
        onView(withText(R.string.error_answer_match)).inRoot(RootMatchers.isPlatformPopup()).check(matches(isDisplayed()));
        onView(withId(R.id.next_btn)).perform(click());
        //Ensure the button object with text "Submit" is greyed out and not enabled
        onView(allOf(withClassName(endsWith("Button")), withText(R.string.submitbutton))).check(matches(not(isEnabled())));
    }

    @Test
    public void canNotifyWrongMaxInputNumberQuestion() throws IOException {
        Survey survey = fillNumberQuestion(numbersurvey, 0, 2000);
        //Gets the maxValue for the first question
        int maxValue = survey.getQuestionGroups().get(0).getQuestions().get(0).getValidationRule().getMaxVal().intValue();
        String tooLargeError = rule.getActivity().getApplicationContext().getResources().getString(R.string.toolargeerr);

        onView(withText(tooLargeError + maxValue)).inRoot(RootMatchers.isPlatformPopup()).check(matches(isDisplayed()));
        onView(withId(R.id.next_btn)).perform(click());
        onView(allOf(withClassName(endsWith("Button")), withText(R.string.submitbutton))).check(matches(not(isEnabled())));
    }

    private Survey fillNumberQuestion(int surveyResId, int firstValue, int secondValue) throws IOException {
        Survey survey = getSurvey(surveyResId);

        openDrawer();
        onView(withText(survey.getName())).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.new_datapoint)).perform(click());
        onView(withId(R.id.input_et)).perform(typeText(String.valueOf(firstValue)));
        onView(withId(R.id.double_entry_et)).perform(typeText(String.valueOf(secondValue)));

        return survey;
    }

    private Survey getSurvey(int resId) throws IOException {
        InputStream input = InstrumentationRegistry.getContext().getResources().openRawResource(resId);
        return installer.persistSurvey(FileUtil.readText(input));
    }

    private void openDrawer() {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    }
}
