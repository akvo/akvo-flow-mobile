/*
 *  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.async.loader;

import android.content.Context;
import android.util.Log;

import org.akvo.flow.api.FlowApi;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.async.loader.base.AsyncLoader;
import org.akvo.flow.domain.Instance;
import org.akvo.flow.exception.HttpException;

import java.io.IOException;

/**
 * Loader for fetching the Instance configuration from FLOW services.
 */
public class InstanceLoader extends AsyncLoader<Instance> {
    private static final String TAG = InstanceLoader.class.getSimpleName();
    private String mAppCode;

    public InstanceLoader(Context context, String appCode) {
        super(context);
        mAppCode = appCode;
    }

    @Override
    public Instance loadInBackground() {
        // Mock up akvoflowsandbox
        /*
        return new Instance("akvoflowsandbox", "akvoflowsandbox", "http://akvoflowsandbox.appspot.com",
                "akvoflowsandbox", "aaaa", "bbb", "ccc");
                */
        try {
            return new FlowApi(FlowApp.getApp().getInstance()).getInstance(mAppCode);
        } catch (IOException | HttpException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }
}
