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

package org.akvo.flow.api.parser.json;

import org.akvo.flow.domain.Instance;
import org.json.JSONException;
import org.json.JSONObject;

public class InstanceParser {
    private static final String TAG = InstanceParser.class.getSimpleName();

    public Instance parseResponse(String response) throws JSONException {
        JSONObject jInstance = new JSONObject(response);
        return parseInstance(jInstance);
    }

    public Instance parseInstance(JSONObject jInstance) throws JSONException {
        return new Instance(
                jInstance.getString(Attrs.NAME),
                jInstance.getString(Attrs.ALIAS),
                jInstance.getString(Attrs.SERVER_BASE),
                jInstance.getString(Attrs.AWS_BUCKET),
                jInstance.getString(Attrs.AWS_ACCESS_KEY_ID),
                jInstance.getString(Attrs.AWS_SECRET_KEY),
                jInstance.getString(Attrs.API_KEY)

        );
    }
    
    interface Attrs {
        // TODO: Validate names
        String NAME              = "appId";// Unique ID
        String ALIAS             = "alias";// Friendly name
        String AWS_BUCKET        = "s3bucket";
        String AWS_ACCESS_KEY_ID = "access-key";
        String AWS_SECRET_KEY    = "secret-key";
        String SERVER_BASE       = "serverBase";
        String API_KEY           = "apiKey";
    }

}
