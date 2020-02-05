/*
 * Copyright (C) 2017-2018 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.net;

import android.util.Base64;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.NonNull;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import static org.akvo.flow.data.util.ApiUrls.HMAC;
import static org.akvo.flow.data.util.ApiUrls.TIMESTAMP;

public class HmacInterceptor implements Interceptor {

    private final String key;
    private final SimpleDateFormat dateFormat;
    private final Encoder encoder;
    private final SignatureHelper signatureHelper;

    public HmacInterceptor(String key, SimpleDateFormat dateFormat, Encoder encoder,
            SignatureHelper signatureHelper) {
        this.dateFormat = dateFormat;
        this.key = key;
        this.encoder = encoder;
        this.signatureHelper = signatureHelper;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        HttpUrl url = request.url();
        String uriString = url.toString();
        int separator = uriString.indexOf('?') + 1;
        String query = uriString.substring(separator);
        String urlBeginning = uriString.substring(0, separator);

        query = appendQueryParam(query, TIMESTAMP, getTimestamp());
        String auth = signatureHelper.getAuthorization(query, key, Base64.DEFAULT);
        query = appendQueryParam(query, HMAC, auth);

        String reconstructedUrl = urlBeginning + query;
        request = request.newBuilder().url(HttpUrl.parse(reconstructedUrl)).build();
        return chain.proceed(request);
    }

    @NonNull
    private String appendQueryParam(String query, String name, String value) {
        return query + "&" + name + "=" + value;
    }

    private String getTimestamp() {
        return encoder.encodeParam(dateFormat.format(new Date()));
    }
}
