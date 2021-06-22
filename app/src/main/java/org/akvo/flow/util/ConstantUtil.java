/*
 * Copyright (C) 2010-2020 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.util;

import org.akvo.flow.R;

/**
 * Class to hold all public constants used in the application
 *
 * @author Christopher Fagiani
 */
public class ConstantUtil {
    /**
     * file system constants
     */
    public static final String ARCHIVE_SUFFIX = ".zip";
    public static final String XML_SUFFIX = ".xml";
    public static final String PROCESSED_OK_SUFFIX = ".processed";
    public static final String PROCESSED_ERROR_SUFFIX = ".error";
    public static final String CASCADE_RES_SUFFIX = ".sqlite.zip";
    public static final String DOT_SEPARATOR = ".";

    public static final String FILE_LOCATION = "sdcard";

    /**
     * size limits
     */
    public static final String SPACE_WARNING_MB_LEVELS = "100, 50, 25, 10, 5, 1";

    /**
     * question types
     */
    public static final String FREE_QUESTION_TYPE = "free";
    public static final String OPTION_QUESTION_TYPE = "option";
    public static final String GEO_QUESTION_TYPE = "geo";
    public static final String VIDEO_QUESTION_TYPE = "video";
    public static final String PHOTO_QUESTION_TYPE = "photo";
    public static final String SCAN_QUESTION_TYPE = "scan";
    public static final String STRENGTH_QUESTION_TYPE = "strength";
    public static final String DATE_QUESTION_TYPE = "date";
    public static final String CASCADE_QUESTION_TYPE = "cascade";
    public static final String GEOSHAPE_QUESTION_TYPE = "geoshape";
    public static final String SIGNATURE_QUESTION_TYPE = "signature";
    public static final String CADDISFLY_QUESTION_TYPE = "caddisfly";

    /**
     * help types
     */
    public static final String TIP_HELP_TYPE = "tip";

    /**
     * response types
     */
    public static final String VALUE_RESPONSE_TYPE = "VALUE";
    public static final String IMAGE_RESPONSE_TYPE = "IMAGE";
    public static final String VIDEO_RESPONSE_TYPE = "VIDEO";
    public static final String GEO_RESPONSE_TYPE = "GEO";
    public static final String DATE_RESPONSE_TYPE = "DATE";
    public static final String CASCADE_RESPONSE_TYPE = "CASCADE";
    public static final String OPTION_RESPONSE_TYPE = "OPTION";
    public static final String SIGNATURE_RESPONSE_TYPE = "SIGNATURE";
    public static final String CADDISFLY_RESPONSE_TYPE = "CADDISFLY";

    /**
     * validation types
     */
    public static final String NUMERIC_VALIDATION_TYPE = "numeric";

    /**
     * survey types
     */
    public static final String SURVEY_TYPE = "survey";

    /**
     * media question support
     */
    public static final String IMAGE_FILE_KEY = "image_path";
    public static final String VIDEO_FILE_KEY = "video_path";
    public static final String PARAM_REMOVE_ORIGINAL = "remove_original";

    public static final String SIGNATURE_NAME_EXTRA = "signature_name";
    public static final String SIGNATURE_QUESTION_ID_EXTRA = "signature_question_id";
    public static final String SIGNATURE_DATAPOINT_ID_EXTRA = "signature_datapoint_id";

    /**
     * Plot measurement result data
     */
    public static final String GEOSHAPE_RESULT = "geoshapeResult";
    public static final String EXTRA_ALLOW_POINTS = "allowPoints";
    public static final String EXTRA_ALLOW_LINE = "allowLine";
    public static final String EXTRA_ALLOW_POLYGON = "allowPolygon";
    public static final String EXTRA_MANUAL_INPUT = "manualInput";

    /**
     * keys for saved state and bundle extras
     */
    public static final String FORM_ID_EXTRA = "SID";
    public static final String RESPONDENT_ID_EXTRA = "survey_respondent_id";
    public static final String READ_ONLY_EXTRA = "readonly";
    public static final String SURVEY_EXTRA = "survey_group";
    public static final String DATA_POINT_ID_EXTRA = "datapoint_id";
    public static final String SURVEY_ID_EXTRA = "survey_group_id";
    public static final String IMAGE_URL_EXTRA = "image_url";
    public static final String FORM_TITLE_EXTRA = "title";
    public static final String FORM_SUBTITLE_EXTRA = "subtitle";
    public static final String QUESTION_ID_EXTRA = "question_id";
    public static final String VIEW_USER_EXTRA = "view_user";
    public static final String REQUEST_QUESTION_ID_EXTRA = "request_question_id";

    /**
     * intents
     */
    public static final String BOOTSTRAP_INTENT = "org.akvo.flow.BOOTSTRAP_NEEDED";
    public static final String GPS_STATUS_PACKAGE_V2 = "com.eclipsim.gpsstatus2";
    public static final String GPS_STATUS_PACKAGE_V1 = "com.eclipsim.gpsstatus";
    public static final String BARCODE_SCAN_INTENT = "com.google.zxing.client.android.SCAN";

    /**
     * zxing barcode extra keys
     */
    public static final String BARCODE_CONTENT = "SCAN_RESULT";

    /**
     * language codes
     */
    public static final String ENGLISH_CODE = "en";

    /**
     * SurveyedLocale meta question IDs. Negative IDs to avoid collisions.
     * Irrelevant for the server side, they are used to identify a locale meta-data
     * response among the rest of the 'real' question answers
     */
    public static final String QUESTION_LOCALE_NAME = "-1";
    public static final String QUESTION_LOCALE_GEO = "-2";

    /**
     * Order By
     */
    public static final int ORDER_BY_DATE = 0;
    public static final int ORDER_BY_DISTANCE = 1;
    public static final int ORDER_BY_STATUS = 2;
    public static final int ORDER_BY_NAME = 3;

    public static final int NOTIFICATION_FORM = 106;

    public static final int NOTIFICATION_BOOTSTRAP = 106;
    public static final int NOTIFICATION_TIME = 107;

    public static final String NOTIFICATION_CHANNEL_ID = "1";
    public static final String NOTIFICATION_CHANNEL_TIME = "2";

    public static final int UN_PUBLISH_NOTIFICATION_ID = 1235;

    /**
     * Caddisfly serialization settings
     */
    public static final String CADDISFLY_ACTION = "org.akvo.flow.action.caddisfly";
    public static final String CADDISFLY_RESOURCE_ID = "caddisflyResourceUuid";
    public static final String CADDISFLY_QUESTION_ID = "questionId";
    public static final String CADDISFLY_QUESTION_TITLE = "questionTitle";
    public static final String CADDISFLY_FORM_ID = "formId";
    public static final String CADDISFLY_DATAPOINT_ID = "datapointId";
    public static final String CADDISFLY_LANGUAGE = "language";
    public static final String CADDISFLY_RESPONSE = "response";
    public static final String CADDISFLY_IMAGE = "image";
    public static final String CADDISFLY_MIME = "text/plain";
    public static final String CADDISFLY_INSTANCE_NAME = "instanceName";

    //broadcasts
    public static final String ACTION_DATA_SYNC = "fieldsurvey.ACTION_DATA_SYNC";

    /**
     * 7 days
     */
    public static final int UPDATE_NOTIFICATION_DELAY_IN_MS = 7 * 60 * 60 * 24 * 1000;

    //requests
    public static final int PHOTO_ACTIVITY_REQUEST = 1;
    public static final int VIDEO_ACTIVITY_REQUEST = 2;
    public static final int SCAN_ACTIVITY_REQUEST = 3;
    public static final int CADDISFLY_REQUEST = 5;
    public static final int PLOTTING_REQUEST = 6;
    public static final int SIGNATURE_REQUEST = 7;
    public static final int FORM_FILLING_REQUEST = 8;
    public static final int GET_PHOTO_ACTIVITY_REQUEST = 9;
    public static final int GET_VIDEO_ACTIVITY_REQUEST = 10;


    //view tags
    public static final int SURVEY_ID_TAG_KEY = R.integer.surveyidkey;
    public static final int RESPONDENT_ID_TAG_KEY = R.integer.respidkey;
    public static final int READ_ONLY_TAG_KEY = R.integer.finishedkey;

    public static final String FILE_PROVIDER_AUTHORITY = "org.akvo.flow.fileprovider";

    public static final int LOCATION_PERMISSION_CODE = 1;
    public static final int STORAGE_PERMISSION_CODE = 2;
    public static final int STORAGE_AND_PHONE_STATE_PERMISSION_CODE = 3;

    public static final String LANGUAGES_EXTRA = "languages";

    /**
     * prevent instantiation
     */
    private ConstantUtil() {
    }

}
