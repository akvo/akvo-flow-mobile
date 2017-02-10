/*
 *  Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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
