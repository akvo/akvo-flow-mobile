/*
 *  Copyright (C) 2015,2017 Stichting Akvo (Akvo Foundation)
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

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import org.akvo.flow.domain.apkupdate.GsonMapper;
import org.akvo.flow.domain.response.value.Signature;

import timber.log.Timber;

public class SignatureValue {

    public static String serialize(Signature signature) {
        GsonMapper mapper = new GsonMapper();
//        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
//        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        try {
            return mapper.write(signature, Signature.class);
        } catch (JsonIOException | JsonSyntaxException e) {
            Timber.e(e.getMessage());
        }
        return "";
    }

    public static Signature deserialize(String data) {
        try {
            GsonMapper mapper = new GsonMapper();
            return mapper.read(data, Signature.class);
        } catch (JsonSyntaxException e) {
            Timber.e("Value is not a valid JSON response: %s", data);
        }
        return new Signature();
    }
}
