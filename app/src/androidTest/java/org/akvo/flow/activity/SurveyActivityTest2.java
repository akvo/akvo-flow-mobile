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
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.akvo.flow.R;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SurveyActivityTest2 {

    @Rule
    public ActivityTestRule<SurveyActivity> mActivityTestRule = new ActivityTestRule<>(
            SurveyActivity.class);

    @Test
    public void surveyActivityTest2() {
        ViewInteraction editText = onView(
                allOf(withId(R.id.username),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        editText.perform(click());

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.username),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        editText2.perform(replaceText("valeria_emulator"), closeSoftKeyboard());

        ViewInteraction editText3 = onView(
                allOf(withId(R.id.device_id),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                1),
                        isDisplayed()));
        editText3.perform(replaceText("valeria_emulator"), closeSoftKeyboard());

        ViewInteraction button = onView(
                allOf(withId(R.id.login_btn), withText("Next"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        button.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction switchCompat = onView(
                allOf(withId(R.id.switch_enable_data), withText("Enable usage of mobile data"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(
                                                is("android.support.v4.widget.NestedScrollView")),
                                        0),
                                3),
                        isDisplayed()));
        switchCompat.perform(click());

        ViewInteraction appCompatImageButton = onView(
                allOf(withContentDescription("Navigate up"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar),
                                        childAtPosition(
                                                withId(R.id.appbar_layout),
                                                0)),
                                1),
                        isDisplayed()));
        appCompatImageButton.perform(click());

        pressBack();

        pressBack();

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.add_data_point_fab),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.drawer_layout),
                                        0),
                                2),
                        isDisplayed()));
        floatingActionButton.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(2995);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatButton = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                2),
                        isDisplayed()));
        appCompatButton.perform(click());

        ViewInteraction textView = onView(
                allOf(withText("New form"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar),
                                        childAtPosition(
                                                IsInstanceOf.<View>instanceOf(
                                                        android.widget.LinearLayout.class),
                                                0)),
                                1),
                        isDisplayed()));
        textView.check(matches(withText("New form")));

        ViewInteraction textView2 = onView(
                allOf(withText("v 2.0"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar),
                                        childAtPosition(
                                                IsInstanceOf.<View>instanceOf(
                                                        android.widget.LinearLayout.class),
                                                0)),
                                2),
                        isDisplayed()));
        textView2.check(matches(withText("v 2.0")));

        ViewInteraction textView3 = onView(
                allOf(withText("group1"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(
                                                android.widget.LinearLayout.class),
                                        0),
                                0),
                        isDisplayed()));
        textView3.check(matches(withText("group1")));

        ViewInteraction textView4 = onView(
                allOf(withText("Submit"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(
                                                android.widget.LinearLayout.class),
                                        1),
                                0),
                        isDisplayed()));
        textView4.check(matches(withText("Submit")));

        pressBack();

        pressBack();

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatImageButton2 = onView(
                allOf(withContentDescription("Drawer open"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar),
                                        childAtPosition(
                                                withClassName(
                                                        is("android.support.design.widget.AppBarLayout")),
                                                0)),
                                1),
                        isDisplayed()));
        appCompatImageButton2.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(4366);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction floatingActionButton2 = onView(
                allOf(withId(R.id.add_data_point_fab),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.drawer_layout),
                                        0),
                                2),
                        isDisplayed()));
        floatingActionButton2.perform(click());

        ViewInteraction textView5 = onView(
                allOf(withText("New group - please change name"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(
                                                android.widget.LinearLayout.class),
                                        1),
                                0),
                        isDisplayed()));
        textView5.check(matches(withText("New group - please change name")));

        ViewInteraction textView6 = onView(
                allOf(withText("New group - please change name"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(
                                                android.widget.LinearLayout.class),
                                        2),
                                0),
                        isDisplayed()));
        textView6.check(matches(withText("New group - please change name")));

        ViewInteraction textView7 = onView(
                allOf(withId(R.id.more_submenu), withContentDescription("More"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.toolbar),
                                        3),
                                0),
                        isDisplayed()));
        textView7.check(matches(withText("")));

        ViewInteraction tabView = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(R.id.tabs),
                                0),
                        5),
                        isDisplayed()));
        tabView.perform(click());

        ViewInteraction tabView2 = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(R.id.tabs),
                                0),
                        4),
                        isDisplayed()));
        tabView2.perform(click());

        ViewInteraction tabView3 = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(R.id.tabs),
                                0),
                        3),
                        isDisplayed()));
        tabView3.perform(click());

        ViewInteraction tabView4 = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(R.id.tabs),
                                0),
                        2),
                        isDisplayed()));
        tabView4.perform(click());

        ViewInteraction actionMenuItemView = onView(
                allOf(withId(R.id.more_submenu), withContentDescription("More"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.toolbar),
                                        3),
                                0),
                        isDisplayed()));
        actionMenuItemView.perform(click());

        ViewInteraction textView8 = onView(
                allOf(withId(R.id.title), withText("Languages"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(
                                                android.widget.LinearLayout.class),
                                        0),
                                0),
                        isDisplayed()));
        textView8.check(matches(withText("Languages")));

        ViewInteraction textView9 = onView(
                allOf(withId(R.id.title), withText("Clear"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(
                                                android.widget.LinearLayout.class),
                                        0),
                                0),
                        isDisplayed()));
        textView9.check(matches(withText("Clear")));
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
