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

package org.akvo.flow.api;

import android.content.Context;
import android.util.Log;

import org.akvo.flow.R;
import org.akvo.flow.api.parser.json.InstanceParser;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.domain.Instance;
import org.akvo.flow.exception.HttpException;
import org.akvo.flow.exception.HttpException.Status;
import org.akvo.flow.util.HttpUtil;
import org.json.JSONException;

import java.io.IOException;

public class FlowServices {
    private static final String TAG = FlowServices.class.getSimpleName();

    private static final String FLOW_SERVICES_URL;

    static {
        Context context = FlowApp.getApp();
        FLOW_SERVICES_URL = context.getResources().getString(R.string.flowServicesUrl);
    }

    public Instance getInstance(String appcode) throws IOException, HttpException {
        // TODO: validate appcode using the control digit
        final String url = FLOW_SERVICES_URL + String.format(Path.APP_CONFIG, appcode);

        String response = HttpUtil.httpGet(url);
        try {
            return new InstanceParser().parseResponse(response);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            throw new HttpException("Invalid JSON response", Status.MALFORMED_RESPONSE);
        }
    }

    interface Path {
        String APP_CONFIG = "/appcode/appconfig/%s";
    }
    
}
