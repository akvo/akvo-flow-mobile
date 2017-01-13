/*
 * Copyright (C) 2010-2016 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.serialization.response.value;

import android.text.TextUtils;

import org.akvo.flow.domain.Option;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class OptionValue {

    public static String serialize(List<Option> values) {
        try {
            JSONArray jOptions = new JSONArray();
            for (Option option : values) {
                JSONObject jOption = new JSONObject();
                jOption.put(Attrs.TEXT, option.getText());
                if (!TextUtils.isEmpty(option.getCode())) {
                    jOption.put(Attrs.CODE, option.getCode());
                }
                if (option.isOther()) {
                    jOption.put(Attrs.IS_OTHER, true);
                }
                jOptions.put(jOption);
            }
            return jOptions.toString();
        } catch (JSONException e) {
            Timber.e(e.getMessage());
        }
        return "";
    }

    public static List<Option> deserialize(String data) {
        try {
            List<Option> options = new ArrayList<>();
            JSONArray jOptions = new JSONArray(data);
            for (int i=0; i<jOptions.length(); i++) {
                JSONObject jOption = jOptions.getJSONObject(i);
                Option option = new Option();
                option.setText(jOption.optString(Attrs.TEXT));
                option.setCode(jOption.optString(Attrs.CODE, null));
                option.setIsOther(jOption.optBoolean(Attrs.IS_OTHER));
                options.add(option);
            }
            return options;
        } catch (JSONException e) {
            Timber.e(e.getMessage());
        }

        // Default to old format
        List<Option> options = new ArrayList<>();
        String[] tokens = data.split("\\|", -1);
        for (String token : tokens) {
            Option o = new Option();
            o.setText(token);
            options.add(o);
        }
        return options;
    }

    public static String getDatapointName(String value) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Option o : deserialize(value)) {
            if (!first) {
                builder.append(" - ");
            }
            builder.append(o.getText());
            first = false;
        }

        return builder.toString();
    }

    interface Attrs {
        String CODE = "code";
        String TEXT = "text";
        String IS_OTHER = "isOther";
    }
}
