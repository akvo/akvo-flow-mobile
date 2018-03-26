/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.akvo.flow.broadcast.BootReceiver;

import javax.inject.Inject;

public class BootReceiverHelper {

    private final Context context;

    @Inject
    public BootReceiverHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public void enableBootReceiver() {
        setComponentState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    public void disableBootReceiver() {
        setComponentState(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    private void setComponentState(int componentState) {
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver, componentState, PackageManager.DONT_KILL_APP);
    }
}
