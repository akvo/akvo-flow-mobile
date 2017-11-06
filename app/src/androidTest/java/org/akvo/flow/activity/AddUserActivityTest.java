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

import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.akvo.flow.R;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AddUserActivityTest {

    @Rule
    public ActivityTestRule<AddUserActivity> mActivityTestRule = new ActivityTestRule<>(
            AddUserActivity.class);

    @Test
    public void testAddUser() {
        ViewInteraction nameEditText = onView(
                allOf(withId(R.id.username), isDisplayed()));
        nameEditText.check(matches(withHint("Username")));

        ViewInteraction deviceEditText = onView(
                allOf(withId(R.id.device_id), isDisplayed()));
        deviceEditText.check(matches(withHint("Device Identifier")));

        ViewInteraction button = onView(
                allOf(withId(R.id.login_btn), isDisplayed()));
        button.check(matches(not(isEnabled())));

        nameEditText.perform(click());
        nameEditText.perform(replaceText("valeria_emulator"), closeSoftKeyboard());
        nameEditText.perform(pressImeActionButton());

        deviceEditText.perform(replaceText("valeria_emulator"), closeSoftKeyboard());

        button.check(matches(isEnabled()));
        deviceEditText.perform(pressImeActionButton());
    }
}
