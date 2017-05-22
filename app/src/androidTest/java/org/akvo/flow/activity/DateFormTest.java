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
import android.support.test.espresso.contrib.PickerActions;
import android.support.test.rule.ActivityTestRule;
import android.widget.DatePicker;

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
import java.util.Calendar;
import java.util.Locale;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static java.util.Calendar.SHORT;
import static org.akvo.flow.tests.R.raw.datesurvey;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;

public class DateFormTest {

    @Rule
    public ActivityTestRule<SurveyActivity> rule = new ActivityTestRule<>(SurveyActivity.class);
    private SurveyInstaller installer;

    @Before
    public void init() {
        Context context = rule.getActivity();
        installer = new SurveyInstaller(context, new SurveyDbAdapter(context));
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
    public void canFillDateQuestion() throws IOException {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        String dateString = getShortDate(cal);

        fillDateQuestion(datesurvey, year, month + 1,
                day); // +1 on month due to January starting at 0
        onView(withId(R.id.date_et)).check(matches(withText(dateString)));
        onView(withId(R.id.next_btn)).perform(click());
        onView(allOf(withClassName(endsWith("Button")), withText(R.string.submitbutton)))
                .check(matches(isEnabled()));
    }

    @Test
    public void canNotSubmitEmptyDateQuestion() throws IOException {
        fillDateQuestion(datesurvey, 0, 0, 0);
        onView(withId(R.id.next_btn)).perform(click());
        onView(allOf(withClassName(endsWith("Button")), withText(R.string.submitbutton)))
                .check(matches(not(isEnabled())));
    }

    private Survey fillDateQuestion(int surveyResId, int year, int month, int day)
            throws IOException {
        Survey survey = getSurvey(surveyResId);

        openDrawer();
        onView(withText(survey.getName())).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.new_datapoint)).perform(click());

        //For debugging purposes we need to ensure that 0 in any date fields will not access the Date Picker and instead
        //just go next
        if (year == 0 | month == 0 | day == 0) {
            return survey;
        } else {
            pickDate(year, month, day);
        }

        return survey;
    }

    private Survey getSurvey(int resId) throws IOException {
        InputStream input = InstrumentationRegistry.getContext().getResources()
                .openRawResource(resId);
        return installer.persistSurvey(FileUtil.readText(input));
    }

    private void openDrawer() {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
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
        onView(withText(R.string.pickdate)).perform(click());
        onView(withClassName(endsWith(DatePicker.class.getName())))
                .perform(PickerActions.setDate(year, month, day));
        onView(withText(R.string.okbutton)).perform(click());
    }
}
