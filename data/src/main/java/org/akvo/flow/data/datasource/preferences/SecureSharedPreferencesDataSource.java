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

package org.akvo.flow.data.datasource.preferences;

import android.text.TextUtils;
import android.util.Base64;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;

@Singleton
public class SecureSharedPreferencesDataSource {

    private static final String PREF_API_KEY = "pref_api_key";
    private static final String PREF_AWS_ACCESS_KEY_ID = "pref_aws_access_key_id";
    private static final String PREF_AWS_BUCKET = "pref_aws_bucket";
    private static final String PREF_AWS_SECRET_KEY = "pref_aws_secret_key";
    private static final String PREF_INSTANCE_URL = "pref_instance_url";
    private static final String PREF_SERVER_BASE = "pref_server_base";
    private static final String PREF_SIGNING_KEY = "pref_signing_key";

    private final SharedPreferencesDataSource sharedPreferencesDataSource;

    @Inject
    public SecureSharedPreferencesDataSource(
            SharedPreferencesDataSource sharedPreferencesDataSource) {
        this.sharedPreferencesDataSource = sharedPreferencesDataSource;
    }

    public Observable<String> getApiKey() {
        return getEncodedPreference(PREF_API_KEY);
    }

    public Observable<Boolean> saveApiKey(String apiKey) {
        return saveEncodedString(PREF_API_KEY, apiKey);
    }

    public Observable<String> getAwsAccessKey() {
        return getEncodedPreference(PREF_AWS_ACCESS_KEY_ID);
    }

    public Observable<Boolean> saveAwsAccessKey(String awsAccessKey) {
        return saveEncodedString(PREF_AWS_ACCESS_KEY_ID, awsAccessKey);
    }

    public Observable<String> getAwsBucket() {
        return getEncodedPreference(PREF_AWS_BUCKET);
    }

    public Observable<Boolean> saveAwsBucket(String awsBucket) {
        return saveEncodedString(PREF_AWS_BUCKET, awsBucket);
    }

    public Observable<String> getAwsSecretKey() {
        return getEncodedPreference(PREF_AWS_SECRET_KEY);
    }

    public Observable<Boolean> saveAwsSecretKey(String awsSecretKey) {
        return saveEncodedString(PREF_AWS_SECRET_KEY, awsSecretKey);
    }

    public Observable<String> getInstanceUrl() {
        return getEncodedPreference(PREF_INSTANCE_URL);
    }

    public Observable<Boolean> saveInstanceUrl(String instanceUrl) {
        return saveEncodedString(PREF_INSTANCE_URL, instanceUrl);
    }

    public Observable<String> getServerBase() {
        return getEncodedPreference(PREF_SERVER_BASE);
    }

    public Observable<Boolean> saveServerBase(String serverBase) {
        return saveEncodedString(PREF_SERVER_BASE, serverBase);
    }

    public Observable<String> getSigningKey() {
        return getEncodedPreference(PREF_SIGNING_KEY);
    }

    public Observable<Boolean> saveSigningKey(String signingKey) {
        return saveEncodedString(PREF_SIGNING_KEY, signingKey);
    }

    private Observable<Boolean> saveEncodedString(String key, String value) {
        if (!TextUtils.isEmpty(value)) {
            sharedPreferencesDataSource.setString(encode(key), encode(value));
        }
        return Observable.just(true);
    }

    private Observable<String> getEncodedPreference(String key) {
        String encodedValue = sharedPreferencesDataSource.getString(encode(key), "");
        if (TextUtils.isEmpty(encodedValue)) {
            return Observable.just("");
        }
        return Observable.just(decode(encodedValue));
    }

    private String encode(String input) {
        return Base64.encodeToString(input.getBytes(), Base64.DEFAULT);
    }

    private String decode(String input) {
        return new String(Base64.decode(input, Base64.DEFAULT));
    }
}
