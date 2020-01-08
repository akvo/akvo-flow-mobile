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

package org.akvo.flow.activity;

import android.Manifest;

import org.akvo.flow.R;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class AddUserActivityTest {

    @Rule
    public ActivityTestRule<AddUserActivity> mActivityTestRule = new ActivityTestRule<>(
            AddUserActivity.class);

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule
            .grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Rule
    public GrantPermissionRule permissionRule2 = GrantPermissionRule
            .grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Rule
    public GrantPermissionRule permissionRule3 = GrantPermissionRule
            .grant(Manifest.permission.READ_PHONE_STATE);

    @BeforeClass
    public static void beforeClass() {
        SurveyRequisite.resetRequisites(InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test
    public void testAddUser() {
        ViewInteraction nameEditText = onView(
                allOf(withId(R.id.username), isDisplayed()));
        nameEditText.check(matches(withHint(R.string.username)));

        ViewInteraction deviceEditText = onView(
                allOf(withId(R.id.device_id), isDisplayed()));
        deviceEditText.check(matches(withHint(R.string.identlabel)));

        ViewInteraction button = onView(
                allOf(withId(R.id.login_btn), isDisplayed()));
        button.check(matches(not(isEnabled())));

        nameEditText.perform(click());
        nameEditText.perform(replaceText("test_username"), closeSoftKeyboard());
        nameEditText.perform(pressImeActionButton());

        deviceEditText.perform(replaceText("test_device"), closeSoftKeyboard());

        button.check(matches(isEnabled()));
        deviceEditText.perform(pressImeActionButton());
    }
}
