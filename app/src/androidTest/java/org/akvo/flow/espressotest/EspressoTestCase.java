package org.akvo.flow.espressotest;

import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import org.akvo.flow.R;
import org.akvo.flow.activity.SurveyActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;

/**
 * Created by MelEnt on 2016-11-02.
 */
@RunWith(AndroidJUnit4.class)
public class EspressoTestCase
{
    @Rule
    public ActivityTestRule<SurveyActivity> testRule = new ActivityTestRule<>(SurveyActivity.class);

    @Test
    public void testDrawer()
    {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
//        onView(withId(R.id.left_drawer)).check(matches())


    }


}
