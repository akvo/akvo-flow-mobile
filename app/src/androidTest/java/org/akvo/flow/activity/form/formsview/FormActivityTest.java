/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.activity.form.formsview;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
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
import org.akvo.flow.ui.view.GeoshapeQuestionView;
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
import static org.akvo.flow.activity.form.FormActivityTestUtil.getDateButton;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getDateEditText;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getDoubleEntryInput;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFreeTextInput;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getGeoButton;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getMediaButton;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getOptionView;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getQuestionHeader;
import static org.akvo.flow.activity.form.FormActivityTestUtil.selectAndVerifyTab;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyAccuracyLabel;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyDoubleEntryTitle;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyGeoLabel;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyHelpTip;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyQuestionHeader;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyToolBar;
import static org.akvo.flow.activity.form.FormActivityTestUtil.withQuestionViewParent;
import static org.akvo.flow.tests.R.raw.all_questions_form;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNot.not;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class FormActivityTest {

    private static final String FORM_TITLE = "Test form";
    private static SurveyInstaller installer;
    private static Survey survey;

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            return getFormActivityIntent(155852013L, "156792013", FORM_TITLE, 0L, false);
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
    public void testViewNonFilledForm() {
        verifyToolBar(survey.getName(), survey.getVersion());

        List<QuestionGroup> questionGroups = survey.getQuestionGroups();
        List<Question> mandatoryQuestions = new ArrayList<>();
        for (int i = 0; i < questionGroups.size(); i++) {
            QuestionGroup group = questionGroups.get(i);
            verifyGroup(mandatoryQuestions, i, group);
        }

        verifySubmitTab(questionGroups, mandatoryQuestions);
    }

    private void verifyGroup(List<Question> mandatoryQuestions, int groupPosition,
            QuestionGroup group) {
        selectAndVerifyTab(groupPosition, group);
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
        verifyQuestionHeader(question);
        verifyHelpTip(question);
        verifyQuestionView(question, questionPosition);
    }

    private void verifyNextButton(QuestionGroup group) {
        ViewInteraction nextButton = onView(
                allOf(withId(R.id.next_btn), withQuestionGroupViewParent(group)));
        nextButton.check(matches(allOf(isEnabled(), withText(R.string.nextbutton))));
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
        caddisflyButton.check(matches(
                allOf(isDisplayed(), isEnabled(), withText(R.string.caddisfly_test))));
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
            verifyBarcodeManualInput(question, BarcodeQuestionViewMultiple.class);
            verifyBarcodeAddButton(question);
            verifyBarcodeManualInputSeparator(question, BarcodeQuestionViewMultiple.class);
        }
        verifyBarcodeScanButton(question, BarcodeQuestionViewMultiple.class);
    }

    private void verifyBarcodeAddButton(Question question) {
        ViewInteraction addButton = onView(allOf(withId(R.id.barcode_add_btn),
                withQuestionViewParent(question, BarcodeQuestionViewMultiple.class)));
        addButton.perform(scrollTo());
        addButton.check(matches(allOf(isDisplayed(), not(isEnabled()))));
    }

    private void verifySingleBarcodeQuestionView(Question question) {
        boolean manualInputEnabled = !question.isLocked();
        if (manualInputEnabled) {
            verifyBarcodeManualInput(question, BarcodeQuestionViewSingle.class);
            verifyBarcodeManualInputSeparator(question, BarcodeQuestionViewSingle.class);
        }
        verifyBarcodeScanButton(question, BarcodeQuestionViewSingle.class);
    }

    private <T extends QuestionView> void verifyBarcodeScanButton(Question question,
            Class<T> parentClass) {
        ViewInteraction scanButton = onView(allOf(withId(R.id.scan_btn),
                withQuestionViewParent(question, parentClass))).perform(scrollTo());
        scanButton
                .check(matches(allOf(isDisplayed(), isEnabled(), withText(R.string.scanbarcode))));
    }

    private <T extends QuestionView> void verifyBarcodeManualInputSeparator(Question question,
            Class<T> parentClass) {
        ViewInteraction manualSeparator = onView(
                allOf(withId(R.id.barcode_manual_input_separator),
                        withQuestionViewParent(question, parentClass))).perform(scrollTo());
        manualSeparator.check(matches(withText(R.string.or)));
        manualSeparator.check(matches(isDisplayed()));
    }

    private <T extends QuestionView> void verifyBarcodeManualInput(Question question,
            Class<T> parentClass) {
        ViewInteraction barcodeInput = onView(allOf(withId(R.id.barcode_input),
                withQuestionViewParent(question, parentClass))).perform(scrollTo());
        barcodeInput.check(matches(withHint(R.string.type_code)));
        barcodeInput.check(matches(withText("")));
        barcodeInput.check(matches(isDisplayed()));
    }

    private void verifyDateQuestionView(Question question) {
        verifyDateInput(question);
        verifyDateButton(question);
    }

    private void verifyDateButton(Question question) {
        ViewInteraction dateButton = getDateButton(question);
        dateButton.check(matches(isDisplayed()));
        dateButton.check(matches(isEnabled()));
        dateButton.check(matches(withText(R.string.pickdate)));
    }

    private void verifyDateInput(Question question) {
        ViewInteraction dateInput = getDateEditText(question);
        dateInput.check(matches(isDisplayed()));
        dateInput.check(matches(withText("")));
    }

    private void verifyPhotoQuestionView(Question question) {
        verifyMediaButton(question, R.string.takephoto);
    }

    private void verifyMediaButton(Question question, int textResId) {
        ViewInteraction mediaButton = getMediaButton(question);
        mediaButton.check(matches(withText(textResId)));
        mediaButton.check(matches(isDisplayed()));
    }

    private void verifyVideoQuestionView(Question question) {
        verifyMediaButton(question, R.string.takevideo);
    }

    private void verifyGeoQuestionView(Question question) {
        verifyGeoLabel(question, R.string.lat);
        verifyGeoLabel(question, R.string.lon);
        verifyGeoLabel(question, R.string.elevation);
        verifyGeoInput(question, R.id.lat_et);
        verifyGeoInput(question, R.id.lon_et);
        verifyGeoInput(question, R.id.height_et);

        verifyAccuracyLabel(question);

        verifyGeoButton(question);
    }

    private void verifyGeoButton(Question question) {
        ViewInteraction geoButton = getGeoButton(question);
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



    private void verifyCascadeQuestionView(Question question) {
        List<Level> levels = question.getLevels();
        if (levels != null && levels.size() > 0) {
            Level level = levels.get(0);
            verifyCascadeFirstLevelNumber(question, level);
            verifyCascadeLevelSpinnerTitle(question);
            verifyCascadeFirstLevelSpinner(question);
        }
    }

    private void verifyCascadeFirstLevelSpinner(Question question) {
        ViewInteraction cascadeFirstLevelSpinner = onView(
                allOf(withId(R.id.cascade_level_spinner),
                        withQuestionViewParent(question, CascadeQuestionView.class)));
        cascadeFirstLevelSpinner.perform(scrollTo());
        cascadeFirstLevelSpinner.check(matches(isDisplayed()));
    }

    private void verifyCascadeLevelSpinnerTitle(Question question) {
        ViewInteraction firstLevelCascadeDescription = onView(
                allOf(withId(R.id.cascade_spinner_item_text),
                        withQuestionViewParent(question, CascadeQuestionView.class)));
        firstLevelCascadeDescription.perform(scrollTo());
        firstLevelCascadeDescription.check(matches(isDisplayed()));
        firstLevelCascadeDescription.check(matches(withText(R.string.select)));
    }

    private void verifyCascadeFirstLevelNumber(Question question, Level level) {
        ViewInteraction firstLevelCascadeNumber = onView(
                allOf(withId(R.id.cascade_level_number),
                        withQuestionViewParent(question, CascadeQuestionView.class)));
        firstLevelCascadeNumber.perform(scrollTo());
        firstLevelCascadeNumber.check(matches(isDisplayed()));
        firstLevelCascadeNumber.check(matches(withText(level.getText())));
    }

    private void verifyFreeTextQuestionView(Question question) {
        verifyFreeTextInput(question);
        if (question.isDoubleEntry()) {
            verifyDoubleEntryTitle(question);
            verifyDoubleEntryInput(question);
        }
    }

    private void verifyDoubleEntryInput(Question question) {
        ViewInteraction repeatInput = getDoubleEntryInput(question);
        repeatInput.check(matches(isDisplayed()));
        repeatInput.check(matches(withText("")));
        repeatInput.perform(click());
        repeatInput.perform(closeSoftKeyboard());
    }

    private void verifyFreeTextInput(Question question) {
        ViewInteraction freeTextQuestionInput = getFreeTextInput(question);
        freeTextQuestionInput.check(matches(withText("")));
        freeTextQuestionInput.perform(click());
        freeTextQuestionInput.perform(closeSoftKeyboard());
    }

    @NonNull
    private <T extends View> Matcher<View> withQuestionGroupViewParent(
            QuestionGroup questionGroup) {
        return isDescendantOfA(allOf(IsInstanceOf.<View>instanceOf(QuestionGroupTab.class),
                withTagValue(is((Object) questionGroup.getOrder()))));
    }

    private void verifyOptionQuestionView(Question question, int questionPosition) {
        List<Option> options = question.getOptions();
        if (options != null) {
                for (int i = 0; i < options.size(); i++) {
                    Option option = options.get(i);
                    ViewInteraction optionView = getOptionView(question, questionPosition, i, option);
                    optionView.perform(scrollTo());
                    optionView.check(matches(isDisplayed()));
                }
        }
    }
}
