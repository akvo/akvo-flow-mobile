/*
 * Copyright (C) 2018 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.akvo.flow.data.entity;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.inject.Inject;

import io.reactivex.annotations.NonNull;

public class FormHeaderParser {

    /**
     * This is empty most of the time but unfortunately the backend still returns the data in such
     * way that the first string is either empty or the device's phone
     */
    private static final int DEVICE = 0;
    private static final int ID = 1;
    private static final int NAME = 2;
    private static final int LANGUAGE = 3;
    private static final int VERSION = 4;

    // SurveyGroup information
    private static final int GROUP_ID = 5;
    private static final int GROUP_NAME = 6;
    private static final int GROUP_MONITORED = 7;
    private static final int GROUP_REGISTRATION_SURVEY = 8;

    private static final int COUNT = 9;// Length of column array

    @Inject
    public FormHeaderParser() {
    }

    public List<ApiFormHeader> parseMultiple(String response) {
        final List<ApiFormHeader> headers = new ArrayList<>();
        if (!TextUtils.isEmpty(response)) {
            if (!response.startsWith(",")) {
                response = "," + response;
            }
            final StringTokenizer strTok = new StringTokenizer(response, "\n");
            while (strTok.hasMoreTokens()) {
                String currentLine = strTok.nextToken();
                headers.add(parse(currentLine));
            }
        }
        return headers;
    }

    public ApiFormHeader parseOne(@NonNull String response) {
        if (response != null && !response.startsWith(",")) {
            response = "," + response;
        }
        return parse(response);
    }

    private ApiFormHeader parse(String response) {
        String[] tuple = response.split(",");
        if (tuple.length < COUNT) {
            throw new IllegalArgumentException(
                    "Wrong survey list format: " + response + ", expected at least " + COUNT
                            + " parts but found " + tuple.length);
        }
        final String id = tuple[ID];
        final String registrationId = tuple[GROUP_REGISTRATION_SURVEY];
        final String registrationSurveyId = TextUtils.isEmpty(registrationId) ? id : registrationId;
        return new ApiFormHeader(id, tuple[NAME], tuple[LANGUAGE], tuple[VERSION],
                Double.parseDouble(tuple[GROUP_ID]), tuple[GROUP_NAME],
                Boolean.parseBoolean(tuple[GROUP_MONITORED]), registrationSurveyId);
    }
}
