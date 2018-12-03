/*
 *  Copyright (C) 2010-2014,2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;

import org.akvo.flow.service.DataSyncService;
import org.akvo.flow.util.ConstantUtil;

/**
 * this class will listen to any Broadcast messages fired by the system and will
 * react to anything it knows how to handle. The intent filters need to be set
 * up correctly in the application manifest
 * 
 * @author Christopher Fagiani
 */
public class SyncDataReceiver extends BroadcastReceiver {

    public static final String CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";

    public void onReceive(Context context, Intent intent) {
        if (isIntentActionExpected(intent.getAction())) {
//            context.startService(new Intent(context, DataSyncService.class));
            ContextCompat.startForegroundService(context, new Intent(context, DataSyncService.class));
        }
    }

    /**
     * Make sure the intent action is not spoofed
     */
    private boolean isIntentActionExpected(String action) {
        return ConstantUtil.DATA_AVAILABLE_INTENT.equals(action)
                || CONNECTIVITY_ACTION.equals(action);
    }
}
