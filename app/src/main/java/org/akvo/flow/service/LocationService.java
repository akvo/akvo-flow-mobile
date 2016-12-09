/*
 *  Copyright (C) 2010-2015 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.service;

import android.app.Service;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.akvo.flow.api.FlowApi;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.exception.PersistentUncaughtExceptionHandler;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.StatusUtil;

import java.util.Timer;
import java.util.TimerTask;

/**
 * service for sending location beacons on a set interval to the server. This
 * can be disabled via the properties menu
 *
 * @author Christopher Fagiani
 */
public class LocationService extends Service {
    @Nullable
    private static Timer timer;
    private LocationManager locMgr;
    private Criteria locationCriteria;
    private static final long INITIAL_DELAY = 60000;
    private static final long INTERVAL = 1800000;
    private static boolean sendBeacon = true;

    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * life cycle method for the service. This is called by the system when the
     * service is started. It will schedule a timerTask that will periodically
     * check the current location and send it to the server
     */
    public int onStartCommand(final Intent intent, int flags, int startid) {
        // we only need to check this on command start since we'll explicitly
        // call endService if they change the preference to false after we're
        // already started
        SurveyDbAdapter database = null;
        try {
            database = new SurveyDbAdapter(this);

            database.open();
            String val = database.getPreference(ConstantUtil.LOCATION_BEACON_SETTING_KEY);
            if (val != null) {
                sendBeacon = Boolean.parseBoolean(val);
            }
        } finally {
            if (database != null) {
                database.close();
            }
        }
        // Safe to lazy initialize the static field, since this method
        // will always be called in the Main Thread
        if (timer == null && sendBeacon) {
            timer = new Timer(true);
            timer.scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run() {
                    //TODO: refactor to not reference service but use a weak reference
                    if (sendBeacon && StatusUtil.hasDataConnection(LocationService.this)) {
                        String provider = locMgr.getBestProvider(locationCriteria, true);
                        if (provider != null) {
                            FlowApi flowApi = new FlowApi();
                            Location lastKnownLocation = locMgr.getLastKnownLocation(provider);
                            Double latitude = null;
                            Double longitude = null;
                            Float accuracy = null;
                            if (lastKnownLocation != null) {
                                latitude = lastKnownLocation.getLatitude();
                                longitude = lastKnownLocation.getLongitude();
                                accuracy = lastKnownLocation.getAccuracy();
                            }
                            flowApi.sendLocation(StatusUtil.getServerBase(LocationService.this),
                                    latitude, longitude, accuracy);
                        }
                    }
                }
            }, INITIAL_DELAY, INTERVAL);
        }
        return Service.START_STICKY;
    }

    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(PersistentUncaughtExceptionHandler.getInstance());
        locMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationCriteria = new Criteria();
        locationCriteria.setAccuracy(Criteria.NO_REQUIREMENT);
    }

    public void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
