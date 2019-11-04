/*
 *  Copyright (C) 2016-2019 Stichting Akvo (Akvo Foundation)
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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import org.akvo.flow.domain.response.value.Media;
import org.akvo.flow.domain.util.GsonMapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

public class MediaValue {

    @NonNull
    public static String serialize(Media media, boolean serializeNulls) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        if (serializeNulls) {
            gsonBuilder.serializeNulls();
        }
        GsonMapper mapper = new GsonMapper(gsonBuilder.create());
        try {
            return mapper.write(media, Media.class);
        } catch (JsonIOException | JsonSyntaxException e) {
            Timber.e(e.getMessage());
        }
        return "";
    }

    @Nullable
    public static Media deserialize(String data) {
        if (TextUtils.isEmpty(data)) {
            return null;
        }
        try {
            GsonMapper mapper = new GsonMapper(new GsonBuilder().create());
            return mapper.read(data, Media.class);
        } catch (JsonIOException | JsonSyntaxException e) {
            Timber.e("Value is not a valid JSON response: " + data);
        }

        // Assume old format - plain image
        Media media = new Media();
        media.setFilename(data);
        return media;
    }
}
