/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

import static org.akvo.flow.data.net.FlowRestApi.CHARSET_UTF8;
import static org.akvo.flow.data.net.FlowRestApi.HMAC_SHA_1_ALGORITHM;
import static org.akvo.flow.data.net.FlowRestApi.Param.HMAC;
import static org.akvo.flow.data.net.FlowRestApi.Param.TIMESTAMP;

public class HMACInterceptor implements Interceptor {

    private final String key;

    public HMACInterceptor(String key) {
        this.key = key;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        HttpUrl url = request.url();
        String uriString = url.toString();
        Timber.i("Intercepted request %s on %s%n%s", uriString, chain.connection(),
                request.headers());
        String query = url.query();
        String urlBeginning = uriString.replace(query, "");

        query = appendNonEncodedQueryParam(query, TIMESTAMP, getTimestamp());
        String auth = getAuthorization(query, key);
        query = appendNonEncodedQueryParam(query, HMAC, auth);

        request = request.newBuilder().url(urlBeginning + query).build();
        Timber.i("Authorised url:: %s", request.url().toString());
        Response response = chain.proceed(request);
        return response;
    }

    @NonNull
    private String appendNonEncodedQueryParam(String query, String name, String value) {
        return query + "&" + name + "=" + value;
    }

    private String getTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return encodeParam(dateFormat.format(new Date()));
    }

    private String encodeParam(@Nullable String param) {
        if (TextUtils.isEmpty(param)) {
            return "";
        }
        try {
            return URLEncoder.encode(param, CHARSET_UTF8);
        } catch (UnsupportedEncodingException e) {
            Timber.e(e.getMessage());
            return "";
        }
    }

    @Nullable
    private String getAuthorization(@NonNull String query, String apiKey) {
        String authorization = null;
        try {
            SecretKeySpec signingKey = new SecretKeySpec(apiKey.getBytes(), HMAC_SHA_1_ALGORITHM);

            Mac mac = Mac.getInstance(HMAC_SHA_1_ALGORITHM);
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(query.getBytes());

            authorization = Base64.encodeToString(rawHmac, Base64.DEFAULT);
        } catch (@NonNull NoSuchAlgorithmException | InvalidKeyException e) {
            Timber.e(e.getMessage());
        }

        return authorization;
    }
}