package org.akvo.flow.espressotest;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.text.format.DateUtils;

import org.akvo.flow.R;
import org.akvo.flow.activity.SurveyActivity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by MelEnt on 2016-11-16.
 */
public class IdlingEspressoTest
{
    @Rule
    public ActivityTestRule<SurveyActivity> testRule = new ActivityTestRule<>(SurveyActivity.class);

    @Before
    public void resetTimeout()
    {
        IdlingPolicies.setMasterPolicyTimeout(60, TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(26, TimeUnit.SECONDS);
    }

    @Test
    public void waitFor5Seconds()
    {
        waitFor(DateUtils.SECOND_IN_MILLIS * 5);
    }

    @Test
    public void waitFor30Seconds()
    {
        waitFor(DateUtils.SECOND_IN_MILLIS * 30);
    }


    private void waitFor(long waitingTime)
    {
        IdlingPolicies.setMasterPolicyTimeout(waitingTime, TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(waitingTime, TimeUnit.SECONDS);

        IdlingResource idlingResource = new ElapsedTimeIdlingResource(waitingTime);
        Espresso.registerIdlingResources(idlingResource);

    }


}
