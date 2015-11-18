package org.akvo.flow.serialization.response.value;

import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.akvo.flow.domain.response.value.CascadeNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CascadeValue {
    private static final String TAG = CascadeValue.class.getSimpleName();

    public static String serialize(List<CascadeNode> values) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(values);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return "";
    }

    public static List<CascadeNode> deserialize(String data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(data, new TypeReference<List<CascadeNode>>(){});
        } catch (IOException e) {
            Log.e(TAG, "Value is not a valid JSON response: " + data);
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
