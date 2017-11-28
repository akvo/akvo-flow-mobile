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
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.TextView;

import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.SurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.Survey;
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
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isFocusable;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.activity.Constants.TEST_FORM_SURVEY_INSTANCE_ID;
import static org.akvo.flow.activity.form.FormActivityTestUtil.addExecutionDelay;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getGeoButton;
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
    private static Survey survey;

    private final DecimalFormat accuracyFormat = new DecimalFormat("#");

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
        Context targetContext = InstrumentationRegistry.getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new SurveyInstaller(targetContext);
        survey = installer.installSurvey(locked_geo_form, InstrumentationRegistry.getContext());
    }

    @After
    public void afterEachTest() {
        installer.deleteResponses(TEST_FORM_SURVEY_INSTANCE_ID);
    }

    @AfterClass
    public static void afterClass() {
        SurveyRequisite.resetRequisites(InstrumentationRegistry.getTargetContext());
        installer.clearSurveys();
    }

    @Test
    public void ensureGeoQuestionProgressDisplayedOnButtonClick() throws Exception {
        clickGeoButton();
        addExecutionDelay(100);

        verifyProgressDisplayed();
    }

    @Test
    public void ensureErrorSnackBarDisplayedUponTimeout() throws Exception {
        clickGeoButton();

        simulateLocationTimeout();

        verifyErrorSnackBarDisplayed();
    }

    @Test
    public void ensureSnackBarRetryShowsProgress() throws Exception {
        clickGeoButton();

        simulateLocationTimeout();

        clickSnackBarRetry();

        verifyProgressDisplayed();
    }

    @Test
    public void ensureLocationValuesDisplayedCorrectly() throws Exception {
        clickGeoButton();

        provideMockLocation(MOCK_ACCURACY_ACCURATE);

        verifyGeoInput(R.id.lat_et, MOCK_LATITUDE + "");
        verifyGeoInput(R.id.lon_et, MOCK_LONGITUDE + "");
        verifyGeoInput(R.id.height_et, MOCK_ALTITUDE + "");

        verifyAccuracy(accuracyFormat.format(MOCK_ACCURACY_ACCURATE), Color.GREEN);
    }

    @Test
    public void ensureLocationValuesDisplayedCorrectlyIfInAccurate() throws Exception {
        clickGeoButton();

        provideMockLocation(MOCK_ACCURACY_INACCURATE);

        verifyAccuracy(accuracyFormat.format(MOCK_ACCURACY_INACCURATE), Color.RED);
    }

    @Test
    public void ensureFieldsResetWhenGeoButtonPress() throws Exception {
        onView(withId(R.id.lat_et)).perform(replaceText(MOCK_LATITUDE + ""));
        onView(withId(R.id.lon_et)).perform(replaceText(MOCK_LONGITUDE + ""));
        onView(withId(R.id.height_et)).perform(replaceText(MOCK_ALTITUDE + ""));
        ViewInteraction accuracyTv = onView(withId(R.id.acc_tv));
        accuracyTv.perform(replaceTextInTextView(getString(R.string.geo_location_accuracy,
                accuracyFormat.format(MOCK_ACCURACY_ACCURATE))));

        clickGeoButton();
        addExecutionDelay(100);

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
                getString(R.string.geo_location_accuracy, accuracy))));
        input.check(matches(hasTextColor(textColor)));
    }

    private void provideMockLocation(float accuracy) {
        LocationManager locationManager = (LocationManager) InstrumentationRegistry.getContext()
                .getSystemService(Context.LOCATION_SERVICE);
        locationManager
                .addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, false,
                        false, false, Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(MOCK_LATITUDE);
        location.setLongitude(MOCK_LONGITUDE);
        location.setAltitude(MOCK_ALTITUDE);
        location.setAccuracy(accuracy);
        locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location);
    }

    private void verifyGeoInput(int resId, String text) {
        ViewInteraction input = onView(withId(resId));
        input.check(matches(isDisplayed()));
        input.check(matches(withText(text)));
        input.check(matches(not(isFocusable())));
    }

    private void clickGeoButton() {
        final List<QuestionGroup> questionGroups = survey.getQuestionGroups();
        final Question question = questionGroups.get(0).getQuestions().get(0);
        ViewInteraction geoButton = getGeoButton(question);
        geoButton.check(matches(isDisplayed()));
        geoButton.perform(click());
    }

    private void verifyProgressDisplayed() {
        ViewInteraction progress = onView(withId(R.id.auto_geo_location_progress));
        progress.check(matches(isDisplayed()));
    }

    private void verifyErrorSnackBarDisplayed() {
        onView(allOf(withId(android.support.design.R.id.snackbar_text),
                withText(R.string.location_timeout)))
                .check(matches(isDisplayed()));
    }

    private void simulateLocationTimeout() {
        final View geoQuestion = rule.getActivity().findViewById(R.id.geo_question_view);
        geoQuestion.post(new Runnable() {
            @Override
            public void run() {
                ((TimedLocationListener.Listener) geoQuestion)
                        .onTimeout();
            }
        });
    }

    private void clickSnackBarRetry() {
        onView(withId(android.support.design.R.id.snackbar_action))
                .perform(click());
        addExecutionDelay(100);
    }

    @NonNull
    private String getString(@StringRes int stringResId, String format) {
        return rule.getActivity().getApplicationContext().getResources()
                .getString(stringResId, format);
    }

    public static Matcher<View> hasTextColor(final int color) {
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

    public static ViewAction replaceTextInTextView(final String value) {
        return new ViewAction() {
            @SuppressWarnings("unchecked")
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
