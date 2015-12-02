package org.akvo.flow.serialization.response.value;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.akvo.flow.domain.response.value.Signature;

import java.io.IOException;

public class SignatureValue {
    private static final String TAG = SignatureValue.class.getSimpleName();

    public static String serialize(Signature signature) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        try {
            return mapper.writeValueAsString(signature);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return "";
    }

    public static Signature deserialize(String data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(data, Signature.class);
        } catch (IOException e) {
            Log.e(TAG, "Value is not a valid JSON response: " + data);
        }
        return new Signature();
    }

}
