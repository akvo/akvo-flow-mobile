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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.flow.activity.CustomMatchers.hasTextInputLayoutHintText;
import static org.akvo.flow.activity.form.FormActivityTestUtil.addExecutionDelay;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getFormActivityIntent;
import static org.akvo.flow.activity.form.FormActivityTestUtil.getString;
import static org.akvo.flow.activity.form.FormActivityTestUtil.verifyCascadeLevelNumber;
import static org.akvo.flow.tests.R.raw.cascade_form;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.content.Intent;
import android.util.SparseArray;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.akvo.flow.R;
import org.akvo.flow.activity.FormActivity;
import org.akvo.flow.activity.form.data.SurveyInstaller;
import org.akvo.flow.activity.form.data.SurveyRequisite;
import org.akvo.flow.domain.Node;
import org.akvo.flow.domain.Question;
import org.akvo.flow.domain.QuestionGroup;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.utils.entity.Level;
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

                ViewInteraction cascadeLevelInputLayout = onView(
                        allOf(withId(R.id.outlinedTextField), withTagValue(is(i))));

                verifyCascadeInitialState(cascadeLevelInputLayout, level.getText());

                int position = random.nextInt(levelNodes.size());
                Node node = levelNodes.get(position);
                levelNodes = cascadeNodes.get((int) node.getId());
                ViewInteraction cascadeLevelInput = onView(
                        allOf(withId(R.id.cascade_level_textview), withTagValue(is(i))));
                selectCascadeItem(cascadeLevelInput, node);

                verifyCascadeNewState(cascadeLevelInput, node);
            }
        }
    }

    private void verifyCascadeInitialState(ViewInteraction cascadeLevelInput, String text) {
        cascadeLevelInput.perform(scrollTo());
        cascadeLevelInput.check(matches(isDisplayed()));
        cascadeLevelInput.check(matches(hasTextInputLayoutHintText(getString(R.string.cascade_level_textview_hint, rule, text))));
    }

    private void selectCascadeItem(ViewInteraction cascadeLevelInput, Node node) {
        cascadeLevelInput.perform(click());
        addExecutionDelay(100);
        onView(withText(node.getName()))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());
        addExecutionDelay(100);
    }

    private void verifyCascadeNewState(ViewInteraction cascadeLevelInput, Node node) {
        cascadeLevelInput.check(matches(withText(node.getName())));
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
