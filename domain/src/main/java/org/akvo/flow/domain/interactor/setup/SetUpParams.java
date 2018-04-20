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

package org.akvo.flow.domain.interactor.setup;

public class SetUpParams {

    private final String apiKey;
    private final String awsAccessKeyId;
    private final String awsBucket;
    private final String awsSecretKey;
    private final String instanceUrl;
    private final String serverBase ;
    private final String signingKey;

    public SetUpParams(String apiKey, String awsAccessKeyId, String awsBucket,
            String awsSecretKey, String instanceUrl, String serverBase, String signingKey) {
        this.apiKey = apiKey;
        this.awsAccessKeyId = awsAccessKeyId;
        this.awsBucket = awsBucket;
        this.awsSecretKey = awsSecretKey;
        this.instanceUrl = instanceUrl;
        this.serverBase = serverBase;
        this.signingKey = signingKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getAwsAccessKeyId() {
        return awsAccessKeyId;
    }

    public String getAwsBucket() {
        return awsBucket;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public String getServerBase() {
        return serverBase;
    }

    public String getSigningKey() {
        return signingKey;
    }
}
