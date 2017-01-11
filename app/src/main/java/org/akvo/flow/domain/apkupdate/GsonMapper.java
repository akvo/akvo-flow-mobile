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

package org.akvo.flow.domain.apkupdate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import timber.log.Timber;

public class GsonMapper {

    private static final String TAG = GsonMapper.class.getSimpleName();

    private final Gson mapper;

    public GsonMapper() {
        this.mapper = new GsonBuilder().create();
    }

    public <T> T read(final String content, final Class<T> type) throws JsonSyntaxException {
        try {
            return this.mapper.fromJson(content, type);
        } catch (JsonSyntaxException e) {
            Timber.e(e, "Error mapping json to class '" + type + "' with contents: '" + content + "'");
            throw e;
        }
    }

    public <T> T read(final String content, final Type type) throws JsonSyntaxException {
        try {
            return this.mapper.fromJson(content, type);
        } catch (JsonSyntaxException e) {
            Timber.e(e, "Error mapping json to class '" + type + "' with contents: '" + content + "'");
            throw e;
        }
    }

    public <T> T read(final InputStream content, final Class<T> type) throws JsonIOException, JsonSyntaxException {
        try {
            return this.mapper.fromJson(new InputStreamReader(content), type);
        } catch (JsonIOException | JsonSyntaxException e) {
            Timber.e(e, "Error mapping json to class '" + type + "' with contents: '" + content + "'");
            throw e;
        }
    }

    public <T> String write(final T content, final Class<T> type) {
        try {
            return this.mapper.toJson(content, type);
        } catch (JsonIOException | JsonSyntaxException e) {
            Timber.e(e, "Error mapping class '" + type + "' to json with contents: '" + content + "'");
            throw e;
        }
    }
}