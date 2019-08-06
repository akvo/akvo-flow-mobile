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
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.SurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.akvo.flow.event.TimedLocationListener;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.DecimalFormat;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isFocusable;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.activity.form.FormActivityTestUtil.addExecutionDelay;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getString;
import static org.akvo.flow.tests.R.raw.locked_geo_form;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsNot.not;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class LockedGeoQuestionViewTest {

    private static final double MOCK_LATITUDE = 10.0;
    private static final double MOCK_LONGITUDE = 20.0;
    private static final double MOCK_ALTITUDE = 1.0;
    private static final float MOCK_ACCURACY_ACCURATE = 4.0f;
    private static final float MOCK_ACCURACY_INACCURATE = 101.0f;

    private static SurveyInstaller installer;

    private final DecimalFormat accuracyFormat = new DecimalFormat("#");

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
            return getFormActivityIntent(155852013L, "156792013", "LockedGeoForm", 0L, false);
        }
    };

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new SurveyInstaller(targetContext);
        installer.installSurvey(locked_geo_form, InstrumentationRegistry.getInstrumentation().getContext());
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
    public void ensureGeoQuestionProgressDisplayedOnButtonClick() {
        closeSoftKeyboard();
        clickGeoButton();

        verifyProgressDisplayed();
    }

    @Test
    public void ensureErrorSnackBarDisplayedUponTimeout() {
        closeSoftKeyboard();
        clickGeoButton();

        simulateLocationTimeout();

        verifyErrorSnackBarDisplayed();
    }

    @Test
    public void ensureSnackBarRetryShowsProgress() {
        clickGeoButton();

        simulateLocationTimeout();

        clickSnackBarRetry();

        verifyProgressDisplayed();
    }

    @Test
    public void ensureLocationValuesDisplayedCorrectly() {
        clickGeoButton();
        addExecutionDelay(100);
        provideMockLocation(MOCK_ACCURACY_ACCURATE);
        addExecutionDelay(100);
        verifyGeoInput(R.id.lat_et, MOCK_LATITUDE + "");
        verifyGeoInput(R.id.lon_et, MOCK_LONGITUDE + "");
        verifyGeoInput(R.id.height_et, MOCK_ALTITUDE + "");

        verifyAccuracy(accuracyFormat.format(MOCK_ACCURACY_ACCURATE), Color.GREEN);
    }

    @Test
    public void ensureLocationValuesDisplayedCorrectlyIfInAccurate() {
        clickGeoButton();
        addExecutionDelay(100);

        provideMockLocation(MOCK_ACCURACY_INACCURATE);
        addExecutionDelay(100);

        verifyAccuracy(accuracyFormat.format(MOCK_ACCURACY_INACCURATE), Color.RED);
    }

    @Test
    public void ensureFieldsResetWhenGeoButtonPress() {
        onView(withId(R.id.lat_et)).perform(replaceText(MOCK_LATITUDE + ""));
        onView(withId(R.id.lon_et)).perform(replaceText(MOCK_LONGITUDE + ""));
        onView(withId(R.id.height_et)).perform(replaceText(MOCK_ALTITUDE + ""));
        closeSoftKeyboard();

        ViewInteraction accuracyTv = onView(withId(R.id.acc_tv));
        accuracyTv.perform(replaceTextInTextView(getString(R.string.geo_location_accuracy, rule,
                accuracyFormat.format(MOCK_ACCURACY_ACCURATE))));

        clickGeoButton();
        onView(withId(android.R.id.button1)).perform(click());
        addExecutionDelay(100);

        onView(withId(R.id.lat_et)).check(matches(withText("")));
        onView(withId(R.id.lon_et)).check(matches(withText("")));
        onView(withId(R.id.height_et)).check(matches(withText("")));
        onView(withId(R.id.acc_tv))
                .check(matches(withText(R.string.geo_location_accuracy_default)));
    }

    @Test
    public void ensureLocationValuesDisplayedCorrectlyWhenCancelled() {
        onView(withId(R.id.lat_et)).perform(replaceText(""));
        onView(withId(R.id.lon_et)).perform(replaceText(""));
        onView(withId(R.id.height_et)).perform(replaceText(""));
        closeSoftKeyboard();

        clickGeoButton();
        addExecutionDelay(100);

        clickCancelButton();

        onView(withId(R.id.lat_et)).check(matches(withText("")));
        onView(withId(R.id.lon_et)).check(matches(withText("")));
        onView(withId(R.id.height_et)).check(matches(withText("")));

        onView(withId(R.id.acc_tv))
                .check(matches(withText(R.string.geo_location_accuracy_default)));
    }

    private void verifyAccuracy(String accuracy, int textColor) {
        ViewInteraction input = onView(withId(R.id.acc_tv));
        input.check(matches(isDisplayed()));
        input.check(matches(withText(
                getString(R.string.geo_location_accuracy, rule, accuracy))));
        input.check(matches(hasTextColor(textColor)));
    }

    private void provideMockLocation(float accuracy) {
        LocationManager locationManager = (LocationManager) InstrumentationRegistry.getInstrumentation().getContext()
                .getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        locationManager
                .addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, false,
                        false, false, Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(MOCK_LATITUDE);
        location.setLongitude(MOCK_LONGITUDE);
        location.setAltitude(MOCK_ALTITUDE);
        location.setAccuracy(accuracy);
        location.setTime(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }

        locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location);
    }

    private void verifyGeoInput(int resId, String text) {
        ViewInteraction input = onView(withId(resId));
        input.check(matches(isDisplayed()));
        input.check(matches(withText(text)));
        input.check(matches(not(isFocusable())));
    }

    private void clickGeoButton() {
        onView(withId(R.id.geo_btn)).check(matches(isCompletelyDisplayed())).perform(click());
    }

    private void clickCancelButton() {
        onView(allOf(withId(R.id.geo_btn), withText(R.string.cancelbutton)))
                .check(matches(isCompletelyDisplayed())).perform(click());
    }

    private void verifyProgressDisplayed() {
        ViewInteraction progress = onView(withId(R.id.auto_geo_location_progress));
        progress.check(matches(isDisplayed()));
    }

    private void verifyErrorSnackBarDisplayed() {
        onView(allOf(withId(com.google.android.material.R.id.snackbar_text),
                withText(R.string.location_timeout)))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

    private void simulateLocationTimeout() {
        final View geoQuestion = rule.getActivity().findViewById(R.id.geo_question_view);
        geoQuestion.post(((TimedLocationListener.Listener) geoQuestion)::onTimeout);
    }

    private void clickSnackBarRetry() {
        onView(withId(com.google.android.material.R.id.snackbar_action))
                .check(matches(allOf(isEnabled(), isClickable())))
                .perform(new ViewAction() {
                             @Override
                             public Matcher<View> getConstraints() {
                                 return isEnabled();
                             }

                             @Override
                             public String getDescription() {
                                 return "click SnackBar Retry Button";
                             }

                             @Override
                             public void perform(UiController uiController, View view) {
                                 view.performClick();
                             }
                         }
                );
    }

    private static Matcher<View> hasTextColor(final int color) {
        return new BoundedMatcher<View, TextView>(TextView.class) {
            @Override
            public boolean matchesSafely(TextView warning) {
                return color == warning.getCurrentTextColor();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with text color: ");
            }
        };
    }

    private static ViewAction replaceTextInTextView(final String value) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return allOf(isDisplayed(), isAssignableFrom(TextView.class));
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((TextView) view).setText(value);
            }

            @Override
            public String getDescription() {
                return "replace text";
            }
        };
    }
}
