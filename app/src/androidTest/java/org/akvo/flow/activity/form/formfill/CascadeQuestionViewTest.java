/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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
import android.util.SparseArray;

import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.SurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.akvo.flow.domain.Level;
import org.akvo.flow.domain.Node;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.Survey;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Random;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static android.support.test.espresso.matcher.ViewMatchers.withTagValue;
import static org.akvo.flow.activity.form.FormActivityTestUtil.addExecutionDelay;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyCascadeLevelNumber;
import static org.akvo.flow.tests.R.raw.cascade_form;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class CascadeQuestionViewTest {

    private static SurveyInstaller installer;
    private static Survey survey;

    private Random random = new Random();

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
        survey = installer.installSurvey(cascade_form, InstrumentationRegistry.getContext());
    }

    @After
    public void afterEachTest() {
        installer.deleteResponses();
    }

    @AfterClass
    public static void afterClass() {
        SurveyRequisite.resetRequisites(InstrumentationRegistry.getTargetContext());
        installer.clearSurveys();
    }

    @Test
    public void ensureCascadesFullyDisplayed() throws Exception {
        final List<QuestionGroup> questionGroups = survey.getQuestionGroups();
        final Question question = questionGroups.get(0).getQuestions().get(0);
        final SparseArray<List<Node>> cascadeNodes = installer
                .getAllNodes(question, InstrumentationRegistry.getContext());
        List<Node> levelNodes = cascadeNodes.get(0);
        List<Level> levels = question.getLevels();
        if (levels != null && levels.size() > 0) {
            for (int i = 0; i < levels.size(); i++) {
                Level level = levels.get(i);
                verifyCascadeLevelNumber(level);

                ViewInteraction cascadeLevelSpinner = onView(
                        allOf(withId(R.id.cascade_level_spinner), withTagValue(is((Object) i))));

                verifyCascadeInitialState(cascadeLevelSpinner);

                int position = random.nextInt(levelNodes.size());
                Node node = levelNodes.get(position);
                levelNodes = cascadeNodes.get((int) node.getId());

                selectSpinnerItem(cascadeLevelSpinner, node.getName());

                verifyCascadeNewState(cascadeLevelSpinner, node);
            }
        }
    }

    private void verifyCascadeInitialState(ViewInteraction cascadeLevelSpinner) {
        cascadeLevelSpinner.perform(scrollTo());
        cascadeLevelSpinner.check(matches(isDisplayed()));
        cascadeLevelSpinner.check(matches(withSpinnerText(R.string.select)));
    }

    private void selectSpinnerItem(ViewInteraction cascadeLevelSpinner, String nodeName) {
        cascadeLevelSpinner.perform(click());
        onData(allOf(is(instanceOf(Node.class)), withName(is(nodeName)))).perform(click());
        addExecutionDelay(100);
    }

    private void verifyCascadeNewState(ViewInteraction cascadeLevelSpinner, Node node) {
        cascadeLevelSpinner.check(matches(withSpinnerText(node.getName())));
    }

    public static Matcher withName(final Matcher nameMatcher){
        return new TypeSafeMatcher<Node>(){
            @Override
            public boolean matchesSafely(Node node) {
                return nameMatcher.matches(node.getName());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("matches with Node   ");
            }
        };
    }
}
