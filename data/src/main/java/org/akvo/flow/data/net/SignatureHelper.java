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

package org.akvo.flow.data.net;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Base64;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

import timber.log.Timber;

public class SignatureHelper {

    private static final String HMAC_SHA_1_ALGORITHM = "HmacSHA1";

    @Inject
    public SignatureHelper() {
    }

    @Nullable
    public String getAuthorization(@NonNull String input, String key, int flag) {
        String authorization = null;
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(),
                    HMAC_SHA_1_ALGORITHM);

            Mac mac = Mac.getInstance(HMAC_SHA_1_ALGORITHM);
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(input.getBytes());

            authorization = Base64.encodeToString(rawHmac, flag);
        } catch (@NonNull NoSuchAlgorithmException | InvalidKeyException e) {
            Timber.e(e.getMessage());
        }

        return authorization;
    }
}
