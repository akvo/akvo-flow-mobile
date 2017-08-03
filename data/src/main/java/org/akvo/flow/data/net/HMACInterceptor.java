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
import android.util.Base64;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

import static org.akvo.flow.data.util.Constants.HMAC;
import static org.akvo.flow.data.util.Constants.TIMESTAMP;

public class HMACInterceptor implements Interceptor {

    private static final String HMAC_SHA_1_ALGORITHM = "HmacSHA1";

    private final String key;
    private final SimpleDateFormat dateFormat;
    private final Encoder encoder;

    public HMACInterceptor(String key, SimpleDateFormat dateFormat, Encoder encoder) {
        this.dateFormat = dateFormat;
        this.key = key;
        this.encoder = encoder;
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
        String auth = getAuthorization(query, key);
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