package org.akvo.flow.serialization.response.value;

import android.text.TextUtils;
import android.util.Log;

import org.akvo.flow.domain.Option;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OptionValue {
    private static final String TAG = OptionValue.class.getSimpleName();

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
            Log.e(TAG, e.getMessage());
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
                option.setCode(jOption.optString(Attrs.CODE));
                option.setIsOther(jOption.optBoolean(Attrs.IS_OTHER));
                options.add(option);
            }
            return options;
        } catch (JSONException e) {
            // TODO: Backwards compatibility; Pipe-separated responses
            Log.e(TAG, e.getMessage());
        }
        return new ArrayList<>();
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
