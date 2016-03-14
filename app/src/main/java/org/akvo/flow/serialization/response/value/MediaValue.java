package org.akvo.flow.serialization.response.value;

import org.akvo.flow.domain.response.value.Media;


public class MediaValue {
    private static final String TAG = MediaValue.class.getSimpleName();

    public static String serialize(Media media) {
        String value = null;
        if (media != null) {
            value = media.getImage();
        }
        return value;
    }

    public static Media deserialize(String data) {
        Media media = new Media();
        media.setImage(data);
        return media;
    }
}
