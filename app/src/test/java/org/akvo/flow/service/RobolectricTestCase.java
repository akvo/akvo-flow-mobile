package org.akvo.flow.service;//package org.akvo.flow.service;

import android.app.Application;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.activity.AddUserActivity;
import org.akvo.flow.activity.AppUpdateActivity;
import org.akvo.flow.activity.SurveyActivity;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.Survey;
import org.akvo.flow.domain.User;
import org.akvo.flow.service.ApkUpdateService;
import org.akvo.flow.util.AutoLog;
import org.akvo.flow.util.HackedPackageManager;
import org.akvo.flow.util.PlatformUtil;
import org.akvo.flow.util.Prefs;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboSharedPreferences;
import org.robolectric.shadows.ShadowIntentService;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by MelEnt on 2016-11-14.
 */

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class RobolectricTestCase
{
    private void changeVersion(Context context, String versionName)
    {
        //hijacking the packagemanager to set custom versionname
        HackedPackageManager.installHack(context);
        HackedPackageManager.globalInfo.versionName = versionName;
    }

    private String getCurrentVersion(Context context)
    {
        return PlatformUtil.getVersionName(context);
    }

    @Test
    public void canDetectAndLaunchAppUpdate() throws IOException, JSONException
    {
        SurveyActivity activity = Robolectric.setupActivity(SurveyActivity.class);
        ApkUpdateService service = Robolectric.setupService(ApkUpdateService.class);
        Context currentContext = activity.getApplicationContext();

        changeVersion(currentContext, "2.0.0");

        JSONObject jsonResponse = PlatformUtil.getResponseJson(currentContext);
        Intent intent = new Intent(activity, service.getClass());

        service.onHandleIntent(intent);
        ShadowIntentService serviceShadow = shadowOf(service);

        queryForActivity(currentContext, serviceShadow, jsonResponse.getString("version"));
    }

    private void queryForActivity(Context context, ShadowIntentService serviceShadow, String latestVersion)
    {
        if(PlatformUtil.isNewerVersion(getCurrentVersion(context), latestVersion))
        {
            while (true)
            {
                Intent nextActivity = serviceShadow.getNextStartedActivity();
                if(nextActivity.getComponent().getClassName().equals(AppUpdateActivity.class.getName()))
                {
                    //success
                    break;
                }
            }
        }
        else
        {
            fail("No update available!");
        }
    }
}

