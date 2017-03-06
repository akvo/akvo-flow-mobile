/*
 * Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
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

/**
 * Class to hold all public constants used in the application
 *
 * @author Christopher Fagiani
 */
public class ConstantUtil {
    /**
     * file system constants
     */
    public static final String FILE_SURVEY_LOCATION_TYPE = "file";
    public static final String ARCHIVE_SUFFIX = ".zip";
    public static final String JPG_SUFFIX = ".jpg";
    public static final String PNG_SUFFIX = ".png";
    public static final String VIDEO_SUFFIX = ".mp4";
    public static final String XML_SUFFIX = ".xml";
    public static final String BOOTSTRAP_DB_FILE = "dbinstructions.sql";
    public static final String PROCESSED_OK_SUFFIX = ".processed";
    public static final String PROCESSED_ERROR_SUFFIX = ".error";
    public static final String BOOTSTRAP_ROLLBACK_FILE = "rollback.sql";
    public static final String CASCADE_RES_SUFFIX = ".sqlite.zip";
    public static final String DOT_SEPARATOR = ".";

    /**
     * survey file locations
     */
    public static final String RESOURCE_LOCATION = "res";
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
    public static final String VIDEO_HELP_TYPE = "video";
    public static final String IMAGE_HELP_TYPE = "image";
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
    public static final String NAME_VALIDATION_TYPE = "name";

    /**
     * scoring types
     */
    public static final String NUMERIC_SCORING = "numeric";
    public static final String TEXT_MATCH_SCORING = "textmatch";

    /**
     * survey types
     */
    public static final String SURVEY_TYPE = "survey";

    /**
     * media question support
     */
    public static final String MEDIA_FILE_KEY = "filename";

    /**
     * Signature result data
     */
    public static final String SIGNATURE_IMAGE = "signature_image";

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
    public static final String USER_ID_KEY = "UID";
    public static final String SURVEY_ID_KEY = "SID";
    public static final String RESPONDENT_ID_KEY = "survey_respondent_id";
    public static final String READONLY_KEY = "readonly";
    public static final String SINGLE_SURVEY_KEY = "single_survey";
    public static final String SURVEY_GROUP = "survey_group";
    public static final String SURVEYED_LOCALE_ID = "surveyed_locale_id";

    /**
     * settings keys
     */
    public static final String SURVEY_LANG_SETTING_KEY = "survey.language";//user selected languages
    public static final String SURVEY_LANG_PRESENT_KEY = "survey.languagespresent";
    public static final String CELL_UPLOAD_SETTING_KEY = "data.cellular.upload";
    public static final String SCREEN_ON_KEY = "screen.keepon";
    public static final String DEVICE_IDENT_KEY = "device.identifier";
    public static final String MAX_IMG_SIZE = "media.img.maxsize";

    /**
     * intents
     */
    public static final String DATA_AVAILABLE_INTENT = "org.akvo.flow.DATA_SUBMITTED";
    public static final String GPS_STATUS_INTENT = "com.eclipsim.gpsstatus.VIEW";
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
     * "code" to prevent unauthorized use of administrative settings/preferences
     */
    public static final String ADMIN_AUTH_CODE = "12345";

    /**
     * property file keys
     */
    public static final String SERVER_BASE = "serverBase";
    public static final String S3_BUCKET = "awsBucket";
    public static final String S3_ACCESSKEY = "awsAccessKeyId";
    public static final String S3_SECRET = "awsSecretKey";

    /**
     * S3 bucket directories (object prefixes)
     */
    public static final String S3_DATA_DIR = "devicezip/";
    public static final String S3_IMAGE_DIR = "images/";
    public static final String S3_SURVEYS_DIR = "surveys/";

    /**
     * resource related constants
     */
    public static final String RESOURCE_PACKAGE = "org.akvo.flow";
    public static final String RAW_RESOURCE = "raw";

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

    /**
     * Max picture size
     * Values must match the ones set in arrays.
     * TODO: Preferences should be managed with SharedPreferences api, to avoid this error prone references
     */
    public static final int IMAGE_SIZE_320_240 = 0;
    public static final int IMAGE_SIZE_640_480 = 1;
    public static final int IMAGE_SIZE_1280_960 = 2;

    public static final int NOTIFICATION_FORMS_SYNCED = 102;
    public static final int NOTIFICATION_ASSIGNMENT_ERROR = 103;
    public static final int NOTIFICATION_HEADER_ERROR = 104;
    public static final int NOTIFICATION_FORM_ERROR = 105;
    public static final int NOTIFICATION_RESOURCE_ERROR = 105;

    public static final int NOTIFICATION_BOOTSTRAP = 106;

    /**
     * Caddisfly serialization settings
     */
    public static final String EXTERNAL_SOURCE_ACTION = "org.akvo.flow.action.externalsource";
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

    //broadcasts
    public static final String ACTION_DATA_SYNC = "fieldsurvey.ACTION_DATA_SYNC";
    public static final String ACTION_SURVEY_SYNC = "fieldsurvey.ACTION_SURVEYS_SYNC";

    //apk update
    public static final int REPEAT_INTERVAL_IN_SECONDS = 1 * 60 * 60 * 24; //every 24Hrs
    public static final int FLEX_INTERVAL_IN_SECONDS = 1 * 60 * 60; //1 hour

    //first runs will be faster
    public static final int FIRST_REPEAT_INTERVAL_IN_SECONDS = 1 * 60;
    public static final int FIRST_FLEX_INTERVAL_IN_SECOND = 30;

    /**
     * 7 days
     */
    public static final int UPDATE_NOTIFICATION_DELAY_IN_MS = 7 * 60 * 60 * 24 * 1000;

    //requests
    public static final int REQUEST_ADD_USER = 0;
    public static final int PHOTO_ACTIVITY_REQUEST = 1;
    public static final int VIDEO_ACTIVITY_REQUEST = 2;
    public static final int SCAN_ACTIVITY_REQUEST = 3;
    public static final int EXTERNAL_SOURCE_REQUEST = 4;
    public static final int CADDISFLY_REQUEST = 5;
    public static final int PLOTTING_REQUEST = 6;
    public static final int SIGNATURE_REQUEST = 7;

    //extras
    public static final String EXTRA_RECORD_ID = "record_id";
    public static final String EXTRA_SURVEY_GROUP = "survey_group";

    /**
     * prevent instantiation
     */
    private ConstantUtil() {
    }

}
