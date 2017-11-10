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
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.akvo.flow.R;
import org.akvo.flow.activity.form.data.SurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.akvo.flow.domain.QuestionResponse;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.SurveyGroup;
import org.akvo.flow.util.ConstantUtil;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyQuestionTitleDisplayed;
import static org.akvo.flow.tests.R.raw.freetextsurvey;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class FreeTextFormActivityViewTest {

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Context targetContext = InstrumentationRegistry.getTargetContext();
            SurveyRequisite.setRequisites(targetContext);
            SurveyInstaller installer = new SurveyInstaller(targetContext);
            Survey survey = installer
                    .installSurvey(freetextsurvey, InstrumentationRegistry.getContext());
            long id = installer.createDataPoint(survey.getSurveyGroup(),
                    new QuestionResponse.QuestionResponseBuilder()
                            .setValue("test")
                            .setType(ConstantUtil.VALUE_RESPONSE_TYPE)
                            .setQuestionId("47313003")
                            .setIteration(-1));
            Context activityContext = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext();
            Intent result = new Intent(activityContext, FormActivity.class);
            result.putExtra(ConstantUtil.FORM_ID_EXTRA, "47313002");
            result.putExtra(ConstantUtil.RESPONDENT_ID_EXTRA, id);
            result.putExtra(ConstantUtil.SURVEY_GROUP_EXTRA,
                    new SurveyGroup(44173002L, "FreeTextForm", null, false));
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
    public void verifyTextQuestion() throws Exception {
        verifyQuestionTitleDisplayed();
        onView(withId(R.id.input_et)).check(matches(withText("test")));
    }
}
