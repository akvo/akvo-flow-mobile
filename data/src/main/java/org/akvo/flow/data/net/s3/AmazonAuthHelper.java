/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.net.s3;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import android.util.Base64;

import org.akvo.flow.data.entity.S3File;
import org.akvo.flow.data.net.S3User;
import org.akvo.flow.data.net.SignatureHelper;

import javax.inject.Inject;

public class AmazonAuthHelper {

    private final SignatureHelper signatureHelper;
    private final S3User s3User;

    @Inject
    public AmazonAuthHelper(SignatureHelper signatureHelper, S3User s3User) {
        this.signatureHelper = signatureHelper;
        this.s3User = s3User;
    }

    @NonNull
    public String getAmazonAuthForPut(String date, String payloadStr, S3File s3File) {
        final String payload = formatPayloadForPut(date, payloadStr, s3File);
        final String signature = signatureHelper
                .getAuthorization(payload, s3User.getSecret(), Base64.NO_WRAP);
        return "AWS " + s3User.getAccessKey() + ":" + signature;
    }

    @NonNull
    public String getAmazonAuthForGet(String date, String payloadStr, String filename) {
        final String payload = String
                .format(payloadStr, date, s3User.getBucket(), filename);
        return createAuthorization(payload);
    }

    @VisibleForTesting
    String formatPayloadForPut(String date, String payloadStr, S3File s3File) {
        return String.format(payloadStr, s3File.getMd5Base64(), s3File.getContentType(), date,
                s3User.getBucket(), s3File.getObjectKey());
    }

    @NonNull
    private String createAuthorization(String payload) {
        final String signature = signatureHelper
                .getAuthorization(payload, s3User.getSecret(), Base64.NO_WRAP);
        return "AWS " + s3User.getAccessKey() + ":" + signature;
    }
}
