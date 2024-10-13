/*
 *  Copyright (C) 2013-2020 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.app;

import androidx.annotation.VisibleForTesting;
import androidx.multidex.MultiDexApplication;

//import com.mapbox.mapboxsdk.Mapbox;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.R;
import org.akvo.flow.data.preference.Prefs;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.interactor.setup.SaveSetup;
import org.akvo.flow.domain.interactor.setup.SetUpParams;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerApplicationComponent;
import org.akvo.flow.injector.module.ApplicationModule;
import org.akvo.flow.service.ApkUpdateWorker;
import org.akvo.flow.service.FileChangeTrackingWorker;
import org.akvo.flow.util.logging.LoggingHelper;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

public class FlowApp extends MultiDexApplication {

    @Inject
    LoggingHelper loggingHelper;

    @Inject
    Prefs prefs;

    @Inject
    @Named("saveSetup")
    UseCase saveSetup;

    @VisibleForTesting
    public ApplicationComponent applicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();

//        Mapbox.getInstance(this, getString(R.string.mapbox_token));

        initializeInjector();
        initLogging();
        startUpdateService();
        startBootstrapFolderTracker();
        updateLoggingInfo();
        saveConfig();
    }

    private void saveConfig() {
        Map<String, Object> params = new HashMap<>(2);
        params.put(SaveSetup.PARAM_SETUP,
                new SetUpParams(BuildConfig.API_KEY, BuildConfig.AWS_ACCESS_KEY_ID,
                        BuildConfig.AWS_BUCKET, BuildConfig.AWS_SECRET_KEY,
                        BuildConfig.INSTANCE_URL, BuildConfig.SERVER_BASE,
                        BuildConfig.SIGNING_KEY));
        saveSetup.execute(new DefaultObserver<Boolean>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }

        }, params);
    }

    private void startBootstrapFolderTracker() {
        FileChangeTrackingWorker.scheduleVerifier(this);
    }

    private void startUpdateService() {
        ApkUpdateWorker.enqueueWork(getApplicationContext());
    }

    private void initializeInjector() {
        this.applicationComponent =
                DaggerApplicationComponent.builder().applicationModule(new ApplicationModule(this))
                        .build();
        this.applicationComponent.inject(this);
    }

    public ApplicationComponent getApplicationComponent() {
        return this.applicationComponent;
    }

    private void initLogging() {
        loggingHelper.init();
    }

    private void updateLoggingInfo() {
        String deviceId = prefs.getString(Prefs.KEY_DEVICE_IDENTIFIER, null);
        loggingHelper.initLoginData(deviceId);
    }
}
