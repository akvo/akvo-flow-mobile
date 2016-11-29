package org.akvo.flow.activity.test;

import static org.junit.Assert.*;

import static org.hamcrest.Matchers.*;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;


import android.database.Cursor;
import android.provider.SyncStateContract;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.akvo.flow.R;
import org.akvo.flow.activity.AddUserActivity;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.util.ConstantUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Created by EnderCrypt on 08/11/16.
 */

@RunWith(AndroidJUnit4.class)
public class AddUserActivityTest
{
    @Rule
    public ActivityTestRule<AddUserActivity> activityTestRule = new ActivityTestRule<>(AddUserActivity.class);

    private ViewInteraction usernameView;
    private ViewInteraction deviceIdView;
    private ViewInteraction loginButtonView;

    @Before
    public void setup()
    {
        usernameView = onView(withId(R.id.username));
        deviceIdView = onView(withId(R.id.device_id));
        loginButtonView = onView(withId(R.id.login_btn));
    }

    private void typeLoginText(String username, String deviceId)
    {
        typeLoginTextOnView(usernameView, username);
        typeLoginTextOnView(deviceIdView, deviceId);
    }

    private void typeLoginTextOnView(ViewInteraction on, String text)
    {
        ViewAction action = clearText();
        if (text != null)
            action = typeText(text);
        on.perform(click(), action, closeSoftKeyboard());
    }

    @Test
    public void ensureLoginButtonDisabledWhenNoInput()
    {
        typeLoginText(null, null);

        loginButtonView
                .check(matches(not(isEnabled()))) // check that button is disabled
                .perform(click());
        assertFalse(activityTestRule.getActivity().isFinishing()); // check that activity didnt changed
    }

    @Test
    public void ensureLoginButtonDisabledWhenNoUser()
    {
        typeLoginText(null, "DeviceID");

        loginButtonView
                .check(matches(not(isEnabled()))) // check that button is disabled
                .perform(click());
        assertFalse(activityTestRule.getActivity().isFinishing()); // check that activity didnt changed
    }

    @Test
    public void ensureLoginButtonDisabledWhenNoDeviceId()
    {
        typeLoginText("Username", null);

        loginButtonView
                .check(matches(not(isEnabled()))) // check that button is disabled
                .perform(click());
        assertFalse(activityTestRule.getActivity().isFinishing()); // check that activity didnt changed
    }

    @Test
    public void ensureLoginButtonWorksWhenFormFilled()
    {
        typeLoginText("Username", "DeviceID");

        loginButtonView
                .check(matches(isEnabled())) // check that button is enabled
                .perform(click());
        assertTrue(activityTestRule.getActivity().isFinishing()); // check that activity changed
    }

    @Test
    public void ensureDataSavedToPrefs()
    {
        SurveyDbAdapter dbAdapter = new SurveyDbAdapter(activityTestRule.getActivity().getApplicationContext()).open();
        dbAdapter.clearAllData();

        ensureLoginButtonWorksWhenFormFilled();

        //moveToFirst called with getUsers() in SurveyDbAdapter
        Cursor users = dbAdapter.getUsers();

        assertEquals("Username", users.getString(users.getColumnIndex("name")));
        assertEquals("DeviceID", dbAdapter.getPreference(ConstantUtil.DEVICE_IDENT_KEY));

        dbAdapter.close();
    }
}
