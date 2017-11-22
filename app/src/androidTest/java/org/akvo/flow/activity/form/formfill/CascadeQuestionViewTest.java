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
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.SurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.akvo.flow.domain.Level;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.Survey;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withTagValue;
import static org.akvo.flow.activity.Constants.TEST_FORM_SURVEY_INSTANCE_ID;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyCascadeLevelNumber;
import static org.akvo.flow.tests.R.raw.cascades_form;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class CascadeQuestionViewTest {

    private static SurveyInstaller installer;
    private static Survey survey;

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            return getFormActivityIntent(155852013L, "156792013", "CascadeForm", 0L, false);
        }
    };

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new SurveyInstaller(targetContext);
        survey = installer.installSurvey(cascades_form, InstrumentationRegistry.getContext());
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
    public void ensureCascadesFullyDisplayed() throws Exception {
        List<QuestionGroup> questionGroups = survey.getQuestionGroups();
        QuestionGroup group = questionGroups.get(0);
        List<Question> questions = group.getQuestions();
        Question question = questions.get(0);
        List<Level> levels = question.getLevels();
        if (levels != null && levels.size() > 0) {
            for (int i = 0; i < levels.size(); i++) {
                Level level = levels.get(i);
                verifyCascadeLevelNumber(level);
                ViewInteraction cascadeLevelSpinner = onView(
                        allOf(withId(R.id.cascade_level_spinner), withTagValue(is((Object) i))));
                cascadeLevelSpinner.perform(scrollTo());
                cascadeLevelSpinner.check(matches(isDisplayed()));
            }
        }
    }
}
