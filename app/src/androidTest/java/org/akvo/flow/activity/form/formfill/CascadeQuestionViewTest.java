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

package org.akvo.flow.activity.form.formfill;

import android.content.Context;
import android.content.Intent;
import android.util.SparseArray;
import android.widget.AdapterView;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

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

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static org.akvo.flow.activity.form.FormActivityTestUtil.addExecutionDelay;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyCascadeLevelNumber;
import static org.akvo.flow.tests.R.raw.cascade_form;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNot.not;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class CascadeQuestionViewTest {

    private static SurveyInstaller installer;
    private static Survey survey;

    private final Random random = new Random();

    @Rule
    public ActivityTestRule<FormActivity> rule = new ActivityTestRule<FormActivity>(
            FormActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            return getFormActivityIntent(155852013L, "156792013", "CascadeForm", 231L, false);
        }
    };

    @BeforeClass
    public static void beforeClass() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SurveyRequisite.setRequisites(targetContext);
        installer = new SurveyInstaller(targetContext);
        survey = installer.installSurvey(cascade_form, InstrumentationRegistry.getInstrumentation().getContext());
    }

    @After
    public void afterEachTest() {
        installer.deleteResponses();
    }

    @AfterClass
    public static void afterClass() {
        SurveyRequisite.resetRequisites(InstrumentationRegistry.getInstrumentation().getTargetContext());
        installer.clearSurveys();
    }

    @Test
    public void ensureCascadesFullyDisplayed() {
        final List<QuestionGroup> questionGroups = survey.getQuestionGroups();
        final Question question = questionGroups.get(0).getQuestions().get(0);
        final SparseArray<List<Node>> cascadeNodes = installer
                .getAllNodes(question, InstrumentationRegistry.getInstrumentation().getContext());
        List<Node> levelNodes = cascadeNodes.get(0);
        List<Level> levels = question.getLevels();
        if (levels != null && levels.size() > 0) {
            for (int i = 0; i < levels.size(); i++) {
                Level level = levels.get(i);
                verifyCascadeLevelNumber(level);

                ViewInteraction cascadeLevelSpinner = onView(
                        allOf(withId(R.id.cascade_level_spinner), withTagValue(is(i))));

                verifyCascadeInitialState(cascadeLevelSpinner);

                int position = random.nextInt(levelNodes.size());
                Node node = levelNodes.get(position);
                levelNodes = cascadeNodes.get((int) node.getId());

                selectSpinnerItem(cascadeLevelSpinner, node);

                verifyCascadeNewState(cascadeLevelSpinner, node);
            }
        }
    }

    private void verifyCascadeInitialState(ViewInteraction cascadeLevelSpinner) {
        cascadeLevelSpinner.perform(scrollTo());
        cascadeLevelSpinner.check(matches(isDisplayed()));
        cascadeLevelSpinner.check(matches(withSpinnerText(R.string.select)));
    }

    private void selectSpinnerItem(ViewInteraction cascadeLevelSpinner, Node node) {
        cascadeLevelSpinner.perform(click());
        addExecutionDelay(100);
        onData(withNode(node))
                .inAdapterView(allOf(
                        isAssignableFrom(AdapterView.class),
                        not(is(withId(R.id.submit_tab)))))
                .perform(click());
        addExecutionDelay(100);
    }

    private void verifyCascadeNewState(ViewInteraction cascadeLevelSpinner, Node node) {
        cascadeLevelSpinner.check(matches(withSpinnerText(node.getName())));
    }

    public static Matcher<Node> withNode(final Node nodeToMatch) {
        return new TypeSafeMatcher<Node>() {
            @Override
            public boolean matchesSafely(Node node) {
                return nodeToMatch.equals(node);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("matches with Node   ");
            }
        };
    }
}
