/*
 * Copyright (C) 2010-2018 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.akvo.flow.serialization.response.value;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.akvo.flow.domain.response.value.CascadeNode;
import org.akvo.flow.domain.util.GsonMapper;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class CascadeValue {

    public static String serialize(List<CascadeNode> values) {
        GsonMapper mapper = new GsonMapper(new GsonBuilder().create());
        try {
            return mapper.write(values);
        } catch (JsonIOException | JsonSyntaxException e) {
            Timber.e(e.getMessage());
        }
        return "";
    }

    public static List<CascadeNode> deserialize(String data) {
        try {
            Type listType = new TypeToken<ArrayList<CascadeNode>>(){}.getType();
            GsonMapper mapper = new GsonMapper(new GsonBuilder().create());
            return mapper.read(data, listType);
        } catch (JsonSyntaxException e) {
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
        List<CascadeNode> cascadeNodes = deserialize(value);
        for (CascadeNode cv : cascadeNodes) {
            if (!first) {
                builder.append(" - ");
            }
            builder.append(cv.getName());
            first = false;
        }

        return builder.toString();
    }
}
