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

package org.akvo.flow.activity.form.submittedformsview;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isFocusable;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getCameraButton;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getDateButton;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getDateEditText;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getDoubleEntryInput;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFreeTextInput;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getGalleryButton;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getGeoButton;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getOptionView;
import static org.akvo.flow.activity.form.FormActivityTestUtil.selectAndVerifyTab;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyAccuracyLabel;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyCascadeLevelNumber;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyDoubleEntryTitle;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyGeoLabel;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyHelpTip;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyQuestionHeader;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyToolBar;
import static org.akvo.flow.activity.form.FormActivityTestUtil.withQuestionViewParent;
import static org.akvo.flow.tests.R.raw.all_questions_form;
import static org.akvo.flow.tests.R.raw.data;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNot.not;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.core.util.Pair;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.google.gson.Gson;

import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.SurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.response.value.CascadeNode;
import org.akvo.flow.domain.response.value.Signature;
import org.akvo.flow.serialization.response.value.CascadeValue;
import org.akvo.flow.serialization.response.value.OptionValue;
import org.akvo.flow.serialization.response.value.SignatureValue;
import org.akvo.flow.ui.model.caddisfly.CaddisflyJsonMapper;
import org.akvo.flow.ui.model.caddisfly.CaddisflyTestResult;
import org.akvo.flow.ui.view.CaddisflyQuestionView;
import org.akvo.flow.ui.view.GeoshapeQuestionView;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.ui.view.barcode.BarcodeQuestionViewReadOnly;
import org.akvo.flow.ui.view.geolocation.GeoQuestionView;
import org.akvo.flow.ui.view.signature.SignatureQuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.utils.entity.Level;
import org.akvo.flow.utils.entity.Option;
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

@LargeTest
@RunWith(AndroidJUnit4.class)
public class FormActivityReadOnlyTest {

    private static final String FORM_TITLE = "Test form";
    private Map<String, QuestionResponse> responseMap;

    private Survey survey;

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
            Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
            SurveyRequisite.setRequisites(targetContext);
            SurveyInstaller installer = new SurveyInstaller(targetContext);
            survey = installer
                    .installSurvey(all_questions_form, InstrumentationRegistry.getInstrumentation().getContext());
            Pair<Long, Map<String, QuestionResponse>> dataPointFromFile = installer
                    .createDataPointFromFile(survey.getSurveyGroup(),
                            InstrumentationRegistry.getInstrumentation().getContext(), data);
            long dataPointId = dataPointFromFile.first;
            responseMap = dataPointFromFile.second;
            return getFormActivityIntent(155852013L, "156792013", FORM_TITLE, dataPointId, true);
        }
    };

    @AfterClass
    public static void afterClass() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SurveyRequisite.resetRequisites(targetContext);
        SurveyInstaller installer = new SurveyInstaller(targetContext);
        installer.clearSurveys();
    }

    @Test
    public void testViewFilledFormResponses() {
        verifyToolBar(survey.getName(), survey.getVersion());
        List<QuestionGroup> questionGroups = survey.getQuestionGroups();
        for (int i = 0; i < questionGroups.size(); i++) {
            QuestionGroup group = questionGroups.get(i);
            verifyGroup(i, group);
        }
    }

    private void verifyGroup(int groupPosition, QuestionGroup group) {
        selectAndVerifyTab(groupPosition, group);
        List<Question> questions = group.getQuestions();
        for (int j = 0; j < questions.size(); j++) {
            Question question = questions.get(j);
            verifyQuestionDisplayed(question, j);
        }
    }

    private void verifyQuestionDisplayed(Question question, int questionPosition) {
        verifyQuestionHeader(question);
        verifyHelpTip(question);
        verifyQuestionView(question, questionPosition);
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

        List<CaddisflyTestResult> caddisflyTestResults = new CaddisflyJsonMapper(new Gson())
                .transform(questionValue);
        ViewInteraction caddislfyRecyclerView = onView(
                allOf(withId(R.id.caddisfly_results_recycler_view),
                        withQuestionViewParent(question,
                                CaddisflyQuestionView.class))).perform(scrollTo());
        for (int i = 0; i < caddisflyTestResults.size(); i++) {
            caddislfyRecyclerView.perform(scrollToPosition(i));
            caddislfyRecyclerView.check(matches(
                    hasDescendant(withText(caddisflyTestResults.get(i).buildResultToDisplay()))));
        }

        ViewInteraction caddisflyButton = onView(allOf(withId(R.id.caddisfly_button),
                withQuestionViewParent(question, CaddisflyQuestionView.class)));
        caddisflyButton.check(matches(not(isDisplayed())));
    }

    private void verifySignatureQuestionView(Question question) {
        String questionValue = getResponseValue(question);
        if (!TextUtils.isEmpty(questionValue)) {
            verifySignatureImage(question);
            verifySignatureNameLabel(question);
            verifySignatureName(question, questionValue);
            verifySignatureButton(question);
        }
    }

    private void verifySignatureButton(Question question) {
        ViewInteraction signatureButton = onView(allOf(withId(R.id.sign_btn),
                withQuestionViewParent(question, SignatureQuestionView.class)));
        signatureButton.check(matches(not(isDisplayed())));
    }

    private void verifySignatureName(Question question, String questionValue) {
        Signature signature = SignatureValue.deserialize(questionValue);
        ViewInteraction name = onView(allOf(withId(R.id.signature_name),
                withQuestionViewParent(question, SignatureQuestionView.class)))
                .perform(scrollTo());
        name.check(matches(allOf(isDisplayed(), withText(signature.getName()), not(isEnabled()))));
    }

    private void verifySignatureNameLabel(Question question) {
        ViewInteraction nameLabel = onView(allOf(withId(R.id.signature_name_label),
                withQuestionViewParent(question, SignatureQuestionView.class)))
                .perform(scrollTo());
        nameLabel.check(matches(allOf(isDisplayed(), withText(R.string.signed_by))));
    }

    private void verifySignatureImage(Question question) {
        ViewInteraction image = onView(allOf(withId(R.id.signature_image),
                withQuestionViewParent(question, SignatureQuestionView.class)))
                .perform(scrollTo());
        image.check(matches(isDisplayed()));
    }

    private void verifyGeoShapeQuestionView(Question question) {
        String questionValue = getResponseValue(question);
        if (!TextUtils.isEmpty(questionValue)) {
            ViewInteraction capturedShapeImage = onView(allOf(withId(R.id.geo_shape_checkmark),
                    withQuestionViewParent(question, GeoshapeQuestionView.class)))
                    .perform(scrollTo());
            capturedShapeImage.check(matches(isDisplayed()));

            ViewInteraction capturedShapeText = onView(allOf(withId(R.id.geo_shape_captured_text),
                    withQuestionViewParent(question, GeoshapeQuestionView.class)))
                    .perform(scrollTo());
            capturedShapeText
                    .check(matches(allOf(isDisplayed(), withText(R.string.geoshape_response))));

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
        verifyDateInput(question);
        verifyDateButton(question);
    }

    private void verifyDateInput(Question question) {
        String questionValue = getResponseValue(question);
        ViewInteraction dateInput = getDateEditText(question);
        dateInput.perform(scrollTo());
        dateInput.check(matches(isDisplayed()));
        DateFormat userDisplayedDateFormat = SimpleDateFormat.getDateInstance();
        userDisplayedDateFormat.setTimeZone(TimeZone.getDefault());
        if (questionValue != null) {
            dateInput.check(matches(withText(
                    userDisplayedDateFormat.format(new Date(Long.parseLong(questionValue))))));
        } else {
            dateInput.check(matches(withText("")));
        }
    }

    private void verifyDateButton(Question question) {
        ViewInteraction dateButton = getDateButton(question);
        dateButton.check(matches(not(isDisplayed())));
    }

    private void verifyPhotoQuestionView(Question question) {
        verifyCameraButton(question);
        verifyGalleryButton(question);
        verifyMediaContent(question);
    }

    private void verifyCameraButton(Question question) {
        ViewInteraction mediaButton = getCameraButton(question);
        mediaButton.check(matches(not(isDisplayed())));
    }

    private void verifyGalleryButton(Question question) {
        ViewInteraction mediaButton = getGalleryButton(question);
        mediaButton.check(matches(not(isDisplayed())));
    }

    private void verifyVideoQuestionView(Question question) {
        verifyCameraButton(question);
        verifyGalleryButton(question);
        verifyMediaContent(question);
    }

    private void verifyMediaContent(Question question) {
        String questionValue = getResponseValue(question);
        if (!TextUtils.isEmpty(questionValue)) {
            ViewInteraction downloadButton = onView(
                    allOf(withId(R.id.media_download),
                            withQuestionViewParent(question, QuestionView.class)))
                    .perform(scrollTo());
            downloadButton.check(matches(allOf(isDisplayed(), isEnabled())));
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

        verifyAccuracyLabel(question);

        verifyGeoButton(question);
    }

    private void verifyGeoButton(Question question) {
        ViewInteraction geoButton = getGeoButton(question);
        geoButton.check(matches(not(isDisplayed())));
    }

    private void verifyGeoInput(Question question, int resId, String text) {
        ViewInteraction input = onView(
                allOf(withId(resId),
                        withQuestionViewParent(question, GeoQuestionView.class)));
        input.perform(scrollTo());
        input.check(matches(isDisplayed()));
        input.check(matches(withText(text)));
        input.check(matches(not(isFocusable())));
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
                verifyCascadeLevelNumber(level);
                verifyCascadeLevelSpinner(values, i);
            }
        }
    }

    private void verifyCascadeLevelSpinner(List<CascadeNode> values, int i) {
        ViewInteraction cascadeLevelSpinner = onView(
                allOf(withId(R.id.cascade_level_textview), withTagValue(is(i))));
        cascadeLevelSpinner.perform(scrollTo());
        cascadeLevelSpinner.check(matches(isDisplayed()));
        cascadeLevelSpinner
                .check(matches(withText(values.get(i).getName())));
    }

    private void verifyFreeTextQuestionView(Question question) {
        String questionValue = verifyFreeTextInput(question);
        if (question.isDoubleEntry()) {
            verifyDoubleEntryTitle(question);
            verifyDoubleEntryInput(question, questionValue);
        }
    }

    private void verifyDoubleEntryInput(Question question, String questionValue) {
        ViewInteraction repeatInput = getDoubleEntryInput(question);
        repeatInput.check(matches(isDisplayed()));
        repeatInput.check(matches(withText(questionValue)));
    }

    private String verifyFreeTextInput(Question question) {
        ViewInteraction freeTextQuestionInput = getFreeTextInput(question);
        String questionValue = getResponseValue(question);
        if (questionValue != null) {
            freeTextQuestionInput.check(matches(withText(questionValue)));
        } else {
            freeTextQuestionInput.check(matches(withText("")));
        }
        return questionValue;
    }

    private String getResponseValue(Question question) {
        QuestionResponse questionResponse = responseMap.get(question.getQuestionId());
        if (questionResponse == null) {
            return null;
        }
        return questionResponse.getValue();
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

}
