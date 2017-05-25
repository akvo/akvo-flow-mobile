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
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.akvo.flow.R;
import org.akvo.flow.data.database.SurveyDbAdapter;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.activity.testhelper.SurveyInstaller;
import org.akvo.flow.activity.testhelper.SurveyRequisite;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.tests.R.raw.optionsurvey;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class OptionsSurveyTest {

    private static Survey survey;
    private static SurveyInstaller installer;

    @Rule
    public ActivityTestRule<SurveyActivity> rule = new ActivityTestRule<>(SurveyActivity.class);

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new SurveyInstaller(new SurveyDbAdapter(targetContext));
        survey = installer.installSurvey(optionsurvey, InstrumentationRegistry.getContext());
    }

    @Before
    public void beforeEachTest() {
        newDataPoint();
    }

    private void newDataPoint() {
        openDrawer();
        selectSurvey();
        clickAddDataPoint();
    }

    private void clickAddDataPoint() {
        onView(withId(R.id.new_datapoint)).perform(click());
    }

    private void selectSurvey() {
        onView(withText(survey.getName())).check(matches(isDisplayed())).perform(click());
    }

    private void openDrawer() {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    }

    @AfterClass
    public static void afterClass() {
        SurveyRequisite.resetRequisites(InstrumentationRegistry.getTargetContext());
        installer.clearSurveys();
    }

    @Test
    public void canFillOptionsQuestion() throws Exception {
        fillOptionsQuestion(0);

        verifySubmitButtonEnabled();
    }

    private void verifySubmitButtonEnabled() {
        onView(allOf(withClassName(endsWith("Button")), withText(R.string.submitbutton)))
                .check(matches(isEnabled()));
    }

    private void fillOptionsQuestion(int option) {
        onView(withId(R.id.question_tv)).check(matches(isDisplayed()));
        //Ensure we get the RadioButton View with the ID (each button/option has an ID starting from 0) and click it
        onView(allOf(withClassName(endsWith("RadioButton")), withId(option)))
                .check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.next_btn)).perform(click());
    }

    @Test
    public void cannotSubmitIfNoOptionSelected() throws Exception {
        onView(withId(R.id.next_btn)).perform(click());
        verifySubmitButtonDisabled();
    }

    private void verifySubmitButtonDisabled() {
        onView(allOf(withClassName(endsWith("Button")), withText(R.string.submitbutton)))
                .check(matches(not(isEnabled())));
    }
}
