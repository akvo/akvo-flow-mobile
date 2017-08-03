/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.data.util;

public class Constants {

    public static final String SURVEYED_LOCALE = "/surveyedlocale";
    public static final String SURVEY_GROUP = "surveyGroupId";
    public static final String PHONE_NUMBER = "phoneNumber";
    public static final String IMEI = "imei";
    public static final String TIMESTAMP = "ts";
    public static final String LAST_UPDATED = "lastUpdateTime";
    public static final String HMAC = "h";
    public static final String VERSION = "ver";
    public static final String ANDROID_ID = "androidId";
    public static final String APK_VERSION_SERVICE_PATH =
            "/deviceapprest?action=getLatestVersion&deviceType=androidPhone&appCode=flowapp";

    public static final int IMAGE_SIZE_320_240 = 0;
    public static final int IMAGE_SIZE_640_480 = 1;
    public static final int IMAGE_SIZE_1280_960 = 2;
}
