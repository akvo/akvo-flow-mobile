/*
 *  Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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

import android.content.Context;
import android.widget.Toast;
import java.lang.ref.WeakReference;

public class ServiceToastRunnable implements Runnable {

    private final WeakReference<Context> contextWeakReference;
    private final String msg;

    public ServiceToastRunnable(Context applicationContext, String msg) {
        this.contextWeakReference = new WeakReference<>(applicationContext);
        this.msg = msg;
    }

    @Override
    public void run() {
        Context context = contextWeakReference.get();
        if (context != null) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
    }
}
