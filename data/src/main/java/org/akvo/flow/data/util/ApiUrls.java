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

package org.akvo.flow.data.util;

public class ApiUrls {
    public static final String DATA_POINTS = "/datapoints";
    public static final String DEVICE_NOTIFICATION = "/devicenotification";
    public static final String PROCESSING_NOTIFICATION = "processor";
    public static final String S3_FILE_PATH = "/{key}/{file}";
    public static final String PHONE_NUMBER = "phoneNumber";
    public static final String IMEI = "imei";
    public static final String TIMESTAMP = "ts";
    public static final String HMAC = "h";
    public static final String VERSION = "ver";
    public static final String ANDROID_ID = "androidId";
    public static final String DEVICE_ID = "devId";
    public static final String FORM_IDS = "formID[]";
    public static final String ACTION = "action";
    public static final String FORM_ID = "formID";
    public static final String FILENAME = "fileName";
    public static final String APK_VERSION_SERVICE_PATH =
            "/deviceapprest?action=getLatestVersion&deviceType=androidPhone&appCode=flowapp";
    public static final String SURVEY_ID = "surveyId";
    public static final String ANDROID_BUILD_VERSION = "androidBuildVersion";

    public static final String FORM_HEADER_PATH = "/surveymanager?action=getSurveyHeader";
    public static final String FORMS_HEADER_PATH = "/surveymanager?action=getAvailableSurveysDevice";
}
