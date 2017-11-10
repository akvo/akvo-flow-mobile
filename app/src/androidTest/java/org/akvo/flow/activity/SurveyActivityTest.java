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
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.ViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import org.akvo.flow.R;
import org.akvo.flow.activity.testhelper.SurveyInstaller;
import org.akvo.flow.activity.testhelper.SurveyRequisite;
import org.akvo.flow.domain.Level;
import org.akvo.flow.domain.Option;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.ui.view.CaddisflyQuestionView;
import org.akvo.flow.ui.view.CascadeQuestionView;
import org.akvo.flow.ui.view.DateQuestionView;
import org.akvo.flow.ui.view.FreetextQuestionView;
import org.akvo.flow.ui.view.GeoshapeQuestionView;
import org.akvo.flow.ui.view.MediaQuestionView;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.ui.view.barcode.BarcodeQuestionViewMultiple;
import org.akvo.flow.ui.view.barcode.BarcodeQuestionViewSingle;
import org.akvo.flow.ui.view.geolocation.GeoQuestionView;
import org.akvo.flow.ui.view.signature.SignatureQuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.isFocusable;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withTagValue;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.activity.ChildPositionMatcher.childAtPosition;
import static org.akvo.flow.activity.Constants.TEST_FORM_SURVEY_INSTANCE_ID;
import static org.akvo.flow.activity.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.ToolBarTitleSubtitleMatcher.withToolbarSubtitle;
import static org.akvo.flow.activity.ToolBarTitleSubtitleMatcher.withToolbarTitle;
import static org.akvo.flow.tests.R.raw.test_form;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNot.not;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SurveyActivityTest {

    private static SurveyInstaller installer;
    private static Survey survey;

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
        survey = installer.installSurvey(test_form, InstrumentationRegistry.getContext());
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
        verifyToolBar();

        List<QuestionGroup> questionGroups = survey.getQuestionGroups();
        List<Question> mandatoryQuestions = new ArrayList<>();
        for (int i = 0; i < questionGroups.size(); i++) {
            QuestionGroup group = questionGroups.get(i);
            //select the tab
            ViewInteraction tab = onView(childAtPosition(childAtPosition(withId(R.id.tabs), 0), i));
            tab.perform(click());
            tab.check(matches(hasDescendant(withText(group.getHeading()))));
            List<Question> questions = group.getQuestions();
            for (int j = 0; j < questions.size(); j++) {
                Question question = questions.get(j);
                if (question.isMandatory()) {
                    mandatoryQuestions.add(question);
                }
                verifyQuestionDisplayed(question, j);
            }
        }

        verifySubmitTab(questionGroups, mandatoryQuestions);

        /**
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
         sendButton.check(matches(not(isEnabled()))); **/
    }

    private void verifyToolBar() {
        onView(withId(R.id.toolbar)).check(matches(withToolbarTitle(is(survey.getName()))));
        onView(withId(R.id.toolbar))
                .check(matches(withToolbarSubtitle(is("v " + survey.getVersion()))));
    }

    private void verifySubmitTab(List<QuestionGroup> questionGroups,
            List<Question> mandatoryQuestions) {
        ViewInteraction submitTab = onView(
                childAtPosition(childAtPosition(withId(R.id.tabs), 0), questionGroups.size()));
        submitTab.perform(click());
        submitTab.check(matches(isDisplayed()));
        submitTab.check(matches(hasDescendant(withText(R.string.submitbutton))));
    }

    private void verifyQuestionDisplayed(Question question, int questionPosition) {
        String questionHeader = getQuestionHeader(question);
        verifyQuestionHeader(questionHeader);
        verifyHelpTip(question);
        verifyQuestionView(question, questionPosition);
        verifyNextButton();
    }

    private void verifyNextButton() {
        ViewInteraction nextButton = onView(withId(R.id.next_btn));
        nextButton.check(matches(allOf(isEnabled(), withText(R.string.nextbutton))));
    }

    private void verifyHelpTip(Question question) {
        if (question.getHelpTypeCount() > 0) {
            ViewInteraction questionHelpTip = onView(
                    allOf(withId(R.id.tip_ib), withViewParent(question, QuestionView.class)));
            questionHelpTip.perform(scrollTo());
            questionHelpTip.check(matches(isDisplayed()));
            questionHelpTip.check(matches(isEnabled()));
        }
    }

    private void verifyQuestionView(Question question, int questionPosition) {
        switch (question.getType()) {
            case ConstantUtil.FREE_QUESTION_TYPE:
                verifyFreeTextQuestionView(question);
                break;
            case ConstantUtil.OPTION_QUESTION_TYPE:
                verifyOptionQuestionView(question, questionPosition);
                break;
            case ConstantUtil.CASCADE_QUESTION_TYPE:
                verifyCascadeQuestionView(question);
                break;
            case ConstantUtil.GEO_QUESTION_TYPE:
                verifyGeoQuestionView(question);
                break;
            case ConstantUtil.PHOTO_QUESTION_TYPE:
                verifyPhotoQuestionView(question);
                break;
            case ConstantUtil.VIDEO_QUESTION_TYPE:
                verifyVideoQuestionView(question);
                break;
            case ConstantUtil.DATE_QUESTION_TYPE:
                verifyDateQuestionView(question);
                break;
            case ConstantUtil.SCAN_QUESTION_TYPE:
                verifyBarcodeQuestionView(question);
                break;
            case ConstantUtil.GEOSHAPE_QUESTION_TYPE:
                verifyGeoShapeQuestionView(question);
                break;
            case ConstantUtil.SIGNATURE_QUESTION_TYPE:
                verifySignatureQuestionView(question);
                break;
            case ConstantUtil.CADDISFLY_QUESTION_TYPE:
                verifyCaddisflyQuestionView(question);
                break;
            default:
                break;
        }
    }

    private void verifyCaddisflyQuestionView(Question question) {
        ViewInteraction caddisflyButton = onView(allOf(withId(R.id.caddisfly_button),
                withViewParent(question, CaddisflyQuestionView.class))).perform(scrollTo());
        caddisflyButton.check(matches(
                allOf(isDisplayed(), isEnabled(), withText(R.string.caddisfly_test))));
    }

    private void verifySignatureQuestionView(Question question) {
        ViewInteraction signatureButton = onView(allOf(withId(R.id.sign_btn),
                withViewParent(question, SignatureQuestionView.class))).perform(scrollTo());
        signatureButton.check(matches(
                allOf(isDisplayed(), isEnabled(), withText(R.string.add_signature))));
    }

    private void verifyGeoShapeQuestionView(Question question) {
        ViewInteraction captureShapeButton = onView(allOf(withId(R.id.capture_shape_btn),
                withViewParent(question, GeoshapeQuestionView.class))).perform(scrollTo());
        captureShapeButton.check(matches(
                allOf(isDisplayed(), isEnabled(), withText(R.string.capture_shape))));
    }

    private void verifyBarcodeQuestionView(Question question) {
        if (question.isAllowMultiple()) {
            verifyMultipleBarcodeQuestionView(question);
        } else {
            verifySingleBarcodeQuestionView(question);
        }
    }

    private void verifyMultipleBarcodeQuestionView(Question question) {
        boolean manualInputEnabled = !question.isLocked();
        if (manualInputEnabled) {
            ViewInteraction barcodeInput = onView(allOf(withId(R.id.barcode_input),
                    withViewParent(question, BarcodeQuestionViewMultiple.class))).perform(scrollTo());
            barcodeInput.check(matches(withHint(R.string.type_code)));
            barcodeInput.check(matches(withText("")));
            barcodeInput.check(matches(isDisplayed()));

            ViewInteraction addButton = onView(allOf(withId(R.id.barcode_add_btn),
                    withViewParent(question, BarcodeQuestionViewMultiple.class)));
            addButton.perform(scrollTo());
            addButton.check(matches(allOf(isDisplayed(), not(isEnabled()))));

            ViewInteraction barcodeManualSeparator = onView(
                    allOf(withId(R.id.barcode_manual_input_separator),
                            withViewParent(question, BarcodeQuestionViewMultiple.class)))
                    .perform(scrollTo());
            barcodeManualSeparator.check(matches(withText(R.string.or)));
            barcodeManualSeparator.check(matches(isDisplayed()));
        }

        ViewInteraction scanButton = onView(allOf(withId(R.id.scan_btn),
                withViewParent(question, BarcodeQuestionViewMultiple.class))).perform(scrollTo());
        scanButton
                .check(matches(allOf(isDisplayed(), isEnabled(), withText(R.string.scanbarcode))));
    }

    private void verifySingleBarcodeQuestionView(Question question) {
        boolean manualInputEnabled = !question.isLocked();
        if (manualInputEnabled) {
            ViewInteraction barcodeInput = onView(allOf(withId(R.id.barcode_input),
                    withViewParent(question, BarcodeQuestionViewSingle.class))).perform(scrollTo());
            barcodeInput.check(matches(withHint(R.string.type_code)));
            barcodeInput.check(matches(withText("")));
            barcodeInput.check(matches(isDisplayed()));

            ViewInteraction barcodeManualSeparator = onView(
                    allOf(withId(R.id.barcode_manual_input_separator),
                            withViewParent(question, BarcodeQuestionViewSingle.class)))
                    .perform(scrollTo());
            barcodeManualSeparator.check(matches(withText(R.string.or)));
            barcodeManualSeparator.check(matches(isDisplayed()));
        }

        ViewInteraction scanButton = onView(allOf(withId(R.id.scan_btn),
                withViewParent(question, BarcodeQuestionViewSingle.class))).perform(scrollTo());
        scanButton
                .check(matches(allOf(isDisplayed(), isEnabled(), withText(R.string.scanbarcode))));
    }

    private void verifyDateQuestionView(Question question) {
        ViewInteraction dateInput = onView(
                allOf(withId(R.id.date_et), withViewParent(question, DateQuestionView.class)))
                .perform(scrollTo());
        dateInput.check(matches(isDisplayed()));
        dateInput.check(matches(withText("")));

        ViewInteraction dateButton = onView(
                allOf(withId(R.id.date_btn), withViewParent(question, DateQuestionView.class)))
                .perform(scrollTo());
        dateButton.check(matches(isDisplayed()));
        dateButton.check(matches(isEnabled()));
        dateButton.check(matches(withText(R.string.pickdate)));
    }

    private void verifyPhotoQuestionView(Question question) {
        ViewInteraction photoButton = onView(
                allOf(withId(R.id.media_btn), withViewParent(question, MediaQuestionView.class)))
                .perform(ViewActions.scrollTo());
        photoButton.check(matches(withText(R.string.takephoto)));
        photoButton.check(matches(isDisplayed()));
    }

    private void verifyVideoQuestionView(Question question) {
        ViewInteraction videoButton = onView(
                allOf(withId(R.id.media_btn), withViewParent(question, MediaQuestionView.class)))
                .perform(ViewActions.scrollTo());
        videoButton.check(matches(withText(R.string.takevideo)));
        videoButton.check(matches(isDisplayed()));
    }

    private void verifyGeoQuestionView(Question question) {
        verifyGeoLabel(question, R.string.lat);
        verifyGeoInput(question, R.id.lat_et);
        verifyGeoLabel(question, R.string.lon);
        verifyGeoInput(question, R.id.lon_et);
        verifyGeoLabel(question, R.string.elevation);
        verifyGeoInput(question, R.id.height_et);

        ViewInteraction accuracyLabel = onView(
                allOf(withId(R.id.acc_tv), withViewParent(question, GeoQuestionView.class)));
        accuracyLabel.perform(scrollTo());
        accuracyLabel.check(matches(isDisplayed()));
        accuracyLabel.check(matches(withText(R.string.geo_location_accuracy_default)));

        ViewInteraction geoButton = onView(
                allOf(withId(R.id.geo_btn), withViewParent(question, GeoQuestionView.class)));
        geoButton.perform(scrollTo());
        geoButton.check(matches(withText(R.string.getgeo)));
        geoButton.check(matches(isEnabled()));
        geoButton.check(matches(isDisplayed()));
    }

    private void verifyGeoInput(Question question, int resId) {
        boolean isManualInputEnabled = !question.isLocked();
        ViewInteraction input = onView(
                allOf(withId(resId), withViewParent(question, GeoQuestionView.class)));
        input.perform(scrollTo());
        input.check(matches(isDisplayed()));
        input.check(matches(withText("")));
        if (isManualInputEnabled) {
            input.check(matches(isFocusable()));
            input.perform(click());
            input.perform(closeSoftKeyboard());
        } else {
            input.check(matches(not(isFocusable())));
        }
    }

    private void verifyGeoLabel(Question question, int resourceId) {
        ViewInteraction label = onView(
                allOf(withText(resourceId), withViewParent(question, GeoQuestionView.class)));
        label.perform(scrollTo());
        label.check(matches(isDisplayed()));
    }

    private void verifyCascadeQuestionView(Question question) {
        List<Level> levels = question.getLevels();
        if (levels != null && levels.size() > 0) {
            Level level = levels.get(0);
            ViewInteraction firstLevelCascadeNumber = onView(
                    allOf(withId(R.id.cascade_level_number),
                            withViewParent(question, CascadeQuestionView.class)));
            firstLevelCascadeNumber.perform(scrollTo());
            firstLevelCascadeNumber.check(matches(isDisplayed()));
            firstLevelCascadeNumber.check(matches(withText(level.getText())));

            ViewInteraction firstLevelCascadeDescription = onView(
                    allOf(withId(R.id.cascade_spinner_item_text),
                            withViewParent(question, CascadeQuestionView.class)));
            firstLevelCascadeDescription.perform(scrollTo());
            firstLevelCascadeDescription.check(matches(isDisplayed()));
            firstLevelCascadeDescription.check(matches(withText(R.string.select)));

            ViewInteraction cascadeFirstLevelSpinner = onView(
                    allOf(withId(R.id.cascade_level_spinner),
                            withViewParent(question, CascadeQuestionView.class)));
            cascadeFirstLevelSpinner.perform(scrollTo());
            cascadeFirstLevelSpinner.check(matches(isDisplayed()));
        }
    }

    @NonNull
    private String getQuestionHeader(Question question) {
        String questionHeader = question.getOrder() + ". " + question.getText();
        if (question.isMandatory()) {
            questionHeader = questionHeader + "*";
        }
        return questionHeader;
    }

    private void verifyQuestionHeader(String questionHeader) {
        ViewInteraction questionTitle = findQuestionTitle(questionHeader);
        questionTitle.perform(scrollTo());
        questionTitle.check(matches(isDisplayed()));
    }

    private void verifyFreeTextQuestionView(Question question) {
        //TODO: add number verifications
        ViewInteraction freeTextQuestionInput = onView(
                allOf(withId(R.id.input_et), withViewParent(question, FreetextQuestionView.class)));
        freeTextQuestionInput.check(matches(withText("")));
        freeTextQuestionInput.perform(click());
        freeTextQuestionInput.perform(closeSoftKeyboard());
        if (question.isDoubleEntry()) {
            ViewInteraction repeatTextView = onView(allOf(withId(R.id.double_entry_title),
                    withViewParent(question, FreetextQuestionView.class)));
            repeatTextView.perform(scrollTo());
            repeatTextView.check(matches(withText(R.string.repeat_answer)));
            repeatTextView.check(matches(isDisplayed()));

            ViewInteraction repeatInput = onView(allOf(withId(R.id.double_entry_et),
                    withViewParent(question, FreetextQuestionView.class)));
            repeatInput.perform(scrollTo());
            repeatInput.check(matches(isDisplayed()));
            repeatInput.check(matches(withText("")));
            repeatInput.perform(click());
            repeatInput.perform(closeSoftKeyboard());
        }
    }

    @NonNull
    private <T extends View> Matcher<View> withViewParent(Question question,
            Class<T> parentClass) {
        return isDescendantOfA(allOf(IsInstanceOf.<View>instanceOf(parentClass),
                withTagValue(is((Object) question.getId()))));
    }

    private void verifyOptionQuestionView(Question question, int questionPosition) {
        List<Option> options = question.getOptions();
        if (options != null) {
            if (question.isAllowMultiple()) {
                //checkboxes
                for (int i = 0; i < options.size(); i++) {
                    Option option = options.get(i);
                    ViewInteraction checkBox = checkBoxWithText(option, i, questionPosition);
                    checkBox.perform(scrollTo());
                    checkBox.check(matches(isDisplayed()));
                }
            } else {
                for (int i = 0; i < options.size(); i++) {
                    Option option = options.get(i);
                    ViewInteraction optionView = radioButtonWithText(option, i);
                    optionView.perform(scrollTo());
                    optionView.check(matches(isDisplayed()));
                }
            }
        }
    }

    private ViewInteraction checkBoxWithText(Option option, int optionPosition,
            int questionPosition) {
        if (option.isOther()) {
            return onView(
                    allOf(childAtPosition(childAtPosition(withId(R.id.question_list),
                            questionPosition), optionPosition + 1), withText(R.string.othertext)));
        }
        return onView(
                allOf(childAtPosition(childAtPosition(withId(R.id.question_list), questionPosition),
                        optionPosition + 1), withText(option.getText())));
    }

    @NonNull
    private Matcher<View> getRowCell(int position, int parentPosition) {
        return childAtPosition(
                childAtPosition(IsInstanceOf.<View>instanceOf(android.widget.TableLayout.class),
                        parentPosition),
                position);
    }

    private ViewInteraction radioButtonWithText(Option option, int childPosition) {
        if (option.isOther()) {
            return onView(allOf(childAtPosition(linearLayoutChild(1), childPosition),
                    withText(R.string.othertext)));
        }
        return onView(allOf(childAtPosition(linearLayoutChild(1), childPosition),
                withText(option.getText())));
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
