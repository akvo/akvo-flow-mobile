/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

import android.Manifest;
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

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static org.akvo.flow.activity.form.FormActivityTestUtil.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.activity.form.FormActivityTestUtil.clickNext;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getString;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonDisabled;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifySubmitButtonEnabled;
import static org.akvo.flow.tests.R.raw.geo_form;
import static org.hamcrest.CoreMatchers.is;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ManualGeoQuestionViewTest {

    private static final double MOCK_LATITUDE = 10.0;
    private static final double MOCK_LONGITUDE = 20.0;

    private static TestSurveyInstaller installer;

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule
            .grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Rule
    public GrantPermissionRule permissionRule2 = GrantPermissionRule
            .grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Rule
    public GrantPermissionRule permissionRule3 = GrantPermissionRule
            .grant(Manifest.permission.READ_PHONE_STATE);

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            return getFormActivityIntent(155852013L, "156792013", "GeoForm", 0L);
        }
    };

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new TestSurveyInstaller(targetContext);
        installer.installSurvey(geo_form, InstrumentationRegistry.getInstrumentation().getContext());
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
    public void ensureUnableToSubmitIfLatitudeMissing() {
        onView(withId(R.id.lon_et)).perform(typeText(MOCK_LONGITUDE + ""));
        closeSoftKeyboard();

        clickNext();

        verifySubmitButtonDisabled();
    }

    @Test
    public void ensureUnableToSubmitIfLongitudeMissing() {
        onView(withId(R.id.lat_et)).perform(typeText(MOCK_LATITUDE + ""));
        closeSoftKeyboard();

        clickNext();

        verifySubmitButtonDisabled();
    }

    @Test
    public void ensureErrorShownWhenLatitudeTooSmall() {
        onView(withId(R.id.lat_et)).perform(typeText(-333 + ""));
        closeSoftKeyboard();

        onView(withId(R.id.lat_et))
                .check(matches(hasErrorText(is(getString(R.string.invalid_latitude, rule)))));
    }

    @Test
    public void ensureErrorShownWhenLatitudeTooLarge() {
        onView(withId(R.id.lat_et)).perform(typeText(333 + ""));
        closeSoftKeyboard();

        onView(withId(R.id.lat_et))
                .check(matches(hasErrorText(is(getString(R.string.invalid_latitude, rule)))));
    }

    @Test
    public void ensureErrorShownWhenLongitudeTooSmall() {
        onView(withId(R.id.lon_et)).perform(typeText(-333 + ""));
        closeSoftKeyboard();

        onView(withId(R.id.lon_et))
                .check(matches(hasErrorText(is(getString(R.string.invalid_longitude, rule)))));
    }

    @Test
    public void ensureErrorShownWhenLongitudeTooLarge() {
        onView(withId(R.id.lon_et)).perform(typeText(333 + ""));
        closeSoftKeyboard();

        onView(withId(R.id.lon_et))
                .check(matches(hasErrorText(is(getString(R.string.invalid_longitude, rule)))));
    }

    @Test
    public void ensureErrorShownWhenAltitudeTooSmall() {
        onView(withId(R.id.height_et)).perform(typeText(-33333 + ""));
        closeSoftKeyboard();

        onView(withId(R.id.height_et))
                .check(matches(hasErrorText(is(getString(R.string.invalid_elevation, rule)))));
    }

    @Test
    public void ensureErrorShownWhenAltitudeTooLarge() {
        onView(withId(R.id.height_et)).perform(typeText(33333 + ""));
        closeSoftKeyboard();

        onView(withId(R.id.height_et))
                .check(matches(hasErrorText(is(getString(R.string.invalid_elevation, rule)))));
    }

    @Test
    public void ensureCanSubmitCorrectValues() {
        onView(withId(R.id.lat_et)).perform(typeText(MOCK_LATITUDE + ""));
        onView(withId(R.id.lon_et)).perform(typeText(MOCK_LONGITUDE + ""));
        closeSoftKeyboard();
        onView(withId(R.id.acc_tv))
                .check(matches(withText(R.string.geo_location_accuracy_default)));

        clickNext();
        verifySubmitButtonEnabled();
    }
}
