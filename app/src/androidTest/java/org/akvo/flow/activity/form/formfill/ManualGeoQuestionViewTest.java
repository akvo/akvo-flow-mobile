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
import android.support.test.espresso.Espresso;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.SurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasErrorText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.activity.form.FormActivityTestUtil.clickNext;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getString;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonDisabled;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonEnabled;
import static org.akvo.flow.tests.R.raw.geo_form;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ManualGeoQuestionViewTest {

    private static final double MOCK_LATITUDE = 10.0;
    private static final double MOCK_LONGITUDE = 20.0;

    private static SurveyInstaller installer;

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            return getFormActivityIntent(155852013L, "156792013", "GeoForm", 0L, false);
        }
    };

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new SurveyInstaller(targetContext);
        installer.installSurvey(geo_form, InstrumentationRegistry.getContext());
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
    public void ensureUnableToSubmitIfLatitudeMissing() throws Exception {
        onView(withId(R.id.lon_et)).perform(typeText(MOCK_LONGITUDE + ""));

        clickNext();

        verifySubmitButtonDisabled();
    }

    @Test
    public void ensureUnableToSubmitIfLongitudeMissing() throws Exception {
        onView(withId(R.id.lat_et)).perform(typeText(MOCK_LATITUDE + ""));

        clickNext();

        verifySubmitButtonDisabled();
    }

    @Test
    public void ensureErrorShownWhenLatitudeTooSmall() throws Exception {
        onView(withId(R.id.lat_et)).perform(typeText(-333 + ""));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.lat_et))
                .check(matches(hasErrorText(getString(R.string.invalid_latitude, rule))));
    }

    @Test
    public void ensureErrorShownWhenLatitudeTooLarge() throws Exception {
        onView(withId(R.id.lat_et)).perform(typeText(333 + ""));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.lat_et))
                .check(matches(hasErrorText(getString(R.string.invalid_latitude, rule))));
    }

    @Test
    public void ensureErrorShownWhenLongitudeTooSmall() throws Exception {
        onView(withId(R.id.lon_et)).perform(typeText(-333 + ""));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.lon_et))
                .check(matches(hasErrorText(getString(R.string.invalid_longitude, rule))));
    }

    @Test
    public void ensureErrorShownWhenLongitudeTooLarge() throws Exception {
        onView(withId(R.id.lon_et)).perform(typeText(333 + ""));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.lon_et))
                .check(matches(hasErrorText(getString(R.string.invalid_longitude, rule))));
    }

    @Test
    public void ensureErrorShownWhenAltitudeTooSmall() throws Exception {
        onView(withId(R.id.height_et)).perform(typeText(-33333 + ""));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.height_et))
                .check(matches(hasErrorText(getString(R.string.invalid_elevation, rule))));
    }

    @Test
    public void ensureErrorShownWhenAltitudeTooLarge() throws Exception {
        onView(withId(R.id.height_et)).perform(typeText(33333 + ""));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.height_et))
                .check(matches(hasErrorText(getString(R.string.invalid_elevation, rule))));
    }

    @Test
    public void ensureCanSubmitCorrectValues() throws Exception {
        onView(withId(R.id.lat_et)).perform(typeText(MOCK_LATITUDE + ""));
        onView(withId(R.id.lon_et)).perform(typeText(MOCK_LONGITUDE + ""));

        onView(withId(R.id.acc_tv))
                .check(matches(withText(R.string.geo_location_accuracy_default)));

        clickNext();
        verifySubmitButtonEnabled();
    }
}
