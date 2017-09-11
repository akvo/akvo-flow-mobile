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
import android.support.test.espresso.contrib.PickerActions;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.widget.DatePicker;

import org.akvo.flow.R;
import org.akvo.flow.activity.testhelper.SurveyInstaller;
import org.akvo.flow.activity.testhelper.SurveyRequisite;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.Locale;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static java.util.Calendar.SHORT;
import static org.akvo.flow.activity.FormActivityTestUtil.clickNext;
import static org.akvo.flow.activity.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.FormActivityTestUtil.verifySubmitButtonDisabled;
import static org.akvo.flow.activity.FormActivityTestUtil.verifySubmitButtonEnabled;
import static org.akvo.flow.tests.R.raw.datesurvey;
import static org.hamcrest.Matchers.endsWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class DateFormActivityTest {

    private static SurveyInstaller installer;

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            return getFormActivityIntent(41713002L, "49803002", "DateForm");
        }
    };

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new SurveyInstaller(targetContext);
        installer.installSurvey(datesurvey, InstrumentationRegistry.getContext());
    }

    @After
    public void afterEachTest() {
        installer.deleteResponses(Constants.TEST_FORM_SURVEY_INSTANCE_ID);
    }

    @AfterClass
    public static void afterClass() {
        SurveyRequisite.resetRequisites(InstrumentationRegistry.getTargetContext());
        installer.clearSurveys();
    }

    @Test
    public void canFillDateQuestion() throws Exception {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        String dateString = getShortDate(cal);

        pickDate(year, month + 1, day); // +1 on month due to January starting at 0
        onView(withId(R.id.date_et)).check(matches(withText(dateString)));
        clickNext();
        verifySubmitButtonEnabled();
    }

    @Test
    public void canNotSubmitEmptyDateQuestion() throws Exception {
        clickNext();
        verifySubmitButtonDisabled();
    }

    /**
     * @param cal Calendar object populated with Calendar.getInstance()
     * @return String representation of today's date, concatenated (ex. Jan 5, 2017)
     */
    private String getShortDate(Calendar cal) {
        StringBuilder dateString = new StringBuilder();
        dateString.append(cal.getDisplayName(Calendar.MONTH, SHORT, Locale.ENGLISH));
        dateString.append(" ");
        dateString.append(cal.get(Calendar.DAY_OF_MONTH));
        dateString.append(", ");
        dateString.append(cal.get(Calendar.YEAR));

        return dateString.toString();
    }

    private void pickDate(int year, int month, int day) {
        onView(withId(R.id.date_btn)).perform(click());
        onView(withClassName(endsWith(DatePicker.class.getName())))
                .perform(PickerActions.setDate(year, month, day));
        onView(withId(android.R.id.button1)).perform(click());
    }
}
