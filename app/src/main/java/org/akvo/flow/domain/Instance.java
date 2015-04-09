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

package org.akvo.flow.domain;

import java.io.Serializable;

public class Instance implements Serializable {
    private String mName;
    private String mAlias;
    private String mServerBase;
    private String mAWSBucket;
    private String mAWSAccessKeyId;
    private String mAWSSecretKey;
    private String mApiKey;

    public Instance(String name, String alias, String serverBase, String awsBucket,
            String awsAccessKeyId, String awsSecretKey, String apiKey) {
        mName = name;
        mAlias = alias;
        mServerBase = serverBase;
        mAWSBucket = awsBucket;
        mAWSAccessKeyId = awsAccessKeyId;
        mAWSSecretKey = awsSecretKey;
        mApiKey = apiKey;
    }

    public String getName() {
        return mName;
    }

    public String getAlias() {
        return mAlias;
    }

    public String getServerBase() {
        return mServerBase;
    }

    public String getAWSBucket() {
        return mAWSBucket;
    }

    public String getAWSAccessKeyId() {
        return mAWSAccessKeyId;
    }

    public String getAWSSecretKey() {
        return mAWSSecretKey;
    }

    public String getApiKey() {
        return mApiKey;
    }

    @Override
    public String toString() {
        return mName;
    }

    @Override
    public boolean equals(Object instance) {
        return instance != null &&
                instance instanceof Instance &&
                ((Instance) instance).getName().equals(mName);
    }
}
