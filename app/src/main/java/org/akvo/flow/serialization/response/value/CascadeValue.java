/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.serialization.response.value;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.akvo.flow.domain.response.value.CascadeNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class CascadeValue {

    public static String serialize(List<CascadeNode> values) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(values);
        } catch (IOException e) {
            Timber.e(e.getMessage());
        }
        return "";
    }

    public static List<CascadeNode> deserialize(String data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(data, new TypeReference<List<CascadeNode>>(){});
        } catch (IOException e) {
            Timber.e("Value is not a valid JSON response: " + data);
        }

        // Default to old format
        List<CascadeNode> values = new ArrayList<>();
        String[] tokens = data.split("\\|", -1);
        for (String token : tokens) {
            CascadeNode v = new CascadeNode();
            v.setName(token);
            values.add(v);
        }
        return values;
    }

    public static String getDatapointName(String value) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (CascadeNode cv : deserialize(value)) {
            if (!first) {
                builder.append(" - ");
            }
            builder.append(cv.getName());
            first = false;
        }

        return builder.toString();
    }
}
