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
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import org.akvo.flow.R;
import org.akvo.flow.activity.testhelper.SurveyInstaller;
import org.akvo.flow.activity.testhelper.SurveyRequisite;
import org.akvo.flow.domain.QuestionGroup;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.activity.ChildPositionMatcher.childAtPosition;
import static org.akvo.flow.activity.Constants.TEST_FORM_SURVEY_INSTANCE_ID;
import static org.akvo.flow.activity.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.tests.R.raw.test_form;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNot.not;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SurveyActivityTest {

    private static SurveyInstaller installer;

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            return getFormActivityIntent(155852013L, "156792013", "Test form");
        }
    };

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new SurveyInstaller(targetContext);
        installer.installSurvey(test_form, InstrumentationRegistry.getContext());
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
    public void surveyActivityTest() {

        //make sure everything is loaded
        addExecutionDelay(5000);

        List<QuestionGroup> questions = ((FormActivity) rule.getActivity()).getQuestionGroups();
        ViewInteraction firstQuestionTitle = findQuestionTitle("1. text question*");
        firstQuestionTitle.check(matches(isDisplayed()));

        ViewInteraction firstQuestionInput = onView(
                allOf(withId(R.id.input_et),
                        questionListItem(0),
                        isDisplayed()));
        firstQuestionInput.check(matches(withText("")));
        firstQuestionInput.perform(click());
        firstQuestionInput.perform(closeSoftKeyboard());

        ViewInteraction secondQuestionTitle = findQuestionTitle("2. option*");
        secondQuestionTitle.check(matches(isDisplayed()));

        ViewInteraction optionOne = radioButtonWithText("yes", 0);
        optionOne.check(matches(isDisplayed()));

        ViewInteraction optionTwo = radioButtonWithText("no", 1);
        optionTwo.check(matches(isDisplayed()));

        ViewInteraction optionThree = radioButtonWithText("maybe", 2);
        optionThree.check(matches(isDisplayed()));

        ViewInteraction thirdQuestionTitle = findQuestionTitle("3. cascade");
        thirdQuestionTitle.check(matches(isDisplayed()));

        ViewInteraction textView4 = onView(
                allOf(withId(R.id.text), withText("1"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.cascade_content),
                                        0),
                                0),
                        isDisplayed()));
        textView4.check(matches(withText("1")));

        ViewInteraction textView5 = onView(
                allOf(withId(R.id.cascade_spinner_item_text),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.spinner),
                                        0),
                                0),
                        isDisplayed()));
        textView5.check(matches(withText("please select")));

        ViewInteraction spinner = onView(
                allOf(withId(R.id.spinner),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.cascade_content),
                                        0),
                                1)));
        spinner.check(matches(isDisplayed()));

        ViewInteraction fourthQuestionTitle = findQuestionTitle("4. number*");
        fourthQuestionTitle.check(matches(isDisplayed()));

        ViewInteraction questionHelpTip = onView(
                allOf(withId(R.id.tip_ib),
                        childAtPosition(linearLayoutChild(0), 1), isDisplayed()));
        questionHelpTip.check(matches(isDisplayed()));

        ViewInteraction fourthQuestionInputOne = onView(
                allOf(withId(R.id.input_et),
                        questionListItem(3),
                        isDisplayed()));
        fourthQuestionInputOne.check(matches(withText("")));

        ViewInteraction fourthQuestionRepeatTitle = onView(
                allOf(withId(R.id.double_entry_title), withText("Please, repeat answer"),
                        childAtPosition(childAtPosition(withId(R.id.question_list), 3), 2)));
        fourthQuestionRepeatTitle.check(matches(isDisplayed()));

        ViewInteraction fourthQuestionInput2 = onView(
                allOf(withId(R.id.double_entry_et),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.question_list),
                                        3),
                                3),
                        isDisplayed()));
        fourthQuestionInput2.check(matches(withText("")));
        fourthQuestionInput2.perform(click());
        fourthQuestionInput2.perform(closeSoftKeyboard());

        ViewInteraction fifthQuestionTitle = findQuestionTitle("5. geolocation");
        fifthQuestionTitle.check(matches(isDisplayed()));

        ViewInteraction latitudeLabel = onView(
                allOf(withText("Lat"),
                        getRowCell(0, 0)));
        latitudeLabel.check(matches(isDisplayed()));


        ViewInteraction latitudeInput = onView(
                allOf(withId(R.id.lat_et),
                        getRowCell(1, 0),
                        isDisplayed()));
        latitudeInput.check(matches(withText("")));
        latitudeInput.perform(click());
        latitudeInput.perform(closeSoftKeyboard());

        ViewInteraction longitudeLabel = onView(
                allOf(withText("Lon"),
                        getRowCell(0, 1)));
        longitudeLabel.check(matches(isDisplayed()));

        ViewInteraction longitudeInput = onView(
                allOf(withId(R.id.lon_et),
                        getRowCell(1, 1),
                        isDisplayed()));
        longitudeInput.check(matches(withText("")));
        longitudeInput.perform(click());
        longitudeInput.perform(closeSoftKeyboard());

        ViewInteraction heightLabel = onView(
                allOf(withText("Height"),
                        getRowCell(0, 2)));
        heightLabel.check(matches(withText("Height")));

        ViewInteraction heightInput = onView(
                allOf(withId(R.id.height_et),
                        getRowCell(1, 2)));
        heightInput.check(matches(withText("")));
        heightInput.perform(click());
        heightInput.perform(closeSoftKeyboard());

        ViewInteraction accuracyLabel = onView(
                allOf(withId(R.id.acc_tv), withText("Accuracy: unknown"),
                        getRowCell(0, 3)));
        accuracyLabel.check(matches(withText("Accuracy: unknown")));

        ViewInteraction geoButton = onView(
                allOf(withId(R.id.geo_btn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.question_list),
                                        4),
                                2),
                        isDisplayed()));
        geoButton.check(matches(withText(R.string.getgeo)));
        geoButton.check(matches(isEnabled()));

        ViewInteraction sixthQuestionTitle = findQuestionTitle("6. photo");
        sixthQuestionTitle.check(matches(isDisplayed()));

        ViewInteraction photoButton = onView(
                allOf(withId(R.id.media_btn),
                        withText(R.string.takephoto))).perform(ViewActions.scrollTo());
        photoButton.check(matches(isDisplayed()));

        ViewInteraction seventhQuestionLabel = findQuestionTitle("7. video");
        seventhQuestionLabel.check(matches(isDisplayed()));

        ViewInteraction videoButton = onView(
                allOf(withId(R.id.media_btn),
                        withText(R.string.takevideo))).perform(scrollTo());
        videoButton.check(matches(isDisplayed()));

        ViewInteraction eightQuestionTitle = findQuestionTitle("8. date*")
                .perform(scrollTo());
        eightQuestionTitle.check(matches(isDisplayed()));

        ViewInteraction dateInput = onView(withId(R.id.date_et)).perform(scrollTo());
        dateInput.check(matches(isDisplayed()));
        dateInput.check(matches(withText("")));

        ViewInteraction dateButton = onView(withId(R.id.date_btn)).perform(scrollTo());
        dateButton.check(matches(withText(R.string.pickdate)));
        dateButton.check(matches(isDisplayed()));

        ViewInteraction ninthQuestionLabel = findQuestionTitle("9. barcode").perform(scrollTo());
        ninthQuestionLabel.check(matches(isDisplayed()));

        ViewInteraction barcodeInput = onView(withId(R.id.barcode_input)).perform(scrollTo());
        barcodeInput.check(matches(withHint(R.string.type_code)));
        barcodeInput.check(matches(withText("")));
        barcodeInput.check(matches(isDisplayed()));

        ViewInteraction barcodeManualSeparator = onView(withId(R.id.barcode_manual_input_separator))
                .perform(scrollTo());
        barcodeManualSeparator.check(matches(withText(R.string.or)));
        barcodeManualSeparator.check(matches(isDisplayed()));

        ViewInteraction scanButton = onView(withId(R.id.scan_btn)).perform(scrollTo());
        scanButton
                .check(matches(allOf(isDisplayed(), isEnabled(), withText(R.string.scanbarcode))));

        ViewInteraction tenthQuestionLabel = findQuestionTitle("10. geoshape").perform(scrollTo());
        tenthQuestionLabel.check(matches(isDisplayed()));

        ViewInteraction captureShapeButton = onView(withId(R.id.capture_shape_btn)).perform(scrollTo());
        captureShapeButton.check(matches(allOf(isDisplayed(), isEnabled(), withText(R.string.capture_shape))));

        ViewInteraction eleventhQuestionLabel = findQuestionTitle("11. signature").perform(scrollTo());
        eleventhQuestionLabel.check(matches(isDisplayed()));

        ViewInteraction signatureButton = onView(withId(R.id.sign_btn)).perform(scrollTo());
        signatureButton.check(matches(
                allOf(isDisplayed(), isEnabled(), withText(R.string.add_signature))));

        ViewInteraction twelfthQuestionLabel = findQuestionTitle("12. caddisfly")
                .perform(scrollTo());
        twelfthQuestionLabel.check(matches(isDisplayed()));

        ViewInteraction caddisflyButton = onView(withId(R.id.caddisfly_button)).perform(scrollTo());
        caddisflyButton.check(matches(
                allOf(isDisplayed(), isEnabled(), withText(R.string.caddisfly_test))));

        Espresso.onView(ViewMatchers.withId(R.id.scroller)).perform(ViewActions.swipeUp());

        ViewInteraction nextButton = onView(withId(R.id.next_btn));
        nextButton.check(matches(allOf(isDisplayed(), isEnabled(), withText(R.string.nextbutton))));

        ViewInteraction submitTab = onView(
                allOf(childAtPosition(childAtPosition(withId(R.id.tabs), 0), 1), isDisplayed()));
        submitTab.perform(click());

        ViewInteraction submitTabHeader = onView(withId(R.id.submit_tab_header));
        submitTabHeader.check(matches(withText(R.string.error_responses)));

        ViewInteraction textView22 = onView(
                allOf(withId(R.id.question_tv), withText("4. number*"),
                        childAtPosition(
                                linearLayoutChild(0),
                                0),
                        isDisplayed()));
        textView22.check(matches(withText("4. number*")));

        ViewInteraction imageButton2 = onView(
                allOf(withId(R.id.tip_ib),
                        childAtPosition(
                                linearLayoutChild(0),
                                1),
                        isDisplayed()));
        imageButton2.check(matches(isDisplayed()));

        ViewInteraction imageView = onView(
                allOf(withId(R.id.open_btn),
                        childAtPosition(
                                linearLayoutChild(0),
                                2),
                        isDisplayed()));
        imageView.check(matches(isDisplayed()));

        ViewInteraction textView23 = onView(
                allOf(withId(R.id.question_tv), withText("1. text question*"),
                        childAtPosition(
                                linearLayoutChild(0),
                                0),
                        isDisplayed()));
        textView23.check(matches(withText("1. text question*")));

        ViewInteraction imageView2 = onView(
                allOf(withId(R.id.open_btn),
                        childAtPosition(
                                linearLayoutChild(0),
                                1),
                        isDisplayed()));
        imageView2.check(matches(isDisplayed()));

        ViewInteraction textView24 = onView(
                allOf(withId(R.id.question_tv), withText("8. date*"),
                        childAtPosition(
                                linearLayoutChild(0),
                                0),
                        isDisplayed()));
        textView24.check(matches(withText("8. date*")));

        ViewInteraction imageView3 = onView(
                allOf(withId(R.id.open_btn),
                        childAtPosition(
                                linearLayoutChild(0),
                                1),
                        isDisplayed()));
        imageView3.check(matches(isDisplayed()));

        ViewInteraction textView25 = onView(
                allOf(withId(R.id.question_tv), withText("2. option*"),
                        childAtPosition(
                                linearLayoutChild(0),
                                0),
                        isDisplayed()));
        textView25.check(matches(withText("2. option*")));

        ViewInteraction imageView4 = onView(
                allOf(withId(R.id.open_btn),
                        childAtPosition(
                                linearLayoutChild(0),
                                1),
                        isDisplayed()));
        imageView4.check(matches(isDisplayed()));

        ViewInteraction sendButton = onView(
                allOf(childAtPosition(
                        withParent(withId(R.id.pager)),
                        5),
                        isDisplayed()));
        sendButton.check(matches(not(isEnabled())));
    }

    @NonNull
    private Matcher<View> getRowCell(int position, int parentPosition) {
        return childAtPosition(
                childAtPosition(IsInstanceOf.<View>instanceOf(android.widget.TableLayout.class),
                        parentPosition),
                position);
    }

    private ViewInteraction radioButtonWithText(String text, int childPosition) {
        return onView(allOf(childAtPosition(linearLayoutChild(1), childPosition),
                withText(text)));
    }

    private ViewInteraction findQuestionTitle(String questionText) {
        return onView(allOf(withId(R.id.question_tv), withText(questionText),
                childAtPosition(linearLayoutChild(0), 0)));
    }

    @NonNull
    private Matcher<View> questionListItem(int position) {
        return childAtPosition(childAtPosition(withId(R.id.question_list), position), 1);
    }

    @NonNull
    private Matcher<View> linearLayoutChild(int position) {
        return childAtPosition(IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                position);
    }

    static void addExecutionDelay(int millis) {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
