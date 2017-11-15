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

package org.akvo.flow.activity.form.filledformsview;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.ViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.util.Pair;
import android.test.suitebuilder.annotation.LargeTest;
import android.text.TextUtils;
import android.view.View;

import org.akvo.flow.R;
import org.akvo.flow.activity.Constants;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.SurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.akvo.flow.domain.Level;
import org.akvo.flow.domain.Option;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.domain.response.value.CascadeNode;
import org.akvo.flow.domain.response.value.Signature;
import org.akvo.flow.serialization.response.value.CascadeValue;
import org.akvo.flow.serialization.response.value.OptionValue;
import org.akvo.flow.serialization.response.value.SignatureValue;
import org.akvo.flow.ui.view.CaddisflyQuestionView;
import org.akvo.flow.ui.view.DateQuestionView;
import org.akvo.flow.ui.view.FreetextQuestionView;
import org.akvo.flow.ui.view.GeoshapeQuestionView;
import org.akvo.flow.ui.view.MediaQuestionView;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.ui.view.barcode.BarcodeQuestionViewReadOnly;
import org.akvo.flow.ui.view.geolocation.GeoQuestionView;
import org.akvo.flow.ui.view.signature.SignatureQuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.isFocusable;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static android.support.test.espresso.matcher.ViewMatchers.withTagValue;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.activity.ChildPositionMatcher.childAtPosition;
import static org.akvo.flow.activity.ToolBarTitleSubtitleMatcher.withToolbarSubtitle;
import static org.akvo.flow.activity.ToolBarTitleSubtitleMatcher.withToolbarTitle;
import static org.akvo.flow.tests.R.raw.all_questions_form;
import static org.akvo.flow.tests.R.raw.data;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNot.not;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class FormActivityReadOnlyTest {

    private Map<String, QuestionResponse> responseMap;

    private Survey survey;

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Context targetContext = InstrumentationRegistry.getTargetContext();
            SurveyRequisite.setRequisites(targetContext);
            SurveyInstaller installer = new SurveyInstaller(targetContext);
            survey = installer
                    .installSurvey(all_questions_form, InstrumentationRegistry.getContext());
            Pair<Long, Map<String, QuestionResponse>> dataPointFromFile = installer
                    .createDataPointFromFile(survey.getSurveyGroup(),
                            InstrumentationRegistry.getContext(), data);
            long id = dataPointFromFile.first;
            responseMap = dataPointFromFile.second;
            Context activityContext = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext();
            Intent result = new Intent(activityContext, FormActivity.class);
            result.putExtra(ConstantUtil.READ_ONLY_EXTRA, true);
            result.putExtra(ConstantUtil.FORM_ID_EXTRA, "156792013");
            result.putExtra(ConstantUtil.RESPONDENT_ID_EXTRA, id);
            result.putExtra(ConstantUtil.SURVEY_GROUP_EXTRA,
                    new SurveyGroup(155852013L, "Test form", null, false));
            result.putExtra(ConstantUtil.SURVEYED_LOCALE_ID_EXTRA,
                    Constants.TEST_FORM_SURVEY_INSTANCE_ID);
            return result;
        }
    };

    @AfterClass
    public static void afterClass() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        SurveyRequisite.resetRequisites(targetContext);
        SurveyInstaller installer = new SurveyInstaller(targetContext);
        installer.clearSurveys();
    }

    @Test
    public void viewResponsesTest() {

        //make sure everything is loaded
        addExecutionDelay(5000);
        verifyToolBar();

        List<QuestionGroup> questionGroups = survey.getQuestionGroups();
        for (int i = 0; i < questionGroups.size(); i++) {
            QuestionGroup group = questionGroups.get(i);
            verifyGroup(i, group);
        }
    }

    private void verifyGroup(int groupPosition, QuestionGroup group) {
        ViewInteraction tab = onView(
                childAtPosition(childAtPosition(withId(R.id.tabs), 0), groupPosition));
        tab.perform(click());
        tab.check(matches(hasDescendant(withText(group.getHeading()))));
        List<Question> questions = group.getQuestions();
        for (int j = 0; j < questions.size(); j++) {
            Question question = questions.get(j);
            verifyQuestionDisplayed(question, j);
        }
    }

    private void verifyToolBar() {
        onView(withId(R.id.toolbar)).check(matches(withToolbarTitle(is(survey.getName()))));
        onView(withId(R.id.toolbar))
                .check(matches(withToolbarSubtitle(is("v " + survey.getVersion()))));
    }

    private void verifyQuestionDisplayed(Question question, int questionPosition) {
        String questionHeader = getQuestionHeader(question);
        verifyQuestionHeader(questionHeader);
        verifyHelpTip(question);
        verifyQuestionView(question, questionPosition);
    }

    private void verifyHelpTip(Question question) {
        if (question.getHelpTypeCount() > 0) {
            ViewInteraction questionHelpTip = onView(
                    allOf(withId(R.id.tip_ib),
                            withQuestionViewParent(question, QuestionView.class)));
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
        String questionValue = getResponseValue(question);
        ViewInteraction caddisflyButton = onView(allOf(withId(R.id.caddisfly_button),
                withQuestionViewParent(question, CaddisflyQuestionView.class))).perform(scrollTo());
        caddisflyButton.check(matches(
                allOf(isDisplayed(), not(isEnabled()), withText(R.string.caddisfly_test))));
    }

    private void verifySignatureQuestionView(Question question) {
        String questionValue = getResponseValue(question);
        if (!TextUtils.isEmpty(questionValue)) {
            ViewInteraction image = onView(allOf(withId(R.id.signature_image),
                    withQuestionViewParent(question, SignatureQuestionView.class)))
                    .perform(scrollTo());
            image.check(matches(isDisplayed()));

            ViewInteraction nameLabel = onView(allOf(withId(R.id.signature_name_label),
                    withQuestionViewParent(question, SignatureQuestionView.class)))
                    .perform(scrollTo());
            nameLabel.check(matches(allOf(isDisplayed(), withText(R.string.signed_by))));

            Signature signature = SignatureValue.deserialize(questionValue);
            ViewInteraction name = onView(allOf(withId(R.id.signature_name),
                    withQuestionViewParent(question, SignatureQuestionView.class)))
                    .perform(scrollTo());
            name.check(matches(allOf(isDisplayed(), withText(signature.getName()))));

            ViewInteraction signatureButton = onView(allOf(withId(R.id.sign_btn),
                    withQuestionViewParent(question, SignatureQuestionView.class)))
                    .perform(scrollTo());
            signatureButton.check(matches(
                    allOf(isDisplayed(), not(isEnabled()), withText(R.string.modify_signature))));
        }
    }

    private void verifyGeoShapeQuestionView(Question question) {
        String questionValue = getResponseValue(question);
        if (!TextUtils.isEmpty(questionValue)) {
            ViewInteraction captureShapeButton = onView(allOf(withId(R.id.capture_shape_btn),
                    withQuestionViewParent(question, GeoshapeQuestionView.class)))
                    .perform(scrollTo());
            captureShapeButton.check(matches(
                    allOf(isDisplayed(), isEnabled(), withText(R.string.view_shape))));
        }
    }

    private void verifyBarcodeQuestionView(Question question) {
        String questionValue = getResponseValue(question);
        if (!TextUtils.isEmpty(questionValue)) {
            String[] items = questionValue.split("\\|");
            ViewInteraction barcodeInput = onView(
                    allOf(withId(R.id.barcode_responses_recycler_view),
                            withQuestionViewParent(question,
                                    BarcodeQuestionViewReadOnly.class))).perform(scrollTo());
            barcodeInput.check(matches(isDisplayed()));
            for (int i = 0; i < items.length; i++) {
                barcodeInput.perform(scrollToPosition(i));
                barcodeInput.check(matches(hasDescendant(withText(items[i]))));
            }
        }
    }

    private void verifyDateQuestionView(Question question) {
        String questionValue = getResponseValue(question);
        ViewInteraction dateInput = onView(
                allOf(withId(R.id.date_et),
                        withQuestionViewParent(question, DateQuestionView.class)))
                .perform(scrollTo());
        dateInput.check(matches(isDisplayed()));
        DateFormat userDisplayedDateFormat = SimpleDateFormat.getDateInstance();
        userDisplayedDateFormat.setTimeZone(TimeZone.getDefault());
        if (questionValue != null) {
            dateInput.check(matches(withText(
                    userDisplayedDateFormat.format(new Date(Long.parseLong(questionValue))))));
        } else {
            dateInput.check(matches(withText("")));
        }

        ViewInteraction dateButton = onView(allOf(withId(R.id.date_btn),
                withQuestionViewParent(question, DateQuestionView.class))).perform(scrollTo());
        dateButton.check(matches(isDisplayed()));
        dateButton.check(matches(not(isEnabled())));
        dateButton.check(matches(withText(R.string.pickdate)));
    }

    private void verifyPhotoQuestionView(Question question) {
        ViewInteraction photoButton = onView(
                allOf(withId(R.id.media_btn),
                        withQuestionViewParent(question, MediaQuestionView.class)))
                .perform(ViewActions.scrollTo());
        photoButton.check(matches(withText(R.string.takephoto)));
        photoButton.check(matches(allOf(isDisplayed(), not(isEnabled()))));
        String questionValue = getResponseValue(question);
        verifyMediaContent(question, questionValue);
    }

    private void verifyVideoQuestionView(Question question) {
        ViewInteraction videoButton = onView(
                allOf(withId(R.id.media_btn),
                        withQuestionViewParent(question, MediaQuestionView.class)))
                .perform(ViewActions.scrollTo());
        videoButton.check(matches(withText(R.string.takevideo)));
        videoButton.check(matches(allOf(isDisplayed(), not(isEnabled()))));
        String questionValue = getResponseValue(question);
        verifyMediaContent(question, questionValue);
    }

    private void verifyMediaContent(Question question, String questionValue) {
        if (!TextUtils.isEmpty(questionValue)) {
            ViewInteraction downloadButton = onView(
                    allOf(withId(R.id.media_download),
                            withQuestionViewParent(question, MediaQuestionView.class)))
                    .perform(scrollTo());
            downloadButton.check(matches(isDisplayed()));
            downloadButton.perform(click());
            downloadButton.check(matches(not(isDisplayed())));
        }
    }

    private void verifyGeoQuestionView(Question question) {
        verifyGeoLabel(question, R.string.lat);
        verifyGeoLabel(question, R.string.lon);
        verifyGeoLabel(question, R.string.elevation);
        String value = getResponseValue(question);
        if (value != null) {
            String[] locationValues = value.split("\\|");
            verifyGeoInput(question, R.id.lat_et, locationValues[0]);
            verifyGeoInput(question, R.id.lon_et, locationValues[1]);
            verifyGeoInput(question, R.id.height_et, locationValues[2]);
        } else {
            verifyGeoInput(question, R.id.lat_et, "");
            verifyGeoInput(question, R.id.lon_et, "");
            verifyGeoInput(question, R.id.height_et, "");
        }

        ViewInteraction accuracyLabel = onView(allOf(withId(R.id.acc_tv),
                withQuestionViewParent(question, GeoQuestionView.class)));
        accuracyLabel.perform(scrollTo());
        accuracyLabel.check(matches(isDisplayed()));
        accuracyLabel.check(matches(withText(R.string.geo_location_accuracy_default)));

        ViewInteraction geoButton = onView(allOf(withId(R.id.geo_btn),
                withQuestionViewParent(question, GeoQuestionView.class)));
        geoButton.perform(scrollTo());
        geoButton.check(matches(withText(R.string.getgeo)));
        geoButton.check(matches(not(isEnabled())));
        geoButton.check(matches(isDisplayed()));
    }

    private void verifyGeoInput(Question question, int resId, String text) {
        ViewInteraction input = onView(
                allOf(withId(resId), withQuestionViewParent(question, GeoQuestionView.class)));
        input.perform(scrollTo());
        input.check(matches(isDisplayed()));
        input.check(matches(withText(text)));
        input.check(matches(not(isFocusable())));
    }

    private void verifyGeoLabel(Question question, int resourceId) {
        ViewInteraction label = onView(
                allOf(withText(resourceId),
                        withQuestionViewParent(question, GeoQuestionView.class)));
        label.perform(scrollTo());
        label.check(matches(isDisplayed()));
    }

    private void verifyCascadeQuestionView(Question question) {
        List<Level> levels = question.getLevels();
        String value = getResponseValue(question);
        if (value == null) {
            return;
        }
        List<CascadeNode> values = CascadeValue.deserialize(value);
        if (levels != null && levels.size() > 0) {
            for (int i = 0; i < levels.size(); i++) {
                Level level = levels.get(i);
                ViewInteraction firstLevelCascadeNumber = onView(
                        allOf(withId(R.id.cascade_level_number), withText(level.getText())));
                firstLevelCascadeNumber.perform(scrollTo());
                firstLevelCascadeNumber.check(matches(isDisplayed()));

                ViewInteraction firstLevelCascadeDescription = onView(
                        allOf(withId(R.id.cascade_level_spinner), withTagValue(is((Object) i))));
                firstLevelCascadeDescription.perform(scrollTo());
                firstLevelCascadeDescription.check(matches(isDisplayed()));
                firstLevelCascadeDescription
                        .check(matches(withSpinnerText(values.get(i).getName())));
            }
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
                allOf(withId(R.id.input_et),
                        withQuestionViewParent(question, FreetextQuestionView.class)));
        freeTextQuestionInput.perform(scrollTo());
        String questionValue = getResponseValue(question);
        if (questionValue != null) {
            freeTextQuestionInput.check(matches(withText(questionValue)));
        } else {
            freeTextQuestionInput.check(matches(withText("")));
        }
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
            repeatInput.check(matches(withText(questionValue)));
            repeatInput.perform(click());
            repeatInput.perform(closeSoftKeyboard());
        }
    }

    private String getResponseValue(Question question) {
        QuestionResponse questionResponse = responseMap.get(question.getQuestionId());
        if (questionResponse == null) {
            return null;
        }
        return questionResponse.getValue();
    }

    @NonNull
    private <T extends View> Matcher<View> withQuestionViewParent(Question question,
            Class<T> parentClass) {
        return isDescendantOfA(allOf(IsInstanceOf.<View>instanceOf(parentClass),
                withTagValue(is((Object) question.getId()))));
    }

    private void verifyOptionQuestionView(Question question, int questionPosition) {
        List<Option> options = question.getOptions();
        String questionValue = getResponseValue(question);
        List<Option> selectedOptions = new ArrayList<>();
        if (!TextUtils.isEmpty(questionValue)) {
            selectedOptions = OptionValue.deserialize(questionValue);
        }
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                Option option = options.get(i);
                ViewInteraction optionView = getOptionView(question, questionPosition, i, option);
                optionView.perform(scrollTo());
                optionView.check(matches(allOf(isDisplayed(), not(isEnabled()))));
                if (selectedOptions.contains(option)) {
                    optionView.check(matches(isChecked()));
                }
            }
        }
    }

    private ViewInteraction getOptionView(Question question, int questionPosition,
            int optionPosition, Option option) {
        if (question.isAllowMultiple()) {
            return checkBoxWithText(option, optionPosition, questionPosition);
        } else {
            return radioButtonWithText(option, optionPosition);
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
