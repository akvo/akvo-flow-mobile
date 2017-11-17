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

package org.akvo.flow.activity.form;

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
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.SurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
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
import org.akvo.flow.ui.view.QuestionGroupTab;
import org.akvo.flow.ui.view.QuestionHeaderView;
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
import static org.akvo.flow.activity.ToolBarTitleSubtitleMatcher.withToolbarSubtitle;
import static org.akvo.flow.activity.ToolBarTitleSubtitleMatcher.withToolbarTitle;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.tests.R.raw.all_questions_form;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNot.not;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class FormActivityTest {

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
        survey = installer.installSurvey(all_questions_form, InstrumentationRegistry.getContext());
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
    public void viewEmptySurveyTest() {

        //make sure everything is loaded
        addExecutionDelay(5000);
        verifyToolBar();

        List<QuestionGroup> questionGroups = survey.getQuestionGroups();
        List<Question> mandatoryQuestions = new ArrayList<>();
        for (int i = 0; i < questionGroups.size(); i++) {
            QuestionGroup group = questionGroups.get(i);
            verifyGroup(mandatoryQuestions, i, group);
        }

        verifySubmitTab(questionGroups, mandatoryQuestions);
    }

    private void verifyGroup(List<Question> mandatoryQuestions, int i, QuestionGroup group) {
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
        verifyNextButton(group);
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
        if (mandatoryQuestions.isEmpty()) {
            ViewInteraction submitTabHeader = onView(withId(R.id.submit_tab_header));
            submitTabHeader.check(matches(withText(R.string.error_empty_form)));
        } else {
            ViewInteraction submitTabHeader = onView(withId(R.id.submit_tab_header));
            submitTabHeader.check(matches(withText(R.string.error_responses)));
            verifyErrorFields(mandatoryQuestions);
        }
        ViewInteraction submitButton = onView(withId(R.id.submit_tab_button));
        submitButton.check(matches(
                allOf(isDisplayed(), withText(R.string.submitbutton), not(isEnabled()))));
    }

    private void verifyErrorFields(List<Question> mandatoryQuestions) {
        for (Question question : mandatoryQuestions) {
            verifyQuestionErrorHeader(question);
            verifyQuestionErrorTip(question);
            verifyQuestionErrorEditButton(question);
        }
    }

    private void verifyQuestionErrorEditButton(Question question) {
        ViewInteraction questionHelpTip = onView(allOf(withId(R.id.invalid_question_open_btn),
                withQuestionViewParent(question, QuestionHeaderView.class)));
        questionHelpTip.check(matches(allOf(isDisplayed(), isEnabled())));
    }

    private void verifyQuestionErrorTip(Question question) {
        if (question.getHelpTypeCount() > 0) {
            ViewInteraction questionHelpTip = onView(allOf(withId(R.id.tip_ib),
                    withQuestionViewParent(question, QuestionHeaderView.class)));
            questionHelpTip.check(matches(allOf(isDisplayed(), isEnabled())));
        }
    }

    private void verifyQuestionErrorHeader(Question question) {
        String questionHeader = getQuestionHeader(question);
        ViewInteraction questionHeaderView = onView(allOf(withId(R.id.question_tv),
                withQuestionViewParent(question, QuestionHeaderView.class)));
        questionHeaderView.check(matches(allOf(isDisplayed(), withText(questionHeader))));
    }

    private void verifyQuestionDisplayed(Question question, int questionPosition) {
        String questionHeader = getQuestionHeader(question);
        verifyQuestionHeader(questionHeader);
        verifyHelpTip(question);
        verifyQuestionView(question, questionPosition);
    }

    private void verifyNextButton(QuestionGroup group) {
        ViewInteraction nextButton = onView(
                allOf(withId(R.id.next_btn), withQuestionGroupViewParent(group)));
        nextButton.check(matches(allOf(isEnabled(), withText(R.string.nextbutton))));
    }

    private void verifyHelpTip(Question question) {
        if (question.getHelpTypeCount() > 0) {
            ViewInteraction questionHelpTip = onView(
                    allOf(withId(R.id.tip_ib), withQuestionViewParent(question, QuestionView.class)));
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
                withQuestionViewParent(question, CaddisflyQuestionView.class))).perform(scrollTo());
        caddisflyButton.check(matches(allOf(isDisplayed(), isEnabled(), withText(R.string.caddisfly_test))));
    }

    private void verifySignatureQuestionView(Question question) {
        ViewInteraction signatureButton = onView(allOf(withId(R.id.sign_btn),
                withQuestionViewParent(question, SignatureQuestionView.class))).perform(scrollTo());
        signatureButton.check(matches(
                allOf(isDisplayed(), isEnabled(), withText(R.string.add_signature))));
    }

    private void verifyGeoShapeQuestionView(Question question) {
        ViewInteraction captureShapeButton = onView(allOf(withId(R.id.capture_shape_btn),
                withQuestionViewParent(question, GeoshapeQuestionView.class))).perform(scrollTo());
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
                    withQuestionViewParent(question, BarcodeQuestionViewMultiple.class)))
                    .perform(scrollTo());
            barcodeInput.check(matches(withHint(R.string.type_code)));
            barcodeInput.check(matches(withText("")));
            barcodeInput.check(matches(isDisplayed()));

            ViewInteraction addButton = onView(allOf(withId(R.id.barcode_add_btn),
                    withQuestionViewParent(question, BarcodeQuestionViewMultiple.class)));
            addButton.perform(scrollTo());
            addButton.check(matches(allOf(isDisplayed(), not(isEnabled()))));

            ViewInteraction barcodeManualSeparator = onView(
                    allOf(withId(R.id.barcode_manual_input_separator),
                            withQuestionViewParent(question, BarcodeQuestionViewMultiple.class)))
                    .perform(scrollTo());
            barcodeManualSeparator.check(matches(withText(R.string.or)));
            barcodeManualSeparator.check(matches(isDisplayed()));
        }

        ViewInteraction scanButton = onView(allOf(withId(R.id.scan_btn),
                withQuestionViewParent(question, BarcodeQuestionViewMultiple.class))).perform(scrollTo());
        scanButton
                .check(matches(allOf(isDisplayed(), isEnabled(), withText(R.string.scanbarcode))));
    }

    private void verifySingleBarcodeQuestionView(Question question) {
        boolean manualInputEnabled = !question.isLocked();
        if (manualInputEnabled) {
            ViewInteraction barcodeInput = onView(allOf(withId(R.id.barcode_input),
                    withQuestionViewParent(question, BarcodeQuestionViewSingle.class))).perform(scrollTo());
            barcodeInput.check(matches(withHint(R.string.type_code)));
            barcodeInput.check(matches(withText("")));
            barcodeInput.check(matches(isDisplayed()));

            ViewInteraction barcodeManualSeparator = onView(
                    allOf(withId(R.id.barcode_manual_input_separator),
                            withQuestionViewParent(question, BarcodeQuestionViewSingle.class)))
                    .perform(scrollTo());
            barcodeManualSeparator.check(matches(withText(R.string.or)));
            barcodeManualSeparator.check(matches(isDisplayed()));
        }

        ViewInteraction scanButton = onView(allOf(withId(R.id.scan_btn),
                withQuestionViewParent(question, BarcodeQuestionViewSingle.class))).perform(scrollTo());
        scanButton
                .check(matches(allOf(isDisplayed(), isEnabled(), withText(R.string.scanbarcode))));
    }

    private void verifyDateQuestionView(Question question) {
        ViewInteraction dateInput = onView(
                allOf(withId(R.id.date_et), withQuestionViewParent(question, DateQuestionView.class)))
                .perform(scrollTo());
        dateInput.check(matches(isDisplayed()));
        dateInput.check(matches(withText("")));

        ViewInteraction dateButton = onView(
                allOf(withId(R.id.date_btn), withQuestionViewParent(question, DateQuestionView.class)))
                .perform(scrollTo());
        dateButton.check(matches(isDisplayed()));
        dateButton.check(matches(isEnabled()));
        dateButton.check(matches(withText(R.string.pickdate)));
    }

    private void verifyPhotoQuestionView(Question question) {
        ViewInteraction photoButton = onView(
                allOf(withId(R.id.media_btn), withQuestionViewParent(question, MediaQuestionView.class)))
                .perform(scrollTo());
        photoButton.check(matches(withText(R.string.takephoto)));
        photoButton.check(matches(isDisplayed()));
    }

    private void verifyVideoQuestionView(Question question) {
        ViewInteraction videoButton = onView(
                allOf(withId(R.id.media_btn), withQuestionViewParent(question, MediaQuestionView.class)))
                .perform(scrollTo());
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
                allOf(withId(R.id.acc_tv), withQuestionViewParent(question, GeoQuestionView.class)));
        accuracyLabel.perform(scrollTo());
        accuracyLabel.check(matches(isDisplayed()));
        accuracyLabel.check(matches(withText(R.string.geo_location_accuracy_default)));

        ViewInteraction geoButton = onView(
                allOf(withId(R.id.geo_btn), withQuestionViewParent(question, GeoQuestionView.class)));
        geoButton.perform(scrollTo());
        geoButton.check(matches(withText(R.string.getgeo)));
        geoButton.check(matches(isEnabled()));
        geoButton.check(matches(isDisplayed()));
    }

    private void verifyGeoInput(Question question, int resId) {
        boolean isManualInputEnabled = !question.isLocked();
        ViewInteraction input = onView(
                allOf(withId(resId), withQuestionViewParent(question, GeoQuestionView.class)));
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
                allOf(withText(resourceId), withQuestionViewParent(question, GeoQuestionView.class)));
        label.perform(scrollTo());
        label.check(matches(isDisplayed()));
    }

    private void verifyCascadeQuestionView(Question question) {
        List<Level> levels = question.getLevels();
        if (levels != null && levels.size() > 0) {
            Level level = levels.get(0);
            ViewInteraction firstLevelCascadeNumber = onView(
                    allOf(withId(R.id.cascade_level_number),
                            withQuestionViewParent(question, CascadeQuestionView.class)));
            firstLevelCascadeNumber.perform(scrollTo());
            firstLevelCascadeNumber.check(matches(isDisplayed()));
            firstLevelCascadeNumber.check(matches(withText(level.getText())));

            ViewInteraction firstLevelCascadeDescription = onView(
                    allOf(withId(R.id.cascade_spinner_item_text),
                            withQuestionViewParent(question, CascadeQuestionView.class)));
            firstLevelCascadeDescription.perform(scrollTo());
            firstLevelCascadeDescription.check(matches(isDisplayed()));
            firstLevelCascadeDescription.check(matches(withText(R.string.select)));

            ViewInteraction cascadeFirstLevelSpinner = onView(
                    allOf(withId(R.id.cascade_level_spinner),
                            withQuestionViewParent(question, CascadeQuestionView.class)));
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
        ViewInteraction freeTextQuestionInput = onView(
                allOf(withId(R.id.input_et), withQuestionViewParent(question, FreetextQuestionView.class)));
        freeTextQuestionInput.perform(scrollTo());
        freeTextQuestionInput.check(matches(withText("")));
        freeTextQuestionInput.perform(click());
        freeTextQuestionInput.perform(closeSoftKeyboard());
        if (question.isDoubleEntry()) {
            ViewInteraction repeatTextView = onView(allOf(withId(R.id.double_entry_title),
                    withQuestionViewParent(question, FreetextQuestionView.class)));
            repeatTextView.perform(scrollTo());
            repeatTextView.check(matches(withText(R.string.repeat_answer)));
            repeatTextView.check(matches(isDisplayed()));

            ViewInteraction repeatInput = onView(allOf(withId(R.id.double_entry_et),
                    withQuestionViewParent(question, FreetextQuestionView.class)));
            repeatInput.perform(scrollTo());
            repeatInput.check(matches(isDisplayed()));
            repeatInput.check(matches(withText("")));
            repeatInput.perform(click());
            repeatInput.perform(closeSoftKeyboard());
        }
    }

    @NonNull
    private <T extends View> Matcher<View> withQuestionViewParent(Question question,
            Class<T> parentClass) {
        return isDescendantOfA(allOf(IsInstanceOf.<View>instanceOf(parentClass),
                withTagValue(is((Object) question.getId()))));
    }

    @NonNull
    private <T extends View> Matcher<View> withQuestionGroupViewParent(QuestionGroup questionGroup) {
        return isDescendantOfA(allOf(IsInstanceOf.<View>instanceOf(QuestionGroupTab.class),
                withTagValue(is((Object) questionGroup.getOrder()))));
    }

    private void verifyOptionQuestionView(Question question, int questionPosition) {
        List<Option> options = question.getOptions();
        if (options != null) {
            if (question.isAllowMultiple()) {
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
    private Matcher<View> linearLayoutChild(int position) {
        return childAtPosition(IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                position);
    }

    private static void addExecutionDelay(int millis) {
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
