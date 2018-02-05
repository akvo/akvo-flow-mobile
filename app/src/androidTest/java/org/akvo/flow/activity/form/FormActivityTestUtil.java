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
import android.support.annotation.StringRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.view.View;

import org.akvo.flow.R;
import org.akvo.flow.activity.Constants;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.domain.Level;
import org.akvo.flow.domain.Option;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.ui.view.DateQuestionView;
import org.akvo.flow.ui.view.FreetextQuestionView;
import org.akvo.flow.ui.view.MediaQuestionView;
import org.akvo.flow.ui.view.QuestionView;
import org.akvo.flow.ui.view.geolocation.GeoQuestionView;
import org.akvo.flow.util.ConstantUtil;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsInstanceOf;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withTagValue;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.activity.ChildPositionMatcher.childAtPosition;
import static org.akvo.flow.activity.ToolBarTitleSubtitleMatcher.withToolbarSubtitle;
import static org.akvo.flow.activity.ToolBarTitleSubtitleMatcher.withToolbarTitle;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;

public class FormActivityTestUtil {

    @NonNull
    public static Intent getFormActivityIntent(long surveyGroupId, String formId,
            String formTitle, long dataPointId, boolean readOnly) {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent result = new Intent(targetContext, FormActivity.class);
        result.putExtra(ConstantUtil.FORM_ID_EXTRA, formId);
        result.putExtra(ConstantUtil.RESPONDENT_ID_EXTRA, dataPointId);
        result.putExtra(ConstantUtil.SURVEY_GROUP_EXTRA,
                new SurveyGroup(surveyGroupId, formTitle, null, false));
        result.putExtra(ConstantUtil.SURVEYED_LOCALE_ID_EXTRA,
                Constants.TEST_FORM_SURVEY_INSTANCE_ID);
        result.putExtra(ConstantUtil.READ_ONLY_EXTRA, readOnly);
        return result;
    }

    public static void verifySubmitButtonEnabled() {
        onView(allOf(withClassName(endsWith("Button")), withText(R.string.submitbutton)))
                .check(matches(isEnabled()));
    }

    public static void verifySubmitButtonDisabled() {
        onView(allOf(withClassName(endsWith("Button")), withText(R.string.submitbutton)))
                .check(matches(not(isEnabled())));
    }

    public static void verifyQuestionTitleDisplayed() {
        onView(withId(R.id.question_tv)).check(matches(isDisplayed()));
    }

    public static void clickNext() {
        onView(withId(R.id.next_btn)).perform(scrollTo()).perform(click());
    }

    public static void fillFreeTextQuestion(String text) {
        onView(withId(R.id.input_et)).perform(typeText(text));
        Espresso.closeSoftKeyboard();
    }

    public static void addExecutionDelay(long millis) {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void verifyToolBar(String formName, double surveyVersion) {
        onView(withId(R.id.toolbar)).check(matches(withToolbarTitle(is(formName))));
        onView(withId(R.id.toolbar))
                .check(matches(withToolbarSubtitle(is("v " + surveyVersion))));
    }

    public static void selectAndVerifyTab(int groupPosition, QuestionGroup group) {
        ViewInteraction tab = onView(
                childAtPosition(childAtPosition(withId(R.id.tabs), 0), groupPosition));
        tab.perform(click());
        tab.check(matches(hasDescendant(withText(group.getHeading()))));
    }

    @NonNull
    public static String getQuestionHeader(Question question) {
        String questionHeader = question.getOrder() + ". " + question.getText();
        if (question.isMandatory()) {
            questionHeader = questionHeader + "*";
        }
        return questionHeader;
    }

    private static ViewInteraction findQuestionTitle(String questionText) {
        return onView(allOf(withId(R.id.question_tv), withText(questionText),
                childAtPosition(linearLayoutChild(0), 0)));
    }

    @NonNull
    private static Matcher<View> linearLayoutChild(int position) {
        return childAtPosition(IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                position);
    }

    public static void verifyQuestionHeader(Question question) {
        String questionHeader = getQuestionHeader(question);
        ViewInteraction questionTitle = findQuestionTitle(questionHeader);
        questionTitle.perform(scrollTo());
        questionTitle.check(matches(isDisplayed()));
    }

    public static void verifyHelpTip(Question question) {
        if (question.getHelpTypeCount() > 0) {
            ViewInteraction questionHelpTip = onView(
                    allOf(withId(R.id.tip_ib),
                            withQuestionViewParent(question, QuestionView.class)));
            questionHelpTip.perform(scrollTo());
            questionHelpTip.check(matches(isDisplayed()));
            questionHelpTip.check(matches(isEnabled()));
        }
    }

    @NonNull
    public static <T extends View> Matcher<View> withQuestionViewParent(Question question,
            Class<T> parentClass) {
        return isDescendantOfA(allOf(IsInstanceOf.<View>instanceOf(parentClass),
                withTagValue(is((Object) question.getId()))));
    }

    @NonNull
    public static ViewInteraction getFreeTextInput(Question question) {
        return onView(allOf(withId(R.id.input_et),
                withQuestionViewParent(question, FreetextQuestionView.class))).perform(scrollTo());
    }

    @NonNull
    public static ViewInteraction getDoubleEntryInput(Question question) {
        return onView(allOf(withId(R.id.double_entry_et),
                withQuestionViewParent(question, FreetextQuestionView.class))).perform(scrollTo());
    }

    public static void verifyDoubleEntryTitle(Question question) {
        ViewInteraction repeatTextView = onView(allOf(withId(R.id.double_entry_title),
                withQuestionViewParent(question, FreetextQuestionView.class)));
        repeatTextView.perform(scrollTo());
        repeatTextView.check(matches(withText(R.string.repeat_answer)));
        repeatTextView.check(matches(isDisplayed()));
    }

    @NonNull
    public static ViewInteraction getMediaButton(Question question) {
        return onView(
                allOf(withId(R.id.media_btn),
                        withQuestionViewParent(question, MediaQuestionView.class)));
    }

    @NonNull
    public static ViewInteraction getDateButton(Question question) {
        return onView(allOf(withId(R.id.date_btn),
                withQuestionViewParent(question, DateQuestionView.class)));
    }

    @NonNull
    public static ViewInteraction getDateEditText(Question question) {
        return onView(
                allOf(withId(R.id.date_et),
                        withQuestionViewParent(question, DateQuestionView.class)))
                .perform(scrollTo());
    }

    @NonNull
    public static ViewInteraction getGeoButton(Question question) {
        return onView(allOf(withId(R.id.geo_btn),
                withQuestionViewParent(question, GeoQuestionView.class)));
    }

    public static void verifyGeoLabel(Question question, int resourceId) {
        ViewInteraction label = onView(
                allOf(withText(resourceId),
                        withQuestionViewParent(question, GeoQuestionView.class)));
        label.perform(scrollTo());
        label.check(matches(isDisplayed()));
    }

    public static void verifyAccuracyLabel(Question question) {
        ViewInteraction accuracyLabel = onView(
                allOf(withId(R.id.acc_tv),
                        withQuestionViewParent(question, GeoQuestionView.class)));
        accuracyLabel.perform(scrollTo());
        accuracyLabel.check(matches(isDisplayed()));
        accuracyLabel.check(matches(withText(R.string.geo_location_accuracy_default)));
    }

    public static ViewInteraction getOptionView(Question question, int questionPosition,
            int optionPosition, Option option) {
        if (question.isAllowMultiple()) {
            return checkBoxWithText(option, optionPosition, questionPosition);
        } else {
            return radioButtonWithText(option, optionPosition);
        }
    }

    public static void verifyCascadeLevelNumber(Level level) {
        ViewInteraction cascadeLevelNumber = onView(
                allOf(withId(R.id.cascade_level_number), withText(level.getText())));
        cascadeLevelNumber.perform(scrollTo());
        cascadeLevelNumber.check(matches(isDisplayed()));
    }

    private static ViewInteraction checkBoxWithText(Option option, int optionPosition,
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

    private static ViewInteraction radioButtonWithText(Option option, int childPosition) {
        if (option.isOther()) {
            return onView(allOf(childAtPosition(linearLayoutChild(1), childPosition),
                    withText(R.string.othertext)));
        }
        return onView(allOf(childAtPosition(linearLayoutChild(1), childPosition),
                withText(option.getText())));
    }

    @NonNull
    public static String getString(@StringRes int stringResId,
            ActivityTestRule<FormActivity> rule, String param) {
        return rule.getActivity().getApplicationContext().getResources()
                .getString(stringResId, param);
    }

    @NonNull
    public static String getString(@StringRes int stringResId,
            ActivityTestRule<FormActivity> rule) {
        return rule.getActivity().getApplicationContext().getResources()
                .getString(stringResId);
    }
}
