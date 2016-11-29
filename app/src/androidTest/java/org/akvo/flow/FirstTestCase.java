package org.akvo.flow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static junit.framework.Assert.*;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;
import android.test.InstrumentationTestCase;
import android.test.mock.MockContext;

import org.akvo.flow.app.FlowApp;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.User;
import org.akvo.flow.util.AutoLog;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import javax.inject.Inject;

/**
 * Created by MelEnt on 2016-11-02.
 */
@RunWith(AndroidJUnit4.class)
public class FirstTestCase extends AndroidTestCase
{
    private String username = "testUser";
    private long id         = 1L;

    private SurveyDbAdapter dbAdapter;

    @Before
    public void init()
    {
        dbAdapter = new SurveyDbAdapter(InstrumentationRegistry.getTargetContext());
        dbAdapter.open();
    }

    @After
    public void deinit()
    {
        dbAdapter.close();
    }

    private User createUser(long id, String username)
    {
        long uid = dbAdapter.createOrUpdateUser(id, username);

        return new User(uid, username);
    }

    @Test
    public void canCreateUser()
    {
        createUser(id, username);
        assertNotNull(dbAdapter.getUser(id));
    }

    @Test
    public void assertDeviceId()
    {
        //
    }


    @Test
    public void canAddUserToFlow()
    {
        User user = createUser(id, username);
        FlowApp.getApp().setUser(user);
        assertEquals(FlowApp.getApp().getUser(), user);
    }

    @Test
    public void canCleanDatabase()
    {
        canCreateUser();
        dbAdapter.clearAllData();
        assertThat(dbAdapter.getUsers().getCount(), equalTo(0));
    }

}
