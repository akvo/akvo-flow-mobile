/*
 *  Copyright (C) 2013-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.api;

import android.net.Uri;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.util.HttpUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import timber.log.Timber;

public class FlowApi {

    private static final String HTTPS_PREFIX = "https";
    private static final String HTTP_PREFIX = "http";

    private final String baseUrl;

    public FlowApi() {
        this.baseUrl = BuildConfig.SERVER_BASE;
    }

    public String getServerTime() throws IOException {
        String serverBase = baseUrl;
        if (serverBase.startsWith(HTTPS_PREFIX)) {
            serverBase = HTTP_PREFIX + serverBase.substring(HTTPS_PREFIX.length());
        }
        final String url = buildServerTimeUrl(serverBase);
        String response = HttpUtil.httpGet(url);
        String time = "";
        if (!TextUtils.isEmpty(response)) {
            JSONObject json;
            try {
                json = new JSONObject(response);
                time = json.getString("time");
            } catch (JSONException e1) {
                Timber.e(e1, "Error fetching time");
            }
        }
        return time;
    }

    @NonNull
    private String buildServerTimeUrl(@NonNull String serverBase) {
        Uri.Builder builder = Uri.parse(serverBase).buildUpon();
        builder.appendPath(Path.TIME_CHECK);
        builder.appendQueryParameter(Param.TIMESTAMP, System.currentTimeMillis() + "");
        return builder.build().toString();
    }

    interface Path {
        String TIME_CHECK = "devicetimerest";
    }

    interface Param {

        String TIMESTAMP = "ts";
    }
}
